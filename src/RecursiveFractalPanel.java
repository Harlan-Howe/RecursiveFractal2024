import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

public class RecursiveFractalPanel extends JPanel implements ComponentListener, MouseListener, MouseMotionListener
{
    private BufferedImage image;
    private boolean needsRefresh, shouldInterrupt;
    private double minMathX, minMathY, maxMathX, maxMathY;
    private final double threshold_squared = 10;
    private final int max_count = 1024;
    private int startCornerX, startCornerY, endCornerX, endCornerY;
    private Stack<ComplexRange> undoStack, redoStack;
    private RecursiveFractalFrame parent;
    private File lastFile = null;
    private int scanMode;

    public static final int MODE_TRADITIONAL = 0;
    public static final int MODE_BLOCKY = 1;
    public static final int MODE_DIVIDE_AND_CONQUER = 2;

    public RecursiveFractalPanel(RecursiveFractalFrame parent)
    {
        super();
        this.parent = parent;
        undoStack = new Stack<ComplexRange>();
        redoStack = new Stack<ComplexRange>();
        this.addComponentListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        MandelbrotThread mt = new MandelbrotThread();
        mt.start();
        performReset();
        startCornerX = -1;
        startCornerY = -1;
        scanMode = MODE_TRADITIONAL;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        synchronized (image)
        {
            g.drawImage(image,0,0,null);
        }
        // draw selection rectangle, if you are dragging.
        if (startCornerX != -1 && startCornerY != -1)
        {
            g.setColor(new Color((int)(256*Math.random()),
                                 (int)(256*Math.random()),
                                 (int)(256*Math.random())));
            g.drawRect(Math.min(startCornerX,endCornerX),
                       Math.min(startCornerY,endCornerY),
                       Math.abs(endCornerX-startCornerX),
                       Math.abs(endCornerY-startCornerY));
        }
    }

    public void setScanMode(int scanMode)
    {
        this.scanMode = scanMode;
        shouldInterrupt = true;
        needsRefresh = true;
    }

    /**
     * Converts a given horizontal pixel value into the mathematical x-coordinate that this pixel represents in our
     * complex plane.
     * @param x the x-location of the pixel, an integer
     * @return the double that represents the real part of the point on the complex plane corresponding to this
     * x-location.
     */
    public double pixelX2MathX(int x)
    {
        if (minMathX == maxMathX)
            return minMathX;
        double frac = ((double)x)/getWidth();
        return minMathX + frac*(maxMathX-minMathX);
    }
    /**
     * Converts a given vertical pixel value into the mathematical y-coordinate that this pixel represents in our
     * complex plane.
     * @param y the y-location of the pixel, an integer
     * @return the double that represents the imaginary part of the point on the complex plane corresponding to this
     * y-location.
     */
    public double pixelY2MathY(int y)
    {
        if (minMathY == maxMathY)
            return minMathY;
        double frac = 1 - ((double)y)/getHeight(); // used 1-y/H because screen is inverted in y.
        return minMathY + frac*(maxMathY-minMathY);
    }

    /**
     * given a positive integer, returns a Color object that is (most likely) similar to the colors
     * that would be returned for count-1 and count+1. This count should always produce the same color,
     * but occasional jumps are acceptable.
     * If count is zero, though, return black.
     * @param count - the number we are converting into a Color
     * @return - the color for this count.
     */
    public Color count2Color(int count)
    {
        if (count == 0)
            return Color.BLACK;
        int c1 = 5*count;
        int c2 = count/8;
        return new Color((1-(count/256)%2)*(255-count%256)+(count/256)%2*(count%256),
                         (1-(c1/256)%2)*(c1%256)+(c1/256)%2*(255-c1%256),
                         (1-(c2/256)%2)*(c2%256)+(c2/256)%2*(255-c1%256));
    }

