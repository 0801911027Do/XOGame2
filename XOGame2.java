import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.Border;

public class XOGame2 extends JFrame {
    private final int SIZE = 3;
    private final JButton[][] buttons = new JButton[SIZE][SIZE];
    private boolean playerXTurn = true;
    private boolean againstAI = false;

    private enum Difficulty {EASY, MEDIUM, HARD}
    private Difficulty currentDifficulty = Difficulty.EASY;

    private final JPanel mainPanel = new JPanel(new CardLayout());
    private final CardLayout cardLayout = (CardLayout) mainPanel.getLayout();
    private JPanel gridPanel; 

    private String registeredUsername = "";
    private String registeredPassword = "";

    private static class WinInfo {
        enum WinType {NONE, ROW, COLUMN, DIAG_1, DIAG_2}
        WinType type = WinType.NONE;
        int index = -1;
    }
    private WinInfo winInfo = new WinInfo();

    private int scoreX = 0;
    private int scoreO = 0;

    // Add score labels as instance variables
    private JLabel scoreLabelX;
    private JLabel scoreLabelO;

    private final Color primaryColor = new Color(0x5DADE2);
    private final Color secondaryColor = new Color(0x1ABC9C);
    private final Color fontColor = new Color(0xECF0F1);
    private final Color neonGlow = new Color(255, 0, 255);
    private final Color colorX = new Color(0, 255, 255);
    private final Color colorO = new Color(255, 29, 39);


    public XOGame2() {
        setTitle("XO Game - Neon AI Edition");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        mainPanel.add(buildLoginPanel(), "Login");
        mainPanel.add(buildMainMenu(), "MainMenu");
        mainPanel.add(buildDifficultySelect(), "DifficultySelect");
        mainPanel.add(buildGameBoard(), "Game");

        add(mainPanel);
        cardLayout.show(mainPanel, "Login");
        setVisible(true);
    }

