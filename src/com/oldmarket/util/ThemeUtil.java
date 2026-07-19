package com.oldmarket.util;

import android.app.Activity;
import android.view.View;
import android.widget.ListView;

public class ThemeUtil {

    public static boolean isDark(Activity a) {
        return Prefs.isDarkTheme(a);
    }

    public static int bg(Activity a) {
        return isDark(a) ? 0xFF1a1a1a : 0xFFf2f2f2;
    }

    public static int cardBg(Activity a) {
        return isDark(a) ? 0xFF2a2a2a : 0xFFffffff;
    }

    public static int textColor(Activity a) {
        return isDark(a) ? 0xFFdddddd : 0xFF111111;
    }

    public static int accentBg(Activity a) {
        return isDark(a) ? 0xFF373737 : 0xFFe9e9e9;
    }

    public static void setRootBg(Activity a, int rootId) {
        View v = a.findViewById(rootId);
        if (v != null) v.setBackgroundColor(bg(a));
    }

    public static void setListBg(Activity a, int listId) {
        View v = a.findViewById(listId);
        if (v instanceof ListView) {
            v.setBackgroundColor(cardBg(a));
            ((ListView) v).setCacheColorHint(cardBg(a));
        }
    }
}
