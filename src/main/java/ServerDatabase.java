import discorddb.sqlitedb.DatabaseTable;
import discorddb.sqlitedb.SQLDatabase;
import org.h2.jdbc.JdbcConnection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class ServerDatabase {
    private static DatabaseTable servers;
    private static final File DATABASE_FILE = new File("assets/servers.db");
    private static DatabaseTable names;
    private static final File NAMES_FILE = new File("assets/names.db");
    Connection connection;
    Statement statement;

    public static void init() {
        try {
            SQLDatabase.createTable("servers", "id bigint primary key not null unique", "name-react varchar(5)", "reply-react varchar(5)");
            servers = SQLDatabase.getTable("servers");
            if (!DATABASE_FILE.exists()) {
                Files.createFile(DATABASE_FILE.toPath());
            }
            // bigint == long (parse server ID strings to long)
            SQLDatabase.createTable("names", "id bigint primary key not null", "foreign key(id) references servers(id)");
            names = SQLDatabase.getTable("names");
            if (!NAMES_FILE.exists()) {
                Files.createFile(NAMES_FILE.toPath());
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DatabaseTable getServers() {
        return servers;
    }

    public static DatabaseTable getNames() {
        return names;
    }

    // action is "add" or "remove"
    public static void updateNameTable(String ID, String name, String action) {
        if (action.equalsIgnoreCase("add")) {
            names.insertQuery(ID, name);
        }
        else if (action.equalsIgnoreCase("remove")) {
            names.deleteQuery(ID, name);
        }
    }

    // nameReact and replyReact are booleans as Strings, "true" or "false" are real values, "-" if no update is needed
    public static void updateServerTable(String ID, String nameReact, String replyReact, boolean create) {
        if (create) {
            servers.insertQuery(ID, nameReact, replyReact);
            return;
        }
        if (nameReact.equals("-") && !replyReact.equals("-")) {
            servers.updateQuery(ID, "reply-react", String.format("reply-react='%s'", replyReact));
        }
        else if (!nameReact.equals("-") && replyReact.equals("-")) {
            servers.updateQuery(ID, "name-react", String.format("name-react='%s'", nameReact));
        }
        else {
            servers.updateQuery(ID, "name-react", String.format("name-react='%s'", nameReact));
            servers.updateQuery(ID, "reply-react", String.format("reply-react='%s'", replyReact));
        }
    }

}
