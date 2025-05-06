package forms;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Juego {
    private JPanel panelMain;
    private JPanel infoPanel;
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

    private int playerRow, playerCol;
    private Component playerLabel;

    private final java.util.Map<String, String> roleFolderMap = java.util.Map.of(
            "Guerrero", "warrior",
            "Mago", "wizard",
            "Curandero", "priest"
    );

    private int playerHealth = 1;
    private int playerGold = 0;
    private List<String> playerItems = new ArrayList<>();

    // Variables para rastrear los ítems en el mapa
    private java.util.Map<Point, String> itemPositions = new java.util.HashMap<>();
    private Point heartPosition;
    private Point goldPosition;
    private Point classItemPosition;

    // Enemigos
    private Timer verticalEnemyTimer;
    private Timer horizontalEnemyTimer;

    private JLabel verticalEnemyLabel;
    private JLabel horizontalEnemyLabel;

    private int vStartRow, vCol;
    private int hStartRow, hStartCol;

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
        playerItems.clear();

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
        // Crear panel principal con BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Panel de información del jugador
        JPanel infoPanel = createPlayerInfoPanel();
        mainPanel.add(createPlayerInfoPanel(), BorderLayout.NORTH);

        // Panel del dungeon
        dungeonPanel = new JPanel(new GridLayout(10, 10, 0, 0));
        dungeonPanel.setBackground(Color.BLACK);

        // Resto del código para cargar el dungeon...
        int tileWidth = 1000 / 10;
        int tileHeight = (800 - infoPanel.getPreferredSize().height) / 10;

        ImageIcon wallIcon = new ImageIcon(TILE_WALL);
        ImageIcon floorIcon = new ImageIcon(TILE_FLOOR);
        Image scaledWallImage = wallIcon.getImage().getScaledInstance(tileWidth, tileHeight, Image.SCALE_SMOOTH);
        Image scaledFloorImage = floorIcon.getImage().getScaledInstance(tileWidth, tileHeight, Image.SCALE_SMOOTH);

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                JLabel tileLabel = new JLabel();
                tileLabel.setHorizontalAlignment(SwingConstants.CENTER);
                tileLabel.setVerticalAlignment(SwingConstants.CENTER);

                boolean isBorder = row == 0 || row == 9 || col == 0 || col == 9;
                ImageIcon icon = isBorder ? new ImageIcon(scaledWallImage) : new ImageIcon(scaledFloorImage);
                tileLabel.setIcon(icon);

                tileLabel.putClientProperty("type", isBorder ? "wall" : "floor");

                dungeonPanel.add(tileLabel);
            }
        }

        mainPanel.add(dungeonPanel, BorderLayout.CENTER);
        panelMain.add(mainPanel, BorderLayout.CENTER);

        panelMain.revalidate();
        panelMain.repaint();

        loadItemsMap(10, 10);
        loadEnemies();
    }

    private void loadEnemies() {
        // Enemigo vertical (no sobre el jugador)
        do {
            vStartRow = (int) (Math.random() * 6) + 2;
            vCol = (int) (Math.random() * 6) + 2;
        } while (vStartRow == playerRow && vCol == playerCol);

        verticalEnemyLabel = createEnemyLabel("src/images/skeleton/skeleton_down.gif");
        placeEnemyAt(verticalEnemyLabel, vStartRow, vCol);

        verticalEnemyTimer = new Timer(1000, new ActionListener() {
            int direction = 1;
            int currentRow = vStartRow;

            @Override
            public void actionPerformed(ActionEvent e) {
                removeEnemyFromTile(currentRow, vCol);
                currentRow += direction;

                if (currentRow <= 1 || currentRow >= 8) {
                    direction *= -1;
                }

                String gif = direction == 1 ? "skeleton_down.gif" : "skeleton_up.gif";
                verticalEnemyLabel.setIcon(new ImageIcon("src/images/skeleton/" + gif));
                placeEnemyAt(verticalEnemyLabel, currentRow, vCol);

                checkCollision(currentRow, vCol);
            }
        });
        verticalEnemyTimer.start();

        // Enemigo horizontal (no sobre el jugador)
        do {
            hStartRow = (int) (Math.random() * 6) + 2;
            hStartCol = (int) (Math.random() * 6) + 2;
        } while (hStartRow == playerRow && hStartCol == playerCol);

        horizontalEnemyLabel = createEnemyLabel("src/images/skeleton/skeleton_right.gif");
        placeEnemyAt(horizontalEnemyLabel, hStartRow, hStartCol);

        horizontalEnemyTimer = new Timer(1000, new ActionListener() {
            int direction = 1;
            int currentCol = hStartCol;

            @Override
            public void actionPerformed(ActionEvent e) {
                removeEnemyFromTile(hStartRow, currentCol);
                currentCol += direction;

                if (currentCol <= 1 || currentCol >= 8) {
                    direction *= -1;
                }

                String gif = direction == 1 ? "skeleton_right.gif" : "skeleton_left.gif";
                horizontalEnemyLabel.setIcon(new ImageIcon("src/images/skeleton/" + gif));
                placeEnemyAt(horizontalEnemyLabel, hStartRow, currentCol);

                checkCollision(hStartRow, currentCol);
            }
        });
        horizontalEnemyTimer.start();
    }

    private void checkCollision(int enemyRow, int enemyCol) {
        if (playerRow == enemyRow && playerCol == enemyCol) {
            playerHealth--;
            updateInfoPanel();
            flashPlayer();

            if (playerHealth <= 0) {
                gameOver();
            }
        }
    }

    private void flashPlayer() {
        Timer flashTimer = new Timer(200, new ActionListener() {
            int flashes = 0;
            boolean visible = true;

            @Override
            public void actionPerformed(ActionEvent e) {
                playerLabel.setVisible(visible);
                visible = !visible;
                flashes++;

                if (flashes >= 6) {
                    ((Timer) e.getSource()).stop();
                    playerLabel.setVisible(true);
                }
            }
        });
        flashTimer.start();
    }

    private void gameOver() {
        verticalEnemyTimer.stop();
        horizontalEnemyTimer.stop();
        JOptionPane.showMessageDialog(panelMain, "¡Game Over!", "Fin del juego", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private JLabel createEnemyLabel(String imagePath) {
        ImageIcon icon = new ImageIcon(imagePath);
        JLabel enemyLabel = new JLabel(icon);
        enemyLabel.setBounds(0, 0, 100, 80);
        return enemyLabel;
    }


    private void placeEnemyAt(JLabel enemy, int row, int col) {
        int index = row * 10 + col;
        Component tile = dungeonPanel.getComponent(index);

        if (tile instanceof JLayeredPane) {
            JLayeredPane pane = (JLayeredPane) tile;

            // Evitar duplicados
            for (Component comp : pane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER)) {
                if (comp == enemy) return;
            }

            if (enemy.getParent() != null) {
                enemy.getParent().remove(enemy);
            }

            pane.add(enemy, JLayeredPane.PALETTE_LAYER);
            pane.moveToFront(enemy);
            pane.revalidate();
            pane.repaint();
        } else {
            // Convertir tile a JLayeredPane
            JLayeredPane layeredPane = new JLayeredPane();
            layeredPane.setPreferredSize(new Dimension(100, 80));

            JLabel bg = new JLabel(((JLabel)tile).getIcon());
            bg.setBounds(0, 0, 100, 80);
            layeredPane.add(bg, JLayeredPane.DEFAULT_LAYER);
            layeredPane.add(enemy, JLayeredPane.PALETTE_LAYER);

            dungeonPanel.remove(index);
            dungeonPanel.add(layeredPane, index);
            dungeonPanel.revalidate();
            dungeonPanel.repaint();
        }
    }


    private void removeEnemyFromTile(int row, int col) {
        int index = row * 10 + col;
        Component tile = dungeonPanel.getComponent(index);

        if (tile instanceof JLayeredPane) {
            JLayeredPane pane = (JLayeredPane) tile;

            for (Component comp : pane.getComponentsInLayer(JLayeredPane.PALETTE_LAYER)) {
                if (comp == verticalEnemyLabel || comp == horizontalEnemyLabel) {
                    pane.remove(comp);
                }
            }

            pane.revalidate();
            pane.repaint();
        }
    }





    private JLabel createEnemyAt(int row, int col, ImageIcon icon) {
        int index = row * 10 + col;
        Component tile = dungeonPanel.getComponent(index);
        int width = tile.getWidth() > 0 ? tile.getWidth() : 100;
        int height = tile.getHeight() > 0 ? tile.getHeight() : 80;

        JLabel enemyLabel = new JLabel(icon);
        enemyLabel.setBounds(0, 0, width, height);

        if (tile instanceof JLayeredPane) {
            ((JLayeredPane)tile).add(enemyLabel, JLayeredPane.PALETTE_LAYER);
        } else {
            JLayeredPane layeredPane = new JLayeredPane();
            layeredPane.setPreferredSize(new Dimension(width, height));

            JLabel bg = new JLabel(((JLabel)tile).getIcon());
            bg.setBounds(0, 0, width, height);
            layeredPane.add(bg, JLayeredPane.DEFAULT_LAYER);
            layeredPane.add(enemyLabel, JLayeredPane.PALETTE_LAYER);

            dungeonPanel.remove(index);
            dungeonPanel.add(layeredPane, index);
        }

        return enemyLabel;
    }






    private JPanel createPlayerInfoPanel() {
        if (infoPanel == null) {
            infoPanel = new JPanel(new GridLayout(1, 4));
            infoPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            infoPanel.setPreferredSize(new Dimension(1000, 60));
            infoPanel.setBackground(new Color(50, 50, 50));

            // Nombre del jugador
            JLabel nameLabel = new JLabel("Nombre: " + playerName, SwingConstants.CENTER);
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            infoPanel.add(nameLabel);

            // Salud del jugador
            JLabel healthLabel = new JLabel("Vidas: " + playerHealth, SwingConstants.CENTER);
            healthLabel.setForeground(Color.WHITE);
            healthLabel.setFont(new Font("Arial", Font.BOLD, 14));
            infoPanel.add(healthLabel);

            // Oro del jugador
            JLabel goldLabel = new JLabel("Oro: " + playerGold, SwingConstants.CENTER);
            goldLabel.setForeground(Color.WHITE);
            goldLabel.setFont(new Font("Arial", Font.BOLD, 14));
            infoPanel.add(goldLabel);

            // Panel de objetos
            JPanel itemsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            itemsPanel.setOpaque(false);
            infoPanel.add(itemsPanel);
        } else {
            // Actualizar los valores si el panel ya existe
            updateInfoPanel();
        }

        return infoPanel;
    }

    private void addItemIcon(JPanel panel, String iconPath) {
        try {
            BufferedImage itemImage = ImageIO.read(new File(iconPath));
            Image scaledItem = itemImage.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            JLabel itemLabel = new JLabel(new ImageIcon(scaledItem));
            panel.add(itemLabel);
        } catch (IOException e) {
            System.err.println("Error al cargar icono: " + iconPath);
        }
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

        // Colocar ítem de clase y guardar su posición
        if (selectedRole.equals("Guerrero") && !floorTiles.isEmpty()) {
            classItemPosition = floorTiles.remove(0);
            placeItemOnTile(classItemPosition, SWORD_ICON);
            itemPositions.put(classItemPosition, "sword");
        } else if (selectedRole.equals("Mago") && !floorTiles.isEmpty()) {
            classItemPosition = floorTiles.remove(0);
            placeItemOnTile(classItemPosition, POTION_ICON);
            itemPositions.put(classItemPosition, "potion");
        } else if (selectedRole.equals("Curandero") && !floorTiles.isEmpty()) {
            classItemPosition = floorTiles.remove(0);
            placeItemOnTile(classItemPosition, MITRA_ICON);
            itemPositions.put(classItemPosition, "mitra");
        }

        // Colocar oro y guardar su posición
        if (!floorTiles.isEmpty()) {
            goldPosition = floorTiles.remove(0);
            placeItemOnTile(goldPosition, DOLLAR_ICON);
            itemPositions.put(goldPosition, "gold");
        }

        // Colocar corazón y guardar su posición
        if (!floorTiles.isEmpty()) {
            heartPosition = floorTiles.remove(0);
            placeItemOnTile(heartPosition, HEART_ICON);
            itemPositions.put(heartPosition, "heart");
        }

        // Posición inicial del jugador
        if (!floorTiles.isEmpty()) {
            Point start = floorTiles.remove(0);
            playerRow = start.x;
            playerCol = start.y;
            placePlayer(playerRow, playerCol, "down");
            setupKeyBindings();
        }
    }

    private void placeItemOnTile(Point position, String itemIconPath) {
        try {
            int row = position.x;
            int col = position.y;
            int index = row * 10 + col;
            JLabel tileLabel = (JLabel) dungeonPanel.getComponent(index);

            BufferedImage floorImage = ImageIO.read(new File(TILE_FLOOR));
            BufferedImage itemImage = ImageIO.read(new File(itemIconPath));

            int width = tileLabel.getWidth() > 0 ? tileLabel.getWidth() : 100;
            int height = tileLabel.getHeight() > 0 ? tileLabel.getHeight() : 80;

            Image scaledFloor = floorImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            int itemSize = Math.min(width, height) / 2;
            Image scaledItem = itemImage.getScaledInstance(itemSize, itemSize, Image.SCALE_SMOOTH);

            BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = combined.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g.drawImage(scaledFloor, 0, 0, null);
            int x = (width - itemSize) / 2;
            int y = (height - itemSize) / 2;
            g.drawImage(scaledItem, x, y, null);
            g.dispose();

            tileLabel.setIcon(new ImageIcon(combined));
        } catch (IOException e) {
            System.err.println("Error al cargar imágenes: " + e.getMessage());
        }
    }

    private void placePlayer(int row, int col, String direction) {
        int index = row * 10 + col;
        Component tileComponent = dungeonPanel.getComponent(index);

        // Restaurar el tile anterior
        if (playerLabel != null) {
            restoreTile(playerLabel);
        }

        try {
            String folder = roleFolderMap.getOrDefault(selectedRole, "warrior");
            String gifPath = "src/images/" + folder + "/" + folder + "_" + direction + ".gif";

            if (!new File(gifPath).exists()) {
                System.err.println("Archivo no encontrado: " + gifPath);
                return;
            }

            // Tamaños por defecto
            int defaultWidth = 100;
            int defaultHeight = 80;
            int width = tileComponent.getWidth() > 0 ? tileComponent.getWidth() : defaultWidth;
            int height = tileComponent.getHeight() > 0 ? tileComponent.getHeight() : defaultHeight;

            JLayeredPane layeredPane;
            if (tileComponent instanceof JLayeredPane) {
                layeredPane = (JLayeredPane) tileComponent;
                // Limpiar componentes excepto el fondo
                for (Component comp : layeredPane.getComponents()) {
                    if (layeredPane.getLayer(comp) != JLayeredPane.DEFAULT_LAYER) {
                        layeredPane.remove(comp);
                    }
                }
            } else {
                layeredPane = new JLayeredPane();
                layeredPane.setPreferredSize(new Dimension(width, height));

                // Crear y configurar el fondo
                JLabel backgroundLabel = new JLabel();
                backgroundLabel.setBounds(0, 0, width, height);

                // Transferir propiedades del tile original
                if (tileComponent instanceof JComponent) {
                    backgroundLabel.putClientProperty("type",
                            ((JComponent) tileComponent).getClientProperty("type"));
                }

                // Cargar imagen de fondo
                try {
                    BufferedImage floorImage = ImageIO.read(new File(TILE_FLOOR));
                    Image scaledFloor = floorImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    backgroundLabel.setIcon(new ImageIcon(scaledFloor));
                } catch (IOException e) {
                    System.err.println("Error al cargar suelo: " + e.getMessage());
                    backgroundLabel.setBackground(Color.GRAY);
                    backgroundLabel.setOpaque(true);
                }

                layeredPane.add(backgroundLabel, JLayeredPane.DEFAULT_LAYER);

                // Reemplazar componente
                dungeonPanel.remove(tileComponent);
                dungeonPanel.add(layeredPane, index);
            }

            // Asegurar que el fondo esté actualizado
            JLabel backgroundLabel = (JLabel) layeredPane.getComponent(0);
            if (backgroundLabel.getIcon() == null) {
                try {
                    BufferedImage floorImage = ImageIO.read(new File(TILE_FLOOR));
                    Image scaledFloor = floorImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    backgroundLabel.setIcon(new ImageIcon(scaledFloor));
                } catch (IOException e) {
                    System.err.println("Error al cargar suelo: " + e.getMessage());
                    backgroundLabel.setBackground(Color.GRAY);
                    backgroundLabel.setOpaque(true);
                }
            }

            // Añadir el personaje (GIF animado)
            JLabel playerGifLabel = new JLabel(new ImageIcon(gifPath));
            playerGifLabel.setBounds(0, 0, width, height);
            layeredPane.add(playerGifLabel, JLayeredPane.PALETTE_LAYER);
            layeredPane.moveToFront(playerGifLabel);

            playerLabel = layeredPane;

        } catch (Exception e) {
            System.err.println("Error al colocar jugador: " + e.getMessage());
        }

        dungeonPanel.revalidate();
        dungeonPanel.repaint();
    }


    private void restoreTile(Component tile) {
        if (tile instanceof JLayeredPane) {
            JLayeredPane pane = (JLayeredPane) tile;
            if (pane.getComponentCount() > 0) {
                Component background = pane.getComponent(0);
                if (background instanceof JLabel) {
                    JLabel backgroundLabel = (JLabel) background;
                    try {
                        BufferedImage floorImage = ImageIO.read(new File(TILE_FLOOR));
                        int width = pane.getWidth() > 0 ? pane.getWidth() : 100;
                        int height = pane.getHeight() > 0 ? pane.getHeight() : 80;
                        Image scaledFloor = floorImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                        backgroundLabel.setIcon(new ImageIcon(scaledFloor));
                    } catch (IOException e) {
                        System.err.println("Error al restaurar suelo: " + e.getMessage());
                        backgroundLabel.setBackground(Color.GRAY);
                        backgroundLabel.setOpaque(true);
                    }
                }
            }

            // Eliminar el personaje si existe
            for (Component comp : pane.getComponents()) {
                if (pane.getLayer(comp) == JLayeredPane.PALETTE_LAYER) {
                    pane.remove(comp);
                }
            }
        }
    }

    private void setupKeyBindings() {
        dungeonPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "moveUp");
        dungeonPanel.getActionMap().put("moveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playerRow > 1) {
                    playerRow--;
                    movePlayer("up");
                }
            }
        });

        dungeonPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "moveLeft");
        dungeonPanel.getActionMap().put("moveLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playerCol > 1) {
                    playerCol--;
                    movePlayer("left");
                }
            }
        });

        dungeonPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "moveDown");
        dungeonPanel.getActionMap().put("moveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playerRow < 8) {
                    playerRow++;
                    movePlayer("down");
                }
            }
        });

        dungeonPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "moveRight");
        dungeonPanel.getActionMap().put("moveRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playerCol < 8) {
                    playerCol++;
                    movePlayer("right");
                }
            }
        });
    }

    private void movePlayer(String direction) {
        Point newPosition = new Point(playerRow, playerCol);

        // Verificar si el jugador está sobre un ítem
        if (itemPositions.containsKey(newPosition)) {
            String itemType = itemPositions.get(newPosition);

            switch (itemType) {
                case "heart":
                    playerHealth++;
                    itemPositions.remove(newPosition);
                    restoreTile(dungeonPanel.getComponent(playerRow * 10 + playerCol));
                    break;

                case "gold":
                    playerGold += 10;
                    itemPositions.remove(newPosition);
                    restoreTile(dungeonPanel.getComponent(playerRow * 10 + playerCol));
                    break;

                case "sword":
                case "potion":
                case "mitra":
                    playerItems.add(itemType);
                    itemPositions.remove(newPosition);
                    restoreTile(dungeonPanel.getComponent(playerRow * 10 + playerCol));
                    break;
            }

            // Actualizar panel de información
            updateInfoPanel();
        }

        placePlayer(playerRow, playerCol, direction);
    }

    private void updateInfoPanel() {
        // Actualizar los componentes existentes en lugar de recrear el panel
        for (Component comp : infoPanel.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText().startsWith("Nombre: ")) {
                    label.setText("Nombre: " + playerName);
                } else if (label.getText().startsWith("Vidas: ")) {
                    label.setText("Vidas: " + playerHealth);
                } else if (label.getText().startsWith("Oro: ")) {
                    label.setText("Oro: " + playerGold);
                }
            } else if (comp instanceof JPanel) {
                // Actualizar el panel de ítems
                JPanel itemsPanel = (JPanel) comp;
                itemsPanel.removeAll();

                // Mostrar todos los objetos recolectados
                for (String item : playerItems) {
                    String iconPath = "";
                    switch (item) {
                        case "sword": iconPath = SWORD_ICON; break;
                        case "potion": iconPath = POTION_ICON; break;
                        case "mitra": iconPath = MITRA_ICON; break;
                    }
                    addItemIcon(itemsPanel, iconPath);
                }

                // Mostrar iconos de oro y corazón (aunque no sean objetos recolectados)
                addItemIcon(itemsPanel, DOLLAR_ICON);
                addItemIcon(itemsPanel, HEART_ICON);
            }
        }

        infoPanel.revalidate();
        infoPanel.repaint();
    }

    public JPanel getPanelMain() {
        return panelMain;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Juego juego = new Juego();
            JFrame frame = new JFrame("Dungeon Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(juego.getPanelMain());
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