    static class GradientPanel extends JPanel {
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
        JPanel panel = new GradientPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel loginTitle = new JLabel("User Login", SwingConstants.CENTER);
        loginTitle.setFont(new Font("Arial", Font.BOLD, 36));
        loginTitle.setForeground(Color.WHITE);
        panel.add(loginTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(fontColor);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(userLabel, gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField();
        panel.add(usernameField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel passLabel = new JLabel("Password (min 8 chars):");
        passLabel.setForeground(fontColor);
        passLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(passLabel, gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField();
        panel.add(passwordField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);

        JButton registerButton = createModernButton("Register", primaryColor);
        JButton loginButton = createModernButton("Login", primaryColor);

        registerButton.addActionListener((ActionEvent e) -> {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword());
            if (user.isEmpty() || pass.length() < 8) {
                JOptionPane.showMessageDialog(title, "Username cannot be empty and password must be at least 8 characters.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            } else {
                registeredUsername = user;
                registeredPassword = pass;
                JOptionPane.showMessageDialog(title, "Registration Successful! Please Login.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        loginButton.addActionListener((ActionEvent e) -> {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword());
            if (user.equals(registeredUsername) && pass.equals(registeredPassword) && !user.isEmpty()) {
                cardLayout.show(mainPanel, "MainMenu");
            } else {
                JOptionPane.showMessageDialog(title, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(registerButton);
        btnPanel.add(loginButton);
        panel.add(btnPanel, gbc);

        return panel;
    }

    private JPanel buildMainMenu() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout());

        
        title = new JLabel("XO Tic Tac Toe", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(Color.WHITE);
        ((JComponent) title).setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);

        // XO Example Image (centered, with winning line)
        JPanel xoExamplePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 30;
                int cell = size / 3;
                int x0 = (getWidth() - size) / 2;
                int y0 = (getHeight() - size) / 2;

                // Draw grid
                g2.setStroke(new BasicStroke(5));
                g2.setColor(new Color(0x00FFFF));
                g2.drawRect(x0, y0, size, size);
                g2.drawLine(x0 + cell, y0, x0 + cell, y0 + size);
                g2.drawLine(x0 + 2 * cell, y0, x0 + 2 * cell, y0 + size);
                g2.drawLine(x0, y0 + cell, x0 + size, y0 + cell);
                g2.drawLine(x0, y0 + 2 * cell, x0 + size, y0 + 2 * cell);

                // Draw X and O
                Font xoFont = new Font("Arial", Font.BOLD, cell - 10);
                g2.setFont(xoFont);

                // Example board
                String[][] board = {
                    {"O", "X", ""},
                    {"", "O", "X"},
                    {"X", "", "O"}
                };
                Color colorO = new Color(255, 29, 39);
                Color colorX = new Color(0, 255, 255);

                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        String s = board[i][j];
                        if (!s.isEmpty()) {
                            g2.setColor(s.equals("O") ? colorO : colorX);
                            int sx = x0 + j * cell + cell / 2 - g2.getFontMetrics().stringWidth(s) / 2;
                            int sy = y0 + i * cell + cell / 2 + g2.getFontMetrics().getAscent() / 2 - 10;
                            g2.drawString(s, sx, sy);
                        }
                    }
                }

                // Draw winning line (diagonal)
                g2.setColor(new Color(255, 255, 0));
                g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x0 + 20, y0 + 20, x0 + size - 20, y0 + size - 20);
            }
        };
        xoExamplePanel.setOpaque(false);
        xoExamplePanel.setPreferredSize(new Dimension(340, 180));
        panel.add(xoExamplePanel, BorderLayout.CENTER);

        // Buttons panel (vertical)
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(20, 80, 40, 80));

        // Two Players button (orange, on top)
        JButton play2PButton = createModernButton("👤 vs 👤 (Two Players)", new Color(0xFFA726));
        play2PButton.setFont(new Font("Segoe UI Emoji", Font.BOLD, 26));
        play2PButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        play2PButton.addActionListener(e -> {
            againstAI = false;
            resetBoard();
            cardLayout.show(mainPanel, "Game");
        });

        // AI button (blue, below)
        JButton playAIButton = createModernButton("👤 vs 🤖 (AI)", primaryColor);
        playAIButton.setFont(new Font("Segoe UI Emoji", Font.BOLD, 26));
        playAIButton.setAlignmentX(Component.CENTER_ALIGNMENT);
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

    // Easy
    JLabel easyIcon = new JLabel("\u263A", SwingConstants.CENTER); // ☺
    easyIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 56));
    easyIcon.setForeground(new Color(0x2ECC71));
    gbc.gridy = 1;
    panel.add(easyIcon, gbc);

    JButton easy = createDifficultyButton("EASY", new Color(0x2ECC71));
    easy.setFont(new Font("Arial", Font.BOLD, 32));
    easy.setForeground(Color.WHITE);
    gbc.gridy = 2;
    panel.add(easy, gbc);

    // Medium
    JLabel mediumIcon = new JLabel("\u263A", SwingConstants.CENTER); // ☺
    mediumIcon.setFont(new Font("Arial", Font.BOLD, 56));
    mediumIcon.setForeground(new Color(0xF1C40F));
    gbc.gridy = 3;
    panel.add(mediumIcon, gbc);

    JButton medium = createDifficultyButton("MEDIUM", new Color(0xF1C40F));
    medium.setFont(new Font("Arial", Font.BOLD, 32));
    medium.setForeground(Color.WHITE);
    gbc.gridy = 4;
    panel.add(medium, gbc);

    // Hard
    JLabel hardIcon = new JLabel("\u263A", SwingConstants.CENTER); // ☺
    hardIcon.setFont(new Font("Arial", Font.BOLD, 56));
    hardIcon.setForeground(new Color(0xE74C3C));
    gbc.gridy = 5;
    panel.add(hardIcon, gbc);

    JButton hard = createDifficultyButton("HARD", new Color(0xE74C3C));
    hard.setFont(new Font("Arial", Font.BOLD, 32));
    hard.setForeground(Color.WHITE);
    gbc.gridy = 6;
    panel.add(hard, gbc);

    JButton back = createModernButton("🔙 Back to Menu", primaryColor);
    gbc.gridy = 7;
    panel.add(back, gbc);

