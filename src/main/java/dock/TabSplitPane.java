package dock;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;

public class TabSplitPane extends SplitPane {
    public TabSplitPane() {
        getItems().addListener((ListChangeListener<Node>) e -> {
            if (e.next()) {
                e.getAddedSubList().stream().filter(o -> o instanceof DetachableTabPane).forEach(tp -> ((DetachableTabPane) tp).parentSplitPane = TabSplitPane.this);
                e.getRemoved().stream().filter(o -> o instanceof DetachableTabPane).forEach(tp -> ((DetachableTabPane) tp).parentSplitPane = null);
            }
        });
    }
}