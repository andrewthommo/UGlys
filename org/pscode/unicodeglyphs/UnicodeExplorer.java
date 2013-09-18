package org.pscode.unicodeglyphs;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.Document;
import java.util.*;
import java.util.logging.*;

public class UnicodeExplorer {

    public static final int codePointColumnWidth = 16;
    public static final int numberUnicodes = 256 * 256;
    private final ArrayList<Font> fontList = new ArrayList<Font>();
    private final SpinnerNumberModel startPage = new SpinnerNumberModel(
            0, 0, numberUnicodes, 1);
    private Font[] fontArray;
    private JList<Font> fonts = new JList<Font>(fontArray);
    private final FontTableCellRenderer fontTableCellRenderer =
            new FontTableCellRenderer();
    private final JTable codePointTable = new JTable(new CodePointTableModel(
            numberUnicodes / codePointColumnWidth, codePointColumnWidth));

    public static void main(String[] args) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                // the GUI as seen by the user (without frame)
                JPanel gui = new JPanel(new BorderLayout());
                gui.setBorder(new EmptyBorder(2, 3, 2, 3));

                UnicodeExplorer ue = new UnicodeExplorer();
                ue.initGui(gui);

                JFrame f = new JFrame("UGlys - Unicode Glyphs");
                f.add(gui);
                // Ensures JVM closes after frame(s) closed and
                // all non-daemon threads are finished
                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                // See http://stackoverflow.com/a/7143398/418556 for demo.
                f.setLocationByPlatform(true);

                ue.setCharacterSpinner(new Integer(65));
                f.pack();
                f.setMinimumSize(f.getSize());

