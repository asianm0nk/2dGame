// client/GameClient.java
package org.example;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final String clientId = UUID.randomUUID().toString();
    private String playerName;
    private final Map<String, Player> players = new ConcurrentHashMap<>();

    private int x = 100, y = 100;
    private final int PLAYER_SIZE = 20;

    public GameClient() {
        setTitle("Multiplayer Game (Swing)");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 👉 Ввод имени игрока
        playerName = JOptionPane.showInputDialog(this, "Введите имя игрока:");
        if (playerName == null || playerName.trim().isEmpty()) playerName = "Игрок";

        GamePanel panel = new GamePanel(players, clientId);
        add(panel);

        connectToServer();
        sendPosition(); // отправка начальной позиции

        // Управление игроком
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int dx = 0, dy = 0;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> dy = -5;
                    case KeyEvent.VK_S -> dy = 5;
                    case KeyEvent.VK_A -> dx = -5;
                    case KeyEvent.VK_D -> dx = 5;
                }

                // Коллизия с краями окна
                x = Math.max(0, Math.min(panel.getWidth() - PLAYER_SIZE, x + dx));
                y = Math.max(0, Math.min(panel.getHeight() - PLAYER_SIZE - 15, y + dy)); // -15 для текста

                sendPosition();
                panel.repaint();
            }
        });

        new Thread(this::listenFromServer).start();

        // Обновление экрана (30 FPS)
        Timer timer = new Timer(33, e -> panel.repaint());
        timer.start();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPosition() {
        String msg = clientId + ":" + x + "," + y + ":" + playerName;
        out.println(msg);
    }

    private void listenFromServer() {
        try {
            String input;
            while ((input = in.readLine()) != null) {
                String[] parts = input.split(":");
                if (parts.length == 3) {
                    String id = parts[0];
                    String[] coords = parts[1].split(",");
                    String name = parts[2];
                    if (coords.length == 2) {
                        int px = Integer.parseInt(coords[0]);
                        int py = Integer.parseInt(coords[1]);
                        players.put(id, new Player(px, py, name));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameClient().setVisible(true));
    }

    // 👤 Модель игрока
    public static class Player {
        int x, y;
        String name;
        public Player(int x, int y, String name) {
            this.x = x;
            this.y = y;
            this.name = name;
        }
    }
}
