package g03.discord.rbod;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

public class RBODUtils {
    static File phrases = new File("assets/phrases.txt");

    // Read text in assets/phrases.txt
    static List<String> readPhrasesFromFile() {
        if (phrases.exists() && phrases.canRead()) {
            try {
                return Files.readAllLines(phrases.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            throw new RuntimeException("phrases.txt does not exist or cannot be read. Read assets/guide.md for more info.");
        }
    }

    // Read token from assets/token.txt
    static String readTokenFromFile(File file) {
        if (file.exists() && file.canRead()) {
            try {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8)
                        .trim();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            throw new RuntimeException(file.getAbsolutePath() + " does not exist or cannot be read. Read assets/guide.md for more info.");
        }
    }

    public static boolean messageContainsExactName(String message, String name) {
        String exactName = "\\b" + Pattern.quote(name) + "\\b";
        Pattern pattern = Pattern.compile(exactName, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(message).find();
    }
}
