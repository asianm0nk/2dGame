package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class GamePanel extends JPanel {
    private final Map<String, GameClient.Player> players;
    private final String clientId;

    public GamePanel(Map<String, GameClient.Player> players, String clientId) {
        this.players = players;
        this.clientId = clientId;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (var entry : players.entrySet()) {
            GameClient.Player p = entry.getValue();

            g.setColor(entry.getKey().equals(clientId) ? Color.RED : Color.BLUE);
            g.fillRect(p.x, p.y, 20, 20);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString(p.name, p.x, p.y + 30);
        }
    }
}
