package org.jack;

import javax.swing.*;
import java.awt.*;

public class HintTextField extends JTextField {
    public HintTextField(String hint) {
        this.hint = hint;
    }
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (getText().isEmpty()) {
            int h = getHeight();
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Insets ins = getInsets();
            FontMetrics fm = g.getFontMetrics();
            int c0 = getBackground().getRGB();
            int c1 = getForeground().getRGB();
            int m = 0xfefefefe;
            int c2 = ((c0 & m) >>> 1) + ((c1 & m) >>> 1);
            g.setColor(new Color(c2, true));
            g.drawString(hint, ins.left, h / 2 + fm.getAscent() / 2 - 2);
        }
    }

    public String betterGetText()
    {
        String text = super.getText();
        if(text.isEmpty())
        {
            return hint;
        }

        return text;
    }
    private final String hint;
}