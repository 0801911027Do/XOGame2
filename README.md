import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import javax.swing.border.Border;

public class XOGame2 extends JFrame {

    private static final long serialVersionUID = 1L;
    private int gridSize = 3;
    private JButton[][] buttons;

    private final class WinState {
        private WinType type;
        private int startRow;
        private int startCol;
        private int endRow;
        private int endCol;
        public enum WinType {
            NONE, ROW, COLUMN, DIAGONAL_1, DIAGONAL_2
        }

        public WinState() {
            reset();
        }

        public void reset() {
            this.type = WinType.NONE;
            this.startRow = -1;
            this.startCol = -1;
            this.endRow = -1;
            this.endCol = -1;
        }

        public WinType getType() { return type; }
        public void setType(WinType type) { this.type = type; }

        public int getStartRow() { return startRow; }
        public void setStartRow(int row) { this.startRow = row; }

        public int getStartCol() { return startCol; }
        public void setStartCol(int col) { this.startCol = col; }

        public int getEndRow() { return endRow; }
        public void setEndRow(int row) { this.endRow = row; }

        public int getEndCol() { return endCol; }
        public void setEndCol(int col) { this.endCol = col; }
    }

    private final WinState winState = new WinState();

    private JPanel buildGridSelectPanel() {
        JPanel panel = new GradientPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(0, 0, 10);
                Color color2 = new Color(0, 25, 90);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        JButton exitBtn = new JButton("⬅️");
        exitBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 32));
        exitBtn.setBackground(new Color(0xE74C3C));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setFocusPainted(false);
        exitBtn.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        exitBtn.setPreferredSize(new Dimension(70, 48));
        exitBtn.addActionListener(e -> cardLayout.show(mainPanel, "MainMenu"));
        topPanel.add(exitBtn);
        panel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel title = new JLabel("CHOOSE A GRID", SwingConstants.CENTER);
        title.setFont(new Font("Arial Black", Font.BOLD, 52));
        title.setForeground(new Color(0, 220, 255));
        centerPanel.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;

        GridButton btn3x3 = new GridButton("3x3", 3, new Color(107, 142, 35));
        btn3x3.addActionListener(e -> {
            gridSize = 3;
            resetBoard();
            cardLayout.show(mainPanel, "Game");
        });
        centerPanel.add(btn3x3, gbc);

        gbc.gridx = 1;

        GridButton btn5x5 = new GridButton("5x5", 5, new Color(0x2196F3));
        btn5x5.addActionListener(e -> {
            gridSize = 5;
            resetBoard();
            cardLayout.show(mainPanel, "Game");
        });
        centerPanel.add(btn5x5, gbc);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private boolean playerXTurn = true;
    private boolean againstAI = false;

    public boolean isPlayerXTurn() {
        return playerXTurn;
    }

    public boolean isAgainstAI() {
        return againstAI;
    }

    public void setPlayerXTurn(boolean playerXTurn) {
        this.playerXTurn = playerXTurn;
    }

    public int getGridSize() {
        return gridSize;
    }

    public JButton[][] getButtons() {
        return buttons;
    }

    enum Difficulty {
        EASY, MEDIUM, HARD
    }
    
    enum GameResult {
        X_WINS,
        O_WINS,
        DRAW
    }

    private Difficulty currentDifficulty = Difficulty.EASY;

    private final JPanel mainPanel = new JPanel(new CardLayout());
    private final CardLayout cardLayout = (CardLayout) mainPanel.getLayout();
    private JPanel gridPanel;

    private final Color secondaryColor = new Color(0x1ABC9C);
    private final Color fontColor = new Color(0xECF0F1);
    private final Color neonGlow = new Color(255, 0, 255);
    private final Color colorX = new Color(0, 255, 255);
    private final Color colorO = new Color(255, 29, 39);
    private final Color primaryColor = new Color(0x5DADE2);

    private int scoreX = 0;
    private int scoreO = 0;

    private static final int WINNING_SCORE = 3;
    private JLabel scoreLabelX;
    private JLabel scoreLabelO;

    private JTextField usernameField;
    private JPasswordField passwordField;
    
    private ImageIcon iconCorrect;
    private ImageIcon iconIncorrect;

    public XOGame2() {
        setTitle("XO Game - Neon AI Edition");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        
        createStatusIcons(); 

        mainPanel.add(buildLoginPanel(), "Login");
        mainPanel.add(buildSplashPanel(), "Splash");
        mainPanel.add(buildMainMenu(), "MainMenu");
        mainPanel.add(buildDifficultySelect(), "DifficultySelect");
        mainPanel.add(buildGridSelectPanel(), "GridSelect");
        mainPanel.add(buildGameBoard(), "Game");

        add(mainPanel);

        String lastUser = null;
        try (BufferedReader br = new BufferedReader(new FileReader("user_session.txt"))) {
            lastUser = br.readLine();
        } catch (IOException ex) {
            System.err.println("An error occurred while reading the session file: " + ex.getMessage());
        }
        if (lastUser != null && !lastUser.isEmpty()) {
            cardLayout.show(mainPanel, "Splash");
        } else {
            cardLayout.show(mainPanel, "Login");
        }
        setVisible(true);
    }

    static class GradientPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth();
            int h = getHeight();
            Color color1 = new Color(0x2C3E50);
            Color color2 = new Color(0x4e54c8);
            GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, w, h);
        }
    }

    private JButton createModernButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Emoji", Font.BOLD, 22));
        button.setBackground(baseColor);
        button.setForeground(fontColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(secondaryColor);
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(baseColor);
            }
        });
        return button;
    }

    private JButton createDifficultyButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Emoji", Font.BOLD, 22));
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(baseColor.darker());
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(baseColor);
            }
        });
        return button;
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new GradientPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(20, 30, 60), 0, h, new Color(30, 60, 120));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel title = new JLabel("User Login", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(fontColor);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(userLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(250, 25));
        usernameField.setFont(new Font("Arial", Font.PLAIN, 18));
        panel.add(usernameField, gbc);

        gbc.gridx = 2;
        JLabel userStatus = createStatusIcon();
        panel.add(userStatus, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(fontColor);
        passLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(250, 25));
        passwordField.setFont(new Font("Arial", Font.PLAIN, 18));
        
        JPanel passwordPanel = createPasswordPanel(passwordField);
        panel.add(passwordPanel, gbc);

        gbc.gridx = 2;
        JLabel passStatus = createStatusIcon();
        panel.add(passStatus, gbc);


        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);

        JButton loginButton = createModernButton("Login", primaryColor);
        JButton registerButton = createModernButton("Register", new Color(0x2ECC71));

        btnPanel.add(loginButton);
        btnPanel.add(registerButton);
        panel.add(btnPanel, gbc);
        
        javax.swing.event.DocumentListener loginValidator = new javax.swing.event.DocumentListener() {
            void validate() {
                String user = usernameField.getText().trim();
                String pass = new String(passwordField.getPassword());

                if (user.isEmpty() || pass.isEmpty()) {
                    userStatus.setIcon(null);
                    passStatus.setIcon(null);
                    return;
                }

                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "SELECT password FROM user_data WHERE username = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, user);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) { 

                        userStatus.setIcon(iconCorrect);
                        
                        String dbPass = rs.getString("password");
                     
                        if (dbPass.equals(pass)) {
                            
                            passStatus.setIcon(iconCorrect);
                        } else {
                           
                            passStatus.setIcon(iconIncorrect);
                        }
                    } else { 
                       
                        userStatus.setIcon(iconIncorrect);
                        passStatus.setIcon(iconIncorrect);
                    }
                } catch (Exception ex) {
                   
                    userStatus.setIcon(null);
                    passStatus.setIcon(null);
                }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { validate(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { validate(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { validate(); }
        };

       
        usernameField.getDocument().addDocumentListener(loginValidator);
       
        
        registerButton.addActionListener(e -> {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(panel);
            parentFrame.setVisible(false);
            new RegisterDialog(parentFrame).setVisible(true);
        });

        loginButton.addActionListener(e -> {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword());
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Please enter username and password.", "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            try (Connection conn = DBConnection.getConnection()) {
                String sql = "SELECT * FROM user_data WHERE username = ? AND password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, user);
                    stmt.setString(2, pass);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            saveUserSession(user);
                            cardLayout.show(mainPanel, "Splash");
                        } else {
                            JOptionPane.showMessageDialog(panel, "Username or password is incorrect.", "Login Failed",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            } catch (SQLException | ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(panel, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    class RegisterDialog extends JDialog {

        private static final long serialVersionUID = 1L;
        public RegisterDialog(JFrame parent) {
            super(parent, "Register", true);
            setSize(600, 600);
            setLocationRelativeTo(parent);
            setResizable(false);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

            JPanel panel = new GradientPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    int w = getWidth(), h = getHeight();
                    GradientPaint gp = new GradientPaint(0, 0, new Color(20, 30, 60), 0, h, new Color(30, 60, 120));
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, w, h);
                }
            };
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(12, 12, 12, 12);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("Register", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 36));
            title.setForeground(Color.WHITE);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 3;
            panel.add(title, gbc);

            gbc.gridwidth = 1;
            gbc.gridy++;
            gbc.gridx = 0;
            JLabel userLabel = new JLabel("Username (4-15 chars):");
            userLabel.setForeground(Color.WHITE);
            userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            panel.add(userLabel, gbc);

            gbc.gridx = 1;
            JTextField userField = new JTextField(16);
            userField.setPreferredSize(new Dimension(250, 25));
            userField.setFont(new Font("Arial", Font.PLAIN, 18));
            panel.add(userField, gbc);

            gbc.gridx = 2;
            JLabel userStatus = createStatusIcon();
            panel.add(userStatus, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            JLabel passLabel = new JLabel("Password (8-15, A-Z, a-z, 0-9, sp):");
            passLabel.setForeground(Color.WHITE);
            passLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            panel.add(passLabel, gbc);

            gbc.gridx = 1;
            JPasswordField passField = new JPasswordField(16);
            passField.setPreferredSize(new Dimension(250, 25));
            passField.setFont(new Font("Arial", Font.PLAIN, 18));
            JPanel passPanel = createPasswordPanel(passField);
            panel.add(passPanel, gbc);

            gbc.gridx = 2;
            JLabel passStatus = createStatusIcon();
            panel.add(passStatus, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            JLabel confirmLabel = new JLabel("Confirm Password:");
            confirmLabel.setForeground(Color.WHITE);
            confirmLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            panel.add(confirmLabel, gbc);

            gbc.gridx = 1;
            JPasswordField confirmField = new JPasswordField(16);
            confirmField.setPreferredSize(new Dimension(250, 25));
            confirmField.setFont(new Font("Arial", Font.PLAIN, 18));
            JPanel confirmPanel = createPasswordPanel(confirmField);
            panel.add(confirmPanel, gbc);

            gbc.gridx = 2;
            JLabel confirmStatus = createStatusIcon();
            panel.add(confirmStatus, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.CENTER;
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
            btnPanel.setOpaque(false);
            JButton submitBtn = new JButton("Submit");
            JButton cancelBtn = new JButton("Cancel");
            submitBtn.setFont(new Font("Arial", Font.BOLD, 18));
            cancelBtn.setFont(new Font("Arial", Font.BOLD, 18));
            btnPanel.add(submitBtn);
            btnPanel.add(cancelBtn);
            panel.add(btnPanel, gbc);

            add(panel);

            userField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                void validate() {
                    String user = userField.getText().trim();
                    if (user.isEmpty()) {
                        userStatus.setIcon(null);
                        return;
                    }
                    if (user.length() < 4 || user.length() > 15) {
                        userStatus.setIcon(iconIncorrect);
                        return;
                    }
                    
                    try (Connection conn = DBConnection.getConnection()) {
                        String sql = "SELECT COUNT(*) FROM user_data WHERE username = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, user);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0) {
                            userStatus.setIcon(iconIncorrect);
                        } else {
                            userStatus.setIcon(iconCorrect);
                        }
                    } catch (Exception ex) {
                        userStatus.setIcon(null);
                    }
                }
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { validate(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { validate(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { validate(); }
            });

            passField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                void validate() {
                    String pass = new String(passField.getPassword());
                    if (pass.isEmpty()) {
                        passStatus.setIcon(null);
                    } else if (isPasswordStrong(pass)) {
                        passStatus.setIcon(iconCorrect);
                    } else {
                        passStatus.setIcon(iconIncorrect);
                    }
                    
                    String confirm = new String(confirmField.getPassword());
                    if (confirm.equals(pass) && isPasswordStrong(pass)) {
                        confirmStatus.setIcon(iconCorrect);
                    } else if (pass.isEmpty() && confirm.isEmpty()) {
                        confirmStatus.setIcon(null);
                    } else {
                        confirmStatus.setIcon(iconIncorrect);
                    }
                }
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { validate(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { validate(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { validate(); }
            });

            confirmField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                void validate() {
                    String pass = new String(passField.getPassword());
                    String confirm = new String(confirmField.getPassword());
                    
                    if (confirm.equals(pass) && isPasswordStrong(pass)) {
                        confirmStatus.setIcon(iconCorrect);
                    } else if (confirm.isEmpty() && pass.isEmpty()) {
                        confirmStatus.setIcon(null);
                    } else {
                        confirmStatus.setIcon(iconIncorrect);
                    }
                }
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { validate(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { validate(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { validate(); }
            });

            submitBtn.addActionListener(e -> {
                boolean validUser = iconCorrect.equals(userStatus.getIcon());
                boolean validPass = iconCorrect.equals(passStatus.getIcon());
                boolean validConfirm = iconCorrect.equals(confirmStatus.getIcon());

                if (!validUser || !validPass || !validConfirm) {
                    JOptionPane.showMessageDialog(this, "Please check your information again.", "Invalid Input",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try (Connection conn = DBConnection.getConnection()) {
                    String user = userField.getText().trim();
                    String pass = new String(passField.getPassword());
                    String sql = "INSERT INTO user_data (username, password) VALUES (?, ?)";

                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, user);
                    stmt.setString(2, pass);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Registration Successful! Please Login.", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    parent.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });

            cancelBtn.addActionListener(e -> {
                dispose();
                parent.setVisible(true);
            });
        }
    }

    private JPanel createPasswordPanel(JPasswordField passwordField) {
        JToggleButton showHideButton = new JToggleButton("👁️");
        showHideButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        showHideButton.setPreferredSize(new Dimension(40, 25));
        showHideButton.setBorder(null);
        showHideButton.setFocusPainted(false);
        final char defaultEchoChar = passwordField.getEchoChar();
        showHideButton.addActionListener(e -> {
            if (showHideButton.isSelected()) {
                passwordField.setEchoChar((char) 0);
                showHideButton.setText("🙈");
            } else {
                passwordField.setEchoChar(defaultEchoChar);
                showHideButton.setText("👁️");
            }
        });

        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordPanel.setOpaque(false);
        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(showHideButton, BorderLayout.EAST);
        passwordPanel.setPreferredSize(new Dimension(250, 25));
        return passwordPanel;
    }
    
    private static final String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[\\*\\/+\\-_&#!.,^@=%\\[\\]{}\\\"$:;?\\\\|,]).{8,15}$";

    private boolean isPasswordStrong(String password) {
        if (password == null) {
            return false;
        }
        return password.matches(passwordRegex);
    }
    
    private JPanel buildMainMenu() {
        JPanel panel = new GradientPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 30), 0, h, new Color(30, 60, 120));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        JButton exitBtn = new JButton("⬅️");
        exitBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 32));
        exitBtn.setBackground(new Color(0xE74C3C));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setFocusPainted(false);
        exitBtn.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        exitBtn.setPreferredSize(new Dimension(70, 48));
        exitBtn.addActionListener(e -> cardLayout.show(mainPanel, "Splash"));
        topPanel.add(exitBtn);
        panel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("XO Tic Tac Toe", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        centerPanel.add(title);

        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 10;
                int cell = size / 3;
                int x0 = (getWidth() - size) / 2;
                int y0 = (getHeight() - size) / 2;

                g2d.setStroke(new BasicStroke(4));
                g2d.setColor(new Color(0x00FFFF));
                g2d.drawRect(x0, y0, size, size);

                for (int i = 1; i < 3; i++) {
                    g2d.drawLine(x0 + i * cell, y0, x0 + i * cell, y0 + size);
                    g2d.drawLine(x0, y0 + i * cell, x0 + size, y0 + i * cell);
                }

                Font xoFont = new Font("Arial", Font.BOLD, cell - 18);
                g2d.setFont(xoFont);
                g2d.setColor(new Color(0x00FFFF));
                g2d.drawString("X", x0 + cell + cell / 4, y0 + cell - cell / 6);
                g2d.drawString("X", x0 + 2 * cell + cell / 4, y0 + 2 * cell - cell / 6);
                g2d.drawString("X", x0 + cell / 4, y0 + 3 * cell - cell / 6);

                g2d.setColor(new Color(255, 29, 39));
                g2d.drawString("O", x0 + cell / 4, y0 + cell - cell / 6);
                g2d.drawString("O", x0 + cell + cell / 4, y0 + 2 * cell - cell / 6);
                g2d.drawString("O", x0 + 2 * cell + cell / 4, y0 + 3 * cell - cell / 6);

                g2d.setStroke(new BasicStroke(7));
                g2d.setColor(Color.YELLOW);
                g2d.drawLine(x0 + 12, y0 + 12, x0 + size - 12, y0 + size - 12);
            }
        };
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(logoPanel);

        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(20, 80, 40, 80));

        Dimension buttonSize = new Dimension(300, 60);

        JButton play2PButton = createModernButton("👤 vs 👤 (Two Players)", new Color(0xFFA726));
        play2PButton.setFont(new Font("Segoe UI Emoji", Font.BOLD, 24));
        play2PButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        play2PButton.setMaximumSize(buttonSize);
        play2PButton.setPreferredSize(buttonSize);
        play2PButton.addActionListener(e -> {
            againstAI = false;
            cardLayout.show(mainPanel, "GridSelect");
        });

        JButton playAIButton = createModernButton("👤 vs 🤖 (AI)", primaryColor);
        playAIButton.setFont(new Font("Segoe UI Emoji", Font.BOLD, 24));
        playAIButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playAIButton.setMaximumSize(buttonSize);
        playAIButton.setPreferredSize(buttonSize);
        playAIButton.addActionListener(e -> cardLayout.show(mainPanel, "DifficultySelect"));

        btnPanel.add(play2PButton);
        btnPanel.add(Box.createVerticalStrut(18));
        btnPanel.add(playAIButton);

        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildDifficultySelect() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.weightx = 1;

        JLabel label = new JLabel("SELECT LEVEL", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 32));
        label.setForeground(Color.WHITE);
        gbc.gridy = 0;
        panel.add(label, gbc);

        JLabel easyIcon = new JLabel("🤖", SwingConstants.CENTER);
        easyIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 56));
        easyIcon.setForeground(new Color(0x2ECC71));
        gbc.gridy = 1;
        panel.add(easyIcon, gbc);

        JButton easy = createDifficultyButton("EASY", new Color(0x2ECC71));
        easy.setFont(new Font("Arial", Font.BOLD, 32));
        easy.setForeground(Color.WHITE);
        gbc.gridy = 2;
        panel.add(easy, gbc);

        JLabel mediumIcon = new JLabel("🤖", SwingConstants.CENTER);
        mediumIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 56));
        mediumIcon.setForeground(new Color(0xFFFF00));
        gbc.gridy = 3;
        panel.add(mediumIcon, gbc);

        JButton medium = createDifficultyButton("MEDIUM", new Color(0xF1C40F));
        medium.setFont(new Font("Arial", Font.BOLD, 32));
        medium.setForeground(Color.WHITE);
        gbc.gridy = 4;
        panel.add(medium, gbc);

        JLabel hardIcon = new JLabel("🤖", SwingConstants.CENTER);
        hardIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 56));
        hardIcon.setForeground(new Color(0xFF0000));
        gbc.gridy = 5;
        panel.add(hardIcon, gbc);

        JButton hard = createDifficultyButton("HARD", new Color(0xFF0000));
        hard.setFont(new Font("Arial", Font.BOLD, 32));
        hard.setForeground(Color.WHITE);
        gbc.gridy = 6;
        panel.add(hard, gbc);

        JButton back = createModernButton("🔙 Back to Menu", primaryColor);
        gbc.gridy = 7;
        panel.add(back, gbc);

        ActionListener difficultyHandler = (ActionEvent e) -> {
            String cmd = ((AbstractButton) e.getSource()).getText();
            switch (cmd) {
                case "EASY" -> currentDifficulty = Difficulty.EASY;
                case "MEDIUM" -> currentDifficulty = Difficulty.MEDIUM;
                case "HARD" -> currentDifficulty = Difficulty.HARD;
            }
            againstAI = true;
            cardLayout.show(mainPanel, "GridSelect");
        };

        easy.addActionListener(difficultyHandler);
        medium.addActionListener(difficultyHandler);
        hard.addActionListener(difficultyHandler);
        back.addActionListener(e -> cardLayout.show(mainPanel, "MainMenu"));

        return panel;
    }

    private JPanel buildGameBoard() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout());

        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setStroke(new BasicStroke(5));
                g2d.setColor(neonGlow);
                int width = getWidth();
                int height = getHeight();
                for (int i = 1; i < gridSize; i++) {
                    int x = i * width / gridSize;
                    g2d.drawLine(x, 0, x, height);
                }
                for (int i = 1; i < gridSize; i++) {
                    int y = i * height / gridSize;
                    g2d.drawLine(0, y, width, y);
                }

                if (winState.getType() != WinState.WinType.NONE) {
                    drawWinningLine(g2d);
                }
            }
        };
        gridPanel.setOpaque(false);

        JButton replayButton = createModernButton("🔁 Replay", new Color(0x27AE60));
        replayButton.addActionListener(e -> {
            resetBoard();
        });

        JButton exitButton = createModernButton("🏠 Exit", new Color(0xE74C3C));
        exitButton.addActionListener(e -> {
            resetOverallScores();
            cardLayout.show(mainPanel, "MainMenu");
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        topPanel.add(replayButton);
        topPanel.add(exitButton);

        JPanel scorePanel = new JPanel();
        scorePanel.setOpaque(false);
        scoreLabelX = new JLabel("X: " + scoreX);
        scoreLabelX.setFont(new Font("Arial", Font.BOLD, 28));
        scoreLabelX.setForeground(colorX);
        scoreLabelO = new JLabel("O: " + scoreO);
        scoreLabelO.setFont(new Font("Arial", Font.BOLD, 28));
        scoreLabelO.setForeground(colorO);
        scorePanel.add(scoreLabelX);
        scorePanel.add(Box.createHorizontalStrut(30));
        scorePanel.add(scoreLabelO);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(gridPanel, BorderLayout.CENTER);
        panel.add(scorePanel, BorderLayout.SOUTH);

        return panel;
    }

    private void drawWinningLine(Graphics2D g2d) {
        g2d.setColor(playerXTurn ? colorX : colorO);
        g2d.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int w = gridPanel.getWidth();
        int h = gridPanel.getHeight();
        int cellW = w / gridSize;
        int cellH = h / gridSize;

        int x1 = winState.getStartCol() * cellW + cellW / 2;
        int y1 = winState.getStartRow() * cellH + cellH / 2;
        int x2 = winState.getEndCol() * cellW + cellW / 2;
        int y2 = winState.getEndRow() * cellH + cellH / 2;

        g2d.drawLine(x1, y1, x2, y2);
    }

    private void resetBoard() {
        playerXTurn = true;
        winState.reset();

        buttons = new JButton[gridSize][gridSize];
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(gridSize, gridSize, 0, 0));

        int fontSize = (gridSize == 3) ? 80 : 40;
        Font font = new Font("Arial", Font.BOLD, fontSize);
        Border emptyBorder = BorderFactory.createEmptyBorder();

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                JButton btn = new JButton("");
                btn.setFont(font);
                btn.setFocusPainted(false);
                btn.setBorder(emptyBorder);
                btn.setContentAreaFilled(false);
                btn.setOpaque(false);

                int row = i, col = j;
                btn.addActionListener(e -> {
                    if (againstAI && !playerXTurn) {
                        return;
                    }
                    handleMove(row, col);
                });

                buttons[i][j] = btn;
                gridPanel.add(btn);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
        updateScoreLabels();
    }

    private void disableAllButtons() {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                buttons[i][j].setEnabled(false);
            }
        }
    }

    private void handleMove(int row, int col) {
        if (buttons[row][col].getText().isEmpty() && winState.getType() == WinState.WinType.NONE) {
            String currentSymbol = playerXTurn ? "X" : "O";
            Color currentColor = playerXTurn ? colorX : colorO;

            buttons[row][col].setText(currentSymbol);
            buttons[row][col].setForeground(currentColor);

            if (checkWin(currentSymbol)) {
                disableAllButtons();
                gridPanel.repaint();
                endGame(playerXTurn ? GameResult.X_WINS : GameResult.O_WINS);
                return;
            }

            if (isBoardFull()) {
                endGame(GameResult.DRAW);
                return;
            }

            playerXTurn = !playerXTurn;

            if (againstAI && !playerXTurn) {
                Timer timer = new Timer(500, e -> makeAIMove());
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    private void updateScoreLabels() {
        SwingUtilities.invokeLater(() -> {
            if (scoreLabelX != null) {
                scoreLabelX.setText("X: " + scoreX);
            }
            if (scoreLabelO != null) {
                scoreLabelO.setText("O: " + scoreO);
            }
        });
    }

    private void makeAIMove() {
        if (playerXTurn) {
            return;
        }

        switch (currentDifficulty) {
            case EASY -> easyMove();
            case MEDIUM -> mediumMove();
            case HARD -> hardMove();
        }
    }

    private void endGame(GameResult result) {
        boolean hasWinner = false;
        String message;

        switch (result) {
            case X_WINS:
                scoreX++;
                playerXStats.addWin();
                playerOStats.resetStreak();
                hasWinner = true;
                checkAchievements(playerXStats, true);
                message = againstAI ? "You win!" : "Player X wins!";
                break;

            case O_WINS:
                scoreO++;
                playerOStats.addWin();
                playerXStats.resetStreak();
                hasWinner = true;
                checkAchievements(playerOStats, false);
                message = againstAI ? "🤖 AI wins!" : "Player O wins!";
                break;

            case DRAW:
            default:
                playerXStats.resetStreak();
                playerOStats.resetStreak();
                message = "It's a Draw!";
                break;
        }

        updateScoreLabels();
        playCelebrationAnimation();

        int delay = hasWinner ? 1000 : 100;
        Timer dialogTimer = new Timer(delay, e -> {
            if (scoreX >= WINNING_SCORE || scoreO >= WINNING_SCORE) {
                showFinalVictoryDialog(scoreX >= WINNING_SCORE ? "X" : "O");
                resetOverallScores();
            } else {
                showRoundResultDialog(message);
            }
        });
        dialogTimer.setRepeats(false);
        dialogTimer.start();
    }


    private void showRoundResultDialog(String message) {
        showResultDialog(message);
    }

    private void showResultDialog(String message) {
        JDialog dialog = new JDialog(this, "Game Over", true);
        dialog.setUndecorated(true);
        dialog.setSize(350, 300);
        dialog.setLocationRelativeTo(this);

        final Color crownColor;
        final String winText;
        final String crownText;

        if (message.contains("X wins") || message.contains("You win!")) {
            winText = "X IS WIN";
            crownColor = colorX;
            crownText = " 👑 ";
        } else if (message.contains("O wins") || message.contains("AI wins")) {
            winText = "O IS WIN";
            crownColor = colorO;
            crownText = " 👑 ";
        } else {
            winText = "DRAW";
            crownColor = new Color(0xFFFF00);
            crownText = "👑";
        }

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(0x2C3E50);
                Color color2 = new Color(0x4A148C);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, w, h, 40, 40);
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFFD700), 5),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)));

        JLabel crown = new JLabel(crownText, SwingConstants.CENTER);
        crown.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        crown.setAlignmentX(Component.CENTER_ALIGNMENT);
        crown.setForeground(crownColor);

        JLabel winLabel = new JLabel(winText, SwingConstants.CENTER);
        winLabel.setFont(new Font("Arial", Font.BOLD, 48));
        winLabel.setForeground(crownColor);
        winLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonsPanel.setOpaque(false);

        JButton nextBtn = createModernButton("➡", new Color(0x2980B9));
        nextBtn.setMaximumSize(new Dimension(100, 50));
        nextBtn.addActionListener(e -> {
            dialog.dispose();
            resetBoard();
        });

        buttonsPanel.add(nextBtn);

        panel.add(crown);
        panel.add(Box.createVerticalStrut(10));
        panel.add(winLabel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(buttonsPanel);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void showFinalVictoryDialog(String winner) {
        JDialog dialog = new JDialog(this, "CHAMPION!", true);
        dialog.setUndecorated(true);
        dialog.setSize(400, 450);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(0x1a237e);
                Color color2 = new Color(0x4a148c);
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel crownLabel = new JLabel("👑", SwingConstants.CENTER);
        crownLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        crownLabel.setForeground(winner.equals("X") ? colorX : colorO);
        crownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel trophyLabel = new JLabel("🏆", SwingConstants.CENTER);
        trophyLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        trophyLabel.setForeground(new Color(0xFFD700));
        trophyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel championLabel = new JLabel(winner + " IS THE CHAMPION!", SwingConstants.CENTER);
        championLabel.setFont(new Font("Arial", Font.BOLD, 36));
        championLabel.setForeground(new Color(0xFFD700));
        championLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreLabel = new JLabel("Final Score:", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 24));
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreNumbersLabel = new JLabel("X " + scoreX + " - " + scoreO + " O", SwingConstants.CENTER);
        scoreNumbersLabel.setFont(new Font("Arial", Font.BOLD, 32));
        scoreNumbersLabel.setForeground(new Color(0x4CAF50));
        scoreNumbersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton newGameBtn = createModernButton("New Championship", new Color(0x2196F3));
        newGameBtn.setFont(new Font("Arial", Font.BOLD, 20));
        newGameBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        newGameBtn.setMaximumSize(new Dimension(250, 50));
        newGameBtn.addActionListener(e -> {
            dialog.dispose();
            resetOverallScores();
            cardLayout.show(mainPanel, "MainMenu");
        });

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFFD700), 3),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)));

        panel.add(crownLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(trophyLabel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(championLabel);
        panel.add(Box.createVerticalStrut(30));
        panel.add(scoreLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(scoreNumbersLabel);
        panel.add(Box.createVerticalStrut(40));
        panel.add(newGameBtn);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void setAIMove(int row, int col) {
        handleMove(row, col);
    }

    private void easyMove() {
        List<Point> emptyCells = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    emptyCells.add(new Point(i, j));
                }
            }
        }
        if (!emptyCells.isEmpty()) {
            Point move = emptyCells.get(new Random().nextInt(emptyCells.size()));
            setAIMove(move.x, move.y);
        }
    }

    private Point findWinningMove(String player) {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    buttons[i][j].setText(player);
                    if (isHypotheticalWin(player)) {
                        buttons[i][j].setText("");
                        return new Point(i, j);
                    }
                    buttons[i][j].setText("");
                }
            }
        }
        return null;
    }

    private void mediumMove() {
        Point winningMove = findWinningMove("O");
        if (winningMove != null) {
            setAIMove(winningMove.x, winningMove.y);
            return;
        }
        Point blockingMove = findWinningMove("X");
        if (blockingMove != null) {
            setAIMove(blockingMove.x, blockingMove.y);
            return;
        }
        easyMove();
    }

    private void hardMove() {
        if (gridSize == 5) {
            mediumMove();
            return;
        }

        int bestScore = Integer.MIN_VALUE;
        Point bestMove = null;

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    buttons[i][j].setText("O");
                    int score = minimax(0, false);
                    buttons[i][j].setText("");
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = new Point(i, j);
                    }
                }
            }
        }
        if (bestMove != null) {
            setAIMove(bestMove.x, bestMove.y);
        }
    }

    private int minimax(int depth, boolean isMaximizing) {
        if (isHypotheticalWin("O")) {
            return 10 - depth;
        }
        if (isHypotheticalWin("X")) {
            return depth - 10;
        }
        if (isBoardFull()) {
            return 0;
        }

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    if (buttons[i][j].getText().isEmpty()) {
                        buttons[i][j].setText("O");
                        bestScore = Math.max(bestScore, minimax(depth + 1, false));
                        buttons[i][j].setText("");
                    }
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    if (buttons[i][j].getText().isEmpty()) {
                        buttons[i][j].setText("X");
                        bestScore = Math.min(bestScore, minimax(depth + 1, true));
                        buttons[i][j].setText("");
                    }
                }
            }
            return bestScore;
        }
    }

    private boolean isBoardFull() {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkWin(String symbol) {
        return isWinningState(symbol, true);
    }

    private boolean isWinningState(String symbol, boolean updateWinInfo) {
        int bingo = (gridSize == 5) ? 4 : 3;

   
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j <= gridSize - bingo; j++) {
                boolean win = true;
                for (int k = 0; k < bingo; k++) {
                    if (!buttons[i][j + k].getText().equals(symbol)) {
                        win = false;
                        break;
                    }
                }
                if (win) {
                    if (updateWinInfo) {
                        winState.setType(WinState.WinType.ROW);
                        winState.setStartRow(i); winState.setStartCol(j);
                        winState.setEndRow(i); winState.setEndCol(j + bingo - 1);
                    }
                    return true;
                }
            }
        }
        
        for (int j = 0; j < gridSize; j++) {
            for (int i = 0; i <= gridSize - bingo; i++) {
                boolean win = true;
                for (int k = 0; k < bingo; k++) {
                    if (!buttons[i + k][j].getText().equals(symbol)) {
                        win = false;
                        break;
                    }
                }
                if (win) {
                    if (updateWinInfo) {
                        winState.setType(WinState.WinType.COLUMN);
                        winState.setStartRow(i); winState.setStartCol(j);
                        winState.setEndRow(i + bingo - 1); winState.setEndCol(j);
                    }
                    return true;
                }
            }
        }
     
        for (int i = 0; i <= gridSize - bingo; i++) {
            for (int j = 0; j <= gridSize - bingo; j++) {
                boolean win = true;
                for (int k = 0; k < bingo; k++) {
                    if (!buttons[i + k][j + k].getText().equals(symbol)) {
                        win = false;
                        break;
                    }
                }
                if (win) {
                    if (updateWinInfo) {
                        winState.setType(WinState.WinType.DIAGONAL_1);
                        winState.setStartRow(i); winState.setStartCol(j);
                        winState.setEndRow(i + bingo - 1); winState.setEndCol(j + bingo - 1);
                    }
                    return true;
                }
            }
        }
   
        for (int i = 0; i <= gridSize - bingo; i++) {
            for (int j = bingo - 1; j < gridSize; j++) {
                boolean win = true;
                for (int k = 0; k < bingo; k++) {
                    if (!buttons[i + k][j - k].getText().equals(symbol)) {
                        win = false;
                        break;
                    }
                }
                if (win) {
                    if (updateWinInfo) {
                        winState.setType(WinState.WinType.DIAGONAL_2);
                        winState.setStartRow(i); winState.setStartCol(j);
                        winState.setEndRow(i + bingo - 1); winState.setEndCol(j - bingo + 1);
                    }
                    return true;
                }
            }
        }

        if (updateWinInfo) {
            winState.setType(WinState.WinType.NONE);
        }
        return false;
    }

    private boolean isHypotheticalWin(String symbol) {
        return isWinningState(symbol, false);
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Error: This application requires a graphical environment to run.");
            System.exit(1);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new XOGame2();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Error starting application: " + e.getMessage(),
                        "Application Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    private class PlayerStats {
        private int wins = 0;
        private int streak = 0;

        public void addWin() {
            wins++;
            streak++;
        }

        public void resetStreak() {
            streak = 0;
        }

        public void reset() {
            wins = 0;
            streak = 0;
        }

        public int getWins() {
            return wins;
        }

        public int getStreak() {
            return streak;
        }
    }

    private final PlayerStats playerXStats = new PlayerStats();
    private final PlayerStats playerOStats = new PlayerStats();

    private void checkAchievements(PlayerStats stats, boolean isPlayerX) {
        if (!againstAI || isPlayerX) {
            String winnerName = isPlayerX ? "Player X" : "Player O";

            if (stats.getWins() == 1) {
                showAchievementPopup(Achievement.FIRST_WIN, winnerName);
            }

            if (stats.getStreak() == 3) {
                showAchievementPopup(Achievement.WINNING_STREAK, winnerName);
            }
        }
    }

    private void showAchievementPopup(Achievement achievement, String winnerName) {
        JDialog dialog = new JDialog(this, "Achievement Unlocked!", true);
        dialog.setUndecorated(true);

        JPanel panel = new GradientPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel icon = new JLabel("🏆", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        icon.setForeground(Color.YELLOW);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel(winnerName + " Unlocked!", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subTitle = new JLabel(achievement.getTitle(), SwingConstants.CENTER);
        subTitle.setFont(new Font("Arial", Font.ITALIC, 18));
        subTitle.setForeground(Color.LIGHT_GRAY);
        subTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel description = new JLabel(achievement.getDescription(), SwingConstants.CENTER);
        description.setFont(new Font("Arial", Font.PLAIN, 16));
        description.setForeground(Color.LIGHT_GRAY);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(icon);
        panel.add(Box.createVerticalStrut(10));
        panel.add(title);
        panel.add(Box.createVerticalStrut(5));
        panel.add(subTitle);
        panel.add(Box.createVerticalStrut(10));
        panel.add(description);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        Timer timer = new Timer(2500, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();

        dialog.setVisible(true);
    }

    private enum Achievement {
        FIRST_WIN("First Victory!", "Win your first game"),
        WINNING_STREAK("Hot Streak!", "Win 3 games in a row");

        private final String title;
        private final String description;

        Achievement(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }

    private void playCelebrationAnimation() {
        if (winState.getType() != WinState.WinType.NONE) {
            Timer timer = new Timer(100, new ActionListener() {
                int count = 0;
                final Color[] celebrationColors = {
                        new Color(0xFFD700), new Color(0x00FFFF),
                        new Color(0xFF1493), new Color(0xADFF2F)
                };

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (count < 10) {
                        for (JButton[] buttonRow : buttons) {
                            for (JButton button : buttonRow) {
                                if (!button.getText().isEmpty()) {
                                    button.setForeground(celebrationColors[count % celebrationColors.length]);
                                }
                            }
                        }
                        count++;
                    } else {
                        ((Timer) e.getSource()).stop();
                        for (int i = 0; i < gridSize; i++) {
                            for (int j = 0; j < gridSize; j++) {
                                if (buttons[i][j].getText().equals("X")) {
                                    buttons[i][j].setForeground(colorX);
                                } else if (buttons[i][j].getText().equals("O")) {
                                    buttons[i][j].setForeground(colorO);
                                }
                            }
                        }
                    }
                }
            });
            timer.start();
        }
    }

    private void resetOverallScores() {
        scoreX = 0;
        scoreO = 0;
        playerXStats.reset();
        playerOStats.reset();
        updateScoreLabels();
    }

    private JPanel buildSplashPanel() {
        JPanel panel = new GradientPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int w = getWidth(), h = getHeight();

                GradientPaint gp = new GradientPaint(0, 0, new Color(10, 20, 40), 0, h, new Color(30, 60, 120));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);

                g2d.setColor(new Color(120, 160, 220, 60));
                int gridSize = Math.min(w, h) * 3 / 4;
                int cell = gridSize / 3;
                int x0 = (w - gridSize) / 2;
                int y0 = (h - gridSize) / 2;
                g2d.setStroke(new BasicStroke(2));
                for (int i = 1; i < 3; i++) {
                    g2d.drawLine(x0 + i * cell, y0, x0 + i * cell, y0 + gridSize);
                    g2d.drawLine(x0, y0 + i * cell, x0 + gridSize, y0 + i * cell);
                }

                Font xoFont = new Font("Arial Black", Font.BOLD, gridSize / 2);
                g2d.setFont(xoFont);
                g2d.setColor(new Color(0x00eaff));
                int xX = x0 + cell / 8;
                int yX = y0 + cell * 2;
                g2d.drawString("X", xX, yX);
                g2d.setColor(new Color(0xff9800));
                int xO = x0 + cell + cell / 6;
                int yO = y0 + cell * 2;
                g2d.drawString("O", xO, yO);
            }
        };
        panel.setLayout(null);

        JLabel logo = new JLabel("<html><span style='font-family:Arial Black,sans-serif;font-size:38px;'>"
                + "</* โค้ด CSS หลัก */\r\n" + //
                                        ":root {\r\n" + //
                                        "    --green: #16a34a;\r\n" + //
                                        "    --bg: #f3f4f6;\r\n" + //
                                        "    --card: #ffffff;\r\n" + //
                                        "    --text: #1f2937;\r\n" + //
                                        "}\r\n" + //
                                        "\r\n" + //
                                        "body {\r\n" + //
                                        "    font-family: 'Sarabun', sans-serif;\r\n" + //
                                        "    background-color: var(--bg);\r\n" + //
                                        "    color: var(--text);\r\n" + //
                                        "    margin: 0;\r\n" + //
                                        "    -webkit-font-smoothing: antialiased;\r\n" + //
                                        "    -moz-osx-font-smoothing: grayscale;\r\n" + //
                                        "}\r\n" + //
                                        "\r\n" + //
                                        ".container {\r\n" + //
                                        "    max-width: 1024px;\r\n" + //
                                        "    margin: 0 auto;\r\n" + //
                                        "    padding: 1rem;\r\n" + //
                                        "}\r\n" + //
                                        "\r\n" + //
                                        ".card {\r\n" + //
                                        "    background: var(--card);\r\n" + //
                                        "    border-radius: 12px;\r\n" + //
                                        "    box-shadow: 0 6px 18px rgba(31, 41, 55, 0.08);\r\n" + //
                                        "    padding: 1rem;\r\n" + //
                                        "}\r\n" + //
                                        "\r\n" + //
                                        ".btn-primary {\r\n" + //
                                        "    background-color: var(--green);\r\n" + //
                                        "    color: #fff;\r\n" + //
                                        "    padding: 0.6rem 1.2rem;\r\n" + //
                                        "    border-radius: 9999px;\r\n" + //
                                        "    border: none;\r\n" + //
                                        "    cursor: pointer;\r\n" + //
                                        "}\r\n" + //
                                        "span style='color:#00eaff;'>X</span>"
                + "<span style='color:#ff9800;'>O</span>"
                + "<span style='color:#fff;'> Tic Tac Toe</span>"
                + "</span></html>");
        logo.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel sub = new JLabel("MULTIPLAYER");
        sub.setFont(new Font("Arial Black", Font.BOLD, 22));
        sub.setForeground(new Color(0x42a5f5));
        sub.setHorizontalAlignment(SwingConstants.CENTER);

        JButton startBtn = createModernButton("Start", new Color(0x90caf9));
        startBtn.setFont(new Font("Arial", Font.BOLD, 28));
        startBtn.addActionListener(e -> cardLayout.show(mainPanel, "MainMenu"));

        JButton logoutBtn = createModernButton("Logout", new Color(0xe74c3c));
        logoutBtn.setFont(new Font("Arial", Font.BOLD, 28));
        logoutBtn.addActionListener(e -> {
            try {
                new FileWriter("user_session.txt").close();
            } catch (IOException ex) {
            }
            resetLoginFields();
            cardLayout.show(mainPanel, "Login");
        });

        panel.add(logo);
        panel.add(sub);
        panel.add(startBtn);
        panel.add(logoutBtn);

        panel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent evt) {
                int w = panel.getWidth();
                int h = panel.getHeight();
                logo.setBounds(0, h / 4 + 5, w, 50);
                sub.setBounds(0, h / 4 + 55, w, 30);
                int btnY = h / 2 + 40;
                int btnW = 140, btnH = 55, gap = 60;
                int totalBtnW = btnW * 2 + gap;
                int btnX = (w - totalBtnW) / 2;
                startBtn.setBounds(btnX, btnY, btnW, btnH);
                logoutBtn.setBounds(btnX + btnW + gap, btnY, btnW, btnH);
            }
        });

        return panel;
    }

    private void resetLoginFields() {
        if (usernameField != null) {
            usernameField.setText("");
        }
        if (passwordField != null) {
            passwordField.setText("");
        }
    }

    private void saveUserSession(String username) {
        try (FileWriter fw = new FileWriter("user_session.txt")) {
            fw.write(username);
        } catch (IOException ex) {
            System.err.println("Error saving session: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Could not save session information.",
                    "File Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createStatusIcons() {
        int size = 20; 
        int strokeWidth = 3;

        BufferedImage correctImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dCorrect = correctImage.createGraphics();
        g2dCorrect.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2dCorrect.setColor(new Color(0x2ECC71)); 
        g2dCorrect.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2dCorrect.drawLine(size / 4, size / 2, size / 2, size * 3 / 4);
        g2dCorrect.drawLine(size / 2, size * 3 / 4, size * 3 / 4, size / 4);
        g2dCorrect.dispose();
        iconCorrect = new ImageIcon(correctImage);

        BufferedImage incorrectImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dIncorrect = incorrectImage.createGraphics();
        g2dIncorrect.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2dIncorrect.setColor(new Color(0xE74C3C)); 
        g2dIncorrect.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2dIncorrect.drawLine(size / 4, size / 4, size * 3 / 4, size * 3 / 4);
        g2dIncorrect.drawLine(size * 3 / 4, size / 4, size / 4, size * 3 / 4);
        g2dIncorrect.dispose();
        iconIncorrect = new ImageIcon(incorrectImage);
    }

    private JLabel createStatusIcon() {
        JLabel label = new JLabel();
        label.setOpaque(false);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(36, 36));
        return label;
    }

    @SuppressWarnings("serial")
    class GridButton extends JButton {
        private final int gridSize;

        public GridButton(String text, int gridSize, Color baseColor) {
            super(text);
            this.gridSize = gridSize;

            setBackground(baseColor);
            setForeground(Color.WHITE);
            setFont(new Font("Arial", Font.BOLD, 32));
            setPreferredSize(new Dimension(180, 180));
            setFocusPainted(false);
            setBorder(BorderFactory.createLineBorder(baseColor.brighter(), 2));

            setVerticalTextPosition(SwingConstants.TOP);
            setHorizontalTextPosition(SwingConstants.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent evt) {
                    setBackground(baseColor.brighter());
                }

                @Override
                public void mouseExited(MouseEvent evt) {
                    setBackground(baseColor);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int iconSize = Math.min(w, h) / 2;
            int x_offset = (w - iconSize) / 2;
            int y_offset = h / 2;

            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.setStroke(new BasicStroke(2));

            int cellSize = iconSize / gridSize;
            for (int i = 1; i < gridSize; i++) {
                g2d.drawLine(x_offset + (i * cellSize), y_offset + 10, x_offset + (i * cellSize), y_offset + iconSize - 10);
                g2d.drawLine(x_offset + 10, y_offset + (i * cellSize), x_offset + iconSize - 10, y_offset + (i * cellSize));
            }
        }
    }
}

class DBConnection {
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        try {
            String url = "jdbc:mysql://localhost:3306/data";
            String user = "root";
            String pass = "1234";

            Class.forName("com.mysql.cj.jdbc.Driver");

            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            props.setProperty("useSSL", "false");
            props.setProperty("allowPublicKeyRetrieval", "true");

            return DriverManager.getConnection(url, props);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("MySQL JDBC Driver not found. Please add it to your project.");
        } catch (SQLException e) {
            throw new SQLException("Database connection failed: " + e.getMessage());
        }
    }
    private DBConnection() {}
}
