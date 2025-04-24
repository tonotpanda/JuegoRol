package forms;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Juego {
    private JPanel panelMain;
    private JLabel messageLabel;
    private JPanel dungeonPanel;

    private String selectedRole;
    private String playerName;

    private static final String WARRIOR_IMAGE = "src/images/warrior/warriorPic.png";
    private static final String WIZARD_IMAGE = "src/images/wizard/wizardPic.png";
    private static final String PRIEST_IMAGE = "src/images/priest/priestPic.png";
    private static final String TILE_WALL = "src/images/dungeon/tile001.png";
    private static final String TILE_FLOOR = "src/images/dungeon/tile004.png";
    private static final String DOLLAR_ICON = "src/images/dungeon/dollar.png";
    private static final String HEART_ICON = "src/images/dungeon/heart.png";
    private static final String MITRA_ICON = "src/images/dungeon/mitra.png";
    private static final String POTION_ICON = "src/images/dungeon/potion.png";
    private static final String SWORD_ICON = "src/images/dungeon/sword.png";

    public Juego() {
        panelMain = new JPanel(new BorderLayout());
        panelMain.setPreferredSize(new Dimension(1000, 800));
        panelMain.setBackground(new Color(245, 245, 245));

        initHeader();
        characterSelection();
    }

    private void initHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(245, 245, 245));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.GRAY));

        messageLabel = new JLabel("Elige tu personaje", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 28));
        messageLabel.setForeground(new Color(60, 60, 60));
        headerPanel.add(messageLabel);

        panelMain.add(headerPanel, BorderLayout.NORTH);
    }

    private void characterSelection() {
        JPanel charactersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20));
        charactersPanel.setBackground(new Color(245, 245, 245));

        charactersPanel.add(createCharacterButton("Guerrero", WARRIOR_IMAGE));
        charactersPanel.add(createCharacterButton("Mago", WIZARD_IMAGE));
        charactersPanel.add(createCharacterButton("Curandero", PRIEST_IMAGE));

        panelMain.add(charactersPanel, BorderLayout.CENTER);
    }

    private JButton createCharacterButton(String role, String imagePath) {
        JButton button = new JButton();
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(250, 350));

        ImageIcon icon = new ImageIcon(imagePath);
        Image scaled = icon.getImage().getScaledInstance(230, 330, Image.SCALE_SMOOTH);
        button.setIcon(new ImageIcon(scaled));

        button.addActionListener(e -> handleCharacterSelection(role));
        return button;
    }

    private void handleCharacterSelection(String role) {
        this.selectedRole = role;

        messageLabel.setText("Has elegido: " + role);

        String name = JOptionPane.showInputDialog(panelMain,
                "Escribe un nombre para tu " + role.toLowerCase() + ":",
                "Nombre del personaje",
                JOptionPane.PLAIN_MESSAGE);

        if (name != null && !name.trim().isEmpty()) {
            this.playerName = name;
            messageLabel.setText("Personaje: " + name + " (" + role + ")");
            panelMain.removeAll();
            panelMain.revalidate();
            panelMain.repaint();
            loadDungeon();
        } else {
            messageLabel.setText("Has elegido: " + role + " (sin nombre)");
        }
    }

    private void loadDungeon() {
        int numRows = 10, numCols = 10;
        dungeonPanel = new JPanel(new GridLayout(numRows, numCols, 0, 0));
        dungeonPanel.setBackground(Color.BLACK);

        int tileWidth = 1000 / numCols;
        int tileHeight = 800 / numRows;

        ImageIcon wallIcon = new ImageIcon(TILE_WALL);
        ImageIcon floorIcon = new ImageIcon(TILE_FLOOR);
        Image scaledWallImage = wallIcon.getImage().getScaledInstance(tileWidth, tileHeight, Image.SCALE_SMOOTH);
        Image scaledFloorImage = floorIcon.getImage().getScaledInstance(tileWidth, tileHeight, Image.SCALE_SMOOTH);

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                JLabel tileLabel = new JLabel();
                tileLabel.setHorizontalAlignment(SwingConstants.CENTER);
                tileLabel.setVerticalAlignment(SwingConstants.CENTER);

                boolean isBorder = row == 0 || row == numRows - 1 || col == 0 || col == numCols - 1;
                ImageIcon icon = isBorder ? new ImageIcon(scaledWallImage) : new ImageIcon(scaledFloorImage);
                tileLabel.setIcon(icon);

                tileLabel.putClientProperty("type", isBorder ? "wall" : "floor");

                dungeonPanel.add(tileLabel);
            }
        }

        panelMain.add(dungeonPanel, BorderLayout.CENTER);
        panelMain.revalidate();
        panelMain.repaint();

        loadItemsMap(numRows, numCols);
    }

    private void loadItemsMap(int numRows, int numCols) {
        List<Point> floorTiles = new ArrayList<>();

        for (int row = 1; row < numRows - 1; row++) {
            for (int col = 1; col < numCols - 1; col++) {
                JLabel tileLabel = (JLabel) dungeonPanel.getComponent(row * numCols + col);
                if ("floor".equals(tileLabel.getClientProperty("type"))) {
                    floorTiles.add(new Point(row, col));
                }
            }
        }

        Collections.shuffle(floorTiles);

        if (selectedRole.equals("Guerrero") && !floorTiles.isEmpty()) {
            placeItemOnTile(floorTiles.remove(0), SWORD_ICON);
        } else if (selectedRole.equals("Mago") && !floorTiles.isEmpty()) {
            placeItemOnTile(floorTiles.remove(0), POTION_ICON);
        } else if (selectedRole.equals("Curandero") && !floorTiles.isEmpty()) {
            placeItemOnTile(floorTiles.remove(0), MITRA_ICON);
        }

        if (!floorTiles.isEmpty()) placeItemOnTile(floorTiles.remove(0), DOLLAR_ICON);
        if (!floorTiles.isEmpty()) placeItemOnTile(floorTiles.remove(0), HEART_ICON);
    }

    private void placeItemOnTile(Point position, String itemIconPath) {
        try {
            int row = position.x;
            int col = position.y;
            int index = row * 10 + col;
            JLabel tileLabel = (JLabel) dungeonPanel.getComponent(index);

            // Cargar imágenes con ImageIO (más confiable que ImageIcon)
            BufferedImage floorImage = ImageIO.read(new File(TILE_FLOOR));
            BufferedImage itemImage = ImageIO.read(new File(itemIconPath));

            // Obtener dimensiones del tile
            int width = tileLabel.getWidth() > 0 ? tileLabel.getWidth() : 100;
            int height = tileLabel.getHeight() > 0 ? tileLabel.getHeight() : 80;

            // Escalar imágenes
            Image scaledFloor = floorImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            int itemSize = Math.min(width, height) / 2;
            Image scaledItem = itemImage.getScaledInstance(itemSize, itemSize, Image.SCALE_SMOOTH);

            // Crear imagen combinada
            BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = combined.createGraphics();

            // Configurar renderizado de alta calidad
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Dibujar el piso
            g.drawImage(scaledFloor, 0, 0, null);

            // Dibujar el ítem centrado
            int x = (width - itemSize) / 2;
            int y = (height - itemSize) / 2;
            g.drawImage(scaledItem, x, y, null);

            g.dispose();

            // Actualizar el icono del tile
            tileLabel.setIcon(new ImageIcon(combined));

        } catch (IOException e) {
            System.err.println("Error al cargar imágenes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Juego");
            frame.setContentPane(new Juego().panelMain);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            Image icon = Toolkit.getDefaultToolkit().getImage("src/images/politecnics.png");
            frame.setIconImage(icon);
        });
    }
}
