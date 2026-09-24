import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

// ================= Stock =================
class Stock {
    String symbol;
    String companyName;
    double currentPrice;

    Stock(String symbol, String companyName, double currentPrice) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
    }

    // Simulate market fluctuation of ±5%
    void fluctuatePrice() {
        double changePercent = (Math.random() * 10) - 5; // -5% to +5%
        currentPrice = currentPrice + (currentPrice * changePercent / 100);
        currentPrice = Math.round(currentPrice * 100.0) / 100.0;
        if (currentPrice < 1) currentPrice = 1; // floor
    }

    @Override
    public String toString() {
        return String.format("%-6s %-20s $%.2f", symbol, companyName, currentPrice);
    }
}

// ================= Transaction =================
class Transaction {
    String type; // BUY or SELL
    String symbol;
    int quantity;
    double priceEach;
    LocalDateTime timestamp;

    Transaction(String type, String symbol, int quantity, double priceEach) {
        this.type = type;
        this.symbol = symbol;
        this.quantity = quantity;
        this.priceEach = priceEach;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %d x %s @ $%.2f = $%.2f",
                timestamp.toLocalDate(), type, quantity, symbol, priceEach, quantity * priceEach);
    }
}

// ================= Portfolio =================
class Portfolio {
    // symbol -> quantity owned
    Map<String, Integer> holdings = new HashMap<>();
    // symbol -> average buy price (for gain/loss calc)
    Map<String, Double> avgBuyPrice = new HashMap<>();
    List<Transaction> history = new ArrayList<>();

    void addStock(String symbol, int qty, double price) {
        int existingQty = holdings.getOrDefault(symbol, 0);
        double existingAvg = avgBuyPrice.getOrDefault(symbol, 0.0);

        double totalCostBefore = existingQty * existingAvg;
        double totalCostNow = qty * price;
        int newQty = existingQty + qty;
        double newAvg = (totalCostBefore + totalCostNow) / newQty;

        holdings.put(symbol, newQty);
        avgBuyPrice.put(symbol, newAvg);
        history.add(new Transaction("BUY", symbol, qty, price));
    }

    boolean removeStock(String symbol, int qty, double price) {
        int existingQty = holdings.getOrDefault(symbol, 0);
        if (qty > existingQty) return false;

        int newQty = existingQty - qty;
        if (newQty == 0) {
            holdings.remove(symbol);
            avgBuyPrice.remove(symbol);
        } else {
            holdings.put(symbol, newQty);
        }
        history.add(new Transaction("SELL", symbol, qty, price));
        return true;
    }

    double getTotalValue(Market market) {
        double total = 0;
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            Stock s = market.getStock(entry.getKey());
            if (s != null) total += s.currentPrice * entry.getValue();
        }
        return total;
    }

    void printSummary(Market market) {
        if (holdings.isEmpty()) {
            System.out.println("No holdings yet.");
            return;
        }
        System.out.printf("%-6s %-8s %-12s %-12s %-10s%n",
                "Sym", "Qty", "AvgBuy", "CurPrice", "Gain/Loss");
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            String symbol = entry.getKey();
            int qty = entry.getValue();
            double avg = avgBuyPrice.get(symbol);
            Stock s = market.getStock(symbol);
            double cur = (s != null) ? s.currentPrice : 0;
            double gainLoss = (cur - avg) * qty;
            System.out.printf("%-6s %-8d $%-11.2f $%-11.2f $%-10.2f%n",
                    symbol, qty, avg, cur, gainLoss);
        }
    }

    void printHistory() {
        if (history.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        for (Transaction t : history) System.out.println(t);
    }
}

// ================= User =================
class User {
    String username;
    double cashBalance;
    Portfolio portfolio = new Portfolio();

    User(String username, double startingBalance) {
        this.username = username;
        this.cashBalance = startingBalance;
    }
}

// ================= Market =================
class Market {
    List<Stock> stocks = new ArrayList<>();

    Market() {
        stocks.add(new Stock("AAPL", "Apple Inc.", 180.00));
        stocks.add(new Stock("GOOG", "Alphabet Inc.", 140.00));
        stocks.add(new Stock("TSLA", "Tesla Inc.", 250.00));
        stocks.add(new Stock("AMZN", "Amazon.com", 130.00));
        stocks.add(new Stock("MSFT", "Microsoft Corp.", 330.00));
    }

    Stock getStock(String symbol) {
        for (Stock s : stocks) {
            if (s.symbol.equalsIgnoreCase(symbol)) return s;
        }
        return null;
    }