    /**
     * Starting with z = (0 + 0i), applies the function z -> z^2 + c over and over again until either
     * a) the magnitude of z exceeds the threshold, in which case we return the count of steps it took, or
     * b) the number of steps we've taken reaches max_count, in which case we return zero.
     * @param c - a complex number we wish to calculate this for.
     * @return - the number of steps that it took for z to exceed the threshold distance from the origin,
     * or zero, if the number of steps reached max_steps.
     */
    public int countStepsToExit(Complex c)
    {
        Complex z = Complex.zero();
        for (int count = 0; count < max_count; count++)
        {
            z = z.squared().plus(c);
            if (z.magnitude_squared()>threshold_squared)
                return count;
        }
        return 0;
    }

    /**
     * convenience function that combines countStepsToExit() with count2Color() to find the color
     * desired for a given pixel.
     * @param x - the x coordinate of the pixel
     * @param y - the y coordinat of the pixel
     * @return the color that should be drawn at that pixel.
     */
    public Color getColorForPixel(int x, int y)
    {
        return count2Color(countStepsToExit(new Complex(pixelX2MathX(x),pixelY2MathY(y))));
    }

    public void performReset()
    {
        if (minMathX == -2 && minMathY == -2 && maxMathX == 2 && maxMathY == 2)
            return;
        undoStack.push(new ComplexRange(new Complex(minMathX, minMathY), new Complex(maxMathX, maxMathY)));
        parent.setUndoMenuEnabled(true);
        setMathBounds(new Complex(-2,-2), new Complex(+2, +2));
        redoStack.clear();
        parent.setRedoMenuEnabled(false);
    }

    public void setMathBounds(Complex cMin, Complex cMax)
    {
        shouldInterrupt=true;
        minMathX = Math.min(cMin.getReal(),cMax.getReal());
        minMathY = Math.min(cMin.getImaginary(), cMax.getImaginary());
        maxMathX = Math.max(cMin.getReal(),cMax.getReal());
        maxMathY = Math.max(cMin.getImaginary(), cMax.getImaginary());
        needsRefresh = true;
    }

    /**
     * reverts to last recorded ComplexRange, if any, and (if so) adds the current (pre-change) range to redoStack.
     * @return whether there are any undo items remaining.
     */
    public void performUndo()
    {
        if (undoStack.empty())
            return;
        ComplexRange presentRange = new ComplexRange(new Complex(minMathX,minMathY), new Complex(maxMathX, maxMathY));
        redoStack.push(presentRange);
        parent.setRedoMenuEnabled(true);
        ComplexRange lastRange = undoStack.pop();
        setMathBounds(lastRange.getMin(), lastRange.getMax());
        parent.setUndoMenuEnabled(!undoStack.empty());
    }

    /**
     * reverts to last "undone" ComplexRange, if any, and (if so) adds the current (pre-change) range back to undoStack.
     * @return whether there are any redo items remaining.
     */
    public void performRedo()
    {
        if (redoStack.empty())
            return;
        ComplexRange presentRange = new ComplexRange(new Complex(minMathX,minMathY), new Complex(maxMathX, maxMathY));
        undoStack.push(presentRange);
        parent.setUndoMenuEnabled(true);
        ComplexRange lastRange = redoStack.pop();
        setMathBounds(lastRange.getMin(), lastRange.getMax());
        parent.setRedoMenuEnabled(!redoStack.empty());
    }

    /**
     * The user has asked us to zoom in, giving a start and end pixels that correspond to opposite
     * corners of a rectangle. We need to update the startMathX, startMathY, endMathX, endMathY to
     * the corresponding corners of the rectangle.
     * (Note the given points may not represent the (left, top) and (right, bottom) corners, so we
     * need to compensate for this.)
     */
    public void updateMathBoundsFromMouseDrag()
    {
        if (startCornerX!=endCornerX && startCornerY!=endCornerY)
        {
            undoStack.push(new ComplexRange(new Complex(minMathX, minMathY), new Complex(maxMathX, maxMathY)));
            parent.setUndoMenuEnabled(true);

            Complex c1 = new Complex(pixelX2MathX(startCornerX),pixelY2MathY(startCornerY));
            Complex c2 = new Complex(pixelX2MathX(endCornerX),pixelY2MathY(endCornerY));
            setMathBounds(c1,c2);

            // since we've changed the bounds, we need to stop the scan in progress (if any) and
            // start over.
            shouldInterrupt = true;
            needsRefresh = true;
            redoStack.clear();
            parent.setRedoMenuEnabled(false);
        }
    }

