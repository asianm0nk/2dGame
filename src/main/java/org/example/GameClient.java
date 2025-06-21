package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class GameClient extends JFrame {
    private final int TILE = 32;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String role = "RED";
    private Point myPos = new Point(0, 0);
    private Point otherPos = new Point(0, 0);
    private Point block = new Point(0, 0);
    private boolean doorOpen = false;
    private final List<String> mapRows = new ArrayList<>();

    private final Map<Character, Image> tileSprites = new HashMap<>();
    private Image playerRed, playerBlue;

    private final JButton btnShowResults = new JButton("Показать результаты");
    private final String playerName;

    public GameClient() {
        playerName = JOptionPane.showInputDialog(this, "Введите имя игрока:", "Имя игрока", JOptionPane.PLAIN_MESSAGE);
        if (playerName == null || playerName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Имя обязательно для входа!");
            System.exit(0);
        }

        setTitle("Client - " + playerName);
        setSize(800, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        loadSprites();

        GamePanel panel = new GamePanel();
        add(panel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(btnShowResults);
        add(bottomPanel, BorderLayout.SOUTH);

        btnShowResults.addActionListener(e -> requestResults());

        connect();

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int dx = 0, dy = 0;
                if (role.equals("RED")) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W -> dy = -1;
                        case KeyEvent.VK_S -> dy = 1;
                        case KeyEvent.VK_A -> dx = -1;
                        case KeyEvent.VK_D -> dx = 1;
                    }
                } else {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP -> dy = -1;
                        case KeyEvent.VK_DOWN -> dy = 1;
                        case KeyEvent.VK_LEFT -> dx = -1;
                        case KeyEvent.VK_RIGHT -> dx = 1;
                    }
                }
                if (dx != 0 || dy != 0) {
                    out.println("MOVE:" + dx + "," + dy);
                }
            }
        });

        setVisible(true);
        panel.repaint();
    }

    private void loadSprites() {
        tileSprites.put('#', load("/sprites/wall.png"));
        tileSprites.put('.', load("/sprites/floor.png"));
        tileSprites.put('K', load("/sprites/button.png"));
        tileSprites.put('D', load("/sprites/door_closed.png"));
        tileSprites.put('O', load("/sprites/door_open.png"));
        tileSprites.put('C', load("/sprites/block.png"));
        tileSprites.put('^', load("/sprites/spikes.png"));
        tileSprites.put('1', load("/sprites/red_only.png"));
        tileSprites.put('2', load("/sprites/blue_only.png"));

        playerRed = load("/sprites/player_red.png");
        playerBlue = load("/sprites/player_blue.png");
    }

    private Image load(String path) {
        return new ImageIcon(Objects.requireNonNull(getClass().getResource(path))).getImage();
    }

    private void connect() {
        try {
            socket = new Socket("localhost", 5555);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Отправляем имя без префикса
            out.println(playerName);

            new Thread(() -> {
                try {
                    String line;
                    int mapLines = 0;
                    boolean readingResults = false;
                    List<String[]> resultsData = new ArrayList<>();

                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("ROLE:")) {
                            role = line.substring(5);
                        } else if (line.startsWith("MAP:")) {
                            mapLines = Integer.parseInt(line.substring(4));
                            mapRows.clear();
                        } else if (mapLines > 0) {
                            mapRows.add(line);
                            mapLines--;
                            if (mapLines == 0) repaint();
                        } else if (line.startsWith("STATE:")) {
                            String[] parts = line.substring(6).split(";");
                            String[] p1 = parts[0].split(",");
                            String[] p2 = parts[1].split(",");
                            String[] b = parts[2].substring(6).split(",");
                            boolean open = parts[3].substring(5).equals("true");

                            int px1 = Integer.parseInt(p1[0]);
                            int py1 = Integer.parseInt(p1[1]);
                            int px2 = Integer.parseInt(p2[0]);
                            int py2 = Integer.parseInt(p2[1]);
                            int bx = Integer.parseInt(b[0]);
                            int by = Integer.parseInt(b[1]);

                            if (role.equals("RED")) {
                                myPos = new Point(px1, py1);
                                otherPos = new Point(px2, py2);
                            } else {
                                myPos = new Point(px2, py2);
                                otherPos = new Point(px1, py1);
                            }
                            block = new Point(bx, by);
                            doorOpen = open;

                            repaint();
                        } else if (line.equals("WIN")) {
                            JOptionPane.showMessageDialog(this, "🎉 Победа!");
                            System.exit(0);
                        } else if (line.equals("RESULTS_START")) {
                            readingResults = true;
                            resultsData.clear();
                        } else if (line.equals("RESULTS_END")) {
                            readingResults = false;
                            SwingUtilities.invokeLater(() -> showResults(resultsData));
                        } else if (readingResults) {
                            String[] parts = line.split(",");
                            if (parts.length == 3) {
                                resultsData.add(parts);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Ошибка подключения к серверу");
            System.exit(1);
        }
    }

    private void requestResults() {
        if (out != null) {
            out.println("GET_RESULTS");
        }
    }

    private void showResults(List<String[]> resultsData) {
        String[] columns = {"Имя", "Время (сек)", "Дата"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        for (String[] row : resultsData) {
            model.addRow(row);
        }

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JFrame resultsFrame = new JFrame("Результаты игр");
        resultsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        resultsFrame.setSize(500, 400);
        resultsFrame.setLocationRelativeTo(this);
        resultsFrame.add(scrollPane);
        resultsFrame.setVisible(true);
    }

    class GamePanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int y = 0; y < mapRows.size(); y++) {
                for (int x = 0; x < mapRows.get(y).length(); x++) {
                    char tile = mapRows.get(y).charAt(x);
                    if (tile == 'D' && doorOpen) tile = 'O';

                    Image img = tileSprites.get(tile);
                    if (img != null) {
                        g.drawImage(img, x * TILE, y * TILE, TILE, TILE, null);
                    } else {
                        g.setColor(Color.PINK);
                        g.fillRect(x * TILE, y * TILE, TILE, TILE);
                    }
                }
            }

            // Блок
            g.drawImage(tileSprites.get('C'), block.x * TILE, block.y * TILE, TILE, TILE, null);

            // Игроки
            g.drawImage(role.equals("RED") ? playerRed : playerBlue, myPos.x * TILE, myPos.y * TILE, TILE, TILE, null);
            g.drawImage(role.equals("RED") ? playerBlue : playerRed, otherPos.x * TILE, otherPos.y * TILE, TILE, TILE, null);
        }
    }

    static class Point {
        int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new);
    }
}