    ActionListener difficultyHandler = e -> {
        String cmd = ((JButton) e.getSource()).getText();
        switch (cmd) {
            case "EASY" -> currentDifficulty = Difficulty.EASY;
            case "MEDIUM" -> currentDifficulty = Difficulty.MEDIUM;
            case "HARD" -> currentDifficulty = Difficulty.HARD;
            default -> {
            }
        }
        againstAI = true;
        resetBoard();
        cardLayout.show(mainPanel, "Game");
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

        gridPanel = new JPanel(new GridLayout(SIZE, SIZE, 5, 5)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_RENDER_QUALITY);

                g2d.setStroke(new BasicStroke(5));
                g2d.setColor(neonGlow);
                int width = getWidth();
                int height = getHeight();
                g2d.drawLine(width / 3, 0, width / 3, height);
                g2d.drawLine(width * 2 / 3, 0, width * 2 / 3, height);
                g2d.drawLine(0, height / 3, width, height / 3);
                g2d.drawLine(0, height * 2 / 3, width, height * 2 / 3);

                if (winInfo.type != WinInfo.WinType.NONE) {
                    drawWinningLine(g2d);
                }
            }
        };

        gridPanel.setOpaque(false);

        Font font = new Font("Arial", Font.BOLD, 80);
        Border emptyBorder = BorderFactory.createEmptyBorder();

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                JButton btn = new JButton("");
                btn.setFont(font);
                btn.setFocusPainted(false);
                btn.setBorder(emptyBorder);
                btn.setContentAreaFilled(false);

                int row = i, col = j;
                btn.addActionListener(e -> handleMove(row, col));

