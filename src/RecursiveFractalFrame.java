import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class RecursiveFractalFrame extends JFrame implements ActionListener
{
    private RecursiveFractalPanel mainPanel;
    private JMenuItem exportImageMI, resetMI, undoMI, redoMI;
    private JMenuItem traditionalSM, pixelatedSM, divideAndConquerSM;
    private String[] scanTypeNames = {"Traditional", "Pixelated", "Divide and Conquer"};
    public RecursiveFractalFrame()
    {
        super("Mandelbrot");
        getContentPane().setLayout(new GridLayout(1,1));
        createMenu();
        mainPanel = new RecursiveFractalPanel(this);
        undoMI.setEnabled(false);
        getContentPane().add(mainPanel);
        setSize(800,800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

    }

    public void createMenu()
    {
        JMenuBar mainMenu = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu navigateMenu = new JMenu("Navigate");
        JMenu scanMenu = new JMenu("Scan");
        mainMenu.add(fileMenu);
        mainMenu.add(navigateMenu);
        mainMenu.add(scanMenu);

        exportImageMI = new JMenuItem("Export Image");
        exportImageMI.addActionListener(this);
        exportImageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.META_MASK));
        fileMenu.add(exportImageMI);

        resetMI = new JMenuItem("Reset to original bounds");
        resetMI.addActionListener(this);
        resetMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.META_MASK));
        navigateMenu.add(resetMI);

        undoMI = new JMenuItem("Undo");
        undoMI.addActionListener(this);
        undoMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.META_MASK));
        undoMI.setEnabled(false);
        navigateMenu.add(undoMI);

        redoMI = new JMenuItem("Redo");
        redoMI.addActionListener(this);
        redoMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.META_MASK+ActionEvent.SHIFT_MASK));
        redoMI.setEnabled(false);
        navigateMenu.add(redoMI);

        traditionalSM = new JCheckBoxMenuItem(scanTypeNames[0]);
        traditionalSM.setSelected(true);
        traditionalSM.addActionListener(this);
        traditionalSM.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.META_MASK));
        scanMenu.add(traditionalSM);

        pixelatedSM = new JCheckBoxMenuItem(scanTypeNames[1]);
        pixelatedSM.setSelected(false);
        pixelatedSM.addActionListener(this);
        pixelatedSM.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.META_MASK));
        scanMenu.add(pixelatedSM);

        divideAndConquerSM = new JCheckBoxMenuItem(scanTypeNames[2]);
        divideAndConquerSM.setSelected(false);
        divideAndConquerSM.addActionListener(this);
        divideAndConquerSM.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.META_MASK));
        scanMenu.add(divideAndConquerSM);


        this.setJMenuBar(mainMenu);

    }

    public void setUndoMenuEnabled(boolean enable) {undoMI.setEnabled(enable);}
    public void setRedoMenuEnabled(boolean enable) {redoMI.setEnabled(enable);}

    public void doExportImage()
    {
        System.out.println("Doing Export.");
        mainPanel.doSaveScreen();
    }

    public void doResetBounds()
    {
        System.out.println("Doing reset.");
        mainPanel.performReset();
    }

    public void doUndo()
    {
        System.out.println("Doing undo.");
        mainPanel.performUndo();

    }

    public void doRedo()
    {
        System.out.println("Doing redo.");
        mainPanel.performRedo();

    }
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == exportImageMI)
            doExportImage();
        if (e.getSource() == resetMI)
            doResetBounds();
        if (e.getSource() == undoMI)
            doUndo();
        if (e.getSource() == redoMI)
            doRedo();
        if (e.getSource() == traditionalSM)
        {
            traditionalSM.setSelected(true);
            pixelatedSM.setSelected(false);
            divideAndConquerSM.setSelected(false);
            mainPanel.setScanMode(RecursiveFractalPanel.MODE_TRADITIONAL);
        }
        if (e.getSource() == pixelatedSM)
        {
            traditionalSM.setSelected(false);
            pixelatedSM.setSelected(true);
            divideAndConquerSM.setSelected(false);
            mainPanel.setScanMode(RecursiveFractalPanel.MODE_PIXELATED);
        }
        if (e.getSource() == divideAndConquerSM)
        {
            traditionalSM.setSelected(false);
            pixelatedSM.setSelected(false);
            divideAndConquerSM.setSelected(true);
            mainPanel.setScanMode(RecursiveFractalPanel.MODE_DIVIDE_AND_CONQUER);
        }


    }
}
