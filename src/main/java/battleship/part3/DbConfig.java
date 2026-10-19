package battleship.part3;
import java.io.*;import java.nio.file.*;import java.sql.*;import java.util.*;
public record DbConfig(String url,String user,String password){
 public static DbConfig load() throws IOException {Path p=Path.of("config","db.properties");if(!Files.exists(p))throw new FileNotFoundException("Chưa có config/db.properties. Chạy setup-config.bat trước.");Properties x=new Properties();try(Reader r=Files.newBufferedReader(p)){x.load(r);}return new DbConfig(x.getProperty("db.url"),x.getProperty("db.username"),x.getProperty("db.password"));}
 public Connection connect() throws SQLException {Properties p=new Properties();p.setProperty("user",user);p.setProperty("password",password);p.setProperty("loginTimeout","8");return DriverManager.getConnection(url,p);}
}