                // should be done last, to avoid flickering, moving,
                // resizing artifacts.
                f.setVisible(true);
            }
        };
        // Swing GUIs should be created and updated on the EDT
        // http://docs.oracle.com/javase/tutorial/uiswing/concurrency
        SwingUtilities.invokeLater(r);
    }

    public void handleCodePointTableSelection(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int row = codePointTable.getSelectedRow();
            int col = codePointTable.getSelectedColumn();
            int codePoint = (row * codePointColumnWidth) + col;
            setCodePointDetailView(codePoint);
        }
    }
    JPanel characterPanel = null;
    JList<Font> supportedFonts = new JList<Font>();
    JLabel bigCharacter = new JLabel();
    JLabel characterDetails = new JLabel();

    public void setFontsForThisCodePoint(int codePoint) {
        DefaultListModel<Font> dlm = new DefaultListModel<Font>();
        for (Font font : fontArray) {
            if (font.canDisplay(codePoint)) {
                dlm.addElement(font);
            }
        }
        supportedFonts.setModel(dlm);
        supportedFonts.setVisibleRowCount(5);
    }

    public void setCodePointDetailView(int codePoint) {
        String s = UnicodeUtil.getCodePointString(codePoint);
        bigCharacter.setText(s);

        StringBuilder sb = new StringBuilder("<html><body><table>");
        sb.append(getTableRow("Character", s));
        sb.append(getTableRow("Name", "" + Character.getName(codePoint)));
        sb.append(getTableRow("Code Point", "" + codePoint));
        sb.append(getTableRow(
                "Is Defined", "" + Character.isDefined(codePoint)));
        sb.append(getTableRow(
                "Is BMP", "" + Character.isBmpCodePoint(codePoint)));
        sb.append(getTableRow(
                "Is ISO Control", "" + Character.isISOControl(codePoint)));
        sb.append(getTableRow(
                "Is Mirrored", "" + Character.isMirrored(codePoint)));

        sb.append(getTableRow(
                "Is Digit", "" + Character.isDigit(codePoint)));
        sb.append(getTableRow(
                "Is Letter", "" + Character.isLetter(codePoint)));
        sb.append(getTableRow(
                "Is Alphabetic", "" + Character.isAlphabetic(codePoint)));
        sb.append(getTableRow(
                "Is Ideographic", "" + Character.isIdeographic(codePoint)));

        sb.append(getTableRow(
                "Is Space Character", "" + Character.isSpaceChar(codePoint)));
        sb.append(getTableRow(
                "Is White Space", "" + Character.isWhitespace(codePoint)));

        sb.append(getTableRow(
                "Is Lower Case", "" + Character.isLowerCase(codePoint)));
        sb.append(getTableRow(
                "Is Title Case", "" + Character.isTitleCase(codePoint)));
        sb.append(getTableRow(
                "Is Upper Case", "" + Character.isUpperCase(codePoint)));


        sb.append("</table></body></html>");
        characterDetails.setText(sb.toString());

        setFontsForThisCodePoint(codePoint);
    }

    public String getTableRow(String key, String value) {
        return "<tr><th>" + key + "</th><td>" + value + "</td></tr>";
    }

    public Component getCharacterPanel() {
        if (characterPanel == null) {
            characterPanel = new JPanel(new BorderLayout(5, 5));

            JPanel characterAndFonts = new JPanel(new BorderLayout(3, 3));
            characterAndFonts.add(bigCharacter, BorderLayout.PAGE_START);
            characterAndFonts.add(
                    new JScrollPane(supportedFonts), BorderLayout.CENTER);

            JSplitPane sp = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    characterAndFonts,
                    new JScrollPane(characterDetails));

            characterPanel.add(sp, BorderLayout.CENTER);
            supportedFonts.setCellRenderer(new FontCellRenderer());
            ListSelectionListener lsl = new ListSelectionListener() {

                @Override
                public void valueChanged(ListSelectionEvent e) {
                    selectFont(supportedFonts.getSelectedValue());
                }
            };
            supportedFonts.addListSelectionListener(lsl);
        }

        return characterPanel;
    }

    @SuppressWarnings("unchecked")
    public void initGui(Container c) {
        if (fontList.size() != 0) {
            return;
        }

        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNameArray = ge.getAvailableFontFamilyNames();

        codePointTable.setDefaultRenderer(Object.class, fontTableCellRenderer);
        codePointTable.setRowSelectionAllowed(false);
        codePointTable.setCellSelectionEnabled(true);
        ListSelectionModel lsm = codePointTable.getSelectionModel();
        lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionListener codePointListSelectionListener =
                new ListSelectionListener() {

                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        handleCodePointTableSelection(e);
                    }
                };
        codePointTable.getSelectionModel().
                addListSelectionListener(codePointListSelectionListener);

        TableColumnModelListener tcml = new TableColumnModelListener() {

            @Override
            public void columnAdded(TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnMarginChanged(ChangeEvent e) {
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
                handleCodePointTableSelection(e);
            }
        };
        codePointTable.getColumnModel().addColumnModelListener(tcml);

        Logger.getLogger(
                UnicodeExplorer.class.getCanonicalName()).log(
                Level.INFO, "fontNameArray: " + fontNameArray.length);
        fontArray = new Font[fontNameArray.length];
        String[] logicalFonts = {
            Font.DIALOG, Font.DIALOG_INPUT,
            Font.MONOSPACED,
            Font.SANS_SERIF, Font.SERIF
        };
        for (int ii = 0; ii < logicalFonts.length; ii++) {
            Font f = new Font(logicalFonts[ii], Font.PLAIN, 1);
            fontArray[ii] = f;
            fontList.add(f);
        }
        int count = 0;
        for (int ii = 0; ii < fontNameArray.length; ii++) {
            Font f = new Font(fontNameArray[ii], Font.PLAIN, 1);
            if (!fontList.contains(f)) {
                fontArray[logicalFonts.length + count++] = f;
                fontList.add(f);
            }
        }

        Logger.getLogger(
                UnicodeExplorer.class.getCanonicalName()).log(
                Level.INFO, "fontArray: " + fontArray.length);

        fonts = new JList<Font>(fontArray);
        fonts.setCellRenderer(new FontCellRenderer());
        fonts.setVisibleRowCount(8);
        Dimension d = fonts.getPreferredSize();
        Dimension d1 = new Dimension(
                (int) (d.getWidth() / 3), (int) d.getHeight());
        fonts.setPreferredSize(d1);
        ListSelectionListener lsl = new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = fonts.getSelectedIndex();
                if (index < 0) {
                    index = 0;
                }
                Font f = fontArray[index].deriveFont(32f);
                fontTableCellRenderer.setDisplayFont(f);
                codePointTable.setRowHeight(f.getSize());
                bigCharacter.setFont(f.deriveFont(128f));
            }
        };
        fonts.addListSelectionListener(lsl);
        JScrollPane fontScroll = new JScrollPane(fonts);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JSpinner page = new JSpinner(startPage);
        tools.add(page);
        ChangeListener cl = new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int index = startPage.getNumber().intValue();
                selectCodePoint(index);
            }
        };
        page.addChangeListener(cl);

        JPanel codePointTableComponent = new JPanel(new BorderLayout(3, 3));
        codePointTableComponent.add(tools, BorderLayout.PAGE_START);

        JScrollPane codePointTableScroll = new JScrollPane(codePointTable);

        ArrayList<Integer> namedCodePoints = new ArrayList<Integer>();
        final FilteredCodePointListModel namedCodePointListModel =
                new FilteredCodePointListModel();
        ListCellRenderer namedCodePointListeCellRenderer =
                new CodePointListCellRenderer();

        String s;
        for (int ii = 0; ii < numberUnicodes; ii++) {
            s = Character.getName(ii);
            if (s != null) {
                s = s.trim().toLowerCase();
                if (!s.startsWith("null")
                        && !s.contains("private")
                        && !s.contains("cjk")
                        && !s.contains("surrogate")) {
                    namedCodePoints.add(ii);
                    namedCodePointListModel.addElement(new Integer(ii));
                }
            }
        }
        final JList<Integer> namedCodePointList =
                new JList<Integer>(namedCodePointListModel);
        ListSelectionListener namedCodePointListSelectionListener =
                new ListSelectionListener() {

                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            Integer i = namedCodePointList.getSelectedValue();
                            startPage.setValue(i);
                        }
                    }
                };
        namedCodePointList.addListSelectionListener(
                namedCodePointListSelectionListener);
        namedCodePointList.setCellRenderer(namedCodePointListeCellRenderer);
        namedCodePointList.setVisibleRowCount(8);

        namedCodePointListModel.setFilter("");

        HashMap<String, Integer> namePartMap = new HashMap<String, Integer>();
        for (int ii = 0; ii < namedCodePoints.size(); ii++) {
            String name = Character.getName(namedCodePoints.get(ii));
            String[] parts = name.split(" ");
            for (String part : parts) {
                if (namePartMap.containsKey(part)) {
                    Integer num = namePartMap.get(part);
                    namePartMap.put(part, num.intValue() + 1);
                } else {
                    namePartMap.put(part, 1);
                }
            }
        }
        int namePartMapSize = namePartMap.size();

        class PartNumber implements Comparable {

            public String part;
            public int number;

            PartNumber(String part, int number) {
                this.part = part;
                this.number = number;
            }

            @Override
            public int compareTo(Object o) {
                PartNumber partNumber2 = (PartNumber) o;
                if (number == partNumber2.number) {
                    return part.compareTo(partNumber2.part);
                } else {
                    return number - partNumber2.number;
                }
            }

            @Override
            public String toString() {
                return "Part: " + part + " \tnumber: " + number;
            }
        }
        ArrayList<PartNumber> partNumbers = new ArrayList<PartNumber>();
        Set keySet = namePartMap.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            int number = (Integer) namePartMap.get(key);
            if (key.length() > 4 && number > 7) {
                partNumbers.add(new PartNumber(key, number));
            }
        }
        Collections.sort(partNumbers);

        partNumbers.add(new PartNumber("", 0));
        String[] names = new String[partNumbers.size()];
        for (int jj = 0; jj < names.length; jj++) {
            names[jj] = partNumbers.get(jj).part;
        }
        Collections.sort(Arrays.asList(names));

        JComboBox<String> codePointNameFilterCombo =
                new JComboBox<String>(names);
        codePointNameFilterCombo.setEditable(true);

        ComboBoxEditor cbe = new BasicComboBoxEditor();
        final JTextField tf = (JTextField) cbe.getEditorComponent();
        Document doc = tf.getDocument();
        DocumentListener dl = new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                namedCodePointListModel.setFilter(tf.getText());
                refreshList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                namedCodePointListModel.setFilter(tf.getText());
                refreshList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                namedCodePointListModel.setFilter(tf.getText());
                refreshList();
            }

            private void refreshList() {
                namedCodePointList.repaint();
                namedCodePointList.scrollRectToVisible(
                        namedCodePointList.getCellBounds(0, 0));
            }
        };
        doc.addDocumentListener(dl);
        codePointNameFilterCombo.setEditor(cbe);

        codePointNameFilterCombo.setEditor(cbe);

        namedCodePointListModel.setFilter("");

        //codePointNameFilterCombo
        JPanel namedCodePointPanel = new JPanel(new BorderLayout(3, 3));
        namedCodePointPanel.add(
                codePointNameFilterCombo, BorderLayout.PAGE_START);
        Dimension sizeOfNamedCodePointList = namedCodePointList.getPreferredSize();
        Dimension thinnerSizeOfNamedCodePointList = new Dimension(
                sizeOfNamedCodePointList.width / 4, sizeOfNamedCodePointList.height);
        namedCodePointList.setPreferredSize(thinnerSizeOfNamedCodePointList);
        namedCodePointPanel.add(
                new JScrollPane(namedCodePointList), BorderLayout.CENTER);

        JSplitPane codePointTableNameSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                codePointTableScroll,
                namedCodePointPanel);
        codePointTableNameSplit.setResizeWeight(1d);
        codePointTableComponent.add(codePointTableNameSplit, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                fontScroll, codePointTableComponent);
        selectFont(new Font(Font.SANS_SERIF, Font.PLAIN, 1));

        JSplitPane splitTopBottom = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, split, getCharacterPanel());
        c.add(splitTopBottom, BorderLayout.CENTER);
    }

    public void setCharacterSpinner(Integer i) {
//        page.setValue(i);
        startPage.setValue(new Integer(65));
    }

    public void selectCodePoint(int codePoint) {
        Logger.getLogger(UnicodeExplorer.class.getName()).log(
                Level.INFO, "code point " + codePoint);
        ListSelectionModel lsm = codePointTable.getSelectionModel();
        int row = codePoint / codePointColumnWidth;
        lsm.setSelectionInterval(row, row);
        int col = codePoint % codePointColumnWidth;
        codePointTable.setColumnSelectionInterval(col, col);
        codePointTable.scrollRectToVisible(
                codePointTable.getCellRect(row, col, false));
    }

    public void selectFont(Font font) {
        int indexDefault = fontList.indexOf(font);
        fonts.setSelectedIndex(indexDefault);
        Rectangle rect = fonts.getCellBounds(indexDefault, indexDefault);
        if (rect != null) {
            fonts.scrollRectToVisible(rect.getBounds());
        }
    }
}

