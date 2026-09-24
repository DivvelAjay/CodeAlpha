import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

// ================= RoomType =================
enum RoomType {
    STANDARD(1500),
    DELUXE(2500),
    SUITE(4000);

    final double pricePerNight;

    RoomType(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }
}

// ================= Room =================
class Room {
    int roomNumber;
    RoomType type;
    boolean available;

    Room(int roomNumber, RoomType type) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.available = true;
    }

    @Override
    public String toString() {
        return String.format("Room %-4d | %-8s | $%.2f/night | %s",
                roomNumber, type, type.pricePerNight, available ? "Available" : "Booked");
    }
}

// ================= Guest =================
class Guest {
    String name;
    String contact;

    Guest(String name, String contact) {
        this.name = name;
        this.contact = contact;
    }
}

// ================= Payment (simulation) =================
class Payment {
    static boolean processPayment(double amount, String cardNumberMasked) {
        // Simulated payment - always succeeds in this mock
        System.out.printf("Processing payment of $%.2f on card ending %s...%n",
                amount, cardNumberMasked.substring(Math.max(0, cardNumberMasked.length() - 4)));
        System.out.println("Payment SUCCESSFUL.");
        return true;
    }
}

// ================= Reservation =================
class Reservation {
    static int counter = 1000;

    int reservationId;
    Guest guest;
    Room room;
    LocalDate checkIn;
    LocalDate checkOut;
    double totalCost;
    String status; // BOOKED or CANCELLED

    Reservation(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        this.reservationId = ++counter;
        this.guest = guest;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        double cost = nights * room.type.pricePerNight;
        if (nights > 5) cost *= 0.90; // 10% discount for long stays
        this.totalCost = cost;
        this.status = "BOOKED";
    }

    void printReceipt() {
        System.out.println("---------- Booking Receipt ----------");
        System.out.println("Reservation ID: " + reservationId);
        System.out.println("Guest: " + guest.name + " (" + guest.contact + ")");
        System.out.println("Room: " + room.roomNumber + " - " + room.type);
        System.out.println("Check-in: " + checkIn + "   Check-out: " + checkOut);
        System.out.printf("Total Cost: $%.2f%n", totalCost);
        System.out.println("Status: " + status);
        System.out.println("--------------------------------------");
    }
}

// ================= Hotel =================
class Hotel {
    List<Room> rooms = new ArrayList<>();
    List<Reservation> reservations = new ArrayList<>();

    Hotel() {
        int num = 100;
        for (RoomType type : RoomType.values()) {
            for (int i = 0; i < 3; i++) {
                rooms.add(new Room(num++, type));
            }
        }
    }

    List<Room> searchAvailable(RoomType type) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.available && (type == null || r.type == type)) result.add(r);
        }
        return result;
    }

    Room getRoomByNumber(int number) {
        for (Room r : rooms) if (r.roomNumber == number) return r;
        return null;
    }

    Reservation book(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        Reservation res = new Reservation(guest, room, checkIn, checkOut);
        room.available = false;
        reservations.add(res);
        return res;
    }

    boolean cancel(int reservationId) {
        for (Reservation r : reservations) {
            if (r.reservationId == reservationId && r.status.equals("BOOKED")) {
                r.status = "CANCELLED";
                r.room.available = true;
                return true;
            }
        }
        return false;
    }

    Reservation findReservation(int id) {
        for (Reservation r : reservations) if (r.reservationId == id) return r;
        return null;
    }
}

// ================= Main App =================
public class HotelReservationSystem {
    static Scanner sc = new Scanner(System.in);
    static Hotel hotel = new Hotel();
    static final String SAVE_FILE = "reservations_save.txt";

