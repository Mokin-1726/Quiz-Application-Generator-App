import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizApplication {
    private static DatabaseManager dbManager;
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            dbManager = new DatabaseManager();
            dbManager.initializeDatabase();
            SwingUtilities.invokeLater(() -> new LoginFrame());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error initializing application: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    public static DatabaseManager getDbManager() { return dbManager; }
}

class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "quiz_app_db";
    private static final String USER = "root";
    private static final String PASS = "Mokin@1726"; // CHANGE THIS
    private Connection conn;
    
    public DatabaseManager() throws SQLException {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); } 
        catch (ClassNotFoundException e) { throw new SQLException("MySQL JDBC Driver not found.", e); }
    }
    
    public void initializeDatabase() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        stmt.close();
        conn.close();
        conn = DriverManager.getConnection(DB_URL + DB_NAME, USER, PASS);
        createTables();
    }
    
    private void createTables() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE NOT NULL, password VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL, full_name VARCHAR(100), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS quizzes (id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(200) NOT NULL, description TEXT, creator_id INT, time_limit INT DEFAULT 30, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (creator_id) REFERENCES users(id))");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS questions (id INT AUTO_INCREMENT PRIMARY KEY, quiz_id INT, question_text TEXT NOT NULL, option_a VARCHAR(500), option_b VARCHAR(500), option_c VARCHAR(500), option_d VARCHAR(500), correct_answer CHAR(1), FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE)");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS quiz_results (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, quiz_id INT, score INT, total_questions INT, completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES users(id), FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE)");
        stmt.close();
        insertDefaultUser();
    }
    
    private void insertDefaultUser() throws SQLException {
        String checkUser = "SELECT COUNT(*) FROM users WHERE username = ?";
        PreparedStatement ps = conn.prepareStatement(checkUser);
        ps.setString(1, "admin");
        ResultSet rs = ps.executeQuery();
        rs.next();
        if (rs.getInt(1) == 0) {
            String insertUser = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)";
            PreparedStatement insertPs = conn.prepareStatement(insertUser);
            insertPs.setString(1, "admin");
            insertPs.setString(2, "admin123");
            insertPs.setString(3, "ADMIN");
            insertPs.setString(4, "Administrator");
            insertPs.executeUpdate();
            insertPs.close();
        }
        rs.close();
        ps.close();
    }
    
    public Connection getConnection() { return conn; }
    
    public User authenticateUser(String username, String password) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, username);
        ps.setString(2, password);
        ResultSet rs = ps.executeQuery();
        User user = null;
        if (rs.next()) {
            user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"), rs.getString("full_name"));
        }
        rs.close();
        ps.close();
        return user;
    }
    
    public boolean registerUser(String username, String password, String role, String fullName) throws SQLException {
        String query = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, fullName);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) return false;
            throw e;
        }
    }
    
    public boolean hasUserCompletedQuiz(int userId, int quizId) throws SQLException {
        String query = "SELECT COUNT(*) FROM quiz_results WHERE user_id = ? AND quiz_id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, userId);
        ps.setInt(2, quizId);
        ResultSet rs = ps.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        ps.close();
        return count > 0;
    }
}

class User {
    private int id;
    private String username, role, fullName;
    
    public User(int id, String username, String role, String fullName) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }
    
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isTeacher() { return "TEACHER".equals(role); }
}

class Quiz {
    private int id;
    private String title, description;
    private int creatorId, timeLimit;
    
    public Quiz(int id, String title, String description, int creatorId, int timeLimit) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.creatorId = creatorId;
        this.timeLimit = timeLimit;
    }
    
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getCreatorId() { return creatorId; }
    public int getTimeLimit() { return timeLimit; }
}

class Question {
    private int id, quizId;
    private String questionText, optionA, optionB, optionC, optionD;
    private char correctAnswer;
    
    public Question(int id, int quizId, String questionText, String optionA, String optionB, String optionC, String optionD, char correctAnswer) {
        this.id = id;
        this.quizId = quizId;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
    }
    
    public int getId() { return id; }
    public int getQuizId() { return quizId; }
    public String getQuestionText() { return questionText; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
    public char getCorrectAnswer() { return correctAnswer; }
}

class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;
    private JComboBox<String> languageCombo;
    private JToggleButton darkModeToggle;
    private boolean isDarkMode = false;
    
    private Color lightBg = new Color(230, 240, 255);
    private Color lightPanel = new Color(255, 255, 255);
    private Color lightText = Color.BLACK;
    private Color darkBg = new Color(30, 35, 45);
    private Color darkPanel = new Color(45, 52, 65);
    private Color darkText = new Color(240, 240, 240);
    private Color primaryBtn = new Color(67, 160, 71);
    private Color primaryBtnHover = new Color(56, 142, 60);
    private Color secondaryBtn = new Color(66, 165, 245);
    private Color secondaryBtnHover = new Color(30, 136, 229);
    private Color accentColor = new Color(156, 39, 176);
    
