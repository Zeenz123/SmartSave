package com.example.myfirstapp.ui;

import com.example.myfirstapp.algo.Trie;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Connects the {@link Trie} data structure to a JavaFX TextField: as the user types,
 * matching entries are fetched with an O(L) prefix lookup and shown in a popup.
 */
public final class AutoComplete {
    private AutoComplete() {
    }

    public static void attach(TextField field, Trie trie) {
        ContextMenu popup = new ContextMenu();
        field.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isBlank() || !field.isFocused()) {
                popup.hide();
                return;
            }
            List<String> matches = trie.withPrefix(newText, 5);
            matches.removeIf(m -> m.equalsIgnoreCase(newText.trim()));
            if (matches.isEmpty()) {
                popup.hide();
                return;
            }
            popup.getItems().clear();
            for (String m : matches) {
                MenuItem item = new MenuItem(m);
                item.setOnAction(e -> {
                    field.setText(m);
                    field.positionCaret(m.length());
                    popup.hide();
                });
                popup.getItems().add(item);
            }
            if (!popup.isShowing()) popup.show(field, Side.BOTTOM, 0, 0);
        });
        field.focusedProperty().addListener((obs, was, now) -> {
            if (!now) popup.hide();
        });
    }
}
