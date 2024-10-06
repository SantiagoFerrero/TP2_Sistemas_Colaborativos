import java.sql.*;

public class ConexionMySQL {
    private static final String URL = "jdbc:mysql://localhost:3306/RestauranteDB";
    private static final String USER = "root";
    private static final String PASSWORD = "Santiago2002";

    public static Connection conectar() {
        Connection conexion = null;
        try {
            conexion = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexión exitosa a la base de datos.");
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
        return conexion;
    }
}