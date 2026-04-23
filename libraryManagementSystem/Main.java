import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.stream.Collectors;

public class Main {

    // ═══════════════════════════════════════════════════
    // CHANGE THIS to your SQLite database path
    // ═══════════════════════════════════════════════════

    static final String DB_URL = "jdbc:sqlite:C:\\Users\\karan\\OneDrive\\Desktop\\Database\\library.db";
    static final String BASE_DIR = "C:\\Users\\karan\\OneDrive\\Desktop\\main\\library website";

    public static void main(String[] args) throws Exception {
        createTables();

        HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);

        server.createContext("/", Main::handleStaticFiles);
        // login endpoint

        server.createContext("/api/login", Main::handleLogin);

        // Book endpoints
        server.createContext("/api/books", Main::handleBooks);
        server.createContext("/api/books/update", Main::handleUpdateBook);

        // Member endpoints
        server.createContext("/api/members", Main::handleMembers);
        server.createContext("/api/members/update", Main::handleUpdateMember);

        // Category endpoints
        server.createContext("/api/categories", Main::handleCategories);

        // Issue & Return endpoints
        server.createContext("/api/issue", Main::handleIssue);
        server.createContext("/api/return", Main::handleReturn);

        // Issued / Returned list endpoints
        server.createContext("/api/issued", Main::handleIssuedList);
        server.createContext("/api/returned", Main::handleReturnedList);

        // Overdue endpoint
        server.createContext("/api/overdue", Main::handleOverdue);

        // Fine endpoints
        server.createContext("/api/fines", Main::handleFines);
        server.createContext("/api/fines/pay", Main::handlePayFine);

        server.setExecutor(null);
        server.start();
        System.out.println("✅ Server running at http://localhost:9000");

        // ═══════════════════════════════════════════════════
        // AUTO-OPEN BROWSER
        // ═══════════════════════════════════════════════════

        // Change this URL to match exactly where your frontend login HTML file is
        // located
        String loginPageUrl = "http://localhost:9000/libraryManagementSystem/Frontend/login.html"; // <-- CHANGE THIS IF YOUR FILE IS DIFFERENT

