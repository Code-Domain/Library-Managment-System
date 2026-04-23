# 📚 Library Management System

A complete **Library Management System** built using **Java (Backend)** and **HTML, CSS, JavaScript (Frontend)** with **SQLite database**.  
This project provides a modern dashboard interface for managing books, members, issuing, returning, and fine tracking.

---

## 🧾 Overview

The Library Management System is designed to simplify the process of managing a library.  
It includes a login interface and a fully interactive dashboard where administrators can perform operations such as:

- Managing books and members
- Issuing and returning books
- Tracking overdue books and fines
- Navigating through different modules using a sidebar UI

This project is ideal for learning **full-stack development using Java and frontend technologies**.

---

## 🎯 Objectives

- Provide a simple and user-friendly interface
- Implement core library operations
- Use SQLite for lightweight database management
- Demonstrate integration between frontend and backend
- Build a complete working system without frameworks

---

## 🏗️ Project Structure

```

libraryManagementSystem/
│
├── Database/
│   ├── library.db              # SQLite database
│   └── library.sqbpro         # SQLite project file
│
├── Frontend/
│   ├── login.html             # Login page
│   └── library_mainpage.html  # Dashboard page
│
├── Main.java                  # Backend server logic
├── sqlite-jdbc-3.45.1.0.jar   # SQLite JDBC driver
├── slf4j-api-2.0.9.jar        # Logging API
├── slf4j-simple-2.0.17.jar    # Logging implementation
└── README.md

```

---

## 🛠️ Technologies Used

### Frontend
- HTML5
- CSS3
- JavaScript (Vanilla JS)

### Backend
- Java
- JDBC (Java Database Connectivity)

### Database
- SQLite

### Libraries
- sqlite-jdbc
- slf4j (for logging)

---

## ✨ Features

### 🔐 Authentication
- Simple login page with username and password fields

### 📊 Dashboard
- Clean and responsive UI
- Overview of system operations
- Dynamic sections for data visualization

### 📚 Book Management
- Add, view, and manage books
- Track availability and stock

### 👥 Member Management
- Register and manage members
- Store member details

### 🔄 Issue & Return System
- Issue books to members
- Return books
- Maintain transaction records

### ⏰ Overdue & Fine Management
- Track overdue books
- Calculate fines
- Payment status tracking

### 📂 Navigation System
- Sidebar menu for quick navigation
- Multiple sections like:
  - Books
  - Members
  - Categories
  - Issued Books
  - Returned Books
  - Fines

### 👤 Admin Controls
- Admin dropdown menu
- Logout functionality

---

## ▶️ How to Run the Project

### Step 1: Setup Backend

1. Open the project in **Eclipse / IntelliJ IDEA / any Java IDE**
2. Add the following JAR files to the project build path:
   - `sqlite-jdbc-3.45.1.0.jar`
   - `slf4j-api-2.0.9.jar`
   - `slf4j-simple-2.0.17.jar`
3. Make sure `library.db` is correctly placed inside the `Database` folder
4. Run:

```

Main.java

```

This will start your backend server.

---

### Step 2: Run Frontend

1. Navigate to the `Frontend` folder
2. Open:

```

login.html

```

3. Enter login credentials (demo/static)
4. After login, open:

```

library_mainpage.html

```

---

## 🌐 How It Works

1. Backend (Java) handles:
   - Database operations
   - Business logic

2. Frontend (HTML/JS):
   - Displays UI
   - Handles user interactions

3. SQLite:
   - Stores all data locally

---

## 📸 Screenshots

### 🔐 Login Page
![Login Page](images/login.png)

### 📊 Dashboard
![Dashboard](images/dashboard.png)

### 📂 Sidebar Menu
![Sidebar](images/sidebar.png)

### 👤 Admin Dropdown
![Admin](images/admin.png)

---

## ⚠️ Important Notes

- Backend must be running before using the frontend
- This project currently uses **static frontend logic**
- SQLite database is stored locally
- No deployment configured by default

---

## 🚀 Future Enhancements

- Deploy backend to cloud (Render / Railway)
- Host frontend using GitHub Pages
- Add real authentication system
- Convert project into REST API architecture
- Add search, filters, and reports
- Improve UI/UX with frameworks like React or Bootstrap
- Implement role-based access control

---

## 🔒 Limitations

- No real authentication system
- Manual navigation between pages
- Not fully connected frontend & backend (can be improved)
- Local database only

---

## 💡 Learning Outcomes

This project helps in understanding:

- Java + JDBC integration
- SQLite database handling
- Frontend UI development
- Full-stack project structure
- Real-world system design basics

---

## 👨‍💻 Author

Developed as a personal project for learning and practice by Code Domain.

---

## 📌 Conclusion

This Library Management System demonstrates how a complete application can be built using **core technologies without frameworks**.  
It serves as a strong foundation for building more advanced and scalable systems in the future.