    /**
     * give the user the option to select a location to save an
     * image file that matches the current display.
     */
    public void doSaveScreen()
    {
        JFileChooser chooser = new JFileChooser();
        if (lastFile != null)
            chooser.setSelectedFile(lastFile);
        chooser.setDialogTitle("Export");
        String[] extensions = {"jpg","gif","png"};
        chooser.setFileFilter(new FileNameExtensionFilter("images",extensions));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            lastFile = chooser.getSelectedFile();
            String filename = lastFile.getPath();
            BufferedImage exportImage = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);
            Graphics2D gExport = exportImage.createGraphics();
            // tell it to draw things well, or you get mushy fonts and square dots.
            gExport.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gExport.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            paintComponent(gExport);
            gExport.dispose();
            int i = filename.lastIndexOf('.');
            if (i<=0)
            {
                try
                {
                    ImageIO.write(exportImage, "png", new File(filename + ".png"));
                }catch(IOException ioExp)
                {
                    System.out.println("Problem writing file.");
                    ioExp.printStackTrace();
                }
            }
            else
            {
                String extension = filename.substring(i+1);
                try
                {
                    ImageIO.write(exportImage, extension, new File(filename));
                }catch(IOException ioExp)
                {
                    System.out.println("Problem writing file.");
                    ioExp.printStackTrace();
                }
            }

        }
    }

    @Override
    /**
     * the user just changed the size of the window (or we initially put this panel into the window)
     * we need to generate a new drawing canvas and tell the computer to restart its scan.
     */
    public void componentResized(ComponentEvent e)
    {
        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        shouldInterrupt = true;
        needsRefresh = true;
        repaint();
    }

    @Override
    public void componentMoved(ComponentEvent e)
    {
        // the user just moved the panel to a new location. We're not doing anything here, but
        // we need this method to fulfill the ComponentListener interface.
    }

    @Override
    public void componentShown(ComponentEvent e)
    {
        // the panel has switched from not being visible to being visible.
        // We're not doing anything here, but we need this method to fulfill the ComponentListener interface.
    }

    @Override
    public void componentHidden(ComponentEvent e)
    {
        // the panel has switched from being visible to not being visible.
        // We're not doing anything here, but we need this method to fulfill the ComponentListener interface.
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        // the user just released the mouse at the same coordinates as he/she originally pressed it.
        // We're not doing anything here, but we need this method to fulfill the MouseListener interface.
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        // the user just pressed the mouse button down. We want to start showing the drag rectangle.
        startCornerX = e.getX();
        startCornerY = e.getY();
        endCornerX = e.getX();
        endCornerY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        // the user just released the mouse button. So we want to initiate the zoom process.
        endCornerX = e.getX();
        endCornerY = e.getY();
        updateMathBoundsFromMouseDrag();
        repaint();
        startCornerX = -1;
        startCornerY = -1;
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        // the user just moved the mouse into this panel.
        // We're not doing anything here, but we need this method to fulfill the MouseListener interface.
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        // the user just moved the mouse out of this panel.
        // we want to cancel any drag that we are doing.

        startCornerX = -1;
        startCornerY = -1;
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        // the user has moved the mouse to a new location with the button held down.
        endCornerX = e.getX();
        endCornerY = e.getY();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        // the user just moved the mouse to a new location without pressing the button.
        // We're not doing anything here, but we need this method to fulfill the MouseMotionListener interface.
    }

    /**
     * this class is a "thread" that will run at the same time as the rest of the program. This is
     * where the actual calculation of the fractal is being done.
     */
    public class MandelbrotThread extends Thread
    {
        public MandelbrotThread()
        {
            super();
        }

        /**
         * this method is automatically called when we are told to "start()." Do not (ever) call
         * this method directly.
         */
        public void run()
        {
            while(true)
            {
                if (needsRefresh && image != null)
                {
                    needsRefresh = false;
                    switch (scanMode)
                    {
                        case MODE_TRADITIONAL:
                            performTraditionalScan();
                            break;
                        case MODE_BLOCKY:
                            performPixelatedScan();
                            break;
                        case MODE_DIVIDE_AND_CONQUER:
                        default:
                            performDivideAndConquerScan(); // you'll be writing this one.
                    }
                }
                // if we don't need to refresh, wait 1/2 a second and check again.
                try
                {
                    Thread.sleep(500); // milliseconds
                } catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
                shouldInterrupt = false;

            }
        }

        // draws the fractal by doing a typical line-by-line scan down the page.
        public void performTraditionalScan()
        {
            Graphics image_g = image.getGraphics();
            for (int y=0; y<getHeight(); y++)
                for (int x=0; x<getWidth(); x++)
                {
                    if (shouldInterrupt)
                        return;
                    // we're using the "synchronized" command here to tell the computer to wait until
                    // any other thread is using the image. (In this case, the Panel's paintComponent()
                    // method. Once it is free, we'll lock it on our behalf, draw a pixel, and unlock it.
                    synchronized (image)
                    {
                        image.setRGB(x, y, getColorForPixel(x, y).getRGB());
                    }
                    // tell the computer to refresh the panel's appearance at its next convenience.
                    repaint();
                }
            needsRefresh = false;
        }

        /**
         * draws the fractal by dividing up the screen into blocks, filling the blocks up with the
         * color for the point at the upper-left corner. After we draw the big blocks, we break them into smaller
         * blocks and repeat until we have made 1x1 pixel blocks.
         */
        public void performPixelatedScan()
        {
            int resolution = Math.min(getWidth(), getHeight());
            int previous_resolution = resolution*2;
            synchronized (image)
            {
                Graphics imgG = image.getGraphics();
                imgG.setColor(getColorForPixel(0,0));
                imgG.fillRect(0,0,resolution,resolution);
            }
            while (resolution > 0)
            {
                for (int y=0; y<getHeight(); y+= resolution)
                    for (int x=0; x<getWidth(); x+= resolution)
                    {
                        if (shouldInterrupt)
                            return;
                        if (x%(previous_resolution)==0 && y%(previous_resolution)==0)
                            continue;
                        synchronized (image)
                        {
                            Graphics imgG = image.getGraphics();
                            imgG.setColor(getColorForPixel(x, y));
                            imgG.fillRect(x,y,resolution,resolution);
                        }
                        repaint();
                    }
                previous_resolution = resolution;
                resolution /=2;

            }
        }

        /**
         * wrapper method to call the recursive divide-and-conquer scan for the whole panel.
         */
        public void performDivideAndConquerScan()
        {
            performDivideAndConquerScan(0,0,getWidth()-1,getHeight()-1);
        }

        /**
         * recursive method to draw the fractal for the box from (left, top) to (right, bottom), inclusive.
         * Draws the fractal by calculating (and displaying) all the points on the border of the rectangle.
         * If these are all the same, saves time by filling in the rectangle with that color. Otherwise,
         * insets the rectangle by 1 pixel inwards, divides it up into quarters, and recursively calls
         * performDivideAndConquerScan for each quarter.
         * @param left - x position of the top left corner of the region to draw, inclusive.
         * @param top - y position of the top left corner of the region to draw, inclusive.
         * @param right - x position of the bottom right corner of the region to draw, inclusive.
         * @param bottom - y position of the bottom right corner of the region to draw, inclusive.
         */
        public void performDivideAndConquerScan(int left, int top, int right, int bottom)
        {
            // TODO: You write this.
            // 1) You'll need to consider the base case of whether this rectangle is "inside out."
            // 2) lso, inside any loops, make sure you are checking the "shouldInterrupt" variable - if this is ever true,
            // you should return immediately. This is so that the user zooming or resizing the window will stop this
            // scan as quickly as possible.
            // 3) To change the color of a pixel at (x,y) to color c, you will make use of:
            //     synchronized(image)
            //     {
            //          image.setRGB(x, y, c.getRGB());
            //     }
            //     make sure that you put as little code as possible inside the synchronized curly brackets... this
            //     is locking the image as long as you are in there, so to make the program responsive, we need to
            //     hold it for as little time as possible. (It's ok to update 1-4 pixels in there, but don't do a
            //     more substantial loop or recursive call.)


        }
    }


}
