import javax.swing.*;
import java.awt.*;

public class RecursiveFractalFrame extends JFrame
{
    private RecursiveFractalPanel mainPanel;

    public RecursiveFractalFrame()
    {
        super("Mandelbrot");
        getContentPane().setLayout(new GridLayout(1,1));
        mainPanel = new RecursiveFractalPanel();
        getContentPane().add(mainPanel);
        setSize(800,800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

    }


}