class FontCellRenderer extends DefaultListCellRenderer {

    float fontSize = 24;
    JLabel label;

    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        Font fontOrig = (Font) value;
        Font temp = fontOrig;
        String pre = "";
        String suf = "";
        int displayIndex = temp.canDisplayUpTo(temp.getFamily());
        if (displayIndex > -1) {
            temp = new Font(Font.MONOSPACED, Font.PLAIN, temp.getSize());
            pre = "(";
            suf = ")";
        }

        label = (JLabel) super.getListCellRendererComponent(
                list,
                pre + fontOrig.getFamily() + suf,
                index, isSelected, cellHasFocus);
        label.setToolTipText(fontOrig.toString());
        label.setFont(temp.deriveFont(fontSize));
        return label;
    }
}

class FontTableCellRenderer extends DefaultTableCellRenderer {

    private Font displayFont;

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            int codePoint = ((Integer) value).intValue();
            boolean isDefined = Character.isDefined(codePoint);
            boolean canDisplay = displayFont.canDisplay(codePoint);
            String s = UnicodeUtil.getCodePointString(codePoint);
            l.setText(s);
            if (displayFont != null) {
                l.setFont(displayFont.deriveFont(
                        (float) (displayFont.getSize() * .8)));
                String tip = "<html><body style='font-size: 64px; "
                        + "font-family: "
                        + displayFont.getFamily()
                        + ";'>&#" + codePoint
                        + " " + Character.getName(codePoint);
                l.setToolTipText(tip);
                l.setForeground(Color.BLACK);
                if (!canDisplay) {
                    l.setForeground(Color.RED);
                }
                if (!isDefined) {
                    l.setForeground(Color.BLUE);
                }
            }
        } else {
            Logger.getLogger(
                    UnicodeExplorer.class.getName(),
                    "We are not getting a JLabel as expected!");
        }

        return c;
    }

    public void setDisplayFont(Font font) {
        displayFont = font;
    }
}

