/*
 * @(#)BezierAnim.java	1.6	98/12/03
 *
 * Copyright 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package bezier;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The BezierAnim class renders an animated Bezier Curve with the drawing
 * style and paint and the filling paint selected by the user.
 */
public class BezierAnim extends JApplet {

    //command line arguments
    static List arguments;

    Demo demo;

    // This map receives the "leaked" objects if the "Leak memory" check box is selected  
    private Map leakMap = new HashMap();

    public void init() {
        getContentPane().add(demo = new Demo());
        getContentPane().add("North", new DemoControls(demo));
    }

    public void start() {
        demo.start();
    }

    public void stop() {
        demo.stop();
    }


    /**
     * The Demo class performs the animation and the painting.
     */
    public class Demo extends JPanel implements Runnable {
        private Thread thread;
        private BufferedImage bimg;
        private static final int NUMPTS = 6;

        //  solid line stoke
        protected BasicStroke solid = new BasicStroke(10.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
        //  dashed line stroke
        protected BasicStroke dashed = new BasicStroke(10.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, new float[]{5}, 0);
        private float animpts[] = new float[NUMPTS * 2];
        private float deltas[] = new float[NUMPTS * 2];
        protected Paint fillPaint, drawPaint;

        // indicates whether or not to fill shape
        protected boolean doFill = true;

        // indicates whether or not to intentionally leak memory
        protected boolean leakMemory = arguments.indexOf("leak") > -1;

        // indicates whether to simulate blocking on the EDT
        private boolean block = arguments.indexOf("block") > -1;
        private int blockCounter = 0;

        // indicates whether to simulate long-running tasks on the EDT
        private boolean longRunning = arguments.indexOf("longRunning") > -1;
        private int longRunningCounter = 0;

        // indicates whether or not to draw shape
        protected boolean doDraw = true;
        protected GradientPaint gradient;
        protected BasicStroke stroke;
        private final Object waitObject = new Object();


        public Demo() {
            setBackground(Color.WHITE);
            gradient = new GradientPaint(0, 0, Color.RED, 200, 200, Color.YELLOW);
            fillPaint = gradient;
            drawPaint = Color.BLUE;
            stroke = solid;
        }


        // generates new points for the path
        public void animate(float[] pts, float[] deltas, int i, int limit) {
            float newpt = pts[i] + deltas[i];
            if (newpt <= 0) {
                newpt = -newpt;
                deltas[i] = (float)(Math.random() * 4.0 + 2.0);
            } else if (newpt >= (float)limit) {
                newpt = 2.0f * limit - newpt;
                deltas[i] = -(float)(Math.random() * 4.0 + 2.0);
            }
            pts[i] = newpt;
        }


        /*
         * generates random points with the specified surface width
         * and height for the path
         */
        public void reset(int w, int h) {
            for (int i = 0; i < animpts.length; i += 2) {
                animpts[i + 0] = (float)(Math.random() * w);
                animpts[i + 1] = (float)(Math.random() * h);
                deltas[i + 0] = (float)(Math.random() * 6.0 + 4.0);
                deltas[i + 1] = (float)(Math.random() * 6.0 + 4.0);
                if (animpts[i + 0] > w / 2.0f) {
                    deltas[i + 0] = -deltas[i + 0];
                }
                if (animpts[i + 1] > h / 2.0f) {
                    deltas[i + 1] = -deltas[i + 1];
                }
            }
            gradient = new GradientPaint(0, 0, Color.RED, w * .7f, h * .7f, Color.YELLOW);
        }


        // calls animate for every point in animpts
        public void step(int w, int h) {
            for (int i = 0; i < animpts.length; i += 2) {
                animate(animpts, deltas, i + 0, w);
                animate(animpts, deltas, i + 1, h);
            }
            if (block && (++blockCounter % 100 == 0)) {
                // Enter synchronized method from EDT every 100th time and sleep
                block(true);
                blockCounter = 0;
            }
        }


        // sets the points of the path and draws and fills the path
        public void drawDemo(Graphics2D g2) {
            float[] ctrlpts = animpts;
            int len = ctrlpts.length;
            float prevx = ctrlpts[len - 2];
            float prevy = ctrlpts[len - 1];
            float curx = ctrlpts[0];
            float cury = ctrlpts[1];
            float midx = (curx + prevx) / 2.0f;
            float midy = (cury + prevy) / 2.0f;
            GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO);
            gp.moveTo(midx, midy);
            if (leakMemory) {
                leakMap.put(new Long(System.currentTimeMillis()), gp);
            }
            for (int i = 2; i <= ctrlpts.length; i += 2) {
                float x1 = (midx + curx) / 2.0f;
                float y1 = (midy + cury) / 2.0f;
                prevx = curx;
                prevy = cury;
                if (i < ctrlpts.length) {
                    curx = ctrlpts[i + 0];
                    cury = ctrlpts[i + 1];
                } else {
                    curx = ctrlpts[0];
                    cury = ctrlpts[1];
                }
                midx = (curx + prevx) / 2.0f;
                midy = (cury + prevy) / 2.0f;
                float x2 = (prevx + midx) / 2.0f;
                float y2 = (prevy + midy) / 2.0f;
                gp.curveTo(x1, y1, x2, y2, midx, midy);
            }
            gp.closePath();
            if (doDraw) {
                g2.setPaint(drawPaint);
                g2.setStroke(stroke);
                g2.draw(gp);
            }
            if (doFill) {
                if (fillPaint instanceof GradientPaint) {
                    fillPaint = gradient;
                }
                g2.setPaint(fillPaint);
                g2.fill(gp);
            }
        }


