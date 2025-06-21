// client/GameClient.java
package org.example;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class GameClient extends JFrame {
    private final int TILE = 40;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String role = "RED";
    private Point myPos = new Point(1, 1);
    private Point otherPos = new Point(13, 5);
    private Point button = new Point(7, 3);
    private Point door = new Point(8, 1);
    private boolean doorOpen = false;

    private final char[][] map = {
            "################".toCharArray(),
            "#........D.....#".toCharArray(),
            "#..............#".toCharArray(),
            "#......K.......#".toCharArray(),
            "#..............#".toCharArray(),
            "#..............#".toCharArray(),
            "################".toCharArray()
    };

    public GameClient() {
        setTitle("Co-op Puzzle Client");
        setSize(640, 480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        GamePanel panel = new GamePanel();
        add(panel);
        connectToServer();
        setVisible(true);

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
                } else if (role.equals("BLUE")) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP -> dy = -1;
                        case KeyEvent.VK_DOWN -> dy = 1;
                        case KeyEvent.VK_LEFT -> dx = -1;
                        case KeyEvent.VK_RIGHT -> dx = 1;
                    }
                }

                int nx = myPos.x + dx;
                int ny = myPos.y + dy;
                if (isFree(nx, ny)) {
                    myPos.translate(dx, dy);
                    out.println("MOVE:" + myPos.x + "," + myPos.y);
                    panel.repaint();
                }
            }
        });

        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("ROLE:")) {
                        role = line.substring(5);
                    } else if (line.startsWith("STATIC:")) {
                        String[] parts = line.substring(7).split(";");
                        String[] b = parts[0].split(",");
                        String[] d = parts[1].split(",");
                        button = new Point(Integer.parseInt(b[0]), Integer.parseInt(b[1]));
                        door = new Point(Integer.parseInt(d[0]), Integer.parseInt(d[1]));
                    } else if (line.startsWith("STATE:")) {
                        String[] parts = line.split(";");
                        String[] p1 = parts[0].substring(6).split(",");
                        String[] p2 = parts[1].split(",");
                        doorOpen = parts[2].equals("DOOR:true");

                        if (role.equals("RED")) {
                            myPos = new Point(Integer.parseInt(p1[0]), Integer.parseInt(p1[1]));
                            otherPos = new Point(Integer.parseInt(p2[0]), Integer.parseInt(p2[1]));
                        } else {
                            myPos = new Point(Integer.parseInt(p2[0]), Integer.parseInt(p2[1]));
                            otherPos = new Point(Integer.parseInt(p1[0]), Integer.parseInt(p1[1]));
                        }
                        panel.repaint();
                    } else if (line.equals("WIN")) {
                        JOptionPane.showMessageDialog(this, "🎉 Победа! Оба игрока добрались до двери!");
                        System.exit(0);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private boolean isFree(int x, int y) {
        if (x < 0 || y < 0 || y >= map.length || x >= map[0].length) return false;
        char tile = map[y][x];
        if (tile == '#') return false;
        if (tile == 'D') return doorOpen;
        return true;
    }

    class GamePanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[0].length; x++) {
                    char tile = map[y][x];
                    if (x == door.x && y == door.y) tile = doorOpen ? '.' : 'D';
                    if (x == button.x && y == button.y) tile = 'K';

                    g.setColor(switch (tile) {
                        case '#' -> Color.DARK_GRAY;
                        case 'D' -> Color.GRAY;
                        case 'K' -> Color.GREEN;
                        default -> Color.LIGHT_GRAY;
                    });
                    g.fillRect(x * TILE, y * TILE, TILE, TILE);
                    g.setColor(Color.BLACK);
                    g.drawRect(x * TILE, y * TILE, TILE, TILE);
                }
            }

            // Другой игрок
            g.setColor(role.equals("RED") ? Color.BLUE : Color.RED);
            g.fillOval(otherPos.x * TILE + 5, otherPos.y * TILE + 5, TILE - 10, TILE - 10);

            // Текущий игрок
            g.setColor(role.equals("RED") ? Color.RED : Color.BLUE);
            g.fillOval(myPos.x * TILE + 5, myPos.y * TILE + 5, TILE - 10, TILE - 10);

            g.setColor(Color.BLACK);
            g.drawString("Я: " + role, 10, getHeight() - 10);
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5555);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Не удалось подключиться к серверу");
            System.exit(1);
        }
    }

    static class Point {
        int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
        public void translate(int dx, int dy) { x += dx; y += dy; }
        public boolean equals(Point p) { return x == p.x && y == p.y; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new);
    }
}