class UnicodeUtil {

    public static String getCodePointString(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}

class CodePointTableModel extends DefaultTableModel {

    public static final String[] COLUMN_NAMES = {
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "A", "B", "C", "D", "E", "F"
    };

    public CodePointTableModel(int rows, int cols) {
        super(rows, cols);
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int row, int col) {
        return (row * getColumnCount()) + col;
    }
}

class CodePointListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        JLabel l = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        Integer i = (Integer) value;
        String s = new String(
                Character.toChars(i))
                + " - " + Character.getName(i.intValue());
        l.setText(s);

        return l;
    }
}

class FilteredCodePointListModel extends DefaultListModel {

    public String filter = "";
    public Object[] filteredElements = new Object[0];

    public void setFilter(String filter) {
        this.filter = filter;
        filterList();
    }

    @SuppressWarnings("unchecked")
    private void filterList() {
        Object[] allElements = super.toArray();
        if (filter.trim().length() == 0) {
            // use entire list.
            filteredElements = allElements;
        } else {
            // filter the list
            ArrayList<Object> allList = new ArrayList<Object>();
            String[] parts = filter.toUpperCase().trim().split(" ");
            for (int ii = 0; ii < super.size(); ii++) {
                Integer codePointInt = (Integer) super.elementAt(ii);
                int codePointNumber = codePointInt.intValue();
                String name = Character.getName(codePointNumber).toUpperCase();
                boolean containsAll = true;
                for (String part : parts) {
                    if (name.indexOf(part) < 0) {
                        containsAll = false;
                    }
                }
                if (containsAll) {
                    allList.add(codePointInt);
                }
            }
            filteredElements = allList.toArray();
        }
    }

    @Override
    public int getSize() {
        return filteredElements.length;
    }

    @Override
    public Object getElementAt(int index) {
        return filteredElements[index];
    }
}