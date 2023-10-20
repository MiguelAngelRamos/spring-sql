package com.example.demo.controller;


import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.*;

@Controller
public class UserController {

    @GetMapping("/findUser")
    public String findUser(@RequestParam String name, Model model) {
        String user = "";
        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/sqlinyeccion", "root", "academyjava"
            );

            PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE name = ?");
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            
            // * Codigo con sql Inyeccion
            //Statement statement = connection.createStatement();
            //ResultSet resultSet = statement.executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
            
            
            /*Cierto, si el código original de la función findUser se está utilizando sin modificaciones, un intento de inyección SQL como ; DROP TABLE users no debería funcionar directamente 
            en ese contexto. La razón es que la consulta SQL está utilizando executeQuery, que no permite la ejecución de consultas que no sean SELECT en JDBC de Java.*/
            while (resultSet.next()) {
                user = resultSet.getString("name");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        
        if (!user.isEmpty()) {
            model.addAttribute("user", user);
            return "home"; // Si el usuario existe, redirige a "home.html".
        } else {
            model.addAttribute("error", "User not found");
            return "index.html"; // Si el usuario no existe, permanece en "index.html" y muestra un error.
        }
    }
    
    
    @PostMapping("/registerUser")
    public String registerUser(@RequestParam String name, @RequestParam String password, Model model) {
        System.out.println("Register User");

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/sqlinyeccion", "root", "academyjava");
             PreparedStatement statement = connection.prepareStatement("INSERT INTO users(name, password) VALUES (?, ?)")) {

            // Hashing the password using BCrypt from jBcrypt library.
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            
            statement.setString(1, name);
            statement.setString(2, hashedPassword);
            statement.executeUpdate();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            model.addAttribute("error", "An error occurred while registering");
            return "register";
        }
        return "login";
    }
    
    @GetMapping("/register")
    public String showRegisterUserForm() {
        return "register";  
    }
    
    @PostMapping("/login")
    public String login(@RequestParam String name, 
                        @RequestParam String password, 
                        Model model) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/sqlinyeccion", "root", "academyjava");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
            if (resultSet.next()) {
                String storedPasswordHash = resultSet.getString("password");
                if (BCrypt.checkpw(password, storedPasswordHash)) {
                    model.addAttribute("name", name);
                    return "home";
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        model.addAttribute("error", "Invalid username or password");
        return "login";
    }




    @GetMapping("/")
    public String index() {
        return "login";
    }
}

/*
 * 
```java
Statement statement = connection.createStatement();
ResultSet resultSet = statement.executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
```

Si `name` se sustituye por la cadena `' OR '1'='1`, la consulta SQL se convierte en:

```sql
SELECT * FROM users WHERE name = '' OR '1'='1';
```

Expliquemos un poco más:

- `SELECT * FROM users WHERE name = '`: este es el comienzo de la instrucción SQL original.
  
- `' OR '1'='1`: esto se inyecta.

Cuando concatenas la cadena de inyección (`' OR '1'='1`) con la consulta original, la parte de la consulta que especifica la columna `name` termina inmediatamente después del primer apóstrofe. Entonces, el resto de la cadena inyectada (`OR '1'='1`) efectivamente altera la lógica de la consulta. Dado que `'1'='1'` es siempre verdadero, la condición completa después del `WHERE` es verdadera para cada fila de la tabla, por lo que se seleccionan todas las filas de la tabla `users`.

Espero que esta explicación aclare cualquier confusión. Si tienes más preguntas o necesitas aclaraciones adicionales, no dudes en preguntar.
 * 
 * */
