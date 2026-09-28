import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StockTradingPlatform {

    // ================= Stock =================
    static class Stock {
        String symbol;
        String companyName;
        double price;

        Stock(String symbol, String companyName, double price) {
            this.symbol = symbol;
            this.companyName = companyName;
            this.price = price;
        }

        void updatePrice(Random rng) {
            double changePercent = (rng.nextDouble() * 6) - 3; // -3% to +3%
            price = Math.max(0.5, price * (1 + changePercent / 100.0));
        }

        @Override
        public String toString() {
            return String.format("%-6s %-22s $%,10.2f", symbol, companyName, price);
        }
    }

    // ================= Transaction =================
    static class Transaction {
        String type;
        String symbol;
        int quantity;
        double price;
        LocalDateTime timestamp;

        Transaction(String type, String symbol, int quantity, double price) {
            this.type = type;
            this.symbol = symbol;
            this.quantity = quantity;
            this.price = price;
            this.timestamp = LocalDateTime.now();
        }

        @Override
        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return String.format("[%s] %-4s %4d x %-6s @ $%,.2f = $%,.2f",
                    timestamp.format(fmt), type, quantity, symbol, price, quantity * price);
        }

        String toFileLine() {
            return type + "," + symbol + "," + quantity + "," + price + "," + timestamp;
        }
    }

    // ================= Holding =================
    static class Holding {
        String symbol;
        int quantity;
        double totalCost;

        Holding(String symbol) {
            this.symbol = symbol;
            this.quantity = 0;
            this.totalCost = 0;
        }

        double avgCost() {
            return quantity == 0 ? 0 : totalCost / quantity;
        }
    }

    // ================= User =================
    static class User {
        String username;
        double cashBalance;
        Map<String, Holding> portfolio = new LinkedHashMap<>();
        List<Transaction> history = new ArrayList<>();

        User(String username, double startingCash) {
            this.username = username;
            this.cashBalance = startingCash;
        }
    }

    // ================= Market =================
    static class Market {
        Map<String, Stock> stocks = new LinkedHashMap<>();
        Random rng = new Random();

        void addStock(Stock s) {
            stocks.put(s.symbol, s);
        }

        void tick() {
            for (Stock s : stocks.values()) s.updatePrice(rng);
        }

        Stock get(String symbol) {
            return stocks.get(symbol.toUpperCase());
        }

        void display() {
            System.out.println("\n----- MARKET DATA -----");
            System.out.printf("%-6s %-22s %s%n", "SYM", "COMPANY", "PRICE");
            for (Stock s : stocks.values()) System.out.println(s);
        }
    }

    // ================= Trading Engine =================
    static class TradingPlatform {
        Market market = new Market();
        User user;
        static final String SAVE_FILE = "portfolio.txt";

        TradingPlatform(User user) {
            this.user = user;
            seedMarket();
        }

        void seedMarket() {
            market.addStock(new Stock("AAPL", "Apple Inc.", 190.00));
            market.addStock(new Stock("GOOG", "Alphabet Inc.", 140.00));
            market.addStock(new Stock("TSLA", "Tesla Inc.", 245.00));
            market.addStock(new Stock("AMZN", "Amazon.com Inc.", 178.00));
            market.addStock(new Stock("MSFT", "Microsoft Corp.", 415.00));
        }

        boolean buy(String symbol, int qty) {
            Stock s = market.get(symbol);
            if (s == null) { System.out.println("No such stock: " + symbol); return false; }
            double cost = s.price * qty;
            if (cost > user.cashBalance) {
                System.out.println("Insufficient funds. Need $" + String.format("%.2f", cost)
                        + ", have $" + String.format("%.2f", user.cashBalance));
                return false;
            }
            user.cashBalance -= cost;
            Holding h = user.portfolio.computeIfAbsent(s.symbol, Holding::new);
            h.quantity += qty;
            h.totalCost += cost;
            Transaction t = new Transaction("BUY", s.symbol, qty, s.price);
            user.history.add(t);
            System.out.println("Bought: " + t);
            return true;
        }

        boolean sell(String symbol, int qty) {
            Holding h = user.portfolio.get(symbol.toUpperCase());
            Stock s = market.get(symbol);
            if (s == null) { System.out.println("No such stock: " + symbol); return false; }
            if (h == null || h.quantity < qty) {
                System.out.println("You don't own enough shares of " + symbol);
                return false;
            }
            double proceeds = s.price * qty;
            double avgCost = h.avgCost();
            h.quantity -= qty;
            h.totalCost -= avgCost * qty;
            if (h.quantity == 0) user.portfolio.remove(s.symbol);
            user.cashBalance += proceeds;
            Transaction t = new Transaction("SELL", s.symbol, qty, s.price);
            user.history.add(t);
            System.out.println("Sold: " + t);
            return true;
        }

        void showPortfolio() {
            System.out.println("\n----- PORTFOLIO: " + user.username + " -----");
            System.out.printf("Cash balance: $%,.2f%n", user.cashBalance);
            if (user.portfolio.isEmpty()) {
                System.out.println("(no holdings)");
            } else {
                double totalValue = 0, totalCost = 0;
                System.out.printf("%-6s %8s %12s %12s %12s %10s%n",
                        "SYM", "QTY", "AVG COST", "CUR PRICE", "MKT VALUE", "P/L");
                for (Holding h : user.portfolio.values()) {
                    Stock s = market.get(h.symbol);
                    double marketValue = s.price * h.quantity;
                    double pl = marketValue - h.totalCost;
                    totalValue += marketValue;
                    totalCost += h.totalCost;
                    System.out.printf("%-6s %8d %12.2f %12.2f %12.2f %+10.2f%n",
                            h.symbol, h.quantity, h.avgCost(), s.price, marketValue, pl);
                }
                double totalPL = totalValue - totalCost;
                System.out.printf("%nTotal holdings value: $%,.2f | Cost basis: $%,.2f | P/L: %+,.2f (%.2f%%)%n",
                        totalValue, totalCost, totalPL, totalCost == 0 ? 0 : (totalPL / totalCost) * 100);
                System.out.printf("Net worth (cash + holdings): $%,.2f%n", totalValue + user.cashBalance);
            }
        }

        void showHistory() {
            System.out.println("\n----- TRANSACTION HISTORY -----");
            if (user.history.isEmpty()) { System.out.println("(none yet)"); return; }
            for (Transaction t : user.history) System.out.println(t);
        }

        void saveToFile() {
            try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
                pw.println("USER," + user.username + "," + user.cashBalance);
                for (Holding h : user.portfolio.values()) {
                    pw.println("HOLDING," + h.symbol + "," + h.quantity + "," + h.totalCost);
                }
                for (Transaction t : user.history) {
                    pw.println("TXN," + t.toFileLine());
                }
                System.out.println("Portfolio saved to " + SAVE_FILE);
            } catch (IOException e) {
                System.out.println("Error saving portfolio: " + e.getMessage());
            }
        }

        void loadFromFile() {
            File f = new File(SAVE_FILE);
            if (!f.exists()) { System.out.println("No saved portfolio found."); return; }
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts[0].equals("USER")) {
                        user.username = parts[1];
                        user.cashBalance = Double.parseDouble(parts[2]);
                    } else if (parts[0].equals("HOLDING")) {
                        Holding h = new Holding(parts[1]);
                        h.quantity = Integer.parseInt(parts[2]);
                        h.totalCost = Double.parseDouble(parts[3]);
                        user.portfolio.put(h.symbol, h);
                    }
                }
                System.out.println("Portfolio loaded from " + SAVE_FILE);
            } catch (IOException e) {
                System.out.println("Error loading portfolio: " + e.getMessage());
            }
        }
    }

    // ================= Main / Console UI =================
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) name = "trader1";

        User user = new User(name, 10000.00);
        TradingPlatform platform = new TradingPlatform(user);

        System.out.println("\nWelcome, " + name + "! Starting cash: $10,000.00");

        boolean running = true;
        while (running) {
            System.out.println("\n===== STOCK TRADING PLATFORM =====");
            System.out.println("1. View market data");
            System.out.println("2. Buy stock");
            System.out.println("3. Sell stock");
            System.out.println("4. View portfolio");
            System.out.println("5. View transaction history");
            System.out.println("6. Advance market (simulate price tick)");
            System.out.println("7. Save portfolio to file");
            System.out.println("8. Load portfolio from file");
            System.out.println("9. Exit");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    platform.market.display();
                    break;
                case "2": {
                    System.out.print("Symbol to buy: ");
                    String sym = sc.nextLine().trim();
                    System.out.print("Quantity: ");
                    int qty = safeInt(sc.nextLine());
                    if (qty > 0) platform.buy(sym, qty);
                    break;
                }
                case "3": {
                    System.out.print("Symbol to sell: ");
                    String sym = sc.nextLine().trim();
                    System.out.print("Quantity: ");
                    int qty = safeInt(sc.nextLine());
                    if (qty > 0) platform.sell(sym, qty);
                    break;
                }
                case "4":
                    platform.showPortfolio();
                    break;
                case "5":
                    platform.showHistory();
                    break;
                case "6":
                    platform.market.tick();
                    System.out.println("Market prices updated.");
                    platform.market.display();
                    break;
                case "7":
                    platform.saveToFile();
                    break;
                case "8":
                    platform.loadFromFile();
                    break;
                case "9":
                    running = false;
                    System.out.println("Goodbye, " + user.username + "!");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
        sc.close();
    }

    static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { System.out.println("Invalid number."); return -1; }
    }
}