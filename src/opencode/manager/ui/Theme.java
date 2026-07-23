package opencode.manager.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

public class Theme {
    public static Color BG;
    public static Color SIDEBAR_BG;
    public static Color ACCENT;
    public static Color TEXT_PRIMARY;
    public static Color TEXT_SECONDARY;
    public static Color BORDER;
    public static Color ROW_ALT;
    public static Color SELECT_BG;
    public static Color TABLE_BG;
    public static Color CARD_BG;

    public static void applyFlatLaf(boolean dark) {
        if (dark) {
            FlatDarkLaf.setup();
            UIManager.put("Table.alternateRowColor", new Color(0x1a1a2e));
        } else {
            FlatLightLaf.setup();
            UIManager.put("Table.alternateRowColor", new Color(0xf8fafc));
        }
        loadColors(dark);
    }

    public static void loadColors(boolean dark) {
        if (dark) {
            BG = new Color(0x1e1e2e);
            SIDEBAR_BG = new Color(0x181825);
            ACCENT = new Color(0x89b4fa);
            TEXT_PRIMARY = new Color(0xcdd6f4);
            TEXT_SECONDARY = new Color(0x9399b2);
            BORDER = new Color(0x313244);
            ROW_ALT = new Color(0x1e1e2e);
            SELECT_BG = new Color(0x2a2a3e);
            TABLE_BG = new Color(0x1e1e2e);
            CARD_BG = new Color(0x181825);
        } else {
            BG = new Color(0xf8fafc);
            SIDEBAR_BG = Color.WHITE;
            ACCENT = new Color(99, 102, 241);
            TEXT_PRIMARY = new Color(30, 41, 59);
            TEXT_SECONDARY = new Color(100, 116, 139);
            BORDER = new Color(226, 232, 240);
            ROW_ALT = new Color(0xf8fafc);
            SELECT_BG = new Color(238, 242, 255);
            TABLE_BG = Color.WHITE;
            CARD_BG = Color.WHITE;
        }
    }
}
