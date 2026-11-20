package br.birdteacher.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class SessionActionsTest {
    @Test void eachRowActsOnItsOwnSessionAndDeletingRequiresTheExactToken() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        var first = new SessionLibrary.Entry("a", "Primeira aula");
        var second = new SessionLibrary.Entry("b", "Segunda aula");
        var copied = new AtomicReference<SessionLibrary.Entry>();
        var deleted = new AtomicReference<SessionLibrary.Entry>();
        var window = edt(() -> {
            Appearance.install();
            var w = new Window();
            w.setVisible(true);
            w.history.doClick();
            w.library.onCopyPrompt(copied::set);
            w.library.onDelete(entry -> { deleted.set(entry); w.library.removed(entry.id()); });
            w.library.load(List.of(first, second));
            return w;
        });
        try {
            var delete = edt(() -> {
                rowButton(window.library, second, "Copiar prompt de revisão").doClick();
                return rowButton(window.library, second, "Excluir sessão");
            });
            assertEquals(second, copied.get());
            var dialog = openDialog(delete);
            edt(() -> {
                var confirm = find(dialog, JButton.class, b -> "Excluir".equals(b.getText()));
                var field = find(dialog, JTextField.class, f -> true);
                assertFalse(confirm.isEnabled());
                assertNotNull(find(dialog, JLabel.class, l -> second.caption().equals(l.getText())));
                for (var wrong : List.of("DELETE", "delet", "delete ")) {
                    field.setText(wrong);
                    assertFalse(confirm.isEnabled());
                }
                field.setText("delete");
                assertTrue(confirm.isEnabled());
                field.setText("");
                assertFalse(confirm.isEnabled());
                find(dialog, JButton.class, b -> "Cancelar".equals(b.getText())).doClick();
                return null;
            });
            assertNull(deleted.get());
            var confirmed = openDialog(delete);
            edt(() -> {
                find(confirmed, JTextField.class, f -> true).setText("delete");
                find(confirmed, JButton.class, b -> "Excluir".equals(b.getText())).doClick();
                return null;
            });
            edt(() -> {
                assertEquals(second, deleted.get());
                assertNotNull(rowButton(window.library, first, "Excluir sessão"));
                assertNull(rowButton(window.library, second, "Excluir sessão"));
                return null;
            });
        } finally { edt(() -> { window.dispose(); return null; }); }
    }

    static JButton rowButton(Container panel, SessionLibrary.Entry entry, String name) {
        return find(panel, JButton.class, b -> name.equals(b.getAccessibleContext().getAccessibleName())
                && entry.caption().equals(b.getAccessibleContext().getAccessibleDescription()));
    }

    private static JDialog openDialog(JButton delete) throws Exception {
        SwingUtilities.invokeLater(delete::doClick);
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < end) {
            var found = edt(() -> {
                for (var w : java.awt.Window.getWindows())
                    if (w instanceof JDialog d && d.isShowing() && d.getTitle().equals("Excluir sessão")) return d;
                return null;
            });
            if (found != null) return found;
            Thread.sleep(15);
        }
        throw new AssertionError("Confirmation dialog did not open");
    }

    private static <T extends Component> T find(Container parent, Class<T> type, Predicate<T> match) {
        for (Component c : parent.getComponents()) {
            if (type.isInstance(c) && match.test(type.cast(c))) return type.cast(c);
            if (c instanceof Container nested) {
                T found = find(nested, type, match);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
