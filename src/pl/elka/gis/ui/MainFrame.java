package pl.elka.gis.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import pl.elka.gis.logic.Controller;
import pl.elka.gis.logic.GraphResolver;
import pl.elka.gis.model.Graph;
import pl.elka.gis.model.ResultSet;
import pl.elka.gis.ui.components.CalculationProgressDialog;
import pl.elka.gis.ui.components.GraphPaintingPanel;
import pl.elka.gis.ui.components.ProgressCallback;
import pl.elka.gis.utils.AppConstants;
import pl.elka.gis.utils.FilePickingUtils;
import pl.elka.gis.utils.Log;
import pl.elka.gis.utils.WindowUtilities;

public class MainFrame extends JFrame implements ProgressCallback {

    private static final String LOG_TAG = "MainFrame";
    private static final String APP_NAME = "K-graph center solver";
    private Dimension mScreenSize;
    private Controller mController;
    private boolean mIsGraphLoaded;
    private GraphGenerationFrame mGraphGenerationFrame;
    private CalculationProgressDialog mProgressDialog;
    private boolean mGraphCalculationInProgress;
    private JMenuItem mProgressOption;
    private JLabel mStatusLabel;
    private String mLastStatusText;
    // private GraphFrame mGraphFrame;
    private GraphPaintingPanel mGraphPanel;
    private ScrollPane mScrollPane;
    private long mGraphCalculateStartTime;
    private long mGraphCalculateEndTime;
    private Graph mGraph;
    private GraphResolver mGraphResolver;

