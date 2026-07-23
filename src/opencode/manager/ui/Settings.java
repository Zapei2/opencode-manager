package opencode.manager.ui;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class Settings {
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".config", "opencode_manager", "settings.properties");
    private final Properties props;
    private boolean darkMode;

    public Settings() {
        props = new Properties();
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.exists(FILE)) {
                try (InputStream in = Files.newInputStream(FILE)) {
                    props.load(in);
                }
            }
        } catch (Exception ignored) {}
        darkMode = "dark".equalsIgnoreCase(props.getProperty("theme", "light"));
    }

    public boolean isDarkMode() { return darkMode; }

    public void setDarkMode(boolean dark) {
        darkMode = dark;
        props.setProperty("theme", dark ? "dark" : "light");
        save();
    }

    private void save() {
        try (OutputStream out = Files.newOutputStream(FILE)) {
            props.store(out, null);
        } catch (Exception ignored) {}
    }
}
