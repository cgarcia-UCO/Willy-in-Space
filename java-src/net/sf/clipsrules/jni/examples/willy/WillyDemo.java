package net.sf.clipsrules.jni.examples.willy;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import javax.imageio.ImageIO; // mjmarin

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.MissingResourceException;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import net.sf.clipsrules.jni.*;

/**
 * This is a personal adaptation of the SudokuDemo example of the clips java
 * native interface to the Willy in the desert problem, an idea of Prof. Manuel
 * Jesus Marín Jiménez.
 * 
 * It is intended to be used to help Computing students to gain knowlege and
 * practice in programming with CLIPS.
 * 
 * This work is licensed under a
 * <a href='http://creativecommons.org/licenses/by-nc-nd/4.0/'>Creative Commons
 * Attribution-NonCommercial-NoDerivatives 4.0 International License
 * (http://creativecommons.org/licenses/by-nc-nd/4.0/), and comes with no
 * additional warranties of any kind.
 * 
 * Its author is Prof. Carlos García Martínez - cgarcia@uco.es
 * 
 * Other collaborators are: * Prof. Manuel Jesus Marín Jiménez * Prof. Amelia
 * Zafra Gómez
 * 
 * The original images were obtained from the following webpages in March 2017,
 * and were marked as \"Labeled for reuse with modification\" according to the
 * Google Image Search Engine - https://pixabay.com/es/photos/arrow/ -
 * http://www.publicdomainpictures.net/view-image.php?image=42224&picture=pointing-down-finger
 * - https://pixabay.com/es/smiley-icono-gestual-gracioso-821993/ -
 * https://commons.wikimedia.org/wiki/File:PEO-snake_alt.svg -
 * https://pixabay.com/es/arco-flecha-eje-perno-arma-307274/ -
 * https://commons.wikimedia.org/wiki/File:Antu_earthquake.svg -
 * https://pixabay.com/es/de-coco-selva-1293036/ -
 * https://commons.wikimedia.org/wiki/File:Quicksand_warning_sign_Texel_2004.jpg
 * - https://pixabay.com/es/bell-notificaci%C3%B3n-comunicaci%C3%B3n-1096279/
 */

public class WillyDemo implements ActionListener, KeyListener {
	JFrame jfrm;
	JPanel mainGrid;

	// JButton clearButton;
	JButton resetButton;
	JButton stepButton;
	JButton nextMoveButton;
	JButton runButton;
	JButton runFastButton;
	JButton nextMapButton;
	JButton previousMapButton;
	JCheckBox multipleActivations;
	JButton offlineButton;
	JPanel buttonGrid;
	JLabel numMovesLabel;
	JLabel numKnownCellsLabel;
	JLabel scoreLabel;
	JLabel resultLabel;
	JLabel resultLabel2;
	JLabel resultLabel3;
	JLabel mapName;
	JTextField searchFField;

	JTextArea facts = null;
	JTextArea agenda = null;
	JTextArea focus = null;
	boolean showMultipleActivations = true;
	JScrollPane scrollPaneAgenda = null;
	JScrollPane scrollPaneFocus = null;
	ImageIcon blankCell;

	boolean solved = false;

	ResourceBundle willyResources;

	Vector<File> maps;
	int indexCurrentMap;
	Environment clips;
	boolean isExecuting = false;
	boolean isReseted = false;
	Thread executionThread = null;
	MyRunnableClass runThread = null;
	MyFastRunnableClass fastRunThread = null;
	MyAllMapsOfflineRunnableClass offlineRunThread = null;

	Vector<Integer> globalNumMoves = new Vector<>();
	Vector<Integer> globalNumKnownCells = new Vector<>();
	Vector<Integer> globalScore = new Vector<>();
	Vector<Double> globalNumDeaths = new Vector<>();
	Vector<Double> globalNumWins = new Vector<>();
	Vector<Double> globalNumUnSolved = new Vector<>();
	double previousMeanScore = 0.;
	int previousIndexMap = 0;
	long offlineIterations = 0;

	static boolean interactiveMode = false;
	static String routeMaps = "./maps";
	static String routeWilly = "./willy.clp";

	static void setRouteMaps(String route) {
		routeMaps = route;
	}

	static void setRouteWilly(String route) {
		routeWilly = route;
	}

	class MyRunnableClass implements Runnable {

		protected boolean shallStop = false;

		public void requestStart() {
			shallStop = false;
		}

		public void requestStop() {
			shallStop = true;
		}