    public MainFrame() {
        super(APP_NAME);
        WindowUtilities.setNativeLookAndFeel();
        mScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(mScreenSize.width / 8, mScreenSize.height / 8);
        setSize(mScreenSize.width * 1 / 4, mScreenSize.height * 1 / 4);
        setPreferredSize(new Dimension(mScreenSize.width * 1 / 4, mScreenSize.height * 1 / 4));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        prepareMenu();

        mController = new Controller();
        mScrollPane = new ScrollPane();

        mLastStatusText = "Idle";
        mStatusLabel = new JLabel(mLastStatusText);
        mStatusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        Container con = getContentPane();
        con.add(mStatusLabel, BorderLayout.SOUTH);
        con.add(mScrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private GraphPaintingPanel getOrCreateGraphPanel() {
        if (mGraphPanel == null) {
            mGraphPanel = new GraphPaintingPanel();
            mGraphPanel.setPreferredSize(new Dimension(AppConstants.MAX_X_Y_VALUE, AppConstants.MAX_X_Y_VALUE));
        }
        return mGraphPanel;
    }

    private void prepareMenu() {
        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        JMenuItem openItem = new JMenuItem("Open");
        openItem.setMnemonic('O');
        file.add(openItem);
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.setMnemonic('w');
        file.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('x');
        file.add(exitItem);
        JMenu actions = new JMenu("Actions");
        actions.setMnemonic('F');
        JMenuItem generateItem = new JMenuItem("Generate graph");
        generateItem.setMnemonic('G');
        actions.add(generateItem);
        JMenuItem countGraph = new JMenuItem("Count graph");
        countGraph.setMnemonic('C');
        actions.add(countGraph);
        JMenuItem stats = new JMenuItem("Statistics");
        stats.setMnemonic('S');
        actions.add(stats);
        mProgressOption = new JMenuItem("Show progress");
        mProgressOption.setMnemonic('p');
        mProgressOption.setEnabled(false);
        // JMenuItem showGraph = new JMenuItem("Show graph");
        // showGraph.setMnemonic('h');
        // actions.add(showGraph);
        actions.add(mProgressOption);

        closeItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                mScrollPane.removeAll();
                if (mGraphPanel != null) {
                    mGraphPanel = null;
                }
                mLastStatusText = "Idle";
                mStatusLabel.setText(mLastStatusText);
                mIsGraphLoaded = false;
            }
        });
        openItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (mGraphCalculationInProgress) {
                    JOptionPane
                            .showMessageDialog(MainFrame.this, "Calculation in progress.\nPlease wait until finished.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                File f = FilePickingUtils.openFileChooser(MainFrame.this);
                if (f != null) {
                    try {
                        mGraph = Graph.fromFile(f);
                        mGraphResolver = new GraphResolver(mGraph);

                        // mController.initController(); // remove old data
                        // FileHandler.readSourceFileContent(f, mController);
                        // getOrCreateGraphPanel().setPlainGraphController(mController);
                        mIsGraphLoaded = true;
                        // mLastStatusText = "File: " + f.getName() + ", Vertexes=" + mController.getVertexSet().size() +
                        // ", Edges="
                        // + mController.getEdgesMap().size();
                        // mStatusLabel.setText(mLastStatusText);
                        // mScrollPane.add(mGraphPanel);
                        // mGraphPanel.refreshGraph();

                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        exitItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        // actions menu
        generateItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (mGraphGenerationFrame != null) {
                    mGraphGenerationFrame.dispose(); // dispose and recreate
                }
                mGraphGenerationFrame = new GraphGenerationFrame(MainFrame.this.getLocationOnScreen());
            }
        });
        countGraph.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!mIsGraphLoaded) {
                    JOptionPane.showMessageDialog(MainFrame.this, "No graph loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (mGraphCalculationInProgress) {
                    JOptionPane
                            .showMessageDialog(MainFrame.this, "Calculation in progress.\nPlease wait until finished.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String val = (String) JOptionPane
                        .showInputDialog(MainFrame.this, "Enter centers count:", "Centers count", JOptionPane.PLAIN_MESSAGE, null, null, "3");
                if ((val != null) && (val.length() > 0)) {
                    try {
                        int centersCount = Integer.parseInt(val);

                        mGraphResolver.resolve(centersCount, MainFrame.this);

                        // mController.resetControllerWithSameData();
                        // mController.setCentersCount(centersCount);
                        // startGraphProcessing();
                        // mController.countGraphData(centersCount, MainFrame.this);
                        // mProgressDialog.setVisible(true);
                    } catch (NumberFormatException ex) {
                        JOptionPane
                                .showMessageDialog(MainFrame.this, "Wrong centers number.", "Error", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            }
        });
        stats.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!mIsGraphLoaded) {
                    JOptionPane.showMessageDialog(MainFrame.this, "No graph loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                showStatsDialog();
                // print all data in the console
                Log.d(LOG_TAG, mController.getControllerData());
            }
        });
        mProgressOption.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (mProgressDialog != null) {
                    mProgressDialog.setVisible(true);
                } else {
                    mProgressDialog = new CalculationProgressDialog(MainFrame.this, "Calculation progress", true, mController
                            .getVertexSet()
                            .size(), mController.getEdgesMap().size(), mController.getCentersCount());
                    mProgressDialog.setVisible(true);
                }
            }
        });
        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);
        bar.add(file);
        bar.add(actions);
    }

    private void showStatsDialog() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vertexes count: " + mController.getVertexSet().size() + "\n");
        sb.append("Edges count: " + mController.getEdgesMap().size() + "\n");
        sb.append("Centers count: " + mController.getCentersCount() + "\n");
        ResultSet lastResult = mController.getResultSet();
        sb.append("Centers in vertexes: " + lastResult.getCentersSetAsString() + "\n");
        sb.append("Longest path vertexes: " + lastResult.getLongestPathVertexSetAsString() + "\n");
        sb.append("Longest path edges: " + lastResult.getLongestPathEdgesSetAsString() + "\n");
        sb.append("Longest path weight: " + lastResult.getLongestPath() + "\n");
        if (mGraphCalculateStartTime != 0 && mGraphCalculateEndTime != 0) {
            long calTime = mGraphCalculateEndTime - mGraphCalculateStartTime;
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss.ms");
            Date resultdate = new Date(calTime);
            sb.append("Calculation time: " + sdf.format(resultdate) + "\n");
        }
        JOptionPane.showMessageDialog(MainFrame.this, sb.toString(), "Statistics", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startGraphProcessing() {
        if (mGraphGenerationFrame != null) {
            mGraphGenerationFrame.dispose(); // dispose this dialog
        }
        mGraphCalculateStartTime = mGraphCalculateEndTime = 0;
        mGraphCalculationInProgress = true;
        mProgressOption.setEnabled(true);
        mGraphCalculateStartTime = System.currentTimeMillis();
        mProgressDialog = new CalculationProgressDialog(this, "Calculation progress", true, mController.getVertexSet().size(), mController
                .getEdgesMap()
                .size(), mController.getCentersCount());
    }

    @Override
    public void updateProgress(float progressValue) {
        Log.d(LOG_TAG, "updateProgress=" + progressValue);
        if (mProgressDialog != null) {
            mProgressDialog.updateProgress(progressValue);
        }
        mStatusLabel.setText("  Progress " + progressValue + "%");
    }

    @Override
    public void calculationError(String errorMessage) {
        Log.d(LOG_TAG, "calculationError=" + errorMessage);
        mGraphCalculateEndTime = System.currentTimeMillis();
        if (mProgressDialog != null) {
            mProgressDialog.dispose();
        }
        JOptionPane.showMessageDialog(MainFrame.this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
        mGraphCalculationInProgress = false;
        mProgressOption.setEnabled(false);
        mStatusLabel.setText(mLastStatusText);
    }

    @Override
    public void calculationFinished() {
        Log.d(LOG_TAG, "calculationFinished");
        mGraphCalculateEndTime = System.currentTimeMillis();
        if (mProgressDialog != null) {
            mProgressDialog.dispose();
        }
        mGraphPanel.refreshGraph();
        // mGraphFrame.refreshGraph();
        mGraphCalculationInProgress = false;
        mProgressOption.setEnabled(false);
        mStatusLabel.setText(mLastStatusText);
        showStatsDialog();
    }
}