import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class HotelReservationSystem {

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ================= Room Category =================
    enum RoomCategory {
        STANDARD(99.00),
        DELUXE(179.00),
        SUITE(299.00);

        final double basePricePerNight;
        RoomCategory(double price) { this.basePricePerNight = price; }
    }

    // ================= Room =================
    static class Room {
        int roomNumber;
        RoomCategory category;
        boolean available;

        Room(int roomNumber, RoomCategory category) {
            this.roomNumber = roomNumber;
            this.category = category;
            this.available = true;
        }

        @Override
        public String toString() {
            return String.format("Room %-4d [%-8s] %.2f/night - %s",
                    roomNumber, category, category.basePricePerNight,
                    available ? "AVAILABLE" : "BOOKED");
        }
    }

    // ================= Reservation =================
    static class Reservation {
        static int nextId = 1000;

        int id;
        String guestName;
        Room room;
        LocalDate checkIn;
        LocalDate checkOut;
        double totalPrice;
        boolean paid;

        Reservation(String guestName, Room room, LocalDate checkIn, LocalDate checkOut) {
            this.id = nextId++;
            this.guestName = guestName;
            this.room = room;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            this.totalPrice = nights * room.category.basePricePerNight;
            this.paid = false;
        }

        long nights() {
            return ChronoUnit.DAYS.between(checkIn, checkOut);
        }

        void printDetails() {
            System.out.println("---------------------------------------------");
            System.out.println("Booking ID   : " + id);
            System.out.println("Guest        : " + guestName);
            System.out.println("Room         : " + room.roomNumber + " (" + room.category + ")");
            System.out.println("Check-in     : " + checkIn.format(DATE_FMT));
            System.out.println("Check-out    : " + checkOut.format(DATE_FMT));
            System.out.println("Nights       : " + nights());
            System.out.printf ("Total price  : $%.2f%n", totalPrice);
            System.out.println("Payment      : " + (paid ? "PAID" : "PENDING"));
            System.out.println("---------------------------------------------");
        }

        String toFileLine() {
            return id + "," + guestName + "," + room.roomNumber + "," + room.category + ","
                    + checkIn + "," + checkOut + "," + totalPrice + "," + paid;
        }
    }

    // ================= Hotel =================
    static class Hotel {
        String name;
        List<Room> rooms = new ArrayList<>();
        Map<Integer, Reservation> reservations = new LinkedHashMap<>();
        static final String SAVE_FILE = "bookings.txt";

        Hotel(String name) {
            this.name = name;
        }

        void addRoom(Room r) {
            rooms.add(r);
        }

        List<Room> searchAvailable(RoomCategory category) {
            List<Room> result = new ArrayList<>();
            for (Room r : rooms) {
                if (r.available && (category == null || r.category == category)) {
                    result.add(r);
                }
            }
            return result;
        }

        Room findRoom(int number) {
            for (Room r : rooms) if (r.roomNumber == number) return r;
            return null;
        }

        Reservation book(String guest, int roomNumber, LocalDate checkIn, LocalDate checkOut) {
            Room r = findRoom(roomNumber);
            if (r == null) { System.out.println("No such room: " + roomNumber); return null; }
            if (!r.available) { System.out.println("Room " + roomNumber + " is not available."); return null; }
            if (!checkOut.isAfter(checkIn)) { System.out.println("Check-out must be after check-in."); return null; }

            Reservation res = new Reservation(guest, r, checkIn, checkOut);
            r.available = false;
            reservations.put(res.id, res);
            System.out.println("Booking created (ID " + res.id + "). Total due: $"
                    + String.format("%.2f", res.totalPrice));
            return res;
        }

        boolean cancel(int reservationId) {
            Reservation res = reservations.get(reservationId);
            if (res == null) { System.out.println("No reservation with ID " + reservationId); return false; }
            res.room.available = true;
            reservations.remove(reservationId);
            System.out.println("Reservation " + reservationId + " cancelled. Room "
                    + res.room.roomNumber + " is now available.");
            return true;
        }

        boolean pay(int reservationId, String cardLast4) {
            Reservation res = reservations.get(reservationId);
            if (res == null) { System.out.println("No reservation with ID " + reservationId); return false; }
            if (res.paid) { System.out.println("Reservation already paid."); return false; }
            System.out.println("Processing payment of $" + String.format("%.2f", res.totalPrice)
                    + " on card ending " + cardLast4 + " ...");
            res.paid = true;
            System.out.println("Payment successful. Receipt ID: PMT-" + res.id + "-" + cardLast4);
            return true;
        }

        void listAllRooms() {
            System.out.println("\n----- ALL ROOMS -----");
            for (Room r : rooms) System.out.println(r);
        }

        void listReservations() {
            System.out.println("\n----- ALL RESERVATIONS -----");
            if (reservations.isEmpty()) { System.out.println("(none)"); return; }
            for (Reservation r : reservations.values()) r.printDetails();
        }

        void saveToFile() {
            try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
                for (Room r : rooms) {
                    pw.println("ROOM," + r.roomNumber + "," + r.category + "," + r.available);
                }
                for (Reservation r : reservations.values()) {
                    pw.println("RES," + r.toFileLine());
                }
                System.out.println("Bookings & availability saved to " + SAVE_FILE);
            } catch (IOException e) {
                System.out.println("Error saving: " + e.getMessage());
            }
        }

        void loadFromFile() {
            File f = new File(SAVE_FILE);
            if (!f.exists()) { System.out.println("No saved data found."); return; }
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                int maxId = 999;
                while ((line = br.readLine()) != null) {
                    String[] p = line.split(",");
                    if (p[0].equals("ROOM")) {
                        Room r = findRoom(Integer.parseInt(p[1]));
                        if (r != null) r.available = Boolean.parseBoolean(p[3]);
                    } else if (p[0].equals("RES")) {
                        int id = Integer.parseInt(p[1]);
                        String guest = p[2];
                        int roomNum = Integer.parseInt(p[3]);
                        RoomCategory cat = RoomCategory.valueOf(p[4]);
                        LocalDate in = LocalDate.parse(p[5]);
                        LocalDate out = LocalDate.parse(p[6]);
                        double total = Double.parseDouble(p[7]);
                        boolean paid = Boolean.parseBoolean(p[8]);

                        Room room = findRoom(roomNum);
                        if (room != null) {
                            Reservation res = new Reservation(guest, room, in, out);
                            res.id = id;
                            res.totalPrice = total;
                            res.paid = paid;
                            reservations.put(id, res);
                            maxId = Math.max(maxId, id);
                        }
                    }
                }
                Reservation.nextId = maxId + 1;
                System.out.println("Bookings & availability loaded from " + SAVE_FILE);
            } catch (IOException e) {
                System.out.println("Error loading: " + e.getMessage());
            }
        }
    }

    // ================= Main / Console UI =================
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Hotel hotel = new Hotel("Grand Alpha Hotel");
        seedRooms(hotel);

        System.out.println("Welcome to " + hotel.name + " Reservation System");

        boolean running = true;
        while (running) {
            System.out.println("\n===== HOTEL RESERVATION SYSTEM =====");
            System.out.println("1. View all rooms");
            System.out.println("2. Search available rooms by category");
            System.out.println("3. Book a room");
            System.out.println("4. Cancel a reservation");
            System.out.println("5. Pay for a reservation (simulated)");
            System.out.println("6. View all reservations");
            System.out.println("7. Save data to file");
            System.out.println("8. Load data from file");
            System.out.println("9. Exit");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    hotel.listAllRooms();
                    break;
                case "2": {
                    System.out.print("Category (STANDARD/DELUXE/SUITE, blank = any): ");
                    String catStr = sc.nextLine().trim().toUpperCase();
                    RoomCategory cat = null;
                    if (!catStr.isEmpty()) {
                        try { cat = RoomCategory.valueOf(catStr); }
                        catch (IllegalArgumentException e) { System.out.println("Unknown category."); break; }
                    }
                    List<Room> avail = hotel.searchAvailable(cat);
                    System.out.println("\n----- AVAILABLE ROOMS -----");
                    if (avail.isEmpty()) System.out.println("(none available)");
                    for (Room r : avail) System.out.println(r);
                    break;
                }
                case "3": {
                    System.out.print("Guest name: ");
                    String guest = sc.nextLine().trim();
                    System.out.print("Room number: ");
                    int roomNum = safeInt(sc.nextLine());
                    LocalDate in = readDate(sc, "Check-in date (yyyy-MM-dd): ");
                    LocalDate out = readDate(sc, "Check-out date (yyyy-MM-dd): ");
                    if (roomNum > 0 && in != null && out != null) {
                        hotel.book(guest, roomNum, in, out);
                    }
                    break;
                }
                case "4": {
                    System.out.print("Reservation ID to cancel: ");
                    int id = safeInt(sc.nextLine());
                    if (id > 0) hotel.cancel(id);
                    break;
                }
                case "5": {
                    System.out.print("Reservation ID to pay for: ");
                    int id = safeInt(sc.nextLine());
                    System.out.print("Card last 4 digits: ");
                    String card = sc.nextLine().trim();
                    if (id > 0) hotel.pay(id, card);
                    break;
                }
                case "6":
                    hotel.listReservations();
                    break;
                case "7":
                    hotel.saveToFile();
                    break;
                case "8":
                    hotel.loadFromFile();
                    break;
                case "9":
                    running = false;
                    System.out.println("Thank you for using " + hotel.name + "!");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
        sc.close();
    }

    static void seedRooms(Hotel hotel) {
        int num = 101;
        for (int i = 0; i < 4; i++) hotel.addRoom(new Room(num++, RoomCategory.STANDARD));
        for (int i = 0; i < 3; i++) hotel.addRoom(new Room(num++, RoomCategory.DELUXE));
        for (int i = 0; i < 2; i++) hotel.addRoom(new Room(num++, RoomCategory.SUITE));
    }

    static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { System.out.println("Invalid number."); return -1; }
    }

    static LocalDate readDate(Scanner sc, String prompt) {
        System.out.print(prompt);
        String s = sc.nextLine().trim();
        try { return LocalDate.parse(s, DATE_FMT); }
        catch (Exception e) { System.out.println("Invalid date format."); return null; }
    }
}