        public Graphics2D createGraphics2D(int w, int h) {
            Graphics2D g2;
            if (bimg == null || bimg.getWidth() != w || bimg.getHeight() != h) {
                bimg = (BufferedImage)createImage(w, h);
                reset(w, h);
            }
            g2 = bimg.createGraphics();
            g2.setBackground(getBackground());
            g2.clearRect(0, 0, w, h);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            return g2;
        }


        public void paint(Graphics g) {
            Dimension d = getSize();
            step(d.width, d.height);
            Graphics2D g2 = createGraphics2D(d.width, d.height);
            drawDemo(g2);
            g2.dispose();
            if (bimg != null) {
                g.drawImage(bimg, 0, 0, this);
            }
        }


        public void start() {
            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }


        public synchronized void stop() {
            thread = null;
        }


        public void run() {
            Thread me = Thread.currentThread();
            while (thread == me) {
                repaint();
                if (block) {
                    // Enter synchronized method from controlling thread but do not sleep
                    block(false);
                }
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    break;
                }
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        onEDTMethod();
                    }
                });
            }
            thread = null;
        }

        public void onEDTMethod() {
            // do something on EDT thread
            int count = 10;
            if (longRunning && (++longRunningCounter % 250 == 0)) {
                longRunningCounter = 0;
                count = 1000000;
            }
            for (int i = 0; i < count ; i++) {
                Integer.parseInt("1234567");
            }
        }

        private synchronized void block(boolean sleep) {
            if (!sleep) {
                return;
            }
            try {
                // does not give up lock on Demo that guards this method
                synchronized (waitObject) {
                    // return after 200 ms
                    waitObject.wait(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    } // End Demo class


    /**
     * The DemoControls class controls fills and strokes.
     */
    static class DemoControls extends JPanel implements ActionListener {
        static TexturePaint tp1, tp2;

        static {
            BufferedImage bi = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
            bi.setRGB(0, 0, 0xff00ff00);
            bi.setRGB(1, 0, 0xffff0000);
            tp1 = new TexturePaint(bi, new Rectangle(0, 0, 2, 1));
            bi = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
            bi.setRGB(0, 0, 0xff0000ff);
            bi.setRGB(1, 0, 0xffff0000);
            tp2 = new TexturePaint(bi, new Rectangle(0, 0, 2, 1));
        }

        Demo demo;
        static Paint drawPaints[] =
            {new Color(0, 0, 0, 0), Color.BLUE, new Color(0, 0, 255, 126),
                Color.BLUE, tp2};
        static String drawName[] =
            {"No Draw", "Blue", "Blue w/ Alpha", "Blue Dash", "Texture"};
        static Paint fillPaints[] =
            {new Color(0, 0, 0, 0), Color.GREEN, new Color(0, 255, 0, 126),
                tp1, new GradientPaint(0, 0, Color.RED, 30, 30, Color.YELLOW)};
        String fillName[] =
            {"No Fill", "Green", "Green w/ Alpha", "Texture", "Gradient"};

        JMenu fillMenu, drawMenu;
        JMenuItem fillMI[] = new JMenuItem[fillPaints.length];
        JMenuItem drawMI[] = new JMenuItem[drawPaints.length];
        JCheckBox leakCheckbox;
        JCheckBox blockCheckbox;
        JCheckBox longRunningCheckbox;
        PaintedIcon fillIcons[] = new PaintedIcon[fillPaints.length];
        PaintedIcon drawIcons[] = new PaintedIcon[drawPaints.length];
        Thread thread;


        public DemoControls(Demo demo) {
            this.demo = demo;

            JMenuBar drawMenuBar = new JMenuBar();
            drawMenuBar.setBorder(null);
            JMenuBar fillMenuBar = new JMenuBar();
            fillMenuBar.setBorder(null);

            drawMenu = drawMenuBar.add(new JMenu("Draw Choice"));
            for (int i = 0; i < drawPaints.length; i++) {
                drawIcons[i] = new PaintedIcon(drawPaints[i]);
                drawMI[i] = drawMenu.add(new JMenuItem(drawName[i]));
                drawMI[i].setIcon(drawIcons[i]);
                drawMI[i].addActionListener(this);
            }
            drawMenu.setIcon(drawIcons[1]);

            fillMenu = fillMenuBar.add(new JMenu("Fill Choice"));
            for (int i = 0; i < fillPaints.length; i++) {
                fillIcons[i] = new PaintedIcon(fillPaints[i]);
                fillMI[i] = fillMenu.add(new JMenuItem(fillName[i]));
                fillMI[i].setIcon(fillIcons[i]);
                fillMI[i].addActionListener(this);
            }
            fillMenu.setIcon(fillIcons[fillPaints.length - 1]);

            leakCheckbox = new JCheckBox("Leak memory", demo.leakMemory);
            leakCheckbox.addActionListener(this);
            blockCheckbox = new JCheckBox("Simulate blocking", demo.block);
            blockCheckbox.addActionListener(this);
            longRunningCheckbox = new JCheckBox("Long-running tasks on EDT", demo.longRunning);
            longRunningCheckbox.addActionListener(this);

            Box optionsBox = Box.createVerticalBox();
            optionsBox.add(leakCheckbox);
            optionsBox.add(blockCheckbox);
            optionsBox.add(longRunningCheckbox);

            add(drawMenuBar);
            add(fillMenuBar);
            add(optionsBox);

        }


        public void actionPerformed(ActionEvent e) {
            Object obj = e.getSource();
            for (int i = 0; i < fillPaints.length; i++) {
                if (obj.equals(fillMI[i])) {
                    demo.doFill = true;
                    demo.fillPaint = fillPaints[i];
                    fillMenu.setIcon(fillIcons[i]);
                    break;
                }
            }
            for (int i = 0; i < drawPaints.length; i++) {
                if (obj.equals(drawMI[i])) {
                    demo.doDraw = true;
                    demo.drawPaint = drawPaints[i];
                    if (((JMenuItem)obj).getText().endsWith("Dash")) {
                        demo.stroke = demo.dashed;
                    } else {
                        demo.stroke = demo.solid;
                    }
                    drawMenu.setIcon(drawIcons[i]);
                    break;
                }
            }
            if (obj.equals(fillMI[0])) {
                demo.doFill = false;
            } else if (obj.equals(drawMI[0])) {
                demo.doDraw = false;
            }
            demo.leakMemory = leakCheckbox.isSelected();
            demo.block = blockCheckbox.isSelected();
            demo.longRunning = longRunningCheckbox.isSelected();
        }


        /**
         * The PaintedIcon class provides little filled icons
         * for the fill and stroke menu choices.
         */
        static class PaintedIcon implements Icon {
            Paint paint;

            public PaintedIcon(Paint p) {
                this.paint = p;
            }


            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setPaint(paint);
                g2.fillRect(x, y, getIconWidth(), getIconHeight());
                g2.setColor(Color.GRAY);
                g2.draw3DRect(x, y, getIconWidth() - 1, getIconHeight() - 1, true);
            }

            public int getIconWidth() {
                return 12;
            }

            public int getIconHeight() {
                return 12;
            }
        } // End PaintedIcon class

    } // End DemoControls class


    public static void main(String args[]) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        arguments = Arrays.asList(args);

        final BezierAnim demo = new BezierAnim();
        demo.init();
        Frame f = new Frame("Java 2D(TM) Demo - BezierAnim");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            public void windowDeiconified(WindowEvent e) {
                demo.start();
            }

            public void windowIconified(WindowEvent e) {
                demo.stop();
            }
        });
        f.add(demo);
        f.pack();
        f.setSize(new Dimension(400, 350));
        f.setVisible(true);
        demo.start();
    }
} // End BezierAnim class