    public LoginFrame() {
        setTitle("Quiz Application - Login");
        setSize(550, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        initComponents();
        applyTheme();
        setVisible(true);
    }
    
    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setBackground(new Color(156, 39, 176));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel langLabel = new JLabel("Language: ");
        langLabel.setForeground(Color.WHITE);
        langLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        String[] languages = {"English", "Español", "हिन्दी"};
        languageCombo = new JComboBox<>(languages);
        languageCombo.setBackground(Color.WHITE);
        languageCombo.setFont(new Font("Arial", Font.PLAIN, 12));
        
        darkModeToggle = new JToggleButton("🌙");
        darkModeToggle.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        darkModeToggle.setFocusPainted(false);
        darkModeToggle.setBackground(new Color(255, 193, 7));
        darkModeToggle.setForeground(Color.BLACK);
        darkModeToggle.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        darkModeToggle.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            darkModeToggle.setText(isDarkMode ? "☀️" : "🌙");
            darkModeToggle.setBackground(isDarkMode ? new Color(255, 235, 59) : new Color(255, 193, 7));
            applyTheme();
        });
        
        topPanel.add(langLabel);
        topPanel.add(languageCombo);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(darkModeToggle);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
        
        JLabel titleLabel = new JLabel("🎓 Quiz Application", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(156, 39, 176));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Welcome! Please login to continue", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        subtitleLabel.setForeground(new Color(100, 100, 100));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.setOpaque(false);
        JLabel userLabel = new JLabel("👤 Username:");
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userLabel.setForeground(new Color(66, 165, 245));
        userPanel.add(userLabel);
        
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 15));
        usernameField.setMaximumSize(new Dimension(320, 40));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(66, 165, 245), 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        JPanel passPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passPanel.setOpaque(false);
        JLabel passLabel = new JLabel("🔒 Password:");
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        passLabel.setForeground(new Color(67, 160, 71));
        passPanel.add(passLabel);
        
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 15));
        passwordField.setMaximumSize(new Dimension(320, 40));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(67, 160, 71), 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        loginButton = new JButton("🚀 Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginButton.setPreferredSize(new Dimension(300, 45));
        loginButton.setMaximumSize(new Dimension(300, 45));
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setBackground(primaryBtn);
        loginButton.setForeground(Color.WHITE);
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        loginButton.addActionListener(e -> handleLogin());
        
        registerButton = new JButton("📝 Register New Account");
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));
        registerButton.setPreferredSize(new Dimension(300, 40));
        registerButton.setMaximumSize(new Dimension(300, 40));
        registerButton.setFocusPainted(false);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.setBackground(secondaryBtn);
        registerButton.setForeground(Color.WHITE);
        registerButton.setOpaque(true);
        registerButton.setBorderPainted(false);
        registerButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        registerButton.addActionListener(e -> new RegistrationDialog(this, isDarkMode));
        
        addColorfulHoverEffect(loginButton, primaryBtn, primaryBtnHover);
        addColorfulHoverEffect(registerButton, secondaryBtn, secondaryBtnHover);
        
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(subtitleLabel);
        centerPanel.add(Box.createVerticalStrut(40));
        centerPanel.add(userPanel);
        centerPanel.add(Box.createVerticalStrut(5));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(usernameField);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(passPanel);
        centerPanel.add(Box.createVerticalStrut(5));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(passwordField);
        centerPanel.add(Box.createVerticalStrut(35));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(loginButton);
        buttonPanel.add(Box.createVerticalStrut(12));
        buttonPanel.add(registerButton);
        
        centerPanel.add(buttonPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel);
        passwordField.addActionListener(e -> handleLogin());
    }
    
    private void addColorfulHoverEffect(JButton button, Color normal, Color hover) {
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(hover); }
            public void mouseExited(MouseEvent e) { button.setBackground(normal); }
        });
    }
    
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            User user = QuizApplication.getDbManager().authenticateUser(username, password);
            if (user != null) {
                this.dispose();
                new DashboardFrame(user, isDarkMode);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void applyTheme() {
        Color bgColor = isDarkMode ? darkBg : lightBg;
        Color panelColor = isDarkMode ? darkPanel : lightPanel;
        Color textColor = isDarkMode ? darkText : lightText;
        getContentPane().setBackground(bgColor);
        for (Component comp : ((JPanel)getContentPane().getComponent(0)).getComponents()) {
            applyThemeToComponent(comp, bgColor, panelColor, textColor);
        }
        loginButton.setBackground(primaryBtn);
        loginButton.setForeground(Color.WHITE);
        registerButton.setBackground(secondaryBtn);
        registerButton.setForeground(Color.WHITE);
        repaint();
    }
    
    private void applyThemeToComponent(Component comp, Color bg, Color panel, Color text) {
        if (comp instanceof JPanel && !((JPanel)comp).getBackground().equals(new Color(156, 39, 176))) {
            comp.setBackground(bg);
        }
        comp.setForeground(text);
        if (comp instanceof JPanel) {
            JPanel p = (JPanel) comp;
            for (Component c : p.getComponents()) {
                applyThemeToComponent(c, bg, panel, text);
            }
        } else if (comp instanceof JTextField || comp instanceof JPasswordField) {
            comp.setBackground(panel);
            comp.setForeground(text);
        } else if (comp instanceof JComboBox) {
            comp.setBackground(Color.WHITE);
            comp.setForeground(Color.BLACK);
        } else if (comp instanceof JLabel && !((JLabel)comp).getText().contains("🎓")) {
            comp.setForeground(text);
        }
    }
}

class RegistrationDialog extends JDialog {
    private JTextField usernameField, fullNameField;
    private JPasswordField passwordField, confirmPasswordField;
    private JComboBox<String> roleCombo;
    private boolean isDarkMode;
    
    private Color primaryBtn = new Color(67, 160, 71);
    private Color secondaryBtn = new Color(244, 67, 54);
    
    public RegistrationDialog(JFrame parent, boolean darkMode) {
        super(parent, "Register New Account", true);
        this.isDarkMode = darkMode;
        setSize(500, 580);
        setLocationRelativeTo(parent);
        setResizable(false);
        initComponents();
        applyTheme();
        setVisible(true);
    }
    
    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        JLabel titleLabel = new JLabel("✨ Create New Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(156, 39, 176));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        fullNameField = new JTextField(20);
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        confirmPasswordField = new JPasswordField(20);
        
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(30));
        
        addColorfulLabelAndField(mainPanel, "👤 Full Name:", fullNameField, new Color(255, 152, 0));
        addColorfulLabelAndField(mainPanel, "🆔 Username:", usernameField, new Color(66, 165, 245));
        addColorfulLabelAndField(mainPanel, "🔒 Password:", passwordField, new Color(76, 175, 80));
        addColorfulLabelAndField(mainPanel, "✅ Confirm Password:", confirmPasswordField, new Color(156, 39, 176));
        
        JLabel roleLabel = new JLabel("🎭 Role:");
        roleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        roleLabel.setForeground(new Color(233, 30, 99));
        String[] roles = {"STUDENT", "TEACHER"};
        roleCombo = new JComboBox<>(roles);
        roleCombo.setMaximumSize(new Dimension(330, 38));
        roleCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        roleCombo.setBackground(Color.WHITE);
        
