import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes
 * Compile and run with: 
 * javac -cp "target/sales-crm-0.0.1-SNAPSHOT.jar" PasswordHashGenerator.java
 * java -cp ".:target/sales-crm-0.0.1-SNAPSHOT.jar" PasswordHashGenerator "password"
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java PasswordHashGenerator <password>");
            System.exit(1);
        }
        
        String password = args[0];
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hashedPassword);
        System.out.println();
        System.out.println("SQL Insert Example:");
        System.out.println("INSERT INTO users (username, password, ...) VALUES ('username', '" + hashedPassword + "', ...);");
    }
}