package org.jack;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.opencsv.exceptions.CsvException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

public class Gui extends JFrame {
    private JPanel GuiPanel;
    private JPanel Header;
    private JPanel Footer;
    private JButton generateButton;
    private JPanel MainP;
    private JPanel Left;
    private JPanel Right;
    private HintTextField psalmsVerses;
    private HintTextField psalmsChapter;
    private JComboBox<String> comboBox1;
    private HintTextField oldTestamentVerses;
    private HintTextField newTestamentVerses;
    private HintTextField oldTestamentChapter;
    private HintTextField newTestamentChapter;
    private JButton importButton;
    private JButton voiceButton;

    private static final VoiceSelector v = new VoiceSelector();

    private boolean doJSON = false;

    public Gui() {
        // Makes sure title is set before possibly being overwritten with "offline mode"
        setTitle("BibleTTS");

        // checks for updates
        UpdateChecker checker = new UpdateChecker(this);
        checker.start();

        $$$setupUI$$$();
        setContentPane(GuiPanel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 225);
        setLocationRelativeTo(null);
        setLocationByPlatform(true);
        comboBox1.addItem("NABRE");
        comboBox1.addItem("ESV");
        comboBox1.addItem("KJV");
        comboBox1.addItem("NIV");
        comboBox1.addItem("CEV");
        setVisible(true);

        Options i = new Options(this);

        generateButton.addActionListener(e -> {
            int multiplier = 1;
            if (Main.doDouble) {
                multiplier = 2;
            }

            // gets goals without doing floating point addition
            double ot_goal = (Double.parseDouble(oldTestamentVerses.betterGetText()) * 10 + Main.otDaily * multiplier * 10) / 10;
            double nt_goal = (Double.parseDouble(newTestamentVerses.betterGetText()) * 10 + Main.ntDaily * multiplier * 10) / 10;
            double ps_goal = (Double.parseDouble(psalmsVerses.betterGetText()) * 10 + Main.psDaily * multiplier * 10) / 10;

            String ot_chapter = oldTestamentChapter.betterGetText();
            String nt_chapter = newTestamentChapter.betterGetText();
            String ps_chapter = psalmsChapter.betterGetText();
            String translation = comboBox1.getItemAt(comboBox1.getSelectedIndex());

            // step 1 psalm 119 detection
            if(ps_chapter.equalsIgnoreCase("psa.119"))
            {
                ot_goal = -99999;
                nt_goal = -99999;
                ps_goal = -99999;
            }

            File offlineBible = new File("bible-tts/offline/" + translation.toLowerCase() + ".csv");

            Reader otReader;
            Reader ntReader;
            Reader psReader;

            if(!offlineBible.exists())
            {
                // online parsing
                String changingBit = Main.getChangingBit();
                try
                {
                    otReader = new Reader("online", changingBit, ot_chapter, translation, ot_goal);
                    ntReader = new Reader("online", changingBit, nt_chapter, translation, nt_goal);
                    psReader = new Reader("online", changingBit, ps_chapter, translation, ps_goal);
                } catch (IOException | CsvException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                // offline parsing
                try
                {
                    OfflineBible.importBible(translation.toLowerCase());
                    otReader = new Reader("offline", "", ot_chapter, translation, ot_goal);
                    ntReader = new Reader("offline", "", nt_chapter, translation, nt_goal);
                    psReader = new Reader("offline", "", ps_chapter, translation, ps_goal);
                } catch (IOException | CsvException ex) {
                    throw new RuntimeException(ex);
                }
            }


            // step 2 psalm 119 offset correction
            if(ps_chapter.equalsIgnoreCase("psa.119"))
            {
                otReader.setOffset(Double.parseDouble(oldTestamentVerses.getText()));
                ntReader.setOffset(Double.parseDouble(newTestamentVerses.getText()));
                psReader.setOffset(Double.parseDouble(psalmsVerses.getText()));
            }

            String combinedTexts = otReader.getFinalText() + ntReader.getFinalText() + psReader.getFinalText();

            String json = "";

            if(doJSON)
            {
                json = "{\"goals\": [" + otReader.getReadingOffset() + ", " + ntReader.getReadingOffset() + ", " + psReader.getReadingOffset() + "], \"chapters\": [" + otReader.getNextChapter() + ", " + ntReader.getNextChapter() + ", " + psReader.getNextChapter() + "]}";

                // for clipboard
                StringSelection stringSelection = new StringSelection(json);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }

            JOptionPane.showMessageDialog(null, otReader.getReadingOffset() + " offset in the Old Testament. Stop at: " + otReader.getNextChapter() + ".\n"
                    + ntReader.getReadingOffset() + " offset in the New Testament. Stop at: " + ntReader.getNextChapter() + ".\n"
                    + psReader.getReadingOffset() + " offset in the book of Psalms. Stop at: " + psReader.getNextChapter() + "."
                    + "\n\n" + json + "\n\nGenerating Audio...");

            Tts file = new Tts(combinedTexts);
            String confirmation = "Done!";
            try {
                String error = file.createFiles();
                if(!error.isEmpty())
                {
                    confirmation = error;
                }
            } catch (InvalidDataException | UnsupportedTagException | IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            JOptionPane.showMessageDialog(null, confirmation);
        });

        importButton.addActionListener(e -> i.createWindow());

        voiceButton.addActionListener(e -> v.createWindow());
    }

    public void setOfflineMode()
    {
        v.setOfflineMode();
        setTitle("BibleTTS (offline)");

        for(int i = comboBox1.getItemCount()-1; i >= 0 ; i--)
        {
            File f = new File("bible-tts/offline/" + comboBox1.getItemAt(i).toLowerCase() + ".csv");
            if(!f.exists())
            {
                comboBox1.removeItemAt(i);
            }
        }

        if(comboBox1.getItemCount() == 0)
        {
            comboBox1.addItem("None found");
            comboBox1.setEnabled(false);
            generateButton.setEnabled(false);
        }
    }

    public void _import(String j)
    {
        JSONObject json = new JSONObject(j);

        JSONArray goals = json.getJSONArray("goals");
        oldTestamentVerses.setText(goals.get(0).toString());
        newTestamentVerses.setText(goals.get(1).toString());
        psalmsVerses.setText(goals.get(2).toString());

        JSONArray chapters = json.getJSONArray("chapters");
        oldTestamentChapter.setText(chapters.get(0).toString());
        newTestamentChapter.setText(chapters.get(1).toString());
        psalmsChapter.setText(chapters.get(2).toString());

    }

    public void setDoJSON(boolean b)
    {
        doJSON = b;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        GuiPanel = new JPanel();
        GuiPanel.setLayout(new BorderLayout(0, 0));
        Header = new JPanel();
        Header.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        GuiPanel.add(Header, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setText("BibleTTS");
        Header.add(label1);
        Footer = new JPanel();
        Footer.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        GuiPanel.add(Footer, BorderLayout.SOUTH);
        comboBox1 = new JComboBox();
        comboBox1.putClientProperty("html.disable", Boolean.FALSE);
        Footer.add(comboBox1);
        voiceButton = new JButton();
        voiceButton.setText("Voices");
        Footer.add(voiceButton);
        importButton = new JButton();
        importButton.setText("Options");
        Footer.add(importButton);
        generateButton = new JButton();
        generateButton.setText("Generate");
        Footer.add(generateButton);
        MainP = new JPanel();
        MainP.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        GuiPanel.add(MainP, BorderLayout.CENTER);
        Left = new JPanel();
        Left.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        MainP.add(Left, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        Left.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Psalms Offsets");
        panel1.add(label2, BorderLayout.NORTH);
        panel1.add(psalmsVerses, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        Left.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("New Testament Offsets");
        panel2.add(label3, BorderLayout.NORTH);
        panel2.add(newTestamentVerses, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        Left.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Old Testament Offsets");
        panel3.add(label4, BorderLayout.NORTH);
        panel3.add(oldTestamentVerses, BorderLayout.CENTER);
        Right = new JPanel();
        Right.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        MainP.add(Right, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        Right.add(panel4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("PS Chapter");
        panel4.add(label5, BorderLayout.NORTH);
        panel4.add(psalmsChapter, BorderLayout.CENTER);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        Right.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("NT Chapter");
        panel5.add(label6, BorderLayout.NORTH);
        panel5.add(newTestamentChapter, BorderLayout.CENTER);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        Right.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("OT Chapter");
        panel6.add(label7, BorderLayout.NORTH);
        panel6.add(oldTestamentChapter, BorderLayout.CENTER);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return GuiPanel;
    }

    private void createUIComponents()
    {
        oldTestamentVerses = new HintTextField("0");
        oldTestamentChapter = new HintTextField("Gen.1");
        newTestamentVerses = new HintTextField("0");
        newTestamentChapter = new HintTextField("Mat.1");
        psalmsVerses = new HintTextField("0");
        psalmsChapter = new HintTextField("Psa.1");
    }
}