        mainPanel.add(roleLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(roleCombo);
        mainPanel.add(Box.createVerticalStrut(30));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        
        JButton registerBtn = new JButton("✅ Register");
        registerBtn.setFont(new Font("Arial", Font.BOLD, 14));
        registerBtn.setPreferredSize(new Dimension(140, 40));
        registerBtn.setBackground(primaryBtn);
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        registerBtn.setOpaque(true);
        registerBtn.setBorderPainted(false);
        registerBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        registerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton cancelBtn = new JButton("❌ Cancel");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 14));
        cancelBtn.setPreferredSize(new Dimension(140, 40));
        cancelBtn.setBackground(secondaryBtn);
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setOpaque(true);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        registerBtn.addActionListener(e -> handleRegistration());
        cancelBtn.addActionListener(e -> dispose());
        
        buttonPanel.add(registerBtn);
        buttonPanel.add(cancelBtn);
        mainPanel.add(buttonPanel);
        add(mainPanel);
    }
    
    private void addColorfulLabelAndField(JPanel panel, String label, JTextField field, Color borderColor) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        lbl.setForeground(borderColor);
        field.setMaximumSize(new Dimension(330, 38));
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 2),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(5));
        panel.add(field);
        panel.add(Box.createVerticalStrut(15));
    }
    
    private void handleRegistration() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String role = (String) roleCombo.getSelectedItem();
        
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            boolean success = QuizApplication.getDbManager().registerUser(username, password, role, fullName);
            if (success) {
                JOptionPane.showMessageDialog(this, "Registration successful! You can now login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Username already exists", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void applyTheme() {
        if (isDarkMode) {
            getContentPane().setBackground(new Color(30, 35, 45));
            JPanel mainPanel = (JPanel) getContentPane().getComponent(0);
            mainPanel.setBackground(new Color(30, 35, 45));
            for (Component comp : mainPanel.getComponents()) {
                if (comp instanceof JTextField || comp instanceof JPasswordField) {
                    comp.setBackground(new Color(45, 52, 65));
                    comp.setForeground(Color.WHITE);
                } else if (comp instanceof JLabel) {
                    comp.setForeground(Color.WHITE);
                }
            }
        }
    }
}
class DashboardFrame extends JFrame {
    private User currentUser;
    private boolean isDarkMode;
    private JPanel mainPanel;
    
    private Color primaryBtn = new Color(67, 160, 71);
    private Color secondaryBtn = new Color(66, 165, 245);
    private Color warningBtn = new Color(255, 152, 0);
    private Color dangerBtn = new Color(244, 67, 54);
    private Color accentColor = new Color(156, 39, 176);
    private Color darkBg = new Color(30, 35, 45);
    private Color darkPanel = new Color(45, 52, 65);
    private Color lightBg = new Color(230, 240, 255);
    private Color lightPanel = Color.WHITE;
    
    public DashboardFrame(User user, boolean darkMode) {
        this.currentUser = user;
        this.isDarkMode = darkMode;
        setTitle("Quiz Dashboard - " + user.getFullName());
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
        applyTheme();
        setVisible(true);
    }
    
    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(accentColor);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel welcomeLabel = new JLabel("👋 Welcome, " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        welcomeLabel.setForeground(Color.WHITE);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setOpaque(false);
        
        JToggleButton darkModeBtn = new JToggleButton(isDarkMode ? "☀️" : "🌙");
        darkModeBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        darkModeBtn.setSelected(isDarkMode);
        darkModeBtn.setBackground(new Color(255, 193, 7));
        darkModeBtn.setForeground(Color.BLACK);
        darkModeBtn.setFocusPainted(false);
        darkModeBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        darkModeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        darkModeBtn.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            darkModeBtn.setText(isDarkMode ? "☀️" : "🌙");
            applyTheme();
        });
        
        JButton logoutBtn = new JButton("🚪 Logout");
        logoutBtn.setFont(new Font("Arial", Font.BOLD, 14));
        logoutBtn.setBackground(dangerBtn);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setOpaque(true);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logout());
        
        controlPanel.add(darkModeBtn);
        controlPanel.add(logoutBtn);
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(controlPanel, BorderLayout.EAST);
        
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        
        if (currentUser.isTeacher() || currentUser.isAdmin()) {
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
            tabbedPane.addTab("📝 Available Quizzes", createQuizListPanel());
            tabbedPane.addTab("➕ Create Quiz", createCreateQuizPanel());
            tabbedPane.addTab("📊 My Results", createResultsPanel());
            centerPanel.add(tabbedPane);
        } else {
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
            tabbedPane.addTab("📝 Available Quizzes", createQuizListPanel());
            tabbedPane.addTab("📊 My Results", createResultsPanel());
            centerPanel.add(tabbedPane);
        }
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel);
    }
    
    private JPanel createQuizListPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Available Quizzes");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        
        String[] columns = {"ID", "Title", "Description", "Questions", "Time (min)", "Status", "Action"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };
        
        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        
        if (isDarkMode) {
            table.setSelectionBackground(new Color(66, 165, 245));
            table.setSelectionForeground(Color.WHITE);
        } else {
            table.setSelectionBackground(new Color(184, 207, 229));
            table.setSelectionForeground(Color.BLACK);
        }
        
        loadQuizzes(model);
        
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox(), table, model, currentUser, this, isDarkMode));
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(new Font("Arial", Font.BOLD, 13));
        refreshBtn.setBackground(secondaryBtn);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setOpaque(true);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            loadQuizzes(model);
            table.repaint();
            table.revalidate();
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshBtn);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void loadQuizzes(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            Connection conn = QuizApplication.getDbManager().getConnection();
            String query = "SELECT q.id, q.title, q.description, q.time_limit, q.creator_id, COUNT(qu.id) as question_count FROM quizzes q LEFT JOIN questions qu ON q.id = qu.quiz_id GROUP BY q.id";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                int quizId = rs.getInt("id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                int questionCount = rs.getInt("question_count");
                int timeLimit = rs.getInt("time_limit");
                int creatorId = rs.getInt("creator_id");
                
                boolean completed = QuizApplication.getDbManager().hasUserCompletedQuiz(currentUser.getId(), quizId);
                String status = completed ? "✓ Completed" : "Available";
                
                String action = "";
                if (creatorId == currentUser.getId() || currentUser.isAdmin()) {
                    action = "Manage/Delete";
                } else if (!completed) {
                    action = "Start";
                } else {
                    action = "View Result";
                }
                
                model.addRow(new Object[]{quizId, title, description, questionCount, timeLimit, status, action});
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading quizzes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createCreateQuizPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Create New Quiz");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        
        JTextField titleField = new JTextField(30);
        JTextArea descArea = new JTextArea(3, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JSpinner timeLimitSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 120, 5));
        
        addFormField(formPanel, "Quiz Title:", titleField);
        addFormField(formPanel, "Description:", new JScrollPane(descArea));
        addFormField(formPanel, "Time Limit (minutes):", timeLimitSpinner);
        
        JButton createBtn = new JButton("✨ Create Quiz");
        createBtn.setFont(new Font("Arial", Font.BOLD, 16));
        createBtn.setBackground(primaryBtn);
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.setOpaque(true);
        createBtn.setBorderPainted(false);
        createBtn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        createBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String desc = descArea.getText().trim();
            int timeLimit = (int) timeLimitSpinner.getValue();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter quiz title");
                return;
            }
            createQuiz(title, desc, timeLimit);
        });
        
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(createBtn);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }
    
    private void addFormField(JPanel panel, String label, Component field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (field instanceof JComponent) {
            ((JComponent) field).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(5));
        panel.add(field);
        panel.add(Box.createVerticalStrut(15));
    }
    
    private void createQuiz(String title, String description, int timeLimit) {
        try {
            Connection conn = QuizApplication.getDbManager().getConnection();
            String query = "INSERT INTO quizzes (title, description, creator_id, time_limit) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setInt(3, currentUser.getId());
            ps.setInt(4, timeLimit);
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    int quizId = keys.getInt(1);
                    JOptionPane.showMessageDialog(this, "Quiz created successfully! Now add questions.");
                    new QuizEditorFrame(quizId, title, currentUser, isDarkMode, this);
                }
                keys.close();
            }
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error creating quiz: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("My Quiz Results");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        
        String[] columns = {"Quiz Title", "Score", "Total Questions", "Percentage", "Date"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        
        if (isDarkMode) {
            table.setSelectionBackground(new Color(66, 165, 245));
            table.setSelectionForeground(Color.WHITE);
        } else {
            table.setSelectionBackground(new Color(184, 207, 229));
            table.setSelectionForeground(Color.BLACK);
        }
        
        loadResults(model);
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private void loadResults(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            Connection conn = QuizApplication.getDbManager().getConnection();
            String query = "SELECT q.title, qr.score, qr.total_questions, qr.completed_at FROM quiz_results qr JOIN quizzes q ON qr.quiz_id = q.id WHERE qr.user_id = ? ORDER BY qr.completed_at DESC";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String title = rs.getString("title");
                int score = rs.getInt("score");
                int total = rs.getInt("total_questions");
                String date = rs.getString("completed_at");
                double percentage = (double) score / total * 100;
                model.addRow(new Object[]{title, score, total, String.format("%.1f%%", percentage), date});
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading results: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame();
        }
    }
    
    private void applyTheme() {
        Color bg = isDarkMode ? darkBg : lightBg;
        Color panel = isDarkMode ? darkPanel : lightPanel;
        Color text = isDarkMode ? Color.WHITE : Color.BLACK;
        
        getContentPane().setBackground(bg);
        mainPanel.setBackground(bg);
        applyThemeRecursive(mainPanel, bg, panel, text);
        repaint();
    }
    
    private void applyThemeRecursive(Container container, Color bg, Color panel, Color text) {
        if (!(container.getBackground().equals(accentColor))) {
            container.setBackground(bg);
        }
        container.setForeground(text);
        
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                Color btnBg = btn.getBackground();
                if (btnBg.equals(primaryBtn) || btnBg.equals(secondaryBtn) || btnBg.equals(warningBtn) || btnBg.equals(dangerBtn) || btnBg.equals(new Color(255, 193, 7))) {
                } else {
                    btn.setBackground(secondaryBtn);
                }
                comp.setForeground(Color.WHITE);
            } else if (comp instanceof JPanel) {
                comp.setBackground(bg);
                comp.setForeground(text);
            } else if (comp instanceof JLabel) {
                comp.setForeground(text);
            } else if (comp instanceof JTextField || comp instanceof JTextArea) {
                comp.setBackground(panel);
                comp.setForeground(text);
            } else if (comp instanceof JTable) {
                JTable table = (JTable) comp;
                table.setBackground(panel);
                table.setForeground(text);
                table.setGridColor(isDarkMode ? new Color(70, 80, 95) : Color.LIGHT_GRAY);
                table.getTableHeader().setBackground(isDarkMode ? new Color(50, 57, 70) : Color.LIGHT_GRAY);
                table.getTableHeader().setForeground(text);
                
                if (isDarkMode) {
                    table.setSelectionBackground(new Color(66, 165, 245));
                    table.setSelectionForeground(Color.WHITE);
                } else {
                    table.setSelectionBackground(new Color(184, 207, 229));
                    table.setSelectionForeground(Color.BLACK);
                }
            } else {
                comp.setBackground(panel);
                comp.setForeground(text);
            }
            
            if (comp instanceof Container) {
                applyThemeRecursive((Container) comp, bg, panel, text);
            }
        }
    }
}