    public static void main(String[] args) {
        System.out.println("=== Welcome to the Hotel Reservation System ===");
        loadReservations();

        boolean running = true;
        while (running) {
            System.out.println("\n1. Search Available Rooms\n2. Book a Room\n3. Cancel a Reservation"
                    + "\n4. View Booking Details\n5. View All Reservations\n6. Save & Exit");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1": searchRooms(); break;
                case "2": bookRoom(); break;
                case "3": cancelReservation(); break;
                case "4": viewBooking(); break;
                case "5": viewAllReservations(); break;
                case "6":
                    saveReservations();
                    running = false;
                    break;
                default: System.out.println("Invalid option.");
            }
        }
        System.out.println("Thank you for using the Hotel Reservation System!");
    }

    static void searchRooms() {
        System.out.print("Filter by type (STANDARD/DELUXE/SUITE) or press Enter for all: ");
        String input = sc.nextLine().trim().toUpperCase();
        RoomType type = null;
        if (!input.isEmpty()) {
            try {
                type = RoomType.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Unknown type, showing all rooms.");
            }
        }
        List<Room> available = hotel.searchAvailable(type);
        if (available.isEmpty()) {
            System.out.println("No available rooms found.");
            return;
        }
        for (Room r : available) System.out.println(r);
    }

    static void bookRoom() {
        System.out.print("Enter room number to book: ");
        int roomNumber = readInt();
        Room room = hotel.getRoomByNumber(roomNumber);
        if (room == null || !room.available) {
            System.out.println("Room not available.");
            return;
        }

        System.out.print("Guest name: ");
        String name = sc.nextLine();
        System.out.print("Contact (phone/email): ");
        String contact = sc.nextLine();

        LocalDate checkIn = readDate("Check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = readDate("Check-out date (YYYY-MM-DD): ");
        if (checkOut == null || checkIn == null || !checkOut.isAfter(checkIn)) {
            System.out.println("Invalid dates. Check-out must be after check-in.");
            return;
        }

        Guest guest = new Guest(name, contact);
        Reservation res = hotel.book(guest, room, checkIn, checkOut);

        System.out.print("Enter card number for payment (mock): ");
        String card = sc.nextLine();
        Payment.processPayment(res.totalCost, card);

        res.printReceipt();
    }

    static void cancelReservation() {
        System.out.print("Enter reservation ID to cancel: ");
        int id = readInt();
        boolean success = hotel.cancel(id);
        System.out.println(success ? "Reservation cancelled." : "Reservation not found or already cancelled.");
    }

    static void viewBooking() {
        System.out.print("Enter reservation ID: ");
        int id = readInt();
        Reservation res = hotel.findReservation(id);
        if (res == null) {
            System.out.println("Reservation not found.");
            return;
        }
        res.printReceipt();
    }

    static void viewAllReservations() {
        if (hotel.reservations.isEmpty()) {
            System.out.println("No reservations yet.");
            return;
        }
        for (Reservation r : hotel.reservations) {
            System.out.printf("ID %d | %s | Room %d (%s) | %s to %s | $%.2f | %s%n",
                    r.reservationId, r.guest.name, r.room.roomNumber, r.room.type,
                    r.checkIn, r.checkOut, r.totalCost, r.status);
        }
    }

    static int readInt() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number, defaulting to 0.");
            return 0;
        }
    }

    static LocalDate readDate(String prompt) {
        System.out.print(prompt);
        String input = sc.nextLine().trim();
        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format.");
            return null;
        }
    }

    // ---- simple file persistence ----
    static void saveReservations() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
            for (Reservation r : hotel.reservations) {
                pw.println(r.reservationId + "," + r.guest.name + "," + r.guest.contact + ","
                        + r.room.roomNumber + "," + r.checkIn + "," + r.checkOut + ","
                        + r.totalCost + "," + r.status);
            }
            System.out.println("Reservations saved to " + SAVE_FILE);
        } catch (IOException e) {
            System.out.println("Could not save reservations: " + e.getMessage());
        }
    }

    static void loadReservations() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                int id = Integer.parseInt(p[0]);
                String guestName = p[1];
                String contact = p[2];
                int roomNumber = Integer.parseInt(p[3]);
                LocalDate checkIn = LocalDate.parse(p[4]);
                LocalDate checkOut = LocalDate.parse(p[5]);
                double cost = Double.parseDouble(p[6]);
                String status = p[7];

                Room room = hotel.getRoomByNumber(roomNumber);
                if (room == null) continue;

                Guest guest = new Guest(guestName, contact);
                Reservation res = new Reservation(guest, room, checkIn, checkOut);
                res.reservationId = id;
                res.totalCost = cost;
                res.status = status;
                if (status.equals("BOOKED")) room.available = false;
                hotel.reservations.add(res);
                if (id > Reservation.counter) Reservation.counter = id;
            }
            System.out.println("Loaded saved reservations.");
        } catch (IOException e) {
            System.out.println("Could not load reservations: " + e.getMessage());
        }
    }
}