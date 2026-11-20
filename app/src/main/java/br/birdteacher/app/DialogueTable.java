package br.birdteacher.app;

import br.birdteacher.domain.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

final class DialogueTable extends JTable {
    private final Rows model = new Rows();

    DialogueTable() {
        setModel(model);
        setRowHeight(48);
        setShowGrid(false);
        setShowHorizontalLines(true);
        setFillsViewportHeight(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getColumnModel().getColumn(0).setMinWidth(64);
        getColumnModel().getColumn(0).setMaxWidth(74);
        getColumnModel().getColumn(1).setMinWidth(122);
        getColumnModel().getColumn(1).setMaxWidth(132);
        getColumnModel().getColumn(2).setPreferredWidth(520);
        setDefaultRenderer(
                Object.class,
                new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean selected,
                            boolean focused,
                            int row,
                            int col) {
                        super.getTableCellRendererComponent(
                                table, value, selected, focused, row, col);
                        var entry = model.rows.get(row);
                        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 8));
                        setToolTipText(String.valueOf(value));
                        if (!selected)
                            setForeground(
                                    col == 1
                                            ? Appearance.color(
                                                    entry.clip().source() == Source.MIC
                                                            ? "speaker"
                                                            : "other")
                                            : Appearance.color(
                                                    col == 2
                                                                    && entry.status()
                                                                            == Transcript.Status
                                                                                    .CONFIRMED
                                                            ? "ink"
                                                            : "muted"));
                        if (col == 2) {
                            var area = new JTextArea(String.valueOf(value));
                            area.setFont(getFont());
                            area.setForeground(getForeground());
                            area.setBackground(getBackground());
                            area.setBorder(getBorder());
                            area.setLineWrap(true);
                            area.setWrapStyleWord(true);
                            area.setSize(
                                    Math.max(1, table.getColumnModel().getColumn(col).getWidth()),
                                    Short.MAX_VALUE);
                            int height = Math.max(48, area.getPreferredSize().height);
                            if (table.getRowHeight(row) != height) table.setRowHeight(row, height);
                            return area;
                        }
                        return this;
                    }
                });
    }

    int rowFor(FeedbackPane.Citation citation) {
        if (citation == null) return -1;
        int found = -1;
        for (int i = 0; i < model.rows.size(); i++) {
            var row = model.rows.get(i);
            if (row.status() != Transcript.Status.CONFIRMED
                    || row.clip().source() != Source.MIC
                    || row.clip().millis() / 1000 != citation.seconds()
                    || !row.text().contains(citation.words())) continue;
            if (found >= 0) return -1;
            found = i;
        }
        return found;
    }

    Transcript last() {
        return model.rows.isEmpty() ? null : model.rows.getLast();
    }

    void clearRows() {
        model.rows.clear();
        model.fireTableDataChanged();
    }

    Transcript selected() {
        return getSelectedRow() < 0 ? null : model.rows.get(getSelectedRow());
    }

    void put(Transcript row) {
        Rectangle viewport = getVisibleRect();
        boolean follow =
                getSelectedRow() < 0
                        && viewport.y + viewport.height >= getHeight() - getRowHeight();
        model.put(row);
        if (follow && getRowCount() > 0)
            scrollRectToVisible(getCellRect(getRowCount() - 1, 0, true));
    }

    private static final class Rows extends AbstractTableModel {
        final List<Transcript> rows = new ArrayList<>();

        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(int column) {
            return List.of("Tempo", "Origem", "Fala transcrita").get(column);
        }

        public Object getValueAt(int index, int column) {
            var r = rows.get(index);
            long t = r.clip().millis() / 1000;
            if (column == 0) return "%02d:%02d".formatted(t / 60, t % 60);
            if (column == 1) return r.clip().source().label;
            return switch (r.status()) {
                case PREVIEW -> r.text() + " …";
                case QUEUED -> "Transcrevendo…";
                default -> r.text();
            };
        }

        void put(Transcript row) {
            int i =
                    Collections.binarySearch(
                            rows, row, java.util.Comparator.comparing(Transcript::clip));
            if (row.status() == Transcript.Status.SILENT) {
                if (i >= 0) {
                    rows.remove(i);
                    fireTableRowsDeleted(i, i);
                }
            } else if (i < 0) {
                rows.add(-i - 1, row);
                fireTableRowsInserted(-i - 1, -i - 1);
            } else {
                if (row.status() == Transcript.Status.QUEUED
                        && rows.get(i).status() == Transcript.Status.PREVIEW) return;
                rows.set(i, row);
                fireTableRowsUpdated(i, i);
            }
        }
    }
}
