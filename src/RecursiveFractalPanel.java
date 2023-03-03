import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class RecursiveFractalPanel extends JPanel implements ComponentListener, MouseListener, MouseMotionListener
{
    private BufferedImage image;
    private boolean needsRefresh, shouldInterrupt;
    private double minMathX, minMathY, maxMathX, maxMathY;
    private final double threshold_squared = 10;
    private final int max_count = 1024;
    private int startCornerX, startCornerY, endCornerX, endCornerY;


    public RecursiveFractalPanel()
    {
        super();
        this.addComponentListener(this);
        needsRefresh = true;
        shouldInterrupt = false;
        minMathX = -2;
        minMathY = -2;
        maxMathX = +2;
        maxMathY = +2;
        MandelbrotThread mt = new MandelbrotThread();
        mt.start();
        startCornerX = -1;
        startCornerY = -1;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        synchronized (image)
        {
            g.drawImage(image,0,0,null);
        }
    }

    public double pixelX2MathX(int x)
    {
        if (minMathX == maxMathX)
            return minMathX;
        double frac = ((double)x)/getWidth();
        return minMathX + frac*(maxMathX-minMathX);
    }

    public double pixelY2MathY(int y)
    {
        if (minMathY == maxMathY)
            return minMathY;
        double frac = 1 - ((double)y)/getHeight(); // used 1-y/H because screen is inverted in y.
        return minMathY + frac*(maxMathY-minMathY);
    }

    public Color count2Color(int count)
    {
        if (count == 0)
            return Color.BLACK;
        return new Color(count%256, (4*count)%256, 255-count%256);
    }

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

    public Color getColorForPixel(int x, int y)
    {
        return count2Color(countStepsToExit(new Complex(pixelX2MathX(x),pixelY2MathY(y))));
    }



    @Override
    public void componentResized(ComponentEvent e)
    {
//        System.out.println("resized to "+getWidth()+" x "+getHeight());
        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);

    }

    @Override
    public void componentMoved(ComponentEvent e)
    {

    }

    @Override
    public void componentShown(ComponentEvent e)
    {

    }

    @Override
    public void componentHidden(ComponentEvent e)
    {

    }

    @Override
    public void mouseClicked(MouseEvent e)
    {

    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        startCornerX = e.getX();
        startCornerY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        endCornerX = e.getX();
        endCornerY = e.getY();
        updateMathBounds();
        repaint();
        startCornerX = -1;
        startCornerY = -1;
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {

    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        startCornerX = -1;
        startCornerY = -1;
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        endCornerX = e.getX();
        endCornerY = e.getY();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {

    }

    public class MandelbrotThread extends Thread
    {
        public MandelbrotThread()
        {


        }

        public void run()
        {
            while(true)
            {
                if (needsRefresh && image != null)
                {
                    needsRefresh = false;
                    performTraditionalScan();


                }

                try
                {
                    Thread.sleep(50);
                } catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
                shouldInterrupt = false;

            }
        }

        public void performTraditionalScan()
        {
            Graphics image_g = image.getGraphics();
            for (int y=0; y<getHeight(); y++)
                for (int x=0; x<getWidth(); x++)
                {
                    if (shouldInterrupt)
                        return;
                    synchronized (image)
                    {
                        image.setRGB(x, y, getColorForPixel(x, y).getRGB());
                    }
                    repaint();
                }
            needsRefresh = false;
        }
    }

}