		public void run() {

			while (!shallStop && isExecuting && areThereActivations()) {
				// stepWilly();
				nextMoveWilly();
				try {
					Thread.sleep(100);// Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					isExecuting = false;
					updateGUI();
					runButton.setText("Run");
					runButton.setActionCommand("Run");
					runFastButton.setText("Run fast");
					runFastButton.setActionCommand("Run fast");
					offlineButton.setText("All maps");
					offlineButton.setActionCommand("RunOffline");
					jfrm.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					executionThread = null;
				}
			});
		}
	};

	class MyFastRunnableClass implements Runnable {

		protected boolean shallStop = false;

		public void requestStart() {
			shallStop = false;
		}

		public void requestStop() {
			shallStop = true;
		}

		public void run() {

			while (!shallStop && isExecuting && areThereActivations()) {
				silentStepWilly();
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					isExecuting = false;
					updateGUI();
					runButton.setText("Run");
					runButton.setActionCommand("Run");
					runFastButton.setText("Run fast");
					runFastButton.setActionCommand("Run fast");
					offlineButton.setText("All maps");
					offlineButton.setActionCommand("RunOffline");
					jfrm.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					executionThread = null;
				}
			});
		}
	};

	class MyAllMapsOfflineRunnableClass implements Runnable {

		protected boolean shallStop = false;

		public void requestStart() {
			shallStop = false;
		}

		public void requestStop() {
			shallStop = true;
		}

		public void run() {

			previousIndexMap = indexCurrentMap;

			while (!shallStop && isExecuting && areThereActivations()) {
				previousMeanScore = getMean(globalScore);
				runOffline();
				offlineIterations++;

				if (offlineIterations <= 1 || Math.abs(previousMeanScore - getMean(globalScore)) > 1.) {
					isExecuting = true;
					clips.reset();
				}
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					isExecuting = false;
					indexCurrentMap = previousIndexMap;
					clearAndLoadClips();
					silentReset();
					runButton.setText("Run");
					runButton.setActionCommand("Run");
					runFastButton.setText("Run fast");
					runFastButton.setActionCommand("Run fast");
					offlineButton.setText("All maps");
					offlineButton.setActionCommand("RunOffline");
					setEnabledResetingButtons(true);
					jfrm.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					executionThread = null;
				}
			});
		}
	};

	/**************/
	/* WillyDemo */
	/**************/
	WillyDemo(boolean visible) {

		JTable theSubGrid;
		int r, c;

		/* ==================================== */
		/* Load the internationalized string */
		/* resources used by the application. */
		/* ==================================== */

		try {
			willyResources = ResourceBundle.getBundle("net.sf.clipsrules.jni.examples.willy.resources.WillyResources",
					Locale.getDefault());
		} catch (MissingResourceException mre) {
			mre.printStackTrace();
			return;
		}

		/* =================================== */
		/* Create the main JFrame container. */
		/* =================================== */

		jfrm = new JFrame("Willy in the Desert");
		jfrm.addKeyListener(this);
		jfrm.getContentPane().setLayout(new BorderLayout());

		/* ============================================================= */
		/* Terminate the program when the user closes the application. */
		/* ============================================================= */

		jfrm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		/* ======================================================= */
		/* Create the JPanel which will contain the willy grid. */
		/* ======================================================= */

		mainGrid = new JPanel();

		GridLayout theLayout = new GridLayout(3, 3);
		theLayout.setHgap(-1);
		theLayout.setVgap(-1);

		mainGrid.setLayout(theLayout);
		mainGrid.setOpaque(true);

		/* ======================================== */
		/* Create each of the nine 3x3 grids that */
		/* will go inside the main willy grid. */
		/* ======================================== */

		for (r = 0; r < 3; r++) {
			for (c = 0; c < 3; c++) {

				DefaultTableModel model = new DefaultTableModel(3, 3) {
					/**
					 * 
					 */
					private static final long serialVersionUID = -66438114883396365L;

					@Override
					public Class<?> getColumnClass(int column) {
						return ImageIcon.class;
					}
				};

				theSubGrid = new JTable(model) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1347789908805054422L;

					public boolean isCellEditable(int rowIndex, int vColIndex) {
						return false;
					}
				};

				theSubGrid.setFocusable(false);

				theSubGrid.setRowSelectionAllowed(false);
				theSubGrid.setShowGrid(true);
				theSubGrid.setRowHeight(50);
				theSubGrid.setGridColor(Color.black);
				theSubGrid.setBorder(BorderFactory.createLineBorder(Color.black, 1));

				// theSubGrid.addFocusListener(this);
				// theSubGrid.addKeyListener(this);

				TableColumn column = null;
				for (int i = 0; i < 3; i++) {
					column = theSubGrid.getColumnModel().getColumn(i);
					column.setMaxWidth(50);
				}

				mainGrid.add(theSubGrid);
			}
		}

		/* ======================================== */
		/* Set up the panel containing the Clear, */
		/* Reset, Solve, and Techniques buttons. */
		/* ======================================== */
		ClassLoader cl = this.getClass().getClassLoader();
		blankCell = new ImageIcon(cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/blank50.png"));

		buttonGrid = new JPanel();

		theLayout = new GridLayout(17, 1);

		buttonGrid.setLayout(theLayout);
		buttonGrid.setOpaque(true);

		mapName = new JLabel();
		mapName.setVisible(true);
		buttonGrid.add(mapName);

		resetButton = new JButton(willyResources.getString("Reset"));
		resetButton.setActionCommand("Reset");
		buttonGrid.add(resetButton);
		resetButton.addActionListener(this);
		resetButton.setToolTipText(willyResources.getString("ResetTip"));
		resetButton.setMnemonic('t');
		resetButton.setFocusable(false);

		stepButton = new JButton(willyResources.getString("Step"));
		stepButton.setActionCommand("Step");
		buttonGrid.add(stepButton);
		stepButton.addActionListener(this);
		stepButton.setToolTipText(willyResources.getString("SolveTip"));
		stepButton.setMnemonic('s');
		stepButton.setFocusable(false);

		nextMoveButton = new JButton("Next move");
		nextMoveButton.setActionCommand("nextMoveWilly");
		nextMoveButton.addActionListener(this);
		nextMoveButton.setEnabled(false);
		buttonGrid.add(nextMoveButton);
		nextMoveButton.setMnemonic('m');
		nextMoveButton.setFocusable(false);

		runButton = new JButton(willyResources.getString("Run"));
		runButton.setActionCommand("Run");
		buttonGrid.add(runButton);
		runButton.addActionListener(this);
		runButton.setMnemonic('r');
		runButton.setFocusable(false);

		runFastButton = new JButton("Run fast");
		runFastButton.setActionCommand("Run fast");
		buttonGrid.add(runFastButton);
		runFastButton.addActionListener(this);
		runFastButton.setMnemonic('f');
		runFastButton.setFocusable(false);

		nextMapButton = new JButton("Next map");
		nextMapButton.setActionCommand("NextMap");
		nextMapButton.addActionListener(this);
		nextMapButton.setMnemonic('n');
		buttonGrid.add(nextMapButton);
		nextMapButton.setFocusable(false);

		previousMapButton = new JButton("Prev. map");
		previousMapButton.setActionCommand("PrevMap");
		previousMapButton.addActionListener(this);
		previousMapButton.setMnemonic('p');
		buttonGrid.add(previousMapButton);
		previousMapButton.setFocusable(false);

		offlineButton = new JButton("All maps");
		offlineButton.setActionCommand("RunOffline");
		offlineButton.addActionListener(this);
		offlineButton.setMnemonic('a');
		buttonGrid.add(offlineButton);
		offlineButton.setFocusable(false);

		JButton creditsButton = new JButton("Credits");
		creditsButton.setActionCommand("Credits");
		creditsButton.setEnabled(true);
		buttonGrid.add(creditsButton);
		creditsButton.addActionListener(this);
		creditsButton.setFocusable(false);

		numMovesLabel = new JLabel("NumMoves: " + 0);
		buttonGrid.add(numMovesLabel);
		numKnownCellsLabel = new JLabel("Cells: " + 1);
		buttonGrid.add(numKnownCellsLabel);
		scoreLabel = new JLabel("Score: 0");
		buttonGrid.add(scoreLabel);
		resultLabel = new JLabel("");
		buttonGrid.add(resultLabel);
		resultLabel2 = new JLabel("");
		resultLabel3 = new JLabel("");
		buttonGrid.add(resultLabel2);
		buttonGrid.add(resultLabel3);

		facts = new JTextArea();
		facts.setRows(26);
		facts.setColumns(35);
		facts.setVisible(true);
		facts.setFont(new Font("monospaced", Font.PLAIN, 12));
		facts.setEditable(false);
		JScrollPane scrollPaneFacts = new JScrollPane(facts, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		facts.setFocusable(false);

		searchFField = new JTextField();
		searchFField.setVisible(true);
		searchFField.setFont(new Font("monospaced", Font.PLAIN, 12));
		searchFField.addKeyListener(this);
		JPanel factsPane = new JPanel();
		BorderLayout factsLayout = new BorderLayout();
		factsPane.setLayout(factsLayout);
		factsPane.add(searchFField, BorderLayout.NORTH);
		factsPane.add(scrollPaneFacts, BorderLayout.CENTER);
		searchFField.requestFocus();

		JPanel agendaGrid = new JPanel();
		JPanel subAgendaGrid = new JPanel();
		BorderLayout theSubLayout = new BorderLayout();
		BorderLayout theLayout2 = new BorderLayout();
		agendaGrid.setLayout(theLayout2);
		agendaGrid.setOpaque(true);
		subAgendaGrid.setLayout(theSubLayout);
		agenda = new JTextArea();
		agenda.setRows(18);
		agenda.setColumns(34);
		agenda.setVisible(true);
		agenda.setFont(new Font("monospaced", Font.PLAIN, 12));
		agenda.setEditable(false);
		scrollPaneAgenda = new JScrollPane(agenda, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		subAgendaGrid.add(scrollPaneAgenda, BorderLayout.CENTER);
		multipleActivations = new JCheckBox("Show multiple/first activations");
		multipleActivations.setSelected(showMultipleActivations);
		multipleActivations.setActionCommand("MultipleFirstCheckBox");
		multipleActivations.addActionListener(this);
		agenda.setFocusable(false);
		multipleActivations.setFocusable(false);

		focus = new JTextArea();
		focus.setRows(7);
		focus.setColumns(34);
		focus.setVisible(true);
		focus.setFont(new Font("monospaced", Font.PLAIN, 12));
		focus.setEditable(false);
		focus.setFocusable(false);
		scrollPaneFocus = new JScrollPane(focus, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		subAgendaGrid.add(scrollPaneFocus, BorderLayout.SOUTH);

		agendaGrid.add(subAgendaGrid, BorderLayout.CENTER);
		agendaGrid.add(multipleActivations, BorderLayout.SOUTH);

		/* ============================================= */
		/* Add the grid and button panels to the pane. */
		/* ============================================= */

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new FlowLayout());
		jfrm.setMinimumSize(new Dimension(1150, 480));
		mainPanel.add(mainGrid);
		mainPanel.add(buttonGrid);
		mainPanel.add(factsPane);
		mainPanel.add(agendaGrid);
		jfrm.getContentPane().add(mainPanel, BorderLayout.CENTER);

		/* ========================== */
		/* Load the willy program. */
		/* ========================== */

		maps = readFolderFiles(routeMaps);

		if (maps.size() <= 0) {
			System.out.println("There are no maps in directory maps");
			System.exit(0);
		}

		indexCurrentMap = 0;
		clips = new Environment();
		clearAndLoadClips();

		// mjmarin adds the icon
		String imagePath = "net/sf/clipsrules/jni/examples/willy/resources/icons/oasisBlue50.png";
		InputStream imgStream = cl.getResourceAsStream(imagePath);
		BufferedImage iconWillyApp;
		try {
			iconWillyApp = ImageIO.read(imgStream);
		} catch (IOException mre) {
			mre.printStackTrace();
			return;
		}
		jfrm.setIconImage(iconWillyApp);

		/**********************/
		/* Display the frame. */
		/**********************/

		jfrm.pack();
		jfrm.setVisible(visible);

		runThread = new MyRunnableClass();
		fastRunThread = new MyFastRunnableClass();
		offlineRunThread = new MyAllMapsOfflineRunnableClass();
		reset();
	}

	protected void setEnabledResetingButtons(boolean state) {
		resetButton.setEnabled(state);
		nextMapButton.setEnabled(state);
		previousMapButton.setEnabled(state);
	}

	protected void setEnabledRunningButtons(boolean state) {
		nextMoveButton.setEnabled(state);
		runButton.setEnabled(state);
		runFastButton.setEnabled(state);
		offlineButton.setEnabled(state);
		stepButton.setEnabled(state);
		multipleActivations.setEnabled(state);
		searchFField.setEnabled(state);
	}

	protected void setVisible(boolean visible) {
		jfrm.setVisible(visible);
	}

	protected void clearAndLoadClips() {
		clips.clear();
		clips.loadFromResource("/net/sf/clipsrules/jni/examples/willy/resources/context.bin");
		clips.eval("(set-current-module MAIN)");

		if (!interactiveMode) {
			if (clips.loadFromFile(routeWilly) != 1) {
				System.err.println("There were some errors while loading your file");
				System.exit(1);
			}
		} else {
			clips.loadFromResource("/net/sf/clipsrules/jni/examples/willy/resources/contextt.bin");
		}
	}

	protected double getMean(Vector<Integer> data) {

		double sum = 0.;

		for (int i = 0; i < data.size(); i++) {
			sum += data.get(i).intValue();
		}

		return (sum / data.size());
	}

	protected double getMeanDouble(Vector<Double> data) {

		double sum = 0.;

		for (int i = 0; i < data.size(); i++) {
			sum += data.get(i).doubleValue();
		}

		return (sum / data.size());
	}

	protected void runOffline() {
		runOffline(1);
	}

	protected void runOffline(int numTries) {

		DecimalFormat numberFormat = new DecimalFormat("0");
		DecimalFormat numberFormat2 = new DecimalFormat("0.0");
		indexCurrentMap = 0;

		int numDied = 0;
		int numSolved = 0;
		int numUnsolved = 0;
		setEnabledResetingButtons(false);
		setEnabledRunningButtons(false);
		offlineButton.setEnabled(true);
		int numMapas = maps.size();

		for (int i = 0; i < numMapas; i++) {

			Vector<Integer> localNumMoves = new Vector<>();
			Vector<Integer> localNumKnownCells = new Vector<>();
			Vector<Integer> localScore = new Vector<>();
			numDied = 0;
			numSolved = 0;
			numUnsolved = 0;
			int numIterations = 0;
			clearAndLoadClips();

			for (int j = 0; j < numTries; j++) {
				numIterations++;
				indexCurrentMap = i;
				silentReset();

				isExecuting = true;

				while (isExecuting && areThereActivations()) {
					silentStepWilly();
				}

				if (jfrm.isVisible()) {
					scoreLabel.setText("Score: " + numberFormat2.format(getMean(globalScore)));
					numMovesLabel.setText("Moves: " + numberFormat2.format(getMean(globalNumMoves)));
					numKnownCellsLabel.setText("Cells: " + numberFormat2.format(getMean(globalNumKnownCells)));
					resultLabel.setText("<html><font color='blue'>W: "
							+ numberFormat.format(getMeanDouble(globalNumWins)) + "%</font><html>");
					resultLabel2.setText("<html><font color='red'>D: "
							+ numberFormat.format(getMeanDouble(globalNumDeaths)) + "%</font></html>");
					resultLabel3
							.setText("<html>U: " + numberFormat.format(getMeanDouble(globalNumUnSolved)) + "%</html>");
					mapName.setText("It:" + offlineIterations + " Maps:" + (i + 1) + "/" + numMapas);
					scoreLabel.repaint();
					numMovesLabel.repaint();
					numKnownCellsLabel.repaint();
					resultLabel.repaint();
					resultLabel2.repaint();
					resultLabel3.repaint();
					mapName.repaint();
				}

				boolean solved = hasWon();
				boolean hasDied = isDied();
				int numMoves = getNumMoves();
				int knownCells = getNumKnownCells();
				int score = getScore();

				globalNumMoves.add(numMoves);
				localNumMoves.add(numMoves);
				globalNumKnownCells.add(knownCells);
				localNumKnownCells.add(knownCells);
				globalScore.add(score);
				localScore.add(score);

				if (solved)
					numSolved++;
				else if (hasDied) {
					numDied++;
					numUnsolved++;
				} else
					numUnsolved++;
			}

			double ratioDeaths = ((double) numDied) / numIterations * 100;
			double ratioWins = ((double) numSolved) / numIterations * 100;
			double ratioUnsolved = ((double) numUnsolved) / numIterations * 100;

			globalNumDeaths.add(ratioDeaths);
			globalNumWins.add(ratioWins);
			globalNumUnSolved.add(ratioUnsolved);

			System.out.print("" + i + "(" + maps.get(i).getName() + "; " + numIterations + "it): ");
			System.out.print("Solved: " + numberFormat2.format(ratioWins) + "% / ");
			System.out.print("Died " + numberFormat2.format(ratioDeaths) + "% / ");
			System.out.print("Unsolved " + numberFormat2.format(ratioUnsolved) + "% / ");

			System.out.println("KnownCells: " + numberFormat.format(getMean(localNumKnownCells)) + " / Moves: "
					+ numberFormat.format(getMean(localNumMoves)) + " / Score: "
					+ numberFormat.format(getMean(localScore)));
		}

		System.out.println("----------------------------------------------");
		System.out.print("Solved: " + numberFormat.format(getMeanDouble(globalNumWins)) + "% / ");
		System.out.print("Died " + numberFormat.format(getMeanDouble(globalNumDeaths)) + "% / ");
		System.out.print("Unsolved " + numberFormat.format(getMeanDouble(globalNumUnSolved)) + "% / ");
		System.out.println("KnownCells: " + numberFormat2.format(getMean(globalNumKnownCells)) + " / Moves: "
				+ numberFormat2.format(getMean(globalNumMoves)) + " / Score: "
				+ numberFormat2.format(getMean(globalScore)));

		if (jfrm.isVisible()) {
			resultLabel.setText("<html><font color='blue'>W: " + numberFormat.format(getMeanDouble(globalNumWins))
					+ "%</font><html>");
			resultLabel2.setText("<html><font color='red'>D: " + numberFormat.format(getMeanDouble(globalNumDeaths))
					+ "%</font></html>");
			resultLabel3.setText("<html>U: " + numberFormat.format(getMeanDouble(globalNumUnSolved)) + "%</html>");
			numKnownCellsLabel.setText("Cells: " + numberFormat2.format(getMean(globalNumKnownCells)));
			numMovesLabel.setText("Moves: " + numberFormat2.format(getMean(globalNumMoves)));
			scoreLabel.setText("Score: " + numberFormat2.format(getMean(globalScore)));
			mapName.setText("It:" + offlineIterations + " Maps:" + numMapas + "/" + numMapas);
			scoreLabel.repaint();
			numMovesLabel.repaint();
			numKnownCellsLabel.repaint();
			resultLabel.repaint();
			resultLabel2.repaint();
			resultLabel3.repaint();
			mapName.repaint();
		}
	}

	/********/
	/* main */
	/********/
	public static void main(java.lang.String args[]) {
		/* =================================================== */
		/* Create the frame on the event dispatching thread. */
		/* =================================================== */

		System.out.println("Possible invocations:");
		System.out.println("java -Djava.library.path=. -jar <this.jar>");
		System.out.println("java -Djava.library.path=. -jar <this.jar> -willy <willyfile>");
		System.out.println("java -Djava.library.path=. -jar <this.jar> -maps <mapsDirectory>");
		System.out.println("java -Djava.library.path=. -jar <this.jar> -willy <willyfile> -maps <mapsDirectory>");
		System.out.println("java -Djava.library.path=. -jar <this.jar> -interactive");

		int iargs = 0;

		while (iargs < args.length) {

			if (args[iargs].equals("-willy")) {
				iargs++;
				String auxWillyFile = args[iargs];

				File willyFile = new File(auxWillyFile);

				if (willyFile.exists()) {
					routeWilly = args[iargs];
				} else {
					System.err.println("" + auxWillyFile + " can not be used as willy's code");
					System.exit(1);
				}
			}

			if (args[iargs].equals("-maps")) {
				iargs++;
				String auxRouteMaps = args[iargs];

				File dirMaps = new File(auxRouteMaps);

				if (dirMaps.isDirectory()) {
					routeMaps = auxRouteMaps;
				} else {
					System.err.println("" + auxRouteMaps + " can not be used as a maps directory");
					System.exit(1);
				}
			}

			if (args[iargs].equals("-interactive")) {
				interactiveMode = true;
			}

			iargs++;
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new WillyDemo(true);
			}
		});
	}

	/* ######################## */
	/* ActionListener Methods */
	/* ######################## */

	/*******************/
	/* actionPerformed */
	/*******************/
	public void actionPerformed(ActionEvent ae) {
		try {
			onActionPerformed(ae);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateFacts() {

		boolean justThisModule = false;

		String searchString = searchFField.getText();

		TreeMap<Long, String> listFacts = new TreeMap<Long, String>();
		String currentModule = clips.eval("(get-focus)").toString();

		PrimitiveValue pv1 = clips.eval("(get-defmodule-list)");

		if (pv1 instanceof MultifieldValue) {
			MultifieldValue modules = (MultifieldValue) pv1;

			for (int iModules = 0; iModules < modules.size(); iModules++) {
				// String targetModule = "InternalMemory";
				// String targetModule = "MAIN";
				// String targetModule = clips.eval("(get-focus)").toString();
				String targetModule = modules.get(iModules).toString();

				if (isAProtectedModule(targetModule))
					continue;

				MultifieldValue mv = (MultifieldValue) clips.eval("(get-deftemplate-list " + targetModule + ")");
				String cadena = "";

				boolean neededToFocus = false;

				if (!justThisModule && ((String) (clips.eval("(get-focus)").toString())).compareTo(targetModule) != 0) {
					clips.eval("(focus " + targetModule + ")");
					neededToFocus = true;
				}

				for (int i = 0; i < mv.size(); i++) {

					PrimitiveValue pv = clips.eval("(find-all-facts ((?f " + mv.get(i).toString() + ")) TRUE)");

					if (pv instanceof MultifieldValue) {
						MultifieldValue mv2 = (MultifieldValue) pv;

						for (int j = 0; j < mv2.size(); j++) {
							cadena = "";
							FactAddressValue fv = (FactAddressValue) mv2.get(j);
							cadena = cadena + "f-" + fv.getFactIndex() + " ";

							for (int k = String.valueOf(fv.getFactIndex()).length() + 1; k < 6; k++)
								cadena = cadena + " ";

							cadena = cadena + "(" + mv.get(i).toString();
							MultifieldValue mv3 = (MultifieldValue) clips
									.eval("(fact-slot-names " + fv.getFactIndex() + ")");

							for (int k = 0; k < mv3.size(); k++) {

								if (mv3.get(k).toString().compareTo("implied") != 0)
									cadena = cadena + " (" + mv3.get(k);

								PrimitiveValue pv4 = (PrimitiveValue) clips
										.eval("(fact-slot-value " + fv.getFactIndex() + " " + mv3.get(k) + ")");

								if (pv4 instanceof MultifieldValue) {
									MultifieldValue mv4 = (MultifieldValue) pv4;

									for (int l = 0; l < mv4.size(); l++) {
										cadena = cadena + " " + mv4.get(l);
									}
								} else {
									cadena = cadena + " " + pv4.getValue();
								}

								if (mv3.get(k).toString().compareTo("implied") != 0)
									cadena = cadena + ")";
							}

							cadena = cadena + ")";

							if (cadena.contains(searchString))
								listFacts.put(fv.getFactIndex(), new String(cadena));
						}
					} else {
						clips.eval("(focus " + currentModule + ")");
						// System.out.println("(focus " + currentModule + ")");
					}
				}

				if (neededToFocus) {
					clips.eval("(pop-focus)");
				}
			}
		}

		// System.out.println("" + hasWon() + ":" + isDied() + ":" +
		// getNumKnownCells());

		facts.setText("");
		Set<Long> keys = listFacts.keySet();
		Iterator<Long> itr = keys.iterator();
		while (itr.hasNext()) {
			// Getting Key
			Long key = itr.next();
			facts.append(listFacts.get(key) + "\n");
		}
	}

	protected boolean areThereActivations() {

		String activation = clips.getIthActivation(0);
		return (activation != null && !activation.trim().equals("") && !activation.trim().equals("null"));
	}

	protected boolean isAProtectedModule(String mName) {
		return (mName.equals("InternalMemory") || mName.equals("InternalFunctions") || mName.equals("Initialiser"));
	}

	protected void updateAgenda() {

		agenda.setText("");
		focus.setText("");

		if (hasWon() || isDied()) {
			agenda.append("");
			focus.append("");
		} else {

			if (!isAProtectedModule(clips.eval("(get-focus)").toString())) {
				int i = 0;
				String activation = clips.getIthActivation(i);
				HashMap<String, String> entries = new HashMap<>();

				while (activation != null && activation.compareTo("null") != 0) {
					activation = activation.substring(1).trim();
					String head = activation.substring(0, activation.indexOf(":"));

					if (showMultipleActivations) {
						agenda.append(i + ": " + activation + "\n");
					} else if (entries.containsKey(head) == false) {
						entries.put(head, activation);
						agenda.append(activation + "\n");
					}
					i++;

					activation = clips.getIthActivation(i);
				}

				MultifieldValue mv = (MultifieldValue) clips.eval("(get-focus-stack)");

				for (int iF = 0; iF < mv.size(); iF++) {
					focus.append(mv.get(iF).toString() + "\n");
				}

			} else {
				agenda.append("");
				focus.append("");
			}
		}

		focus.setCaretPosition(0);
		agenda.setCaretPosition(0);
	}

	protected void nextMoveWilly() {

		if (isDied() || hasWon()) {
			isExecuting = false;
			return;
		}

		String currentModule = clips.eval("(get-focus)").toString();
		int iterations = 0;

		if (isAProtectedModule(currentModule)) {

			while (iterations < 10000 && isAProtectedModule(currentModule) && areThereActivations()) {
				silentStepWilly();
				iterations++;
				currentModule = clips.eval("(get-focus)").toString();

				if (isDied() || hasWon()) {
					isExecuting = false;
					break;
				}
			}

		} else {

			while (iterations < 10000 && !isAProtectedModule(currentModule) && areThereActivations()) {
				silentStepWilly();
				iterations++;
				currentModule = clips.eval("(get-focus)").toString();

				if (isDied() || hasWon()) {
					isExecuting = false;
					break;
				}
			}
		}

		updateGUI();
	}

	protected void silentStepWilly() {

		if (isDied() || hasWon()) {
			isExecuting = false;
			return;
		}

		int iterations = 0;
		if (!isAProtectedModule(clips.eval("(get-focus)").toString())) {
			clips.run(1);
		} else {
			while (iterations < 10000 && isAProtectedModule(clips.eval("(get-focus)").toString())
					&& areThereActivations()) {
				clips.run(1);
				iterations++;

				if (isDied() || hasWon()) {
					isExecuting = false;
					break;
				}
			}
		}
	}

	protected void updateGUI() {

		resultLabel.setText("");
		resultLabel2.setText("");
		resultLabel3.setText("");

		try {
			updateGrid();
		} catch (Exception e) {
			e.printStackTrace();
		}

		updateFacts();
		updateAgenda();
		numMovesLabel.setText("Moves: " + getNumMoves());
		numKnownCellsLabel.setText("Cells: " + getNumKnownCells());
		scoreLabel.setText("Score: " + getScore());

		if (isDied()) {
			resultLabel.setText("<html><font color='red'>YOU DIED!</font></html>");
		} else if (hasWon())
			resultLabel.setText("<html><font color='blue'>YOU WON!</font></html>");
		else
			resultLabel.setText("");

		if (isDied() || hasWon() || ((String) (clips.eval("(get-focus)").toString())).compareTo("FALSE") == 0) {
			setEnabledResetingButtons(true);
			setEnabledRunningButtons(false);
			isExecuting = false;
		} else if (!isExecuting) {
			setEnabledResetingButtons(true);
			setEnabledRunningButtons(true);
		}
	}

	protected void stepWilly() {
		silentStepWilly();
		updateGUI();
	}

	protected void silentReset() {

		solved = false;
		clips.reset();
		readFile(maps.get(indexCurrentMap));

		clips.eval("(focus Initialiser)");
		silentStepWilly();
		isReseted = true;
	}

	protected void reset() {

		silentReset();
		File file = maps.get(indexCurrentMap);
		int lengthMapName = file.getName().length() - 4;
		mapName.setText(file.getName().substring(0, lengthMapName));
		updateGUI();
	}

	public void runFastWilly() {

		isExecuting = true;
		jfrm.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		executionThread = new Thread(fastRunThread);
		fastRunThread.requestStart();
		executionThread.start();
	}

	/*************/
	/* runWilly */
	/*************/
	public void runWilly() {

		isExecuting = true;
		jfrm.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		executionThread = new Thread(runThread);
		runThread.requestStart();
		executionThread.start();
	}

	/*********************/
	/* onActionPerformed */
	/*********************/
	public void onActionPerformed(ActionEvent ae) throws Exception {

		/* ========================== */
		/* Handle the Reset button. */
		/* ========================== */

		if (ae.getActionCommand().equals("Reset")) {
			reset();
		} else if (ae.getActionCommand().equals("nextMoveWilly")) {
			/* ============== */
			/* Reset CLIPS. */
			/* ============== */

			if (!isReseted) {
				clips.reset();
				clips.eval("(focus Initialiser)");
				stepWilly();
				isReseted = true;
			}

			jfrm.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			setEnabledRunningButtons(false);
			setEnabledResetingButtons(false);
			nextMoveWilly();
		} else if (ae.getActionCommand().equals("NextMap")) {
			indexCurrentMap = (indexCurrentMap + 1) % maps.size();
			clearAndLoadClips();
			reset();
		} else if (ae.getActionCommand().equals("PrevMap")) {
			indexCurrentMap = (indexCurrentMap + maps.size() - 1) % maps.size();
			clearAndLoadClips();
			reset();
		} else if (ae.getActionCommand().equals("Credits")) {
			JDialog dialog = new JDialog(jfrm, "Credits", true);
			JTextPane label = new JTextPane();
			label.setContentType("text/html");
			label.setText(
					"<html><p>This is a personal adaptation of the SudokuDemo example of the clips java native interface to the Willy in the desert problem, an idea of Prof. Manuel Jesus Marín Jiménez.</p>"
							+ "<p>It is intended to be used to help Computing students to gain knowlege and practice in programming with CLIPS.</p>"
							+ "<p><b>This work is licensed under a <a href='http://creativecommons.org/licenses/by-nc-nd/4.0/'>Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International License</a> "
							// + "<a rel='license'
							// href='http://creativecommons.org/licenses/by-nc-nd/4.0/'><img
							// alt='Creative Commons License'
							// style='border-width:0'
							// src='https://i.creativecommons.org/l/by-nc-nd/4.0/88x31.png'
							// /></a>"
							+ "(http://creativecommons.org/licenses/by-nc-nd/4.0/),"
							+ "and comes with no additional warranties of any kind." + "</b></p>"
							+ "<p>Its author is Prof. Carlos García Martínez - cgarcia@uco.es</p>"
							+ "<p>Other collaborators are:</p>" + "<ul><li>Prof. Manuel Jesus Marín Jiménez</li>"
							+ "<li>Prof. Amelia Zafra Gómez</li>" + "</ul>"
							+ "<p>The original images were obtained from the following webpages in March 2017, and were marked as \"Labeled for reuse with modification\" according to the Google Image Search Engine"
							+ "<ul>" + "<li>https://pixabay.com/es/photos/arrow/</li>"
							+ "<li>http://www.publicdomainpictures.net/view-image.php?image=42224&picture=pointing-down-finger</li>"
							+ "<li>https://pixabay.com/es/smiley-icono-gestual-gracioso-821993/</li>"
							+ "<li>https://commons.wikimedia.org/wiki/File:PEO-snake_alt.svg</li>"
							+ "<li>https://pixabay.com/es/arco-flecha-eje-perno-arma-307274/</li>"
							+ "<li>https://commons.wikimedia.org/wiki/File:Antu_earthquake.svg</li>"
							+ "<li>https://pixabay.com/es/de-coco-selva-1293036/</li>"
							+ "<li>https://commons.wikimedia.org/wiki/File:Quicksand_warning_sign_Texel_2004.jpg</li>"
							+ "<li>https://pixabay.com/es/bell-notificaci%C3%B3n-comunicaci%C3%B3n-1096279/</li>"
							+ "</ul>" + "</html>");
			label.setEditable(false);
			JScrollPane scrollCredits = new JScrollPane(label, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			dialog.setMinimumSize(new Dimension(800, 500));
			dialog.add(scrollCredits);
			dialog.setVisible(true);
		}

		/* ========================== */
		/* Handle the Step button. */
		/* ========================== */

		else if (ae.getActionCommand().equals("RunOffline")) {
			clips.reset();

			isExecuting = true;
			offlineButton.setText("Stop");
			offlineButton.setActionCommand("StopOffline");
			jfrm.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			executionThread = new Thread(offlineRunThread);
			offlineRunThread.requestStart();
			executionThread.start();
		} else if (ae.getActionCommand().equals("Run fast")) {

			/* ============== */
			/* Reset CLIPS. */
			/* ============== */

			if (!isReseted) {
				clips.reset();
				clips.eval("(focus Initialiser)");
				stepWilly();
				isReseted = true;
			}

			/* =================== */
			/* Solve the puzzle. */
			/* =================== */
			isExecuting = true;
			runFastButton.setText("Stop");
			runFastButton.setActionCommand("StopFast");
			setEnabledRunningButtons(false);
			setEnabledResetingButtons(false);
			runFastButton.setEnabled(true);
			runFastWilly();
		}

		else if (ae.getActionCommand().equals("Step")) {
			/* ============== */
			/* Reset CLIPS. */
			/* ============== */

			if (!isReseted) {
				clips.reset();
				clips.eval("(focus Initialiser)");
				stepWilly();
				isReseted = true;
			}

			jfrm.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			stepWilly();
		}

		else if (ae.getActionCommand().equals("Run")) {
			/* ============== */
			/* Reset CLIPS. */
			/* ============== */

			if (!isReseted) {
				System.err.println("isReseted: " + isReseted);
				clips.reset();
				clips.eval("(focus Initialiser)");
				stepWilly();
				isReseted = true;
				System.err.println("isReseted: " + isReseted);
			}

			/* =================== */
			/* Solve the puzzle. */
			/* =================== */
			isExecuting = true;
			runButton.setText("Stop");
			runButton.setActionCommand("Stop");
			setEnabledResetingButtons(false);
			setEnabledRunningButtons(false);
			runButton.setEnabled(true);
			runWilly();
		}

		else if (ae.getActionCommand().equals("Stop")) {

			runThread.requestStop();
		}

		else if (ae.getActionCommand().equals("StopFast")) {

			fastRunThread.requestStop();
		}

		else if (ae.getActionCommand().equals("StopOffline")) {
			offlineRunThread.requestStop();
		}

		else if (ae.getActionCommand().equals("MultipleFirstCheckBox")) {
			showMultipleActivations = !showMultipleActivations;
			updateAgenda();
		}

		/* =============================== */
		/* Handle the Techniques button. */
		/* =============================== */

		// else if (ae.getActionCommand().equals("Techniques")) {
		// String evalStr;
		// String messageStr = "<html><p style=\"font-size:95%\">";
		//
		// evalStr = "(find-all-facts ((?f technique)) TRUE)";
		//
		// MultifieldValue mv = (MultifieldValue) clips.eval(evalStr);
		// int tNum = mv.size();
		//
		// for (int i = 1; i <= tNum; i++) {
		// evalStr = "(find-fact ((?f technique-employed)) " + "(eq ?f:priority
		// " + i + "))";
		//
		// mv = (MultifieldValue) clips.eval(evalStr);
		// if (mv.size() == 0)
		// continue;
		//
		// FactAddressValue fv = (FactAddressValue) mv.get(0);
		//
		// messageStr = messageStr + ((NumberValue)
		// fv.getFactSlot("priority")).intValue() + ". "
		// + ((LexemeValue) fv.getFactSlot("reason")).lexemeValue() + "<br>";
		// }
		//
		// JOptionPane.showMessageDialog(jfrm, messageStr,
		// willyResources.getString("SolutionTechniques"),
		// JOptionPane.PLAIN_MESSAGE);
		// }
	}

	protected int getNumKnownCells() {
		String evalNumKnownCells = "(find-all-facts ((?f cell)) TRUE)";
		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}

		PrimitiveValue pv = clips.eval(evalNumKnownCells);
		int numKnownCells = 0;

		if (pv instanceof MultifieldValue) {
			try {
				numKnownCells = ((MultifieldValue) pv).size();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			numKnownCells = 0;
		}

		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		return numKnownCells;
	}

	protected int getNumMoves() {
		String evalMoves = "(find-all-facts ((?f numMoves)) TRUE)";
		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}

		PrimitiveValue pv = clips.eval(evalMoves);
		int numMoves = 0;

		if (pv instanceof MultifieldValue && ((MultifieldValue) pv).size() > 0) {
			try {
				numMoves = Integer.parseInt(
						((FactAddressValue) (((MultifieldValue) pv).get(0))).getFactSlot("numMoves").toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				numMoves = Integer.parseInt(pv.getValue().toString());
			} catch (java.lang.NumberFormatException e) {
				numMoves = 0;
			}
		}

		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		return numMoves;
	}

	protected int getScore() {
		int score = 0;

		if (hasWon()) {
			score = 1000 - getNumMoves() + getNumKnownCells();

			if (hasKilledTheSnake())
				score += 1000;

		} else {
			score = 10 * getNumKnownCells() - getNumMoves();
		}

		if (isDied())
			score -= 1000;

		return score;
	}

	protected boolean isDied() {
		String evalDied = "(find-all-facts ((?f died)) TRUE)";
		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}

		PrimitiveValue pv = clips.eval(evalDied);
		boolean hasDied = false;

		if (pv instanceof MultifieldValue && ((MultifieldValue) pv).size() > 0) {
			hasDied = true;
		} else {
			hasDied = false;
		}

		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		return hasDied;
	}

	protected boolean hasKilledTheSnake() {
		String evalKilledSnake = "(find-all-facts ((?f hasKilledTheSnake)) TRUE)";
		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}

		PrimitiveValue pv = clips.eval(evalKilledSnake);
		boolean hasKilledSnake = false;

		if (pv instanceof MultifieldValue && ((MultifieldValue) pv).size() > 0) {
			hasKilledSnake = true;
		} else {
			hasKilledSnake = false;
		}

		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		return hasKilledSnake;
	}

	protected boolean hasWon() {
		String evalWon = "(find-all-facts ((?f won)) TRUE)";
		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}

		PrimitiveValue pv = clips.eval(evalWon);
		boolean hasWon = false;

		if (pv instanceof MultifieldValue && ((MultifieldValue) pv).size() > 0) {
			hasWon = true;
		} else {
			hasWon = false;
		}

		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		return hasWon;
	}

	protected boolean hasArrow() {
		String evalArrow = "(find-all-facts ((?f internalHasArrow)) TRUE)";
		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}

		PrimitiveValue pv = clips.eval(evalArrow);
		boolean hasArrow = false;

		if (pv instanceof MultifieldValue && ((MultifieldValue) pv).size() > 0) {
			hasArrow = true;
		} else {
			hasArrow = false;
		}

		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		return hasArrow;
	}

	private void manageUnknownOasisCell(JTable theTable, int r, int c, int rowGroup, int colGroup) throws Exception {
		String evalOasis = "(find-all-facts ((?f at))" + "(and (eq ?f:item O) " + "(eq ?f:row "
				+ (r + (rowGroup * 3) + 1) + ") " + "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")))";

		String evalUnknown = "(find-all-facts ((?f cell))" + "(and (eq ?f:row " + (r + (rowGroup * 3) + 1) + ") "
				+ "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")))";
		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		MultifieldValue mvOasis = (MultifieldValue) clips.eval(evalOasis);
		MultifieldValue mvUnknown = (MultifieldValue) clips.eval(evalUnknown);
		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		if (mvOasis.size() == 1 && mvUnknown.size() <= 0) {
			ClassLoader cl = this.getClass().getClassLoader();
			ImageIcon icon = new ImageIcon(
					cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/oasis_unknown50.png"));
			addImage(icon, theTable, r, c);
			// theTable.setValueAt(icon, r, c);
		}
	}

	private void manageUnknownSnakeCell(JTable theTable, int r, int c, int rowGroup, int colGroup) throws Exception {
		String evalSnake = "(find-all-facts ((?f at))" + "(and (eq ?f:item S) " + "(eq ?f:row "
				+ (r + (rowGroup * 3) + 1) + ") " + "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")))";
		String evalUnknown = "(find-all-facts ((?f cell))" + "(and (eq ?f:row " + (r + (rowGroup * 3) + 1) + ") "
				+ "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")))";
		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		MultifieldValue mvSnake = (MultifieldValue) clips.eval(evalSnake);
		MultifieldValue mvUnknown = (MultifieldValue) clips.eval(evalUnknown);
		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		if (mvSnake.size() == 1 && mvUnknown.size() <= 0) {
			ClassLoader cl = this.getClass().getClassLoader();
			ImageIcon icon = new ImageIcon(
					cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/snake_unknown50.png"));
			addImage(icon, theTable, r, c);
			// theTable.setValueAt(icon, r, c);
		}
	}

	private void manageSnakeRedCell(JTable theTable, int r, int c, int rowGroup, int colGroup) throws Exception {
		String evalSnake = "(find-all-facts ((?f at) (?f2 at))" + "(and (eq ?f:item S) " + "(eq ?f:row "
				+ (r + (rowGroup * 3) + 1) + ") " + "(eq ?f:column " + (c + (colGroup * 3) + 1) + ") "
				+ "(eq ?f2:item W) " + "(eq ?f2:row " + (r + (rowGroup * 3) + 1) + ") " + "(eq ?f2:column "
				+ (c + (colGroup * 3) + 1) + ")" + "))";

		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		MultifieldValue mvSnake = (MultifieldValue) clips.eval(evalSnake);
		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		if (mvSnake.size() > 0) {
			ClassLoader cl = this.getClass().getClassLoader();
			ImageIcon icon = new ImageIcon(
					cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/snakeRed50.png"));
			addImage(icon, theTable, r, c);
		}
	}

	private void manageQuickSandRedCell(JTable theTable, int r, int c, int rowGroup, int colGroup) throws Exception {
		String quickSandSnake = "(find-all-facts ((?f at) (?f2 at))" + "(and (eq ?f:item A) " + "(eq ?f:row "
				+ (r + (rowGroup * 3) + 1) + ") " + "(eq ?f:column " + (c + (colGroup * 3) + 1) + ") "
				+ "(eq ?f2:item W) " + "(eq ?f2:row " + (r + (rowGroup * 3) + 1) + ") " + "(eq ?f2:column "
				+ (c + (colGroup * 3) + 1) + ")" + "))";

		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		MultifieldValue mvQuickSand = (MultifieldValue) clips.eval(quickSandSnake);
		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		if (mvQuickSand.size() > 0) {
			ClassLoader cl = this.getClass().getClassLoader();
			ImageIcon icon = new ImageIcon(
					cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/quickSandRed50.png"));
			addImage(icon, theTable, r, c);
		}
	}

	private void manageOasisBlueCell(JTable theTable, int r, int c, int rowGroup, int colGroup) throws Exception {
		String evalOasis = "(find-all-facts ((?f at) (?f2 at))" + "(and (eq ?f:item O) " + "(eq ?f:row "
				+ (r + (rowGroup * 3) + 1) + ") " + "(eq ?f:column " + (c + (colGroup * 3) + 1) + ") "
				+ "(eq ?f2:item W) " + "(eq ?f2:row " + (r + (rowGroup * 3) + 1) + ") " + "(eq ?f2:column "
				+ (c + (colGroup * 3) + 1) + ")" + "))";

		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		MultifieldValue mvOasis = (MultifieldValue) clips.eval(evalOasis);
		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		if (mvOasis.size() > 0) {
			ClassLoader cl = this.getClass().getClassLoader();
			ImageIcon icon = new ImageIcon(
					cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/oasisBlue50.png"));
			addImage(icon, theTable, r, c);
		}
	}

	private void manageUnknownQuickSandCell(JTable theTable, int r, int c, int rowGroup, int colGroup)
			throws Exception {
		String evalQuickSand = "(find-all-facts ((?f at))" + "(and (eq ?f:item A) " + "(eq ?f:row "
				+ (r + (rowGroup * 3) + 1) + ") " + "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")))";

		String evalUnknown = "(find-all-facts ((?f cell))" + "(and (eq ?f:row " + (r + (rowGroup * 3) + 1) + ") "
				+ "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")))";

		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		MultifieldValue mvQuickSand = (MultifieldValue) clips.eval(evalQuickSand);
		MultifieldValue mvUnknown = (MultifieldValue) clips.eval(evalUnknown);
		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		if (mvQuickSand.size() == 1 && mvUnknown.size() <= 0) {
			ClassLoader cl = this.getClass().getClassLoader();
			ImageIcon icon = new ImageIcon(
					cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/quickSand_unknown50.png"));
			addImage(icon, theTable, r, c);
		}
	}

	private void manageEarthQuakeCell(JTable theTable, int r, int c, int rowGroup, int colGroup) throws Exception {
		String evalStr = "(find-all-facts ((?f cell))" + "(and (eq ?f:row " + (r + (rowGroup * 3) + 1) + ") "
				+ "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")" + "(neq FALSE (member$ A ?f:contents))))";

		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		MultifieldValue mv = (MultifieldValue) clips.eval(evalStr);
		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		if (mv.size() == 1) {

			ClassLoader cl = this.getClass().getClassLoader();
			ImageIcon iconTop = new ImageIcon(
					cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/earthquake50.png"));
			addImage(iconTop, theTable, r, c);
		}
	}

	private void addImage(ImageIcon iconTop, JTable theTable, int r, int c) {
		ImageIcon iconBase = (ImageIcon) theTable.getValueAt(r, c);
		if (iconBase != null) {
			Image base = iconBase.getImage();
			Image topImage = iconTop.getImage();
			BufferedImage combined = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = combined.createGraphics();
			g.drawImage(base, 0, 0, null);
			g.drawImage(topImage, 0, 0, null);
			g.dispose();
			ImageIcon newImage = new ImageIcon(combined);
			theTable.setValueAt(newImage, r, c);
		} else {
			theTable.setValueAt(iconTop, r, c);
		}
	}

	private void manageRingBellCell(JTable theTable, int r, int c, int rowGroup, int colGroup) throws Exception {
		String evalStr = "(find-all-facts ((?f cell))" + "(and " + "(eq ?f:row " + (r + (rowGroup * 3) + 1) + ") "
				+ "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")" + "(neq FALSE (member$ S ?f:contents))))";

		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		MultifieldValue mv = (MultifieldValue) clips.eval(evalStr);
		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		if (mv.size() == 1) {
			ClassLoader cl = this.getClass().getClassLoader();
			ImageIcon iconTop = new ImageIcon(
					cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/ringBell50.png"));
			addImage(iconTop, theTable, r, c);
		}
	}

	private void manageFreeCell(JTable theTable, int r, int c, int rowGroup, int colGroup) throws Exception {
		String evalStr = "(find-all-facts ((?f cell))" + "(and (eq ?f:row " + (r + (rowGroup * 3) + 1) + ") "
				+ "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")" + "(= (length$ ?f:contents) 0)" + "))";

		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		PrimitiveValue pv = clips.eval(evalStr);

		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}

		if (pv instanceof MultifieldValue && ((MultifieldValue) pv).size() == 1) {

			ClassLoader cl = this.getClass().getClassLoader();
			ImageIcon iconTop = new ImageIcon(
					cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/ok50.png"));
			addImage(iconTop, theTable, r, c);
		}
	}

	private PrimitiveValue isWillHeading() {

		String evalDirection = "(find-all-facts ((?f heading)) TRUE)";
		MultifieldValue mv2 = (MultifieldValue) clips.eval(evalDirection);
		PrimitiveValue pv2 = null;

		if (mv2.size() > 0) {
			FactAddressValue fv = (FactAddressValue) mv2.get(0);
			MultifieldValue mv3 = (MultifieldValue) clips.eval("(fact-slot-names " + fv.getFactIndex() + ")");

			for (int k = 0; k < mv3.size(); k++) {
				// System.out.println(mv3.get(k).toString());
				if (mv3.get(k).toString().compareTo("implied") == 0)
					pv2 = (PrimitiveValue) clips.eval("(fact-slot-value " + fv.getFactIndex() + " " + mv3.get(k) + ")");
				if (pv2 instanceof MultifieldValue) {
					MultifieldValue mv4 = (MultifieldValue) pv2;

					if (mv4.size() > 0) {
						pv2 = mv4.get(0);
					} else {
						pv2 = null;
					}
				}
			}
		}

		return pv2;
	}

	private PrimitiveValue isWillyFiring() {

		String evalDirection = "(find-all-facts ((?f firing)(?f2 internalHasArrow)) TRUE)";
		MultifieldValue mv2 = (MultifieldValue) clips.eval(evalDirection);
		PrimitiveValue pv2 = null;

		if (mv2.size() > 0) {
			FactAddressValue fv = (FactAddressValue) mv2.get(0);
			MultifieldValue mv3 = (MultifieldValue) clips.eval("(fact-slot-names " + fv.getFactIndex() + ")");

			for (int k = 0; k < mv3.size(); k++) {
				if (mv3.get(k).toString().compareTo("implied") == 0)
					pv2 = (PrimitiveValue) clips.eval("(fact-slot-value " + fv.getFactIndex() + " " + mv3.get(k) + ")");
				if (pv2 instanceof MultifieldValue) {
					MultifieldValue mv4 = (MultifieldValue) pv2;

					if (mv4.size() > 0) {
						pv2 = mv4.get(0);
					} else {
						pv2 = null;
					}
				}
			}
		}

		return pv2;
	}

	private void manageWillyCell(JTable theTable, int r, int c, int rowGroup, int colGroup) throws Exception {
		String evalWilly = "(find-all-facts ((?f at))" + "(and (eq ?f:item W) " + "(eq ?f:row "
				+ (r + (rowGroup * 3) + 1) + ") " + "(eq ?f:column " + (c + (colGroup * 3) + 1) + ")))";

		boolean neededToFocus = false;

		if (((String) (clips.eval("(get-focus)").toString())).compareTo("InternalMemory") != 0) {
			clips.eval("(focus InternalMemory)");
			neededToFocus = true;
		}
		MultifieldValue mv = (MultifieldValue) clips.eval(evalWilly);

		if (mv.size() == 1) {

			PrimitiveValue pv2 = isWillHeading();
			PrimitiveValue pv3 = isWillyFiring();
			ImageIcon iconTop = null;
			ClassLoader cl = this.getClass().getClassLoader();

			if (pv2 != null) {
				if (pv2.getValue().toString().compareTo("north") == 0) {
					iconTop = new ImageIcon(
							cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/capback50.png"));
				} else if (pv2.getValue().toString().compareTo("south") == 0) {
					iconTop = new ImageIcon(
							cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/capfront50.png"));
				} else if (pv2.getValue().toString().compareTo("east") == 0) {
					iconTop = new ImageIcon(
							cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/capright50.png"));
				} else if (pv2.getValue().toString().compareTo("west") == 0) {
					iconTop = new ImageIcon(
							cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/capleft50.png"));
				}
			} else if (pv3 != null) {
				if (pv3.getValue().toString().compareTo("north") == 0) {
					iconTop = new ImageIcon(
							cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/bowNorth50.png"));
				} else if (pv3.getValue().toString().compareTo("south") == 0) {
					iconTop = new ImageIcon(
							cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/bowSouth50.png"));
				} else if (pv3.getValue().toString().compareTo("east") == 0) {
					iconTop = new ImageIcon(
							cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/bowEast50.png"));
				} else if (pv3.getValue().toString().compareTo("west") == 0) {
					iconTop = new ImageIcon(
							cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/bowWest50.png"));
				}
			} else {
				iconTop = new ImageIcon(
						cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/think50smaller.png"));
			}

			if (hasArrow()) {
				ImageIcon arrowIcon = new ImageIcon(
						cl.getResource("net/sf/clipsrules/jni/examples/willy/resources/icons/arrow50.png"));
				addImage(arrowIcon, theTable, r, c);
			}

			addImage(iconTop, theTable, r, c);
		}
		if (neededToFocus) {
			clips.eval("(pop-focus)");
		}
	}

	/**************/
	/* updateGrid */
	/**************/
	private void updateGrid() throws Exception {

		for (int i = 0; i < 9; i++) {
			JTable theTable = (JTable) mainGrid.getComponent(i);
			int rowGroup = i / 3;
			int colGroup = i % 3;

			for (int r = 0; r < 3; r++) {
				for (int c = 0; c < 3; c++) {
					theTable.setValueAt(blankCell, r, c);

					if (!interactiveMode) {
						manageUnknownSnakeCell(theTable, r, c, rowGroup, colGroup);
						manageUnknownQuickSandCell(theTable, r, c, rowGroup, colGroup);
						manageUnknownOasisCell(theTable, r, c, rowGroup, colGroup);
					}
					manageWillyCell(theTable, r, c, rowGroup, colGroup);
					manageFreeCell(theTable, r, c, rowGroup, colGroup);
					manageRingBellCell(theTable, r, c, rowGroup, colGroup);
					manageEarthQuakeCell(theTable, r, c, rowGroup, colGroup);
					manageQuickSandRedCell(theTable, r, c, rowGroup, colGroup);
					manageOasisBlueCell(theTable, r, c, rowGroup, colGroup);
					manageSnakeRedCell(theTable, r, c, rowGroup, colGroup);
				}
			}
		}

		jfrm.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public Vector<File> readFolderFiles(String folder) {
		Vector<File> vectorFiles = new Vector<File>();
		File folderFile = new File(folder);

		if (!folderFile.exists() || !folderFile.isDirectory()) {
			System.err.println("" + folder + " can not be used as the maps directory");
			System.exit(1);
		}

		if (folderFile.exists()) {
			File[] files = folderFile.listFiles();
			Arrays.sort(files);
			for (File file : files) {
				boolean isFolder = file.isDirectory();
				if (!isFolder) {
					// check the extension .map
					String nameFile = file.getName();
					if (nameFile.endsWith(".map")) {
						vectorFiles.add(file);
					}
				}
			}
		}
		return vectorFiles;
	}

	void readFile(File file) {
		clips.eval("(set-current-module InternalMemory)");
		try {
			boolean thereIsASnake = false;
			boolean thereIsAWilly = false;
			HashMap<String, String> map = new HashMap<>();
			FileReader fr = new FileReader(file);
			@SuppressWarnings("resource")
			BufferedReader bf = new BufferedReader(fr);

			String sCadena;

			// Read line to line
			while ((sCadena = bf.readLine()) != null) {

				StringTokenizer st = new StringTokenizer(sCadena);
				String element = st.nextToken();
				int row = Integer.parseInt(st.nextToken());
				int column = Integer.parseInt(st.nextToken());
				String key = "" + row + ":" + column;

				boolean containsAnything = map.containsKey(key);
				String whatIsThere = "";

				if (containsAnything)
					whatIsThere = map.get(key);

				if (element.equals("W") && !thereIsAWilly && !containsAnything) {
					clips.assertString("(at (item W) (row " + row + ") (column " + column + "))");
					thereIsAWilly = true;
					map.put(key, "W");
				} else if (element.equals("S") && !thereIsASnake
						&& (!containsAnything || (!whatIsThere.equals("W") && !whatIsThere.equals("A")))) {
					clips.assertString("(at (item S) (row " + row + ") (column " + column + "))");
					thereIsASnake = true;
					map.put(key, "S");
				} else if (element.equals("A") && !containsAnything) {
					clips.assertString("(at (item A) (row " + row + ") (column " + column + "))");
					map.put(key, "A");
				} else if (element.equals("O")
						&& (!containsAnything || (!whatIsThere.equals("A") && !whatIsThere.equals("W")))) {
					clips.assertString("(at (item O) (row " + row + ") (column " + column + "))");
					map.put(key, "O");
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		clips.eval("(set-current-module MAIN)");
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {

		if (!isExecuting) {
			if (!e.isAltDown()) {
				updateFacts();

				if (interactiveMode) {
					// System.out.println("eeeeeeeeeeeeeeeeeeUYYYYYYYYYYYYYYYYYYYY");
					if (!e.isControlDown()) {
						if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
							clips.eval("(assert (introducedKey right))");
							stepWilly();
							stepWilly();
						} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
							clips.eval("(assert (introducedKey left))");
							stepWilly();
							stepWilly();
						} else if (e.getKeyCode() == KeyEvent.VK_UP) {
							clips.eval("(assert (introducedKey up))");
							stepWilly();
							stepWilly();
						} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
							clips.eval("(assert (introducedKey down))");
							stepWilly();
							stepWilly();
						}
					} else {
						if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
							clips.eval("(assert (introducedKey right) (fire))");
							stepWilly();
							stepWilly();
						} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
							clips.eval("(assert (introducedKey left) (fire))");
							stepWilly();
							stepWilly();
						} else if (e.getKeyCode() == KeyEvent.VK_UP) {
							clips.eval("(assert (introducedKey up) (fire))");
							stepWilly();
							stepWilly();
						} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
							clips.eval("(assert (introducedKey down) (fire))");
							stepWilly();
							stepWilly();
						}
					}
				}
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}
}
