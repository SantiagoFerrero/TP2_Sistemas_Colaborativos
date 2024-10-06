import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Restaurante {
    private static Connection conexion;

    public Restaurante() {
        conexion = ConexionMySQL.conectar();
    }

    // Registrar un nuevo cliente
    public void registrarCliente(String nombre, String telefono, String direccion) {
        String query = "INSERT INTO Cliente (nombre, telefono, direccion) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conexion.prepareStatement(query)) {
            stmt.setString(1, nombre);
            stmt.setString(2, telefono);
            stmt.setString(3, direccion);
            stmt.executeUpdate();
            System.out.println("Cliente registrado correctamente.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Registrar un nuevo producto
    public void registrarProducto(String nombre, double precio, int stock) {
        String query = "INSERT INTO Producto (nombre, precio, stock) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conexion.prepareStatement(query)) {
            stmt.setString(1, nombre);
            stmt.setDouble(2, precio);
            stmt.setInt(3, stock);
            stmt.executeUpdate();
            System.out.println("Producto registrado correctamente.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Ver el stock de productos
    public void verStock() {
        String query = "SELECT * FROM Producto";
        try (Statement stmt = conexion.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Nombre: " + rs.getString("nombre") + 
                                   ", Precio: " + rs.getDouble("precio") + ", Stock: " + rs.getInt("stock"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Registrar un pedido
    public void registrarPedido(int clienteId) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Ingrese el nombre del producto (o 'fin' para terminar):");

        try {
            // Comenzar una transacción para asegurar consistencia en caso de error
            conexion.setAutoCommit(false);
            
            // Insertar el pedido y obtener el ID generado
            String queryPedido = "INSERT INTO Pedido (cliente_id) VALUES (?)";
            try (PreparedStatement stmtPedido = conexion.prepareStatement(queryPedido, Statement.RETURN_GENERATED_KEYS)) {
                stmtPedido.setInt(1, clienteId);
                stmtPedido.executeUpdate();
                ResultSet generatedKeys = stmtPedido.getGeneratedKeys();
                int pedidoId = 0;
                if (generatedKeys.next()) {
                    pedidoId = generatedKeys.getInt(1);
                }
                
                while (true) {
                    System.out.print("Nombre del producto: ");
                    String nombreProducto = scanner.nextLine();
                    if (nombreProducto.equalsIgnoreCase("fin")) {
                        break;
                    }

                    int cantidad = 0;
                    boolean cantidadValida = false;

                    while (!cantidadValida) {
                        System.out.print("Ingrese la cantidad de " + nombreProducto + ": ");
                        try {
                            cantidad = scanner.nextInt();
                            scanner.nextLine(); // Limpiar el buffer
                            cantidadValida = true; // Salir del bucle si se ingresa un número válido
                        } catch (InputMismatchException e) {
                            System.out.println("Entrada inválida. Por favor, ingrese un número entero para la cantidad.");
                            scanner.nextLine(); // Limpiar el buffer en caso de error
                        }
                    }

                    // Verificar si el producto existe y tiene suficiente stock
                    String queryProducto = "SELECT id, stock FROM Producto WHERE nombre = ?";
                    try (PreparedStatement stmtProducto = conexion.prepareStatement(queryProducto)) {
                        stmtProducto.setString(1, nombreProducto);
                        ResultSet rsProducto = stmtProducto.executeQuery();

                        if (rsProducto.next()) {
                            int productoId = rsProducto.getInt("id");
                            int stockActual = rsProducto.getInt("stock");

                            if (stockActual >= cantidad) {
                                // Resta el stock del producto
                                String queryUpdateStock = "UPDATE Producto SET stock = stock - ? WHERE id = ?";
                                try (PreparedStatement stmtUpdateStock = conexion.prepareStatement(queryUpdateStock)) {
                                    stmtUpdateStock.setInt(1, cantidad);
                                    stmtUpdateStock.setInt(2, productoId);
                                    stmtUpdateStock.executeUpdate();
                                }

                                // Insertar en PedidoProducto
                                String queryPedidoProducto = "INSERT INTO PedidoProducto (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)";
                                try (PreparedStatement stmtPedidoProducto = conexion.prepareStatement(queryPedidoProducto)) {
                                    stmtPedidoProducto.setInt(1, pedidoId);
                                    stmtPedidoProducto.setInt(2, productoId);
                                    stmtPedidoProducto.setInt(3, cantidad);
                                    stmtPedidoProducto.executeUpdate();
                                }
                            } else {
                                System.out.println("Stock insuficiente para el producto: " + nombreProducto);
                            }
                        } else {
                            System.out.println("Producto no encontrado: " + nombreProducto);
                        }
                    }
                }

                // Confirmar la transacción
                conexion.commit();
                System.out.println("Pedido registrado exitosamente.");
            } catch (SQLException e) {
                e.printStackTrace();
                conexion.rollback(); // Deshacer la transacción en caso de error
            } finally {
                conexion.setAutoCommit(true); // Restablecer el modo de autocommit
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Imprimir ticket de un pedido
    public void imprimirTicket(int pedidoId) {
        String query = "SELECT pr.nombre AS producto, pp.cantidad, pr.precio, (pp.cantidad * pr.precio) AS subtotal " +
                       "FROM PedidoProducto pp " +
                       "JOIN Producto pr ON pp.producto_id = pr.id " +
                       "WHERE pp.pedido_id = ?";
        try (PreparedStatement stmt = conexion.prepareStatement(query)) {
            stmt.setInt(1, pedidoId);
            ResultSet rs = stmt.executeQuery();
            
            double total = 0;
            System.out.println("=== Ticket de Pedido ===");
            System.out.println("Pedido ID: " + pedidoId);
            System.out.println("------------------------");

            while (rs.next()) {
                String nombreProducto = rs.getString("producto");
                int cantidad = rs.getInt("cantidad");
                double precio = rs.getDouble("precio");
                double subtotal = rs.getDouble("subtotal");
                total += subtotal;
                
                System.out.printf("Producto: %s\nCantidad: %d\nPrecio unitario: $%.2f\nSubtotal: $%.2f\n\n", 
                                  nombreProducto, cantidad, precio, subtotal);
            }
            
            System.out.println("------------------------");
            System.out.printf("Total a pagar: $%.2f\n", total);
            System.out.println("========================");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Restaurante restaurante = new Restaurante();
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\nMenú de opciones:");
            System.out.println("1. Registrar cliente");
            System.out.println("2. Ver stock de productos");
            System.out.println("3. Registrar producto");
            System.out.println("4. Registrar pedido");
            System.out.println("5. Imprimir ticket");
            System.out.println("6. Salir");
            System.out.print("Selecciona una opción: ");
            int opcion = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer

            switch (opcion) {
                case 1:
                    System.out.print("Nombre del cliente: ");
                    String nombre = scanner.nextLine();
                    System.out.print("Teléfono: ");
                    String telefono = scanner.nextLine();
                    System.out.print("Dirección: ");
                    String direccion = scanner.nextLine();
                    restaurante.registrarCliente(nombre, telefono, direccion);
                    break;
                case 2:
                    restaurante.verStock();
                    break;
                case 3:
                    System.out.print("Nombre del producto: ");
                    String nombreProducto = scanner.nextLine();
                    System.out.print("Precio: ");
                    double precio = scanner.nextDouble();
                    System.out.print("Stock inicial: ");
                    int stock = scanner.nextInt();
                    restaurante.registrarProducto(nombreProducto, precio, stock);
                    break;
                case 4:
                    System.out.print("ID del cliente: ");
                    int clienteId = scanner.nextInt();
                    restaurante.registrarPedido(clienteId);
                    break;
                case 5:
                    System.out.print("ID del pedido: ");
                    int pedidoId = scanner.nextInt();
                    restaurante.imprimirTicket(pedidoId);
                    break;
                case 6:
                    System.out.println("Saliendo...");
                    return;
                default:
                    System.out.println("Opción no válida.");
                    break;
            }
        }
    }
}