class QuizEditorFrame extends JFrame {
    private int quizId;
    private String quizTitle;
    private User creator;
    private boolean isDarkMode;
    private JFrame parentFrame;
    private DefaultTableModel questionTableModel;
    
    private Color primaryBtn = new Color(67, 160, 71);
    private Color dangerBtn = new Color(244, 67, 54);
    
    public QuizEditorFrame(int quizId, String quizTitle, User creator, boolean isDarkMode, JFrame parent) {
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.creator = creator;
        this.isDarkMode = isDarkMode;
        this.parentFrame = parent;
        
        setTitle("Quiz Editor - " + quizTitle);
        setSize(1100, 700);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        initComponents();
        loadQuestions();
        setVisible(true);
    }
    
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Managing: " + quizTitle);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(closeBtn, BorderLayout.EAST);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(650);
        
        JPanel listPanel = createQuestionsListPanel();
        JPanel formPanel = createAddQuestionPanel();
        
        splitPane.setLeftComponent(listPanel);
        splitPane.setRightComponent(formPanel);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        add(mainPanel);
        
        if (isDarkMode) {
            applyDarkTheme(mainPanel);
        }
    }
    
    private JPanel createQuestionsListPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        JLabel label = new JLabel("Questions in this Quiz");
        label.setFont(new Font("Arial", Font.BOLD, 16));
        
        String[] columns = {"ID", "Question", "Correct Answer", "Actions"};
        questionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };
        
        JTable table = new JTable(questionTableModel);
        table.setRowHeight(50);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        
        if (isDarkMode) {
            table.setSelectionBackground(new Color(66, 165, 245));
            table.setSelectionForeground(Color.WHITE);
        } else {
            table.setSelectionBackground(new Color(184, 207, 229));
            table.setSelectionForeground(Color.BLACK);
        }
        
        table.getColumn("Actions").setCellRenderer(new DeleteButtonRenderer());
        table.getColumn("Actions").setCellEditor(new DeleteButtonEditor(new JCheckBox(), this));
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        panel.add(label, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createAddQuestionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Add New Question");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel qLabel = new JLabel("Question:");
        qLabel.setFont(new Font("Arial", Font.BOLD, 13));
        JTextArea questionArea = new JTextArea(3, 25);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JScrollPane qScroll = new JScrollPane(questionArea);
        qScroll.setMaximumSize(new Dimension(400, 80));
        qScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel optALabel = new JLabel("Option A:");
        optALabel.setFont(new Font("Arial", Font.BOLD, 13));
        JTextField optAField = new JTextField(25);
        optAField.setMaximumSize(new Dimension(400, 30));
        
        JLabel optBLabel = new JLabel("Option B:");
        optBLabel.setFont(new Font("Arial", Font.BOLD, 13));
        JTextField optBField = new JTextField(25);
        optBField.setMaximumSize(new Dimension(400, 30));
        
        JLabel optCLabel = new JLabel("Option C:");
        optCLabel.setFont(new Font("Arial", Font.BOLD, 13));
        JTextField optCField = new JTextField(25);
        optCField.setMaximumSize(new Dimension(400, 30));
        
        JLabel optDLabel = new JLabel("Option D:");
        optDLabel.setFont(new Font("Arial", Font.BOLD, 13));
        JTextField optDField = new JTextField(25);
        optDField.setMaximumSize(new Dimension(400, 30));
        
        JLabel correctLabel = new JLabel("Correct Answer:");
        correctLabel.setFont(new Font("Arial", Font.BOLD, 13));
        
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup group = new ButtonGroup();
        JRadioButton radioA = new JRadioButton("A");
        JRadioButton radioB = new JRadioButton("B");
        JRadioButton radioC = new JRadioButton("C");
        JRadioButton radioD = new JRadioButton("D");
        
        group.add(radioA);
        group.add(radioB);
        group.add(radioC);
        group.add(radioD);
        
        radioPanel.add(radioA);
        radioPanel.add(radioB);
        radioPanel.add(radioC);
        radioPanel.add(radioD);
        radioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        radioA.setSelected(true);
        
        JButton addBtn = new JButton("➕ Add Question");
        addBtn.setFont(new Font("Arial", Font.BOLD, 14));
        addBtn.setBackground(primaryBtn);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);
        addBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addBtn.setMaximumSize(new Dimension(220, 42));
        
        addBtn.addActionListener(e -> {
            String question = questionArea.getText().trim();
            String optA = optAField.getText().trim();
            String optB = optBField.getText().trim();
            String optC = optCField.getText().trim();
            String optD = optDField.getText().trim();
            
            char correct = 'A';
            if (radioB.isSelected()) correct = 'B';
            else if (radioC.isSelected()) correct = 'C';
            else if (radioD.isSelected()) correct = 'D';
            
            if (question.isEmpty() || optA.isEmpty() || optB.isEmpty() || optC.isEmpty() || optD.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (addQuestion(question, optA, optB, optC, optD, correct)) {
                questionArea.setText("");
                optAField.setText("");
                optBField.setText("");
                optCField.setText("");
                optDField.setText("");
                radioA.setSelected(true);
                loadQuestions();
            }
        });
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(qLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(qScroll);
        panel.add(Box.createVerticalStrut(10));
        panel.add(optALabel);
        panel.add(Box.createVerticalStrut(3));
        panel.add(optAField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(optBLabel);
        panel.add(Box.createVerticalStrut(3));
        panel.add(optBField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(optCLabel);
        panel.add(Box.createVerticalStrut(3));
        panel.add(optCField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(optDLabel);
        panel.add(Box.createVerticalStrut(3));
        panel.add(optDField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(correctLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(radioPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(addBtn);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private boolean addQuestion(String question, String optA, String optB, String optC, String optD, char correct) {
        try {
            Connection conn = QuizApplication.getDbManager().getConnection();
            String query = "INSERT INTO questions (quiz_id, question_text, option_a, option_b, option_c, option_d, correct_answer) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, quizId);
            ps.setString(2, question);
            ps.setString(3, optA);
            ps.setString(4, optB);
            ps.setString(5, optC);
            ps.setString(6, optD);
            ps.setString(7, String.valueOf(correct));
            
            int affected = ps.executeUpdate();
            ps.close();
            
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Question added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding question: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
    
    private void loadQuestions() {
        questionTableModel.setRowCount(0);
        try {
            Connection conn = QuizApplication.getDbManager().getConnection();
            String query= "SELECT * FROM questions WHERE quiz_id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, quizId);
            ResultSet rs = ps.executeQuery();
while (rs.next()) {
            int id = rs.getInt("id");
            String question = rs.getString("question_text");
            char correct = rs.getString("correct_answer").charAt(0);
            String displayQ = question.length() > 50 ? question.substring(0, 47) + "..." : question;
            questionTableModel.addRow(new Object[]{id, displayQ, "Option " + correct, "Delete"});
        }
        rs.close();
        ps.close();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error loading questions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

public void deleteQuestion(int questionId) {
    int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this question?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
    if (confirm == JOptionPane.YES_OPTION) {
        try {
            Connection conn = QuizApplication.getDbManager().getConnection();
            String query = "DELETE FROM questions WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, questionId);
            int affected = ps.executeUpdate();
            ps.close();
            
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Question deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadQuestions();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting question: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

private void applyDarkTheme(Container container) {
    Color darkBg = new Color(30, 35, 45);
    Color darkPanel = new Color(45, 52, 65);
    
    container.setBackground(darkBg);
    container.setForeground(Color.WHITE);
    
    for (Component comp : container.getComponents()) {
        if (comp instanceof JButton) {
            JButton btn = (JButton) comp;
            if (!btn.getBackground().equals(primaryBtn) && !btn.getBackground().equals(dangerBtn)) {
                btn.setBackground(new Color(66, 165, 245));
            }
            comp.setForeground(Color.WHITE);
        } else if (comp instanceof JTextField || comp instanceof JTextArea) {
            comp.setBackground(darkPanel);
            comp.setForeground(Color.WHITE);
        } else if (comp instanceof JLabel) {
            comp.setForeground(Color.WHITE);
        } else {
            comp.setBackground(darkBg);
            comp.setForeground(Color.WHITE);
        }
        
        if (comp instanceof Container) {
            applyDarkTheme((Container) comp);
        }
    }
}
}
class QuizTakingFrame extends JFrame {
    private int quizId;
    private String quizTitle;
    private int timeLimit;
    private User student;
    private boolean isDarkMode;
    
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private char[] userAnswers;
    
    private JLabel timerLabel;
    private JLabel questionLabel;
    private JTextArea questionTextArea;
    private ButtonGroup optionGroup;
    private JRadioButton optionA, optionB, optionC, optionD;
    private JButton prevBtn, nextBtn, submitBtn;
    private JProgressBar progressBar;
    
    private TimerThread timerThread;
    private int remainingSeconds;
    
    private Color primaryBtn = new Color(67, 160, 71);
    private Color secondaryBtn = new Color(66, 165, 245);
    private Color warningBtn = new Color(255, 152, 0);
    private Color accentColor = new Color(156, 39, 176);
    private Color darkBg = new Color(30, 35, 45);
    private Color darkPanel = new Color(45, 52, 65);
    private Color lightBg = new Color(230, 240, 255);
    private Color lightPanel = Color.WHITE;
    
    public QuizTakingFrame(int quizId, String quizTitle, int timeLimit, User student, boolean isDarkMode) {
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.timeLimit = timeLimit;
        this.student = student;
        this.isDarkMode = isDarkMode;
        this.remainingSeconds = timeLimit * 60;
        
        setTitle("Taking Quiz: " + quizTitle);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        loadQuestions();
        
        if (questions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "This quiz has no questions yet!", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }
        
        userAnswers = new char[questions.size()];
        for (int i = 0; i < userAnswers.length; i++) {
            userAnswers[i] = ' ';
        }
        
        initComponents();
        displayQuestion(0);
        startTimer();
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(QuizTakingFrame.this, "Are you sure you want to exit? Your progress will be lost!", "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    stopTimer();
                    dispose();
                }
            }
        });
        
        setVisible(true);
    }
    
    private void loadQuestions() {
        questions = new ArrayList<>();
        try {
            Connection conn = QuizApplication.getDbManager().getConnection();
            String query = "SELECT * FROM questions WHERE quiz_id = ? ORDER BY id";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, quizId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                questions.add(new Question(
                    rs.getInt("id"),
                    rs.getInt("quiz_id"),
                    rs.getString("question_text"),
                    rs.getString("option_a"),
                    rs.getString("option_b"),
                    rs.getString("option_c"),
                    rs.getString("option_d"),
                    rs.getString("correct_answer").charAt(0)
                ));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading questions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JLabel titleLbl = new JLabel(quizTitle);
        titleLbl.setFont(new Font("Arial", Font.BOLD, 20));
        
        timerLabel = new JLabel("⏱️ Time: " + formatTime(remainingSeconds));
        timerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        timerLabel.setForeground(new Color(220, 53, 69));
        
        topPanel.add(titleLbl, BorderLayout.WEST);
        topPanel.add(timerLabel, BorderLayout.EAST);
        
        progressBar = new JProgressBar(0, questions.size());
        progressBar.setValue(1);
        progressBar.setStringPainted(true);
        progressBar.setString("Question 1 of " + questions.size());
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        questionLabel = new JLabel("Question 1:");
        questionLabel.setFont(new Font("Arial", Font.BOLD, 16));
        questionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        questionTextArea = new JTextArea(4, 50);
        questionTextArea.setFont(new Font("Arial", Font.PLAIN, 15));
        questionTextArea.setLineWrap(true);
        questionTextArea.setWrapStyleWord(true);
        questionTextArea.setEditable(false);
        questionTextArea.setFocusable(false);
        questionTextArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        questionTextArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        optionGroup = new ButtonGroup();
        
        optionA = createOptionButton("A");
        optionB = createOptionButton("B");
        optionC = createOptionButton("C");
        optionD = createOptionButton("D");
        
        optionGroup.add(optionA);
        optionGroup.add(optionB);
        optionGroup.add(optionC);
        optionGroup.add(optionD);
        
        optionsPanel.add(optionA);
        optionsPanel.add(Box.createVerticalStrut(12));
        optionsPanel.add(optionB);
        optionsPanel.add(Box.createVerticalStrut(12));
        optionsPanel.add(optionC);
        optionsPanel.add(Box.createVerticalStrut(12));
        optionsPanel.add(optionD);
        
        centerPanel.add(questionLabel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(questionTextArea);
        centerPanel.add(optionsPanel);
        centerPanel.add(Box.createVerticalGlue());
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        prevBtn = new JButton("⬅️ Previous");
        prevBtn.setFont(new Font("Arial", Font.BOLD, 14));
        prevBtn.setBackground(secondaryBtn);
        prevBtn.setForeground(Color.WHITE);
        prevBtn.setFocusPainted(false);
        prevBtn.setOpaque(true);
        prevBtn.setBorderPainted(false);
        prevBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        prevBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        prevBtn.setEnabled(false);
        prevBtn.addActionListener(e -> navigateQuestion(-1));
        
        nextBtn = new JButton("Next ➡️");
        nextBtn.setFont(new Font("Arial", Font.BOLD, 14));
        nextBtn.setBackground(secondaryBtn);
        nextBtn.setForeground(Color.WHITE);
        nextBtn.setFocusPainted(false);
        nextBtn.setOpaque(true);
        nextBtn.setBorderPainted(false);
        nextBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        nextBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextBtn.addActionListener(e -> navigateQuestion(1));
        
        submitBtn = new JButton("✅ Submit Quiz");
        submitBtn.setFont(new Font("Arial", Font.BOLD, 14));
        submitBtn.setBackground(primaryBtn);
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.setOpaque(true);
        submitBtn.setBorderPainted(false);
        submitBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        submitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitBtn.addActionListener(e -> submitQuiz());
        
        navPanel.add(prevBtn);
        navPanel.add(nextBtn);
        navPanel.add(submitBtn);
        
        bottomPanel.add(navPanel, BorderLayout.CENTER);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(progressBar, BorderLayout.AFTER_LINE_ENDS);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        if (isDarkMode) {
            applyDarkTheme(mainPanel);
        }
    }
    
    private JRadioButton createOptionButton(String option) {
        JRadioButton btn = new JRadioButton();
        btn.setFont(new Font("Arial", Font.PLAIN, 14));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> saveAnswer(option.charAt(0)));
        return btn;
    }
    
    private void displayQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;
        
        currentQuestionIndex = index;
        Question q = questions.get(index);
        
        questionLabel.setText("Question " + (index + 1) + ":");
        questionTextArea.setText(q.getQuestionText());
        
        optionA.setText("A. " + q.getOptionA());
        optionB.setText("B. " + q.getOptionB());
        optionC.setText("C. " + q.getOptionC());
        optionD.setText("D. " + q.getOptionD());
        
        optionGroup.clearSelection();
        char savedAnswer = userAnswers[index];
        if (savedAnswer == 'A') optionA.setSelected(true);
        else if (savedAnswer == 'B') optionB.setSelected(true);
        else if (savedAnswer == 'C') optionC.setSelected(true);
        else if (savedAnswer == 'D') optionD.setSelected(true);
        
        prevBtn.setEnabled(index > 0);
        nextBtn.setEnabled(index < questions.size() - 1);
        
        progressBar.setValue(index + 1);
        progressBar.setString("Question " + (index + 1) + " of " + questions.size());
    }
    
    private void saveAnswer(char answer) {
        userAnswers[currentQuestionIndex] = answer;
    }
    
    private void navigateQuestion(int direction) {
        int newIndex = currentQuestionIndex + direction;
        if (newIndex >= 0 && newIndex < questions.size()) {
            displayQuestion(newIndex);
        }
    }
    
    private void submitQuiz() {
        int unanswered = 0;
        for (char answer : userAnswers) {
            if (answer == ' ') unanswered++;
        }
        
        if (unanswered > 0) {
            int confirm = JOptionPane.showConfirmDialog(this, "You have " + unanswered + " unanswered questions. Submit anyway?", "Confirm Submit", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        
        stopTimer();
        
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (userAnswers[i] == questions.get(i).getCorrectAnswer()) {
                score++;
            }
        }
        
        saveResult(score);
        showResult(score);
        dispose();
    }
    
    private void saveResult(int score) {
        try {
            Connection conn = QuizApplication.getDbManager().getConnection();
            String query = "INSERT INTO quiz_results (user_id, quiz_id, score, total_questions) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, student.getId());
            ps.setInt(2, quizId);
            ps.setInt(3, score);
            ps.setInt(4, questions.size());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saving result: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showResult(int score) {
        double percentage = (double) score / questions.size() * 100;
        String message = String.format("Quiz Completed!\n\nScore: %d / %d\nPercentage: %.1f%%\n\n%s", score, questions.size(), percentage, getGradeMessage(percentage));
        JOptionPane.showMessageDialog(this, message, "Quiz Result", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private String getGradeMessage(double percentage) {
        if (percentage >= 90) return "Excellent! 🌟";
        else if (percentage >= 75) return "Great job! 👍";
        else if (percentage >= 60) return "Good effort! 💪";
        else if (percentage >= 50) return "You passed! ✓";
        else return "Keep practicing! 📚";
    }
    
    private void startTimer() {
        timerThread = new TimerThread();
        timerThread.start();
    }
    
    private void stopTimer() {
        if (timerThread != null) {
            timerThread.stopTimer();
        }
    }
    
    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
    
    class TimerThread extends Thread {
        private volatile boolean running = true;
        
        public void run() {
            while (running && remainingSeconds > 0) {
                try {
                    Thread.sleep(1000);
                    remainingSeconds--;
                    
                    SwingUtilities.invokeLater(() -> {
                        timerLabel.setText("⏱️ Time: " + formatTime(remainingSeconds));
                        if (remainingSeconds <= 60) {
                            timerLabel.setForeground(Color.RED);
                        } else if (remainingSeconds <= 300) {
                            timerLabel.setForeground(new Color(255, 140, 0));
                        }
                    });
                    
                    if (remainingSeconds == 0) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(QuizTakingFrame.this, "Time's up! The quiz will be submitted automatically.", "Time Up", JOptionPane.WARNING_MESSAGE);
                            submitQuiz();
                        });
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        public void stopTimer() {
            running = false;
            interrupt();
        }
    }
    
    private void applyDarkTheme(Container container) {
        Color darkBg = new Color(30, 35, 45);
        Color darkPanel = new Color(45, 52, 65);
        
        container.setBackground(darkBg);
        container.setForeground(Color.WHITE);
        
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                Color btnBg = btn.getBackground();
                if (!btnBg.equals(primaryBtn) && !btnBg.equals(secondaryBtn) && !btnBg.equals(warningBtn)) {
                    btn.setBackground(secondaryBtn);
                }
                comp.setForeground(Color.WHITE);
            } else if (comp instanceof JTextField || comp instanceof JTextArea) {
                comp.setBackground(darkPanel);
                comp.setForeground(Color.WHITE);
            } else if (comp instanceof JLabel) {
                comp.setForeground(Color.WHITE);
            } else if (comp instanceof JRadioButton) {
                comp.setBackground(darkBg);
                comp.setForeground(Color.WHITE);
            } else {
                comp.setBackground(darkBg);
                comp.setForeground(Color.WHITE);
            }
            
            if (comp instanceof Container) {
                applyDarkTheme((Container) comp);
            }
        }
    }
}

class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setFont(new Font("Arial", Font.BOLD, 12));
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String text = (value == null) ? "" : value.toString();
        setText(text);
        
        if (isSelected || hasFocus) {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
        } else {
            if (text.equals("Start")) {
                setBackground(new Color(67, 160, 71));
            } else if (text.equals("Manage/Delete")) {
                setBackground(new Color(255, 152, 0));
            } else if (text.equals("View Result")) {
                setBackground(new Color(66, 165, 245));
            } else {
                setBackground(new Color(156, 39, 176));
            }
            setForeground(Color.WHITE);
        }
        
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isSelected ? Color.BLACK : Color.WHITE, 1),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        return this;
    }
}

class ButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String label;
    private boolean clicked;
    private JTable table;
    private DefaultTableModel model;
    private User currentUser;
    private JFrame parentFrame;
    private boolean isDarkMode;
    
    public ButtonEditor(JCheckBox checkBox, JTable table, DefaultTableModel model, User user, JFrame parent, boolean darkMode) {
        super(checkBox);
        this.table = table;
        this.model = model;
        this.currentUser = user;
        this.parentFrame = parent;
        this.isDarkMode = darkMode;
        
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(e -> fireEditingStopped());
    }
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        label = (value == null) ? "" : value.toString();
        button.setText(label);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);
        
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        clicked = true;
        return button;
    }
    
    public Object getCellEditorValue() {
        if (clicked) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int quizId = (int) model.getValueAt(row, 0);
                String title = (String) model.getValueAt(row, 1);
                String action = (String) model.getValueAt(row, 6);
                int timeLimit = (int) model.getValueAt(row, 4);
                
                handleQuizAction(quizId, title, action, timeLimit);
            }
        }
        clicked = false;
        return label;
    }
    
    private void handleQuizAction(int quizId, String title, String action, int timeLimit) {
        if ("Manage/Delete".equals(action)) {
            String[] options = {"📝 Manage Questions", "🗑️ Delete Quiz", "Cancel"};
            int choice = JOptionPane.showOptionDialog(button,
                "What would you like to do with this quiz?",
                "Quiz Management",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
            
            if (choice == 0) {
                new QuizEditorFrame(quizId, title, currentUser, isDarkMode, parentFrame);
            } else if (choice == 1) {
                deleteQuiz(quizId, title);
            }
        } else if ("Start".equals(action)) {
            try {
                boolean completed = QuizApplication.getDbManager().hasUserCompletedQuiz(currentUser.getId(), quizId);
                if (completed) {
                    JOptionPane.showMessageDialog(button, "You have already completed this quiz!", "Already Completed", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                new QuizTakingFrame(quizId, title, timeLimit, currentUser, isDarkMode);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(button, "Error starting quiz: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if ("View Result".equals(action)) {
            showQuizResult(quizId);
        }
    }
    
    private void deleteQuiz(int quizId, String title) {
        int confirm = JOptionPane.showConfirmDialog(button,
            "Are you sure you want to DELETE the entire quiz:\n\"" + title + "\"?\n\n" +
            "This will permanently delete:\n" +
            "• All questions in this quiz\n" +
            "• All student results\n" +
            "• This action CANNOT be undone!",
            "⚠️ Confirm Quiz Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = QuizApplication.getDbManager().getConnection();
                String deleteQuery = "DELETE FROM quizzes WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(deleteQuery);
                ps.setInt(1, quizId);
                
                int affected = ps.executeUpdate();
                ps.close();
                
                if (affected > 0) {
                    JOptionPane.showMessageDialog(button,
                        "Quiz \"" + title + "\" has been successfully deleted!",
                        "✅ Quiz Deleted",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    for (int i = 0; i < model.getRowCount(); i++) {
                        if ((int)model.getValueAt(i, 0) == quizId) {
                            model.removeRow(i);
                            break;
                        }
                    }
                    table.repaint();
                } else {
                    JOptionPane.showMessageDialog(button,
                        "Failed to delete quiz. Please try again.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(button,
                    "Database error while deleting quiz:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void showQuizResult(int quizId) {
        try {
            Connection conn = QuizApplication.getDbManager().getConnection();
            String query = "SELECT score, total_questions, completed_at FROM quiz_results WHERE user_id = ? AND quiz_id = ? ORDER BY completed_at DESC LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, currentUser.getId());
            ps.setInt(2, quizId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int score = rs.getInt("score");
                int total = rs.getInt("total_questions");
                String date = rs.getString("completed_at");
                double percentage = (double) score / total * 100;
                
                String message = String.format("Your Quiz Result\n\nScore: %d / %d\nPercentage: %.1f%%\nCompleted: %s", score, total, percentage, date);
                JOptionPane.showMessageDialog(button, message, "Quiz Result", JOptionPane.INFORMATION_MESSAGE);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(button, "Error loading result: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean stopCellEditing() {
        clicked = false;
        return super.stopCellEditing();
    }
}

class DeleteButtonRenderer extends JButton implements TableCellRenderer {
    public DeleteButtonRenderer() {
        setOpaque(true);
        setText("🗑️ Delete");
        setFont(new Font("Arial", Font.BOLD, 12));
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected || hasFocus) {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            ));
        } else {
            setBackground(new Color(220, 53, 69));
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            ));
        }
        return this;
    }
}

class DeleteButtonEditor extends DefaultCellEditor {
    private JButton button;
    private boolean clicked;
    private int row;
    private QuizEditorFrame parentFrame;
    
    public DeleteButtonEditor(JCheckBox checkBox, QuizEditorFrame parent) {
        super(checkBox);
        this.parentFrame = parent;
        
        button = new JButton("🗑️ Delete");
        button.setOpaque(true);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addActionListener(e -> fireEditingStopped());
    }
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.row = row;
        clicked = true;
        
        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        
        return button;
    }
    
    public Object getCellEditorValue() {
        if (clicked) {
            JTable table = (JTable) button.getParent();
            int questionId = (int) table.getModel().getValueAt(row, 0);
            parentFrame.deleteQuestion(questionId);
        }
        clicked = false;
        return "Delete";
    }
    
    public boolean stopCellEditing() {
        clicked = false;
        return super.stopCellEditing();
    }
}