    void tick() {
        for (Stock s : stocks) s.fluctuatePrice();
    }

    void printMarket() {
        System.out.println("---- Market Prices ----");
        for (Stock s : stocks) System.out.println(s);
    }
}

// ================= Main App =================
public class StockTradingPlatform {
    static Scanner sc = new Scanner(System.in);
    static Market market = new Market();
    static User user;
    static final String SAVE_FILE = "portfolio_save.txt";

    public static void main(String[] args) {
        System.out.println("=== Welcome to the Stock Trading Platform ===");
        System.out.print("Enter your username: ");
        String username = sc.nextLine();
        user = new User(username, 10000.00); // starting cash
        loadPortfolio();

        boolean running = true;
        while (running) {
            market.tick(); // simulate live price movement each loop
            System.out.println("\n1. View Market Prices\n2. Buy Stock\n3. Sell Stock"
                    + "\n4. View Portfolio\n5. View Transaction History\n6. Save & Exit");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1": market.printMarket(); break;
                case "2": buyStock(); break;
                case "3": sellStock(); break;
                case "4": viewPortfolio(); break;
                case "5": user.portfolio.printHistory(); break;
                case "6":
                    savePortfolio();
                    running = false;
                    break;
                default: System.out.println("Invalid option.");
            }
        }
        System.out.println("Goodbye, " + user.username + "!");
    }

    static void buyStock() {
        market.printMarket();
        System.out.print("Enter symbol to buy: ");
        String symbol = sc.nextLine().toUpperCase();
        Stock s = market.getStock(symbol);
        if (s == null) { System.out.println("Stock not found."); return; }

        System.out.print("Quantity: ");
        int qty = readInt();
        double cost = qty * s.currentPrice;

        if (cost > user.cashBalance) {
            System.out.println("Insufficient funds. Needed $" + String.format("%.2f", cost)
                    + ", have $" + String.format("%.2f", user.cashBalance));
            return;
        }
        user.cashBalance -= cost;
        user.portfolio.addStock(symbol, qty, s.currentPrice);
        System.out.printf("Bought %d shares of %s for $%.2f%n", qty, symbol, cost);
    }

    static void sellStock() {
        System.out.print("Enter symbol to sell: ");
        String symbol = sc.nextLine().toUpperCase();
        Stock s = market.getStock(symbol);
        if (s == null) { System.out.println("Stock not found."); return; }

        System.out.print("Quantity: ");
        int qty = readInt();

        boolean success = user.portfolio.removeStock(symbol, qty, s.currentPrice);
        if (!success) { System.out.println("You don't own that many shares."); return; }

        double proceeds = qty * s.currentPrice;
        user.cashBalance += proceeds;
        System.out.printf("Sold %d shares of %s for $%.2f%n", qty, symbol, proceeds);
    }

    static void viewPortfolio() {
        System.out.printf("Cash Balance: $%.2f%n", user.cashBalance);
        user.portfolio.printSummary(market);
        double total = user.cashBalance + user.portfolio.getTotalValue(market);
        System.out.printf("Total Account Value: $%.2f%n", total);
    }

    static int readInt() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number, defaulting to 0.");
            return 0;
        }
    }

    // ---- simple file persistence: username, cash, then symbol,qty,avgPrice per line ----
    static void savePortfolio() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
            pw.println(user.username);
            pw.println(user.cashBalance);
            for (String symbol : user.portfolio.holdings.keySet()) {
                pw.println(symbol + "," + user.portfolio.holdings.get(symbol)
                        + "," + user.portfolio.avgBuyPrice.get(symbol));
            }
            System.out.println("Portfolio saved to " + SAVE_FILE);
        } catch (IOException e) {
            System.out.println("Could not save portfolio: " + e.getMessage());
        }
    }

    static void loadPortfolio() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String savedUser = br.readLine();
            String savedCash = br.readLine();
            if (savedUser == null || savedCash == null) return;

            if (!savedUser.equals(user.username)) return; // only load matching user
            user.cashBalance = Double.parseDouble(savedCash);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                String symbol = parts[0];
                int qty = Integer.parseInt(parts[1]);
                double avg = Double.parseDouble(parts[2]);
                user.portfolio.holdings.put(symbol, qty);
                user.portfolio.avgBuyPrice.put(symbol, avg);
            }
            System.out.println("Loaded saved portfolio for " + user.username);
        } catch (IOException e) {
            System.out.println("Could not load portfolio: " + e.getMessage());
        }
    }
}