                buttons[i][j] = btn;
                gridPanel.add(btn);
            }
        }
        
        JButton backButton = createModernButton("🔙 Menu", primaryColor);
        JButton resetButton = createModernButton("🔁 Reset", primaryColor);
        JButton settingsButton = createModernButton("⚙️ Settings", primaryColor);
        JButton resetScoreButton = createModernButton("🧹 Reset Score", new Color(0x8E44AD)); // ปุ่มรีเซ็ตคะแนน

        backButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "MainMenu");
            resetBoard();
        });

        resetButton.addActionListener(e -> resetBoard());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        topPanel.add(backButton);
        topPanel.add(resetButton);
        topPanel.add(settingsButton);

        // Score Panel
        JPanel scorePanel = new JPanel();
        scorePanel.setOpaque(false);
        scoreLabelX = new JLabel("X: " + scoreX);
        scoreLabelX.setFont(new Font("Arial", Font.BOLD, 28));
        scoreLabelX.setForeground(colorX);
        scoreLabelO = new JLabel("O: " + scoreO);
        scoreLabelO.setFont(new Font("Arial", Font.BOLD, 28));
        scoreLabelO.setForeground(colorO);
        scorePanel.add(scoreLabelX);
        scorePanel.add(scoreLabelO);

        // รวม topPanel และ scorePanel ใน panel เดียว
        JPanel northPanel = new JPanel();
        northPanel.setOpaque(false);
        northPanel.setLayout(new BorderLayout());
        northPanel.add(topPanel, BorderLayout.NORTH);
        northPanel.add(scorePanel, BorderLayout.SOUTH);

        panel.add(northPanel, BorderLayout.NORTH);
        panel.add(gridPanel, BorderLayout.CENTER);
        return panel;
    }
    /**
     * 
     */

    /**
     * Draws a line over the winning combination on the game grid.
     * The line color corresponds to the player who won (X or O).
     * The line is drawn with rounded caps and joins, and its position and orientation
     * depend on the type and index of the winning combination (row, column, or diagonal).
     *
     * @param g2d the Graphics2D context used for drawing the winning line
     */
    private void drawWinningLine(Graphics2D g2d) {
        if (playerXTurn) {
            g2d.setColor(colorX);
        } else {
            g2d.setColor(colorO);
        }
        g2d.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int w = gridPanel.getWidth();
        int h = gridPanel.getHeight();
        int cellW = w / 3;
        int cellH = h / 3;
        int offset = 20;

        switch (winInfo.type) {
            case ROW: {
                int y = cellH * winInfo.index + cellH / 2;
                g2d.drawLine(offset, y, w - offset, y);
                break;
            }
            case COLUMN: {
                int x = cellW * winInfo.index + cellW / 2;
                g2d.drawLine(x, offset, x, h - offset);
                break;
            }
            case DIAG_1:
                g2d.drawLine(offset, offset, w - offset, h - offset);
                break;
            case DIAG_2:
                g2d.drawLine(w - offset, offset, offset, h - offset);
                break;
            case NONE:
            default:
                break;
        }
    }

    private void resetBoard() {
        playerXTurn = true;
        winInfo = new WinInfo();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
            }
        }
        if (gridPanel != null) {
            gridPanel.repaint();
        }
    }

    private void disableAllButtons() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                buttons[i][j].setEnabled(false);
            }
        }
    }

    private void handleMove(int row, int col) {
        if (!buttons[row][col].getText().isEmpty() || winInfo.type != WinInfo.WinType.NONE) return;

        String currentSymbol = playerXTurn ? "X" : "O";
        Color currentColor = playerXTurn ? colorX : colorO;

        buttons[row][col].setText(currentSymbol);
        buttons[row][col].setForeground(currentColor);

        if (checkWin(currentSymbol)) {
            disableAllButtons();
            gridPanel.repaint();
            if (currentSymbol.equals("X")) scoreX++;
            else scoreO++;
            updateScoreLabels();
            String winnerMessage = playerXTurn ? (againstAI ? "You win!" : "Player X wins!") : (againstAI ? "🤖 AI wins!" : "Player O wins!");
            endGame(winnerMessage);
            return;
        }

        if (isBoardFull()) {
            endGame("It's a Draw!");
            return;
        }

        playerXTurn = !playerXTurn;

        if (againstAI && !playerXTurn) {
            Timer timer = new Timer(500, e -> makeAIMove());
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void makeAIMove() {
        if (playerXTurn) return;

        switch (currentDifficulty) {
            case EASY -> easyMove();
            case MEDIUM -> mediumMove();
            case HARD -> hardMove();
        }
    }

    private void endGame(String message) {
        showResultDialog(message);
    }

    /**
     * @param message
     */
    private void showResultDialog(String message) {
        JDialog dialog = new JDialog((Frame) title, "Game Over", true);
        dialog.setUndecorated(true);
        dialog.setSize(350, 300);
        dialog.setLocationRelativeTo(title);

        // Determine winner and crown color
        String winText = "DRAW";
        final Color[] crownColor = { Color.YELLOW };
        if (message.contains("Player X") || (message.contains("win") && playerXTurn && !againstAI)) {
            winText = "X";
            crownColor[0] = new Color(0x2980B9); // Blue
        } else if (message.contains("Player O") || (message.contains("win") && !playerXTurn && !againstAI)) {
            winText = "O";
            crownColor[0] = new Color(0xE74C3C); // Red
        } else if (message.contains("AI wins")) {
            winText = "O";
            crownColor[0] = new Color(0xE74C3C); // Red
        } else if (message.contains("You win!")) {
            winText = "X";
            crownColor[0] = new Color(0x2980B9); // Blue
        }

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(30, 30, 60, 230));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel crown = new JLabel("\uD83D\uDC51", SwingConstants.CENTER);
        crown.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        crown.setAlignmentX(Component.CENTER_ALIGNMENT);
        crown.setForeground(crownColor[0]);

        // Custom painting for colored crown
        crown = new JLabel("\uD83D\uDC51", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(crownColor[0]);
                g.setFont(getFont());
                g.drawString(getText(), 0, getHeight() - 10);
            }
        };
        crown.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        crown.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel winLabel = new JLabel(winText, SwingConstants.CENTER);
        winLabel.setFont(new Font("Arial", Font.BOLD, 48));
        winLabel.setForeground(crownColor[0]);
        winLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton homeBtn = createModernButton("HOME", new Color(0x2980B9));
        homeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        homeBtn.setMaximumSize(new Dimension(200, 50));
        homeBtn.addActionListener(e -> {
            dialog.dispose();
            cardLayout.show(mainPanel, "MainMenu");
            resetBoard();
        });

        JButton replayBtn = createModernButton("REPLAY", new Color(0x27AE60));
        replayBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        replayBtn.setMaximumSize(new Dimension(200, 50));
        replayBtn.addActionListener(e -> {
            dialog.dispose();
            resetBoard();
        });

        panel.add(crown);
        panel.add(Box.createVerticalStrut(10));
        panel.add(winLabel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(homeBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(replayBtn);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void setAIMove(int row, int col) {
        handleMove(row, col);
    }

    private void easyMove() {
        List<Point> emptyCells = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
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
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
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
        int bestScore = Integer.MIN_VALUE;
        Point bestMove = null;

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    buttons[i][j].setText("O");
                    int score = minimax(false);
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

    private int minimax(boolean isMaximizing) {
        if (isHypotheticalWin("O")) return 10; // ใช้เมธอดสำหรับ AI
        if (isHypotheticalWin("X")) return -10; // ใช้เมธอดสำหรับ AI
        if (isBoardFull()) return 0;

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (buttons[i][j].getText().isEmpty()) {
                        buttons[i][j].setText("O");
                        bestScore = Math.max(bestScore, minimax(false));
                        buttons[i][j].setText("");
                    }
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (buttons[i][j].getText().isEmpty()) {
                        buttons[i][j].setText("X");
                        bestScore = Math.min(bestScore, minimax(true));
                        buttons[i][j].setText("");
                    }
                }
            }
            return bestScore;
        }
    }

    private boolean isBoardFull() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkWin(String symbol) {
        return isWinningState(symbol);
    }

    private boolean isWinningState(String symbol) {
        for (int i = 0; i < SIZE; i++) {
            if (buttons[i][0].getText().equals(symbol) && buttons[i][1].getText().equals(symbol) && buttons[i][2].getText().equals(symbol)) {
                winInfo.type = WinInfo.WinType.ROW;
                winInfo.index = i;
                return true;
            }
        }
        for (int i = 0; i < SIZE; i++) {
            if (buttons[0][i].getText().equals(symbol) && buttons[1][i].getText().equals(symbol) && buttons[2][i].getText().equals(symbol)) {
                winInfo.type = WinInfo.WinType.COLUMN;
                winInfo.index = i;
                return true;
            }
        }
        if (buttons[0][0].getText().equals(symbol) && buttons[1][1].getText().equals(symbol) && buttons[2][2].getText().equals(symbol)) {
            winInfo.type = WinInfo.WinType.DIAG_1;
            return true;
        }
        if (buttons[0][2].getText().equals(symbol) && buttons[1][1].getText().equals(symbol) && buttons[2][0].getText().equals(symbol)) {
            winInfo.type = WinInfo.WinType.DIAG_2;
            return true;
        }

        winInfo.type = WinInfo.WinType.NONE;
        return false;
    }

    private boolean isHypotheticalWin(String symbol) {
        for (int i = 0; i < SIZE; i++) {
            if (buttons[i][0].getText().equals(symbol) && buttons[i][1].getText().equals(symbol) && buttons[i][2].getText().equals(symbol)) {
                return true;
            }
        }
        for (int i = 0; i < SIZE; i++) {
            if (buttons[0][i].getText().equals(symbol) && buttons[1][i].getText().equals(symbol) && buttons[2][i].getText().equals(symbol)) {
                return true;
            }
        }
        if (buttons[0][0].getText().equals(symbol) && buttons[1][1].getText().equals(symbol) && buttons[2][2].getText().equals(symbol)) {
            return true;
        }
        return buttons[0][2].getText().equals(symbol) && buttons[1][1].getText().equals(symbol) && buttons[2][0].getText().equals(symbol);
    }

    private void updateScoreLabels() {
        if (scoreLabelX != null) scoreLabelX.setText("X: " + scoreX);
        if (scoreLabelO != null) scoreLabelO.setText("O: " + scoreO);
    }
    /**
     * Main method to start the game.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
        }
        invokeLater(XOGame2::new);
    }
}