package opencode.manager.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcIJTheme;
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
            FlatArcDarkOrangeIJTheme.setup();
        } else {
            FlatArcIJTheme.setup();
        }
        patchUIManager();
        loadColors();
    }

    private static void patchUIManager() {
        UIManager.put("Button.arc", 12);
        UIManager.put("Button.margin", new Insets(6, 18, 6, 18));
        UIManager.put("Button.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("Component.arrowType", "chevron");
        UIManager.put("Table.rowHeight", 40);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("TableHeader.height", 34);
        UIManager.put("Tree.rowHeight", 34);
        UIManager.put("Tree.font", new Font("SansSerif", Font.PLAIN, 16));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 12);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("TabbedPane.tabHeight", 36);
        UIManager.put("SplitPaneDivider.style", "grip");
        UIManager.put("OptionPane.messageFont", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("ScrollPane.smoothScrolling", true);
        UIManager.put("Tree.paintLines", false);
        UIManager.put("Table.selectionInsets", new Insets(2, 0, 2, 0));
    }

    public static void loadColors() {
        BG = UIManager.getColor("Panel.background");
        SIDEBAR_BG = UIManager.getColor("Panel.background");
        ACCENT = UIManager.getColor("Component.accentColor");
        TEXT_PRIMARY = UIManager.getColor("Label.foreground");
        TEXT_SECONDARY = UIManager.getColor("Label.disabledForeground");
        BORDER = UIManager.getColor("Component.borderColor");
        TABLE_BG = UIManager.getColor("Table.background");
        SELECT_BG = UIManager.getColor("Table.selectionBackground");
        CARD_BG = UIManager.getColor("TextArea.background");
        ROW_ALT = UIManager.getColor("Table.alternateRowColor");
        if (ROW_ALT == null) ROW_ALT = TABLE_BG;
    }
}