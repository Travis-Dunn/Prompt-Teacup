package com.alchemysoftware.teacup.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;

import com.alchemysoftware.whitetail.error.ErrorHandler;
import com.alchemysoftware.whitetail.image.Image;
import com.alchemysoftware.whitetail.renderer.GLTexture;
import com.alchemysoftware.whitetail.renderer.MasterRenderer;

/**
 * Rasterizes a label's text with Java2D into a single-channel coverage
 * texture at the label's exact framebuffer pixel size.
 */
final class Baker {
    /* Transparent border so bilinear sampling never clips glyph edges. */
    private static final int PAD = 2;

    /* Must agree with the hints in Draw() so layout matches rasterization. */
    private static final FontRenderContext FRC =
            new FontRenderContext(null, true, true);

    private static final String METRICS_STR = "Hg";

    private Baker() {}

    /**
     * Bakes label.text in label.style at the given scale. On success the
     * label's material holds the new texture and its geometry fields are
     * updated; the previous texture, if any, is the caller's to retire.
     *
     * @return the new texture, or null after reporting a fatal error
     */
    static GLTexture Bake(Label label, float scale,
                          GLTexture.Properties props) {
        TextStyle s = label.style;
        Font font;
        LineMetrics lm;
        ArrayList<TextLayout> lines;
        float ascent, descent, lineH, boxW, boxH, factor;
        float[] offX;
        float minX, minY, maxX, maxY;
        int texMinX, texMinY, texW, texH, i, n;
        BufferedImage img;
        byte[] coverage;
        Image image;

        assert s != null : ERR_STR_STYLE_NULL;
        assert s.font != null : ERR_STR_FONT_NULL;
        assert s.size > 0.0f : ErrStrSize(s.size);
        assert s.lineSpacing > 0.0f : ErrStrLineSpacing(s.lineSpacing);
        assert s.wrapW >= 0.0f : ErrStrWrapW(s.wrapW);
        assert s.align != null : ERR_STR_ALIGN_NULL;

        try {
            font = DeriveFont(s, scale);
            lm = font.getLineMetrics(METRICS_STR, FRC);
            ascent = lm.getAscent();
            descent = lm.getDescent();
            lineH = (ascent + descent + lm.getLeading()) * s.lineSpacing;

            lines = Layout(label.text, font, s.wrapW * scale);
            n = lines.size();

            boxW = 0.0f;
            for (i = 0; i < n; ++i) boxW = Math.max(boxW, Advance(lines, i));
            boxH = ascent + (n - 1) * lineH + descent;

            factor = s.align.factor;
            offX = new float[n];
            for (i = 0; i < n; ++i)
                offX[i] = factor * (boxW - Advance(lines, i));

            /* Logical box with the first baseline at y = 0, grown to cover
               any ink that overhangs it. */
            minX = 0.0f;
            minY = -ascent;
            maxX = boxW;
            maxY = boxH - ascent;
            for (i = 0; i < n; ++i) {
                Rectangle r;
                if (lines.get(i) == null) continue;
                r = lines.get(i).getPixelBounds(FRC, offX[i], i * lineH);
                if (r.isEmpty()) continue;
                minX = Math.min(minX, r.x);
                minY = Math.min(minY, r.y);
                maxX = Math.max(maxX, r.x + r.width);
                maxY = Math.max(maxY, r.y + r.height);
            }

            texMinX = (int) Math.floor(minX) - PAD;
            texMinY = (int) Math.floor(minY) - PAD;
            texW = (int) Math.ceil(maxX) + PAD - texMinX;
            texH = (int) Math.ceil(maxY) + PAD - texMinY;

            img = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
            Draw(img, lines, offX, lineH, -texMinX, -texMinY);
            coverage = Coverage(img);
        } catch (OutOfMemoryError e) {
            ErrorHandler.LogFatalExcpAndStop(ErrStrBakeOOM(label.text), e);
            return null;
        } catch (RuntimeException e) {
            ErrorHandler.LogFatalExcpAndStop(ErrStrBakeExcp(label.text), e);
            return null;
        }

        if ((image = Image.Create(texW, texH, Image.FORMAT_GRAY_8, coverage))
                == null) return null;

        GLTexture texture = MasterRenderer.CreateTexture(image, props);
        if (texture == null) return null;

        label.texW = texW;
        label.texH = texH;
        label.anchorX = -texMinX + factor * boxW;
        label.anchorY = -texMinY;
        label.boxW = boxW;
        label.boxH = boxH;
        label.ascent = ascent;

        return texture;
    }

