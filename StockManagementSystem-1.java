import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/*
 * Stock Management System
 * Technologies: Java Swing + JDBC + MySQL
 *
 * Before running:
 * 1. Create the database using schema.sql.
 * 2. Add MySQL Connector/J to the project classpath.
 * 3. Update DB_USER and DB_PASSWORD below.
 */
public class StockManagementSystem extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/stock_management";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "your_password";

    private final JTextField idField = new JTextField(8);
    private final JTextField nameField = new JTextField(14);
    private final JTextField categoryField = new JTextField(12);
    private final JTextField quantityField = new JTextField(8);
    private final JTextField priceField = new JTextField(8);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Product Name", "Category", "Quantity", "Price"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public StockManagementSystem() {
        setTitle("Stock Management System");
        setSize(850, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        buildScreen();
        loadProducts();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void buildScreen() {
        JLabel heading = new JLabel("STOCK MANAGEMENT SYSTEM", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel form = new JPanel(new GridLayout(2, 5, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder("Product Details"));
        addField(form, "Product ID", idField);
        addField(form, "Product Name", nameField);
        addField(form, "Category", categoryField);
        addField(form, "Quantity", quantityField);
        addField(form, "Price", priceField);

        JButton addButton = new JButton("Add Product");
        JButton updateButton = new JButton("Update Product");
        JButton deleteButton = new JButton("Delete Product");
        JButton clearButton = new JButton("Clear");
        JButton refreshButton = new JButton("Refresh");
        JButton reportButton = new JButton("Stock Report");

        addButton.addActionListener(e -> addProduct());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());
        clearButton.addActionListener(e -> clearFields());
        refreshButton.addActionListener(e -> loadProducts());
        reportButton.addActionListener(e -> showReport());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);
        buttons.add(refreshButton);
        buttons.add(reportButton);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int r = table.getSelectedRow();
                idField.setText(model.getValueAt(r, 0).toString());
                nameField.setText(model.getValueAt(r, 1).toString());
                categoryField.setText(model.getValueAt(r, 2).toString());
                quantityField.setText(model.getValueAt(r, 3).toString());
                priceField.setText(model.getValueAt(r, 4).toString());
            }
        });

        setLayout(new BorderLayout(10, 10));
        add(heading, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        center.add(form, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(buttons, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    private void addField(JPanel panel, String label, JTextField field) {
        JPanel cell = new JPanel(new BorderLayout(3, 3));
        cell.add(new JLabel(label), BorderLayout.NORTH);
        cell.add(field, BorderLayout.CENTER);
        panel.add(cell);
    }

    private boolean validateFields(boolean requireId) {
        if ((requireId && idField.getText().trim().isEmpty())
                || nameField.getText().trim().isEmpty()
                || categoryField.getText().trim().isEmpty()
                || quantityField.getText().trim().isEmpty()
                || priceField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all product details.");
            return false;
        }
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            if (quantity < 0 || price < 0) throw new NumberFormatException();
            if (requireId) Integer.parseInt(idField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid ID, non-negative quantity and price.");
            return false;
        }
        return true;
    }

    private void addProduct() {
        if (!validateFields(false)) return;
        String sql = "INSERT INTO products(product_name, category, quantity, price) VALUES(?,?,?,?)";
        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nameField.getText().trim());
            ps.setString(2, categoryField.getText().trim());
            ps.setInt(3, Integer.parseInt(quantityField.getText().trim()));
            ps.setDouble(4, Double.parseDouble(priceField.getText().trim()));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Product added successfully!");
            clearFields();
            loadProducts();
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void updateProduct() {
        if (!validateFields(true)) return;
        String sql = "UPDATE products SET product_name=?, category=?, quantity=?, price=? WHERE product_id=?";
        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nameField.getText().trim());
            ps.setString(2, categoryField.getText().trim());
            ps.setInt(3, Integer.parseInt(quantityField.getText().trim()));
            ps.setDouble(4, Double.parseDouble(priceField.getText().trim()));
            ps.setInt(5, Integer.parseInt(idField.getText().trim()));
            int rows = ps.executeUpdate();
            JOptionPane.showMessageDialog(this, rows > 0 ? "Product updated successfully!" : "Product ID not found.");
            loadProducts();
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void deleteProduct() {
        if (idField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a product or enter its ID.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Delete this product?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement("DELETE FROM products WHERE product_id=?")) {
            ps.setInt(1, Integer.parseInt(idField.getText().trim()));
            int rows = ps.executeUpdate();
            JOptionPane.showMessageDialog(this, rows > 0 ? "Product deleted successfully!" : "Product ID not found.");
            clearFields();
            loadProducts();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Product ID must be a number.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void loadProducts() {
        model.setRowCount(0);
        String sql = "SELECT product_id, product_name, category, quantity, price FROM products ORDER BY product_id";
        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("product_id"), rs.getString("product_name"),
                        rs.getString("category"), rs.getInt("quantity"), rs.getDouble("price")
                });
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void showReport() {
        String sql = "SELECT COUNT(*) AS total_products, COALESCE(SUM(quantity),0) AS total_units, " +
                     "COALESCE(SUM(quantity * price),0) AS stock_value FROM products";
        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String report = "Total product types: " + rs.getInt("total_products")
                        + "\nTotal units in stock: " + rs.getInt("total_units")
                        + "\nTotal stock value: " + String.format("%.2f", rs.getDouble("stock_value"));
                JOptionPane.showMessageDialog(this, report, "Stock Report", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        categoryField.setText("");
        quantityField.setText("");
        priceField.setText("");
        table.clearSelection();
    }

    private void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "Database connection or SQL error:\n" + ex.getMessage()
                + "\n\nCheck MySQL is running, schema.sql is executed, and DB settings are correct.",
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StockManagementSystem().setVisible(true));
    }
}
