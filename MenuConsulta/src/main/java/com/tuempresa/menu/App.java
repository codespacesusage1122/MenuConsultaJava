package com.tuempresa.menu;

// Importaciones de JavaFX para la interfaz gráfica
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Importaciones JDBC para la base de datos
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;


public class App extends Application {

    private static final String DB_URL = "jdbc:sqlite:menu.db";
    private TextArea resultDisplayArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Menú de Consulta de Productos con JavaFX");
        crearTablaYInsertarDatos();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar por nombre o categoría");
        searchField.setPrefWidth(250);

        Button searchByNameButton = new Button("Buscar por Nombre");
        Button searchByCategoryButton = new Button("Buscar por Categoría");
        Button showAllButton = new Button("Mostrar Todos");

        searchBox.getChildren().addAll(new Label("Búsqueda:"), searchField, searchByNameButton, searchByCategoryButton, showAllButton);
        root.setTop(searchBox);

        resultDisplayArea = new TextArea();
        resultDisplayArea.setEditable(false);
        resultDisplayArea.setPromptText("Los resultados de la búsqueda aparecerán aquí...");
        root.setCenter(resultDisplayArea);

        HBox actionButtonsBox = new HBox(10);
        actionButtonsBox.setAlignment(Pos.BOTTOM_RIGHT);
        actionButtonsBox.setPadding(new Insets(10));

        Button addItemButton = new Button("Agregar Nuevo Ítem");
        Button deleteItemButton = new Button("Eliminar Ítem por ID");

        actionButtonsBox.getChildren().addAll(addItemButton, deleteItemButton);
        root.setBottom(actionButtonsBox);

        searchByNameButton.setOnAction(e -> buscarItemsPorNombreFX(searchField.getText()));
        searchByCategoryButton.setOnAction(e -> buscarItemsPorCategoriaFX(searchField.getText()));
        showAllButton.setOnAction(e -> mostrarTodosItemsMenuFX());
        addItemButton.setOnAction(e -> agregarNuevoItemFX());
        deleteItemButton.setOnAction(e -> eliminarItemPorIdFX());

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void crearTablaYInsertarDatos() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String sqlCreateTable = "CREATE TABLE IF NOT EXISTS ITEMS_MENU (" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "nombre TEXT NOT NULL," +
                                    "descripcion TEXT," +
                                    "precio REAL," +
                                    "categoria TEXT" +
                                    ");";
            stmt.execute(sqlCreateTable);
            System.out.println("Tabla ITEMS_MENU creada o ya existente.");

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ITEMS_MENU;");
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Insertando datos de ejemplo...");
                String sqlInsert1 = "INSERT INTO ITEMS_MENU (nombre, descripcion, precio, categoria) VALUES ('Hamburguesa Clásica', 'Carne, lechuga, tomate, queso', 8.50, 'Plato Principal');";
                String sqlInsert2 = "INSERT INTO ITEMS_MENU (nombre, descripcion, precio, categoria) VALUES ('Pizza Margarita', 'Tomate, mozzarella, albahaca', 12.00, 'Plato Principal');";
                String sqlInsert3 = "INSERT INTO ITEMS_MENU (nombre, descripcion, precio, categoria) VALUES ('Ensalada César', 'Lechuga, pollo, crutones, aderezo', 7.00, 'Entrada');";
                String sqlInsert4 = "INSERT INTO ITEMS_MENU (nombre, descripcion, precio, categoria) VALUES ('Refresco', 'Coca-Cola, Pepsi, Sprite', 2.50, 'Bebida');";
                String sqlInsert5 = "INSERT INTO ITEMS_MENU (nombre, descripcion, precio, categoria) VALUES ('Tarta de Manzana', 'Postre casero con helado', 5.00, 'Postre');";

                stmt.execute(sqlInsert1);
                stmt.execute(sqlInsert2);
                stmt.execute(sqlInsert3);
                stmt.execute(sqlInsert4);
                stmt.execute(sqlInsert5);
                System.out.println("Datos de ejemplo insertados.");
            } else {
                System.out.println("La tabla ya contiene datos. No se insertaron datos de ejemplo.");
            }

        } catch (SQLException e) {
            System.err.println("Error al crear tabla o insertar datos: " + e.getMessage());
            showAlert("Error de DB", "Error al inicializar la base de datos: " + e.getMessage(), AlertType.ERROR);
        }
    }

    private void mostrarTodosItemsMenuFX() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Ítems del Menú Completos ---\n");
        String sql = "SELECT id, nombre, descripcion, precio, categoria FROM ITEMS_MENU;";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.isBeforeFirst()) {
                sb.append("No hay ítems en el menú.\n");
            } else {
                while (rs.next()) {
                    sb.append("ID: ").append(rs.getInt("id"))
                      .append(", Nombre: ").append(rs.getString("nombre"))
                      .append(", Descripción: ").append(rs.getString("descripcion"))
                      .append(", Precio: $").append(String.format("%.2f", rs.getDouble("precio")))
                      .append(", Categoría: ").append(rs.getString("categoria")).append("\n");
                }
            }
        } catch (SQLException e) {
            sb.append("Error al consultar ítems del menú: ").append(e.getMessage()).append("\n");
            System.err.println("Error al consultar ítems del menú: " + e.getMessage());
            showAlert("Error de Consulta", "Error al consultar ítems: " + e.getMessage(), AlertType.ERROR);
        }
        resultDisplayArea.setText(sb.toString());
    }

    private void buscarItemsPorNombreFX(String nombreBusqueda) {
        if (nombreBusqueda.trim().isEmpty()) {
            showAlert("Entrada Inválida", "Por favor, ingrese un nombre para buscar.", AlertType.WARNING);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--- Resultados de la Búsqueda por Nombre ('").append(nombreBusqueda).append("') ---\n");

        String terminoBusquedaSQL = getSpanishEquivalent(nombreBusqueda);

        String sql = "SELECT id, nombre, descripcion, precio, categoria FROM ITEMS_MENU WHERE LOWER(nombre) LIKE LOWER(?);";

        try (Connection conn = DriverManager.getConnection(DB_URL); // Corregido: DriverManager.getConnection
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + terminoBusquedaSQL + "%");
            ResultSet rs = pstmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                sb.append("No se encontraron ítems con ese nombre.\n");
            } else {
                while (rs.next()) {
                    sb.append("ID: ").append(rs.getInt("id"))
                      .append(", Nombre: ").append(rs.getString("nombre"))
                      .append(", Descripción: ").append(rs.getString("descripcion"))
                      .append(", Precio: $").append(String.format("%.2f", rs.getDouble("precio")))
                      .append(", Categoría: ").append(rs.getString("categoria")).append("\n");
                }
            }
        } catch (SQLException e) {
            sb.append("Error al buscar ítems por nombre: ").append(e.getMessage()).append("\n");
            System.err.println("Error al buscar ítems por nombre: " + e.getMessage());
            showAlert("Error de Consulta", "Error al buscar ítems por nombre: " + e.getMessage(), AlertType.ERROR);
        }
        resultDisplayArea.setText(sb.toString());
    }

    private void buscarItemsPorCategoriaFX(String categoriaBusqueda) {
        if (categoriaBusqueda.trim().isEmpty()) {
            showAlert("Entrada Inválida", "Por favor, ingrese una categoría para buscar.", AlertType.WARNING);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--- Resultados de la Búsqueda por Categoría ('").append(categoriaBusqueda).append("') ---\n");

        String sql = "SELECT id, nombre, descripcion, precio, categoria FROM ITEMS_MENU WHERE LOWER(categoria) LIKE LOWER(?);";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + categoriaBusqueda + "%");
            ResultSet rs = pstmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                sb.append("No se encontraron ítems en esa categoría.\n");
            } else {
                while (rs.next()) {
                    sb.append("ID: ").append(rs.getInt("id"))
                      .append(", Nombre: ").append(rs.getString("nombre"))
                      .append(", Descripción: ").append(rs.getString("descripcion"))
                      .append(", Precio: $").append(String.format("%.2f", rs.getDouble("precio")))
                      .append(", Categoría: ").append(rs.getString("categoria")).append("\n");
                }
            }
        } catch (SQLException e) {
            sb.append("Error al buscar ítems por categoría: ").append(e.getMessage()).append("\n");
            System.err.println("Error al buscar ítems por categoría: " + e.getMessage());
            showAlert("Error de Consulta", "Error al buscar ítems por categoría: " + e.getMessage(), AlertType.ERROR);
        }
        resultDisplayArea.setText(sb.toString());
    }

    private void agregarNuevoItemFX() {
        TextInputDialog dialogNombre = new TextInputDialog();
        dialogNombre.setTitle("Agregar Ítem");
        dialogNombre.setHeaderText("Ingrese los detalles del nuevo ítem.");
        dialogNombre.setContentText("Nombre:");
        Optional<String> resultNombre = dialogNombre.showAndWait();
        if (!resultNombre.isPresent() || resultNombre.get().trim().isEmpty()) {
            showAlert("Cancelado", "Operación cancelada o nombre vacío.", AlertType.INFORMATION);
            return;
        }
        String nombre = resultNombre.get().trim();

        TextInputDialog dialogDesc = new TextInputDialog();
        dialogDesc.setTitle("Agregar Ítem");
        dialogDesc.setHeaderText("Ingrese la descripción.");
        dialogDesc.setContentText("Descripción:");
        Optional<String> resultDesc = dialogDesc.showAndWait();
        String descripcion = resultDesc.isPresent() ? resultDesc.get().trim() : "";

        TextInputDialog dialogPrecio = new TextInputDialog();
        dialogPrecio.setTitle("Agregar Ítem");
        dialogPrecio.setHeaderText("Ingrese el precio.");
        dialogPrecio.setContentText("Precio:");
        Optional<String> resultPrecio = dialogPrecio.showAndWait();
        double precio = 0.0;
        if (resultPrecio.isPresent() && !resultPrecio.get().trim().isEmpty()) {
            try {
                precio = Double.parseDouble(resultPrecio.get().trim());
            } catch (NumberFormatException e) {
                showAlert("Error de Entrada", "Precio inválido. Ingrese un número.", AlertType.ERROR);
                return;
            }
        } else {
            showAlert("Cancelado", "Operación cancelada o precio vacío.", AlertType.INFORMATION);
            return;
        }


        TextInputDialog dialogCategoria = new TextInputDialog();
        dialogCategoria.setTitle("Agregar Ítem");
        dialogCategoria.setHeaderText("Ingrese la categoría.");
        dialogCategoria.setContentText("Categoría:");
        Optional<String> resultCategoria = dialogCategoria.showAndWait();
        String categoria = resultCategoria.isPresent() ? resultCategoria.get().trim() : "";


        String sql = "INSERT INTO ITEMS_MENU (nombre, descripcion, precio, categoria) VALUES (?, ?, ?, ?);";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, descripcion);
            pstmt.setDouble(3, precio);
            pstmt.setString(4, categoria);

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas > 0) {
                showAlert("Éxito", "Ítem '" + nombre + "' agregado exitosamente.", AlertType.INFORMATION);
                mostrarTodosItemsMenuFX();
            } else {
                showAlert("Fallo", "No se pudo agregar el ítem.", AlertType.ERROR);
            }
        } catch (SQLException e) {
            showAlert("Error de DB", "Error al agregar nuevo ítem: " + e.getMessage(), AlertType.ERROR);
            System.err.println("Error al agregar nuevo ítem: " + e.getMessage());
        }
    }

    private void eliminarItemPorIdFX() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Eliminar Ítem");
        dialog.setHeaderText("Ingrese el ID del ítem a eliminar.");
        dialog.setContentText("ID:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            showAlert("Cancelado", "Operación cancelada o ID vacío.", AlertType.INFORMATION);
            return;
        }

        int idEliminar;
        try {
            idEliminar = Integer.parseInt(result.get().trim());
        } catch (NumberFormatException e) {
            showAlert("Error de Entrada", "ID inválido. Por favor, ingrese un número entero.", AlertType.ERROR);
            return;
        }

        String sql = "DELETE FROM ITEMS_MENU WHERE id = ?;";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEliminar);
            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                showAlert("Éxito", "Ítem con ID " + idEliminar + " eliminado exitosamente.", AlertType.INFORMATION);
                mostrarTodosItemsMenuFX();
            } else {
                showAlert("Fallo", "No se encontró ningún ítem con ID " + idEliminar + ".", AlertType.WARNING);
            }
        } catch (SQLException e) {
            showAlert("Error de DB", "Error al eliminar ítem: " + e.getMessage(), AlertType.ERROR);
            System.err.println("Error al eliminar ítem: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String getSpanishEquivalent(String searchTerm) {
        switch (searchTerm.toLowerCase()) {
            case "hamburger":
            case "burger":
                return "hamburguesa";
            case "pizza":
                return "pizza";
            case "salad":
                return "ensalada";
            case "drink":
            case "soda":
                return "refresco";
            case "dessert":
            case "cake":
            case "pie":
                return "tarta";
            default:
                return searchTerm;
        }
    }
}