    private static Font DeriveFont(TextStyle s, float scale) {
        HashMap<TextAttribute, Object> attrs =
                new HashMap<TextAttribute, Object>();

        attrs.put(TextAttribute.SIZE, s.size * scale);
        attrs.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        attrs.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        attrs.put(TextAttribute.TRACKING, s.tracking);
        return s.font.deriveFont(attrs);
    }

    /* One layout per output line; null for an empty line. */
    private static ArrayList<TextLayout> Layout(String text, Font font,
                                                float wrapPx) {
        ArrayList<TextLayout> lines = new ArrayList<TextLayout>();
        String[] paras = text.split("\n", -1);
        AttributedString as;
        LineBreakMeasurer lbm;
        int i;

        for (i = 0; i < paras.length; ++i) {
            if (paras[i].length() == 0) {
                lines.add(null);
                continue;
            }

            as = new AttributedString(paras[i]);
            as.addAttribute(TextAttribute.FONT, font);

            if (wrapPx > 0.0f) {
                lbm = new LineBreakMeasurer(as.getIterator(), FRC);
                while (lbm.getPosition() < paras[i].length())
                    lines.add(lbm.nextLayout(wrapPx));
            } else {
                lines.add(new TextLayout(as.getIterator(), FRC));
            }
        }
        return lines;
    }

    /* Trailing spaces left by wrapping don't count toward alignment. */
    private static float Advance(ArrayList<TextLayout> lines, int i) {
        TextLayout l = lines.get(i);
        return l == null ? 0.0f : l.getVisibleAdvance();
    }

    private static void Draw(BufferedImage img, ArrayList<TextLayout> lines,
                             float[] offX, float lineH, int ox, int oy) {
        Graphics2D g = img.createGraphics();
        int i;

        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Color.WHITE);

            for (i = 0; i < lines.size(); ++i) {
                if (lines.get(i) == null) continue;
                lines.get(i).draw(g, ox + offX[i], oy + i * lineH);
            }
        } finally {
            g.dispose();
        }
    }

    /* The alpha channel of white-on-transparent text is its coverage. */
    private static byte[] Coverage(BufferedImage img) {
        int[] argb = ((DataBufferInt) img.getRaster().getDataBuffer())
                .getData();
        byte[] coverage = new byte[argb.length];
        int i;

        for (i = 0; i < argb.length; ++i)
            coverage[i] = (byte) (argb[i] >>> 24);
        return coverage;
    }

    private static final String CLASS = "Text";
    private static final String ERR_STR_STYLE_NULL = CLASS +
            " failed to bake a label because its style was null.\n";
    private static final String ERR_STR_FONT_NULL = CLASS +
            " failed to bake a label because its style's font was null.\n";
    private static final String ERR_STR_ALIGN_NULL = CLASS +
            " failed to bake a label because its style's alignment was " +
            "null.\n";
    private static String ErrStrSize(float size) {
        return CLASS + " failed to bake a label because its style's size [" +
                size + "] was not greater than zero.\n";
    }
    private static String ErrStrLineSpacing(float lineSpacing) {
        return CLASS + " failed to bake a label because its style's line " +
                "spacing [" + lineSpacing + "] was not greater than zero.\n";
    }
    private static String ErrStrWrapW(float wrapW) {
        return CLASS + " failed to bake a label because its style's wrap " +
                "width [" + wrapW + "] was negative.\n";
    }
    private static String ErrStrBakeOOM(String text) {
        return CLASS + " failed to bake the label [" + text + "] because " +
                "an OutOfMemoryError was encountered.\n";
    }
    private static String ErrStrBakeExcp(String text) {
        return CLASS + " failed to bake the label [" + text + "] because " +
                "Java2D threw an exception.\n";
    }
}