        try {
            // Wait 1 second to ensure the server is fully ready to accept connections
            Thread.sleep(1000);

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    System.out.println("🌐 Opening login page in default browser...");
                    desktop.browse(java.net.URI.create(loginPageUrl));
                }
            } else {
                System.out.println("⚠️ Desktop browsing not supported on this system.");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not open browser automatically: " + e.getMessage());
        }
    }

    static void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/libraryManagementSystem/Frontend/login.html";
        }

        File file = new File(BASE_DIR + path);

        System.out.println("Requested: " + path);
        System.out.println("Looking at: " + file.getAbsolutePath());

        if (!file.exists()) {
            String res = "404 Not Found";
            exchange.sendResponseHeaders(404, res.length());
            exchange.getResponseBody().write(res.getBytes());
            exchange.close();
            return;
        }

        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());

        exchange.getResponseHeaders().add("Content-Type", getContentType(path));
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    // ═══════════════════════════════════════════════════
    // DATABASE INITIALIZATION
    // ═══════════════════════════════════════════════════
    static void createTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();

            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "id TEXT PRIMARY KEY, title TEXT, author TEXT, " +
                    "isbn TEXT, cat TEXT, qty INTEGER, available INTEGER)");

            stmt.execute("CREATE TABLE IF NOT EXISTS members (" +
                    "id TEXT PRIMARY KEY, name TEXT, email TEXT, " +
                    "phone TEXT, type TEXT, status TEXT, " +
                    "gender TEXT, dob TEXT, nic TEXT, address TEXT, " +
                    "start_date TEXT, expiry_date TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS issues (" +
                    "issue_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "book_id TEXT, book_title TEXT, member_id TEXT, member_name TEXT, " +
                    "issue_date TEXT, due_date TEXT, status TEXT, " +
                    " FOREIGN KEY(book_id) REFERENCES books(id), " +
                    " FOREIGN KEY(member_id) REFERENCES members(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS returns (" +
                    "return_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "issue_id INTEGER, book_id TEXT, book_title TEXT, " +
                    "member_id TEXT, member_name TEXT, " +
                    "issue_date TEXT, due_date TEXT, return_date TEXT, " +
                    "days_overdue INTEGER, fine_amount REAL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS fines (" +
                    "fine_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "issue_id INTEGER, member_id TEXT, member_name TEXT, " +
                    "book_title TEXT, amount REAL, status TEXT, " +
                    "created_date TEXT, paid_date TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS admins (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "email TEXT, " +
                    "password TEXT, " +
                    "role TEXT, " +
                    "status TEXT)");

            System.out.println("✅ Database tables verified/created.");
        } catch (Exception e) {
            System.out.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════
    // BOOK ENDPOINTS (GET, POST, DELETE)
    // ═══════════════════════════════════════════════════
    static void handleBooks(HttpExchange exchange) throws IOException {
        addCors(exchange);
        String method = exchange.getRequestMethod();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if ("GET".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    String id = query.split("=")[1];
                    PreparedStatement ps = conn.prepareStatement("SELECT * FROM books WHERE id = ?");
                    ps.setString(1, id);
                    ResultSet rs = ps.executeQuery();
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first)
                            json.append(",");
                        first = false;
                        json.append(bookToJson(rs));
                    }
                    json.append("]");
                    sendJson(exchange, json.toString());
                } else {
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM books");
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first)
                            json.append(",");
                        first = false;
                        json.append(bookToJson(rs));
                    }
                    json.append("]");
                    sendJson(exchange, json.toString());
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                String id = getValue(body, "id");
                String title = getValue(body, "title");
                String author = getValue(body, "author");
                String isbn = getValue(body, "isbn");
                String cat = getValue(body, "cat");
                int qty = safeParseInt(getValue(body, "qty"));
                int available = safeParseInt(getValue(body, "available"));

                PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO books VALUES (?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, id);
                ps.setString(2, title);
                ps.setString(3, author);
                ps.setString(4, isbn);
                ps.setString(5, cat);
                ps.setInt(6, qty);
                ps.setInt(7, available);
                int rows = ps.executeUpdate();
                if (rows > 0)
                    sendJson(exchange, "{\"status\":\"Book Added\"}");
                else
                    sendJson(exchange, "{\"error\":\"Book ID already exists\"}", 409);
            } else if ("DELETE".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    String id = query.split("=")[1];
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE id = ?");
                    ps.setString(1, id);
                    ps.executeUpdate();
                    sendJson(exchange, "{\"status\":\"Deleted\"}");
                } else {
                    sendJson(exchange, "{\"error\":\"Missing book id\"}", 400);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // Update book
    static void handleUpdateBook(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if (!"POST".equals(exchange.getRequestMethod()))
            return;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String body = readBody(exchange);
            String id = getValue(body, "id");
            String title = getValue(body, "title");
            String author = getValue(body, "author");
            String isbn = getValue(body, "isbn");
            String cat = getValue(body, "cat");
            int qty = safeParseInt(getValue(body, "qty"));
            int available = safeParseInt(getValue(body, "available"));

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE books SET title=?, author=?, isbn=?, cat=?, qty=?, available=? WHERE id=?");
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, isbn);
            ps.setString(4, cat);
            ps.setInt(5, qty);
            ps.setInt(6, available);
            ps.setString(7, id);
            ps.executeUpdate();
            sendJson(exchange, "{\"status\":\"Book Updated\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════════════════
    // MEMBER ENDPOINTS (GET, POST, DELETE)
    // ═══════════════════════════════════════════════════
    static void handleMembers(HttpExchange exchange) throws IOException {
        addCors(exchange);
        String method = exchange.getRequestMethod();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if ("GET".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("search=")) {
                    String search = java.net.URLDecoder.decode(query.split("=")[1], StandardCharsets.UTF_8);
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM members WHERE name LIKE ? OR id LIKE ?");
                    ps.setString(1, "%" + search + "%");
                    ps.setString(2, "%" + search + "%");
                    ResultSet rs = ps.executeQuery();
                    sendJson(exchange, resultSetToJson(rs, "members"));
                } else {
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM members");
                    sendJson(exchange, resultSetToJson(rs, "members"));
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO members VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
                ps.setString(1, getValue(body, "id"));
                ps.setString(2, getValue(body, "name"));
                ps.setString(3, getValue(body, "email"));
                ps.setString(4, getValue(body, "phone"));
                ps.setString(5, getValue(body, "type"));
                ps.setString(6, getValue(body, "status"));
                ps.setString(7, getValue(body, "gender"));
                ps.setString(8, getValue(body, "dob"));
                ps.setString(9, getValue(body, "nic"));
                ps.setString(10, getValue(body, "address"));
                ps.setString(11, getValue(body, "start_date"));
                ps.setString(12, getValue(body, "expiry_date"));
                int rows = ps.executeUpdate();
                if (rows > 0)
                    sendJson(exchange, "{\"status\":\"Member Registered\"}");
                else
                    sendJson(exchange, "{\"error\":\"Member ID already exists\"}", 409);
            } else if ("DELETE".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    String id = query.split("=")[1];
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM members WHERE id = ?");
                    ps.setString(1, id);
                    ps.executeUpdate();
                    sendJson(exchange, "{\"status\":\"Member Deleted\"}");
                } else {
                    sendJson(exchange, "{\"error\":\"Missing member id\"}", 400);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // Update member
    static void handleUpdateMember(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if (!"POST".equals(exchange.getRequestMethod()))
            return;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String body = readBody(exchange);
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE members SET name=?, email=?, phone=?, type=?, status=?, gender=?, dob=?, nic=?, address=?, start_date=?, expiry_date=? WHERE id=?");
            ps.setString(1, getValue(body, "name"));
            ps.setString(2, getValue(body, "email"));
            ps.setString(3, getValue(body, "phone"));
            ps.setString(4, getValue(body, "type"));
            ps.setString(5, getValue(body, "status"));
            ps.setString(6, getValue(body, "gender"));
            ps.setString(7, getValue(body, "dob"));
            ps.setString(8, getValue(body, "nic"));
            ps.setString(9, getValue(body, "address"));
            ps.setString(10, getValue(body, "start_date"));
            ps.setString(11, getValue(body, "expiry_date"));
            ps.setString(12, getValue(body, "id"));
            ps.executeUpdate();
            sendJson(exchange, "{\"status\":\"Member Updated\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════════════════
    // CATEGORY ENDPOINTS
    // ═══════════════════════════════════════════════════
    static void handleCategories(HttpExchange exchange) throws IOException {
        addCors(exchange);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if ("GET".equals(exchange.getRequestMethod())) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT cat, COUNT(*) as count FROM books GROUP BY cat ORDER BY count DESC");
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first)
                        json.append(",");
                    first = false;
                    json.append("{")
                            .append("\"name\":\"").append(esc(rs.getString("cat"))).append("\",")
                            .append("\"count\":").append(rs.getInt("count"))
                            .append("}");
                }
                json.append("]");
                sendJson(exchange, json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════════════════
    // ISSUE BOOK ENDPOINT
    // ═══════════════════════════════════════════════════
    static void handleIssue(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if (!"POST".equals(exchange.getRequestMethod()))
            return;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String body = readBody(exchange);
            String bookIds = getValue(body, "book_ids");
            String memberId = getValue(body, "member_id");
            String issueDate = getValue(body, "issue_date");
            String dueDate = getValue(body, "due_date");

            if (memberId.isEmpty()) {
                sendJson(exchange, "{\"error\":\"Member ID is required\"}", 400);
                return;
            }

            // Get member name
            PreparedStatement mps = conn.prepareStatement("SELECT name FROM members WHERE id = ?");
            mps.setString(1, memberId);
            ResultSet mrs = mps.executeQuery();
            if (!mrs.next()) {
                sendJson(exchange, "{\"error\":\"Member not found\"}", 404);
                return;
            }
            String memberName = mrs.getString("name");

            String[] ids = bookIds.split(",");
            for (String bookId : ids) {
                bookId = bookId.trim();
                if (bookId.isEmpty())
                    continue;

                // Get book info & check availability
                PreparedStatement bps = conn.prepareStatement("SELECT title, available FROM books WHERE id = ?");
                bps.setString(1, bookId);
                ResultSet brs = bps.executeQuery();
                if (!brs.next())
                    continue;
                int avail = brs.getInt("available");
                if (avail <= 0)
                    continue;
                String bookTitle = brs.getString("title");

                // Insert issue record
                PreparedStatement ips = conn.prepareStatement(
                        "INSERT INTO issues (book_id, book_title, member_id, member_name, issue_date, due_date, status) VALUES (?,?,?,?,?,?,?)");
                ips.setString(1, bookId);
                ips.setString(2, bookTitle);
                ips.setString(3, memberId);
                ips.setString(4, memberName);
                ips.setString(5, issueDate);
                ips.setString(6, dueDate);
                ips.setString(7, "Issued");
                ips.executeUpdate();

                // Decrease available count
                PreparedStatement ups = conn
                        .prepareStatement("UPDATE books SET available = available - 1 WHERE id = ?");
                ups.setString(1, bookId);
                ups.executeUpdate();
            }
            sendJson(exchange, "{\"status\":\"Books Issued\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════════════════
    // RETURN BOOK ENDPOINT
    // ═══════════════════════════════════════════════════
    static void handleReturn(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if (!"POST".equals(exchange.getRequestMethod()))
            return;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String body = readBody(exchange);
            String issueIds = getValue(body, "issue_ids");
            String returnDate = getValue(body, "return_date");

            if (issueIds.isEmpty()) {
                sendJson(exchange, "{\"error\":\"Issue IDs are required\"}", 400);
                return;
            }

            String[] ids = issueIds.split(",");
            for (String issueIdStr : ids) {
                issueIdStr = issueIdStr.trim();
                if (issueIdStr.isEmpty())
                    continue;
                int issueId = safeParseInt(issueIdStr);

                // Get issue details
                PreparedStatement gps = conn
                        .prepareStatement("SELECT * FROM issues WHERE issue_id = ? AND status = 'Issued'");
                gps.setInt(1, issueId);
                ResultSet grs = gps.executeQuery();
                if (!grs.next())
                    continue;

                String bookId = grs.getString("book_id");
                String bookTitle = grs.getString("book_title");
                String memberId = grs.getString("member_id");
                String memberName = grs.getString("member_name");
                String issueDate = grs.getString("issue_date");
                String dueDate = grs.getString("due_date");

                // Calculate overdue days (LKR 5.00 per day)
                long dueTime = java.sql.Date.valueOf(dueDate).getTime();
                long retTime = java.sql.Date.valueOf(returnDate).getTime();
                long diffMs = retTime - dueTime;
                int daysOverdue = diffMs > 0 ? (int) (diffMs / (1000 * 60 * 60 * 24)) : 0;
                double fineAmount = daysOverdue * 5.00;

                // Insert return record
                PreparedStatement rps = conn.prepareStatement(
                        "INSERT INTO returns (issue_id, book_id, book_title, member_id, member_name, issue_date, due_date, return_date, days_overdue, fine_amount) VALUES (?,?,?,?,?,?,?,?,?,?)");
                rps.setInt(1, issueId);
                rps.setString(2, bookId);
                rps.setString(3, bookTitle);
                rps.setString(4, memberId);
                rps.setString(5, memberName);
                rps.setString(6, issueDate);
                rps.setString(7, dueDate);
                rps.setString(8, returnDate);
                rps.setInt(9, daysOverdue);
                rps.setDouble(10, fineAmount);
                rps.executeUpdate();

                // Update issue status
                PreparedStatement ups = conn
                        .prepareStatement("UPDATE issues SET status = 'Returned' WHERE issue_id = ?");
                ups.setInt(1, issueId);
                ups.executeUpdate();

                // Increase book available count
                PreparedStatement bps = conn
                        .prepareStatement("UPDATE books SET available = available + 1 WHERE id = ?");
                bps.setString(1, bookId);
                bps.executeUpdate();

                // Create fine record if overdue
                if (daysOverdue > 0) {
                    PreparedStatement fps = conn.prepareStatement(
                            "INSERT INTO fines (issue_id, member_id, member_name, book_title, amount, status, created_date) VALUES (?,?,?,?,?,?,?)");
                    fps.setInt(1, issueId);
                    fps.setString(2, memberId);
                    fps.setString(3, memberName);
                    fps.setString(4, bookTitle);
                    fps.setDouble(5, fineAmount);
                    fps.setString(6, "Unpaid");
                    fps.setString(7, returnDate);
                    fps.executeUpdate();

                    // Restrict member
                    PreparedStatement mps = conn.prepareStatement(
                            "UPDATE members SET status = 'Restricted' WHERE id = ? AND status = 'Active'");
                    mps.setString(1, memberId);
                    mps.executeUpdate();
                }
            }
            sendJson(exchange, "{\"status\":\"Books Returned\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════════════════
    // ISSUED BOOKS LIST
    // ═══════════════════════════════════════════════════
    static void handleIssuedList(HttpExchange exchange) throws IOException {
        addCors(exchange);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String query = exchange.getRequestURI().getQuery();
            PreparedStatement ps;
            if (query != null && query.startsWith("member_id=")) {
                String mid = query.split("=")[1];
                ps = conn.prepareStatement(
                        "SELECT * FROM issues WHERE member_id = ? AND status = 'Issued' ORDER BY issue_id DESC");
                ps.setString(1, mid);
            } else {
                ps = conn.prepareStatement("SELECT * FROM issues WHERE status = 'Issued' ORDER BY issue_id DESC");
            }
            ResultSet rs = ps.executeQuery();
            sendJson(exchange, issueResultSetToJson(rs));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════════════════
    // RETURNED BOOKS LIST
    // ═══════════════════════════════════════════════════
    static void handleReturnedList(HttpExchange exchange) throws IOException {
        addCors(exchange);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM returns ORDER BY return_id DESC");
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first)
                    json.append(",");
                first = false;
                json.append("{")
                        .append("\"return_id\":").append(rs.getInt("return_id")).append(",")
                        .append("\"issue_id\":").append(rs.getInt("issue_id")).append(",")
                        .append("\"book_id\":\"").append(esc(rs.getString("book_id"))).append("\",")
                        .append("\"book_title\":\"").append(esc(rs.getString("book_title"))).append("\",")
                        .append("\"member_id\":\"").append(esc(rs.getString("member_id"))).append("\",")
                        .append("\"member_name\":\"").append(esc(rs.getString("member_name"))).append("\",")
                        .append("\"issue_date\":\"").append(rs.getString("issue_date")).append("\",")
                        .append("\"due_date\":\"").append(rs.getString("due_date")).append("\",")
                        .append("\"return_date\":\"").append(rs.getString("return_date")).append("\",")
                        .append("\"days_overdue\":").append(rs.getInt("days_overdue")).append(",")
                        .append("\"fine_amount\":").append(rs.getDouble("fine_amount"))
                        .append("}");
            }
            json.append("]");
            sendJson(exchange, json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════════════════
    // OVERDUE BOOKS LIST
    // ═══════════════════════════════════════════════════
    static void handleOverdue(HttpExchange exchange) throws IOException {
        addCors(exchange);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String today = new java.sql.Date(System.currentTimeMillis()).toString();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT *, CAST(julianday(?) - julianday(due_date) AS INTEGER) as days_overdue, " +
                            "CAST((julianday(?) - julianday(due_date)) * 5.00 AS REAL) as fine " +
                            "FROM issues WHERE status = 'Issued' AND due_date < ? ORDER BY days_overdue DESC");
            ps.setString(1, today);
            ps.setString(2, today);
            ps.setString(3, today);
            ResultSet rs = ps.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first)
                    json.append(",");
                first = false;
                json.append("{")
                        .append("\"issue_id\":").append(rs.getInt("issue_id")).append(",")
                        .append("\"book_id\":\"").append(esc(rs.getString("book_id"))).append("\",")
                        .append("\"book_title\":\"").append(esc(rs.getString("book_title"))).append("\",")
                        .append("\"member_id\":\"").append(esc(rs.getString("member_id"))).append("\",")
                        .append("\"member_name\":\"").append(esc(rs.getString("member_name"))).append("\",")
                        .append("\"issue_date\":\"").append(rs.getString("issue_date")).append("\",")
                        .append("\"due_date\":\"").append(rs.getString("due_date")).append("\",")
                        .append("\"days_overdue\":").append(rs.getInt("days_overdue")).append(",")
                        .append("\"fine\":").append(String.format("%.2f", rs.getDouble("fine")))
                        .append("}");
            }
            json.append("]");
            sendJson(exchange, json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════════════════
    // FINE ENDPOINTS
    // ═══════════════════════════════════════════════════
    static void handleFines(HttpExchange exchange) throws IOException {
        addCors(exchange);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String query = exchange.getRequestURI().getQuery();
            PreparedStatement ps;
            if (query != null && query.startsWith("member_id=")) {
                String mid = query.split("=")[1];
                ps = conn.prepareStatement("SELECT * FROM fines WHERE member_id = ? ORDER BY fine_id DESC");
                ps.setString(1, mid);
            } else if (query != null && query.startsWith("status=")) {
                String status = query.split("=")[1];
                ps = conn.prepareStatement("SELECT * FROM fines WHERE status = ? ORDER BY fine_id DESC");
                ps.setString(1, status);
            } else {
                ps = conn.prepareStatement("SELECT * FROM fines ORDER BY fine_id DESC");
            }
            ResultSet rs = ps.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first)
                    json.append(",");
                first = false;
                json.append("{")
                        .append("\"fine_id\":").append(rs.getInt("fine_id")).append(",")
                        .append("\"issue_id\":").append(rs.getInt("issue_id")).append(",")
                        .append("\"member_id\":\"").append(esc(rs.getString("member_id"))).append("\",")
                        .append("\"member_name\":\"").append(esc(rs.getString("member_name"))).append("\",")
                        .append("\"book_title\":\"").append(esc(rs.getString("book_title"))).append("\",")
                        .append("\"amount\":").append(String.format("%.2f", rs.getDouble("amount"))).append(",")
                        .append("\"status\":\"").append(rs.getString("status")).append("\",")
                        .append("\"created_date\":\"").append(rs.getString("created_date")).append("\",")
                        .append("\"paid_date\":\"")
                        .append(rs.getString("paid_date") != null ? rs.getString("paid_date") : "").append("\"")
                        .append("}");
            }
            json.append("]");
            sendJson(exchange, json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════
    // LOGIN ENDPOINT (Updated for admins table)
    // ═══════════════════════════════════════
    static void handleLogin(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if (!"POST".equals(exchange.getRequestMethod()))
            return;

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String body = readBody(exchange);
            String email = getValue(body, "email");
            String password = getValue(body, "password");

            if (email.isEmpty() || password.isEmpty()) {
                sendJson(exchange, "{\"success\":false, \"error\":\"Email and password are required\"}", 400);
                return;
            }

            // Check against the admins table
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, email, role FROM admins WHERE (email = ? OR id = ?) AND status = 'Active'");
            ps.setString(1, email);
            ps.setString(2, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String dbId = rs.getString("id");
                String dbEmail = rs.getString("email");
                String dbName = rs.getString("name");
                String dbRole = rs.getString("role");

                // Fetch the actual password from the row to compare
                PreparedStatement ps2 = conn.prepareStatement("SELECT password FROM admins WHERE id = ?");
                ps2.setString(1, dbId);
                ResultSet rs2 = ps2.executeQuery();
                rs2.next();
                String dbPassword = rs2.getString("password");

                if (password.equals(dbPassword)) {
                    sendJson(exchange, "{\"success\":true, \"id\":\"" + esc(dbId) + "\", \"name\":\"" + esc(dbName)
                            + "\", \"email\":\"" + esc(dbEmail) + "\", \"type\":\"" + esc(dbRole) + "\"}");
                } else {
                    sendJson(exchange, "{\"success\":false, \"error\":\"Incorrect password\"}", 401);
                }
            } else {
                sendJson(exchange, "{\"success\":false, \"error\":\"Admin account not found or inactive\"}", 404);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"success\":false, \"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // Pay a fine
    static void handlePayFine(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if (!"POST".equals(exchange.getRequestMethod()))
            return;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String body = readBody(exchange);
            int fineId = safeParseInt(getValue(body, "fine_id"));
            String paidDate = getValue(body, "paid_date");
            String memberId = getValue(body, "member_id");

            if (memberId.isEmpty()) {
                sendJson(exchange, "{\"error\":\"Member ID is required\"}", 400);
                return;
            }

            // Update fine status
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE fines SET status = 'Paid', paid_date = ? WHERE fine_id = ?");
            ps.setString(1, paidDate);
            ps.setInt(2, fineId);
            ps.executeUpdate();

            // Check if member has any other unpaid fines
            PreparedStatement cps = conn
                    .prepareStatement("SELECT COUNT(*) FROM fines WHERE member_id = ? AND status = 'Unpaid'");
            cps.setString(1, memberId);
            ResultSet crs = cps.executeQuery();
            crs.next();
            int unpaidCount = crs.getInt(1);

            // If no more unpaid fines, un-restrict the member
            if (unpaidCount == 0) {
                PreparedStatement mps = conn.prepareStatement(
                        "UPDATE members SET status = 'Active' WHERE id = ? AND status = 'Restricted'");
                mps.setString(1, memberId);
                mps.executeUpdate();
            }

            sendJson(exchange, "{\"status\":\"Fine Paid\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, "{\"error\":\"" + esc(e.getMessage()) + "\"}", 500);
        }
    }

    // ═══════════════════════════════════════════════════
    // HELPER: Book ResultSet to JSON
    // ═══════════════════════════════════════════════════
    static String bookToJson(ResultSet rs) throws SQLException {
        return "{" +
                "\"id\":\"" + esc(rs.getString("id")) + "\"," +
                "\"title\":\"" + esc(rs.getString("title")) + "\"," +
                "\"author\":\"" + esc(rs.getString("author")) + "\"," +
                "\"isbn\":\"" + esc(rs.getString("isbn")) + "\"," +
                "\"cat\":\"" + esc(rs.getString("cat")) + "\"," +
                "\"qty\":" + rs.getInt("qty") + "," +
                "\"available\":" + rs.getInt("available") +
                "}";
    }

    // ═══════════════════════════════════════════════════
    // HELPER: Generic ResultSet to JSON (for members)
    // ═══════════════════════════════════════════════════
    static String resultSetToJson(ResultSet rs, String type) throws SQLException {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        while (rs.next()) {
            if (!first)
                json.append(",");
            first = false;
            if ("members".equals(type)) {
                json.append("{")
                        .append("\"id\":\"").append(esc(rs.getString("id"))).append("\",")
                        .append("\"name\":\"").append(esc(rs.getString("name"))).append("\",")
                        .append("\"email\":\"").append(esc(rs.getString("email"))).append("\",")
                        .append("\"phone\":\"").append(esc(rs.getString("phone"))).append("\",")
                        .append("\"type\":\"").append(esc(rs.getString("type"))).append("\",")
                        .append("\"status\":\"").append(esc(rs.getString("status"))).append("\",")
                        .append("\"gender\":\"").append(esc(rs.getString("gender"))).append("\",")
                        .append("\"dob\":\"").append(rs.getString("dob")).append("\",")
                        .append("\"nic\":\"").append(esc(rs.getString("nic"))).append("\",")
                        .append("\"address\":\"").append(esc(rs.getString("address"))).append("\",")
                        .append("\"start_date\":\"").append(rs.getString("start_date")).append("\",")
                        .append("\"expiry_date\":\"").append(rs.getString("expiry_date")).append("\"")
                        .append("}");
            }
        }
        json.append("]");
        return json.toString();
    }

    // ═══════════════════════════════════════════════════
    // HELPER: Issue ResultSet to JSON
    // ═══════════════════════════════════════════════════
    static String issueResultSetToJson(ResultSet rs) throws SQLException {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        while (rs.next()) {
            if (!first)
                json.append(",");
            first = false;
            json.append("{")
                    .append("\"issue_id\":").append(rs.getInt("issue_id")).append(",")
                    .append("\"book_id\":\"").append(esc(rs.getString("book_id"))).append("\",")
                    .append("\"book_title\":\"").append(esc(rs.getString("book_title"))).append("\",")
                    .append("\"member_id\":\"").append(esc(rs.getString("member_id"))).append("\",")
                    .append("\"member_name\":\"").append(esc(rs.getString("member_name"))).append("\",")
                    .append("\"issue_date\":\"").append(rs.getString("issue_date")).append("\",")
                    .append("\"due_date\":\"").append(rs.getString("due_date")).append("\",")
                    .append("\"status\":\"").append(esc(rs.getString("status"))).append("\"")
                    .append("}");
        }
        json.append("]");
        return json.toString();
    }

    // ═══════════════════════════════════════════════════
    // HELPERS: HTTP utilities
    // ═══════════════════════════════════════════════════
    static void sendJson(HttpExchange exchange, String json) throws IOException {
        sendJson(exchange, json, 200);
    }

    static void sendJson(HttpExchange exchange, String json, int code) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            try {
                exchange.sendResponseHeaders(204, -1);
            } catch (IOException ignored) {
            }
        }
    }

    static String readBody(HttpExchange exchange) throws IOException {
        return new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining());
    }

    // Safe integer parser (prevents NumberFormatException crash)
    static int safeParseInt(String value) {
        if (value == null || value.trim().isEmpty())
            return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Safe JSON value extractor (handles whitespace and edge cases)
    static String getValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1)
            return "";
        start += pattern.length();

        // Skip whitespace
        while (start < json.length() && json.charAt(start) == ' ')
            start++;

        if (start >= json.length())
            return "";

        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf('"', start);
            if (end == -1)
                return "";
            return json.substring(start, end);
        } else {
            int end = json.indexOf(",", start);
            if (end == -1)
                end = json.indexOf("}", start);
            if (end == -1)
                return "";
            return json.substring(start, end).trim();
        }
    }

    // Escape strings for JSON (prevents JSON injection crashes)
    static String esc(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    static String getContentType(String path) {
        if (path.endsWith(".html"))
            return "text/html";
        if (path.endsWith(".css"))
            return "text/css";
        if (path.endsWith(".js"))
            return "application/javascript";
        return "text/plain";
    }
}