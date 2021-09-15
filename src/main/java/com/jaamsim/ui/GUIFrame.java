/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2018 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.Commands.Command;
import com.jaamsim.Commands.DefineCommand;
import com.jaamsim.Commands.DefineViewCommand;
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.Graphics.BillboardText;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.OverlayText;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.Graphics.TextEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RateLimiter;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.EventErrorListener;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTimeListener;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;


/**
 * The main window for a Graphical Simulation.  It provides the controls for managing then
 * EventManager (run, pause, ...) and the graphics (zoom, pan, ...)
 */
public class GUIFrame extends OSFixJFrame implements EventTimeListener, EventErrorListener {
	private static GUIFrame instance;

	// global shutdown flag
	static private AtomicBoolean shuttingDown;

	private JMenu fileMenu;
	private JMenu viewMenu;
	private JMenu windowMenu;
	private JMenu windowList;
	private JMenu optionMenu;
	private JMenu unitsMenu;
	private JMenu helpMenu;
	private static JToggleButton snapToGrid;
	private static JToggleButton xyzAxis;
	private static JToggleButton grid;
	private JCheckBoxMenuItem alwaysTop;
	private JCheckBoxMenuItem graphicsDebug;
	private JMenuItem printInputItem;
	private JMenuItem saveConfigurationMenuItem;  // "Save"
	private JLabel clockDisplay;
	private JLabel speedUpDisplay;
	private JLabel remainingDisplay;

	private JToggleButton controlRealTime;
	private JSpinner spinner;

	private JButton fileSave;
	private JButton undo;
	private JButton redo;
	private JButton undoDropdown;
	private JButton redoDropdown;

	private JToggleButton showLinks;
	private JToggleButton createLinks;

	private Entity selectedEntity;
	private JToggleButton alignLeft;
	private JToggleButton alignCentre;
	private JToggleButton alignRight;

	private JToggleButton bold;
	private JToggleButton italic;
	private JButton fontSelector;
	private JTextField textHeight;
	private JButton largerText;
	private JButton smallerText;
	private ColorIcon colourIcon;
	private JButton fontColour;

	private JButton increaseZ;
	private JButton decreaseZ;

	private JToggleButton outline;
	private JSpinner lineWidth;
	private ColorIcon lineColourIcon;
	private JButton lineColour;

	private JToggleButton fill;
	private ColorIcon fillColourIcon;
	private JButton fillColour;

	private RoundToggleButton controlStartResume;
	private ImageIcon runPressedIcon;
	private ImageIcon pausePressedIcon;
	private RoundToggleButton controlStop;
	private JTextField pauseTime;
	private JTextField gridSpacing;

	private JLabel locatorPos;

	//JButton toolButtonIsometric;
	private JToggleButton lockViewXYPlane;

	private int lastValue = -1;
	private JProgressBar progressBar;
	private static Image iconImage;

	private static final RateLimiter rateLimiter;

	private static boolean SAFE_GRAPHICS;

	// Collection of default window parameters
	public static int DEFAULT_GUI_WIDTH = 1160;
	public static int COL1_WIDTH;
	public static int COL2_WIDTH;
	public static int COL3_WIDTH;
	public static int COL4_WIDTH;
	public static int COL1_START;
	public static int COL2_START;
	public static int COL3_START;
	public static int COL4_START;
	public static int HALF_TOP;
	public static int HALF_BOTTOM;
	public static int TOP_START;
	public static int BOTTOM_START;
	public static int LOWER_HEIGHT;
	public static int LOWER_START;
	public static int VIEW_HEIGHT;
	public static int VIEW_WIDTH;

	public static int VIEW_OFFSET = 50;

	private static final String LAST_USED_FOLDER = "";
	private static final String LAST_USED_3D_FOLDER = "3D_FOLDER";
	private static final String LAST_USED_IMAGE_FOLDER = "IMAGE_FOLDER";

	private static final String RUN_TOOLTIP = GUIFrame.formatToolTip("Run", "Starts or resumes the simulation run.");
	private static final String PAUSE_TOOLTIP = "<html><b>Pause</b></html>";  // Use a small tooltip for Pause so that it does not block the simulation time display

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			LogBox.logLine("Unable to change look and feel.");
		}

		try {
			URL file = GUIFrame.class.getResource("/resources/images/icon.png");
			iconImage = Toolkit.getDefaultToolkit().getImage(file);
		}
		catch (Exception e) {
			LogBox.logLine("Unable to load icon file.");
			iconImage = null;
		}

		shuttingDown = new AtomicBoolean(false);
		rateLimiter = RateLimiter.create(60);
	}

	private GUIFrame() {
		super();

		getContentPane().setLayout( new BorderLayout() );
		setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
		this.addWindowListener(new CloseListener());

		// Initialize the working environment
		initializeMenus();
		initializeButtonBar();
		initializeMainToolBars();

		this.setIconImage(GUIFrame.getWindowIcon());

		//Set window size
		setResizable( true );  //FIXME should be false, but this causes the window to be sized
		                       //      and positioned incorrectly in the Windows 7 Aero theme
		pack();

		controlStartResume.setSelected( false );
		controlStartResume.setEnabled( false );
		controlStop.setSelected( false );
		controlStop.setEnabled( false );
		setProgress( 0 );
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled( false );
		JPopupMenu.setDefaultLightWeightPopupEnabled( false );

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				Simulation.setControlPanelWidth(getSize().width);
			}
		});
	}

	@Override
	public Dimension getPreferredSize() {
		Point fix = OSFix.getSizeAdustment();
		return new Dimension(DEFAULT_GUI_WIDTH + fix.x, super.getPreferredSize().height);
	}

	public static synchronized GUIFrame getInstance() {
		return instance;
	}

	public static synchronized GUIFrame createInstance() {
		instance = new GUIFrame();
		GUIFrame.registerCallback(new Runnable() {
			@Override
			public void run() {
				SwingUtilities.invokeLater(new UIUpdater(instance));
			}
		});
		return instance;
	}

	public static final void registerCallback(Runnable r) {
		rateLimiter.registerCallback(r);
	}

	public static final void updateUI() {
		rateLimiter.queueUpdate();
	}

	/**
	 * Listens for window events for the GUI.
	 *
	 */
	private class CloseListener extends WindowAdapter implements ActionListener {
		@Override
		public void windowClosing(WindowEvent e) {
			GUIFrame.this.close();
		}

		@Override
		public void actionPerformed( ActionEvent event ) {
			GUIFrame.this.close();
		}

		@Override
		public void windowDeiconified(WindowEvent e) {

			// Re-open the view windows
			for (View v : View.getAll()) {
				if (v.showWindow())
					RenderManager.inst().createWindow(v);
			}

			// Re-open the tools
			Simulation.showActiveTools();
			FrameBox.reSelectEntity();
		}

		@Override
		public void windowIconified(WindowEvent e) {

			// Close all the tools
			Simulation.closeAllTools();

			// Save whether each window is open or closed
			for (View v : View.getAll()) {
				v.setKeepWindowOpen(v.showWindow());
			}

			// Close all the view windows
			RenderManager.clear();
		}

		@Override
		public void windowActivated(WindowEvent e) {

			// Re-open the view windows
			for (int i=0; i<View.getAll().size(); i++) {
				View v = View.getAll().get(i);
				if (v != null && v.showWindow())
					RenderManager.inst().createWindow(v);
			}

			// Re-open the tools
			Simulation.showActiveTools();
			FrameBox.reSelectEntity();
		}
	}

	/**
	 * Perform exit window duties
	 */
	void close() {
		// check for unsaved changes
		if (InputAgent.isSessionEdited()) {
			boolean confirmed = GUIFrame.showSaveChangesDialog(this);
			if (!confirmed)
				return;
		}
		InputAgent.closeLogFile();
		GUIFrame.shutdown(0);
	}

	/**
	 * Clears the simulation and user interface prior to loading a new model
	 */
	public void clear() {
		currentEvt.clear();
		currentEvt.setTraceListener(null);
		// Clear the simulation
		Simulation.clear();
		FrameBox.clear();
		EntityPallet.clear();
		RenderManager.clear();

		this.updateForSimulationState(GUIFrame.SIM_STATE_LOADED);

		// Clear the title bar
		setTitle(Simulation.getModelName());

		// Clear the status bar
		setProgress( 0 );
		speedUpDisplay.setText("0");
		remainingDisplay.setText("-");
		locatorPos.setText( "-" );

		// Read the autoload configuration file
		InputAgent.clear();
		InputAgent.setRecordEdits(false);
		InputAgent.readResource("<res>/inputs/autoload.cfg");
		InputAgent.setPreDefinedEntityCount( Entity.getAll().get( Entity.getAll().size() - 1 ).getEntityNumber());

		updateForUndo();
	}

	/**
	 * Sets up the Control Panel's menu bar.
	 */
	private void initializeMenus() {

		// Set up the individual menus
		this.initializeFileMenu();
		this.initializeViewMenu();
		this.initializeWindowMenu();
		this.initializeOptionsMenu();
		this.initializeUnitsMenu();
		this.initializeHelpMenu();

		// Add the individual menu to the main menu
		JMenuBar mainMenuBar = new JMenuBar();
		mainMenuBar.add( fileMenu );
		mainMenuBar.add( viewMenu );
		mainMenuBar.add( windowMenu );
		mainMenuBar.add( optionMenu );
		mainMenuBar.add( unitsMenu );
		mainMenuBar.add( helpMenu );

		// Add main menu to the window
		setJMenuBar( mainMenuBar );
	}

	// ******************************************************************************************************
	// MENU BAR
	// ******************************************************************************************************

	/**
	 * Sets up the File menu in the Control Panel's menu bar.
	 */
	private void initializeFileMenu() {

		// File menu creation
		fileMenu = new JMenu( "File" );
		fileMenu.setMnemonic( 'F' );
		fileMenu.setEnabled( false );

		// 1) "New" menu item
		JMenuItem newMenuItem = new JMenuItem( "New" );
		newMenuItem.setMnemonic( 'N' );
		newMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.newModel();
			}
		} );
		fileMenu.add( newMenuItem );

		// 2) "Open" menu item
		JMenuItem configMenuItem = new JMenuItem( "Open..." );
		configMenuItem.setMnemonic( 'O' );
		configMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.load();
			}
		} );
		fileMenu.add( configMenuItem );

		// 3) "Save" menu item
		saveConfigurationMenuItem = new JMenuItem( "Save" );
		saveConfigurationMenuItem.setMnemonic( 'S' );
		saveConfigurationMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.save();
			}
		} );
		fileMenu.add( saveConfigurationMenuItem );

		// 4) "Save As..." menu item
		JMenuItem saveConfigurationAsMenuItem = new JMenuItem( "Save As..." );
		saveConfigurationAsMenuItem.setMnemonic( 'V' );
		saveConfigurationAsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.saveAs();

			}
		} );
		fileMenu.add( saveConfigurationAsMenuItem );

		// 5) "Import..." menu item
		JMenu importGraphicsMenuItem = new JMenu( "Import..." );
		importGraphicsMenuItem.setMnemonic( 'I' );

		JMenuItem importImages = new JMenuItem( "Images..." );
		importImages.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				DisplayEntityFactory.importImages(GUIFrame.this);
			}
		} );
		importGraphicsMenuItem.add( importImages );

		JMenuItem import3D = new JMenuItem( "3D Assets..." );
		import3D.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				DisplayEntityFactory.import3D(GUIFrame.this);
			}
		} );
		importGraphicsMenuItem.add( import3D );

		fileMenu.add( importGraphicsMenuItem );

		// 6) "Print Input Report" menu item
		printInputItem = new JMenuItem( "Print Input Report" );
		printInputItem.setMnemonic( 'I' );
		printInputItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.printInputFileKeywords();
			}
		} );
		fileMenu.add( printInputItem );

		// 7) "Exit" menu item
		JMenuItem exitMenuItem = new JMenuItem( "Exit" );
		exitMenuItem.setMnemonic( 'x' );
		exitMenuItem.addActionListener(new CloseListener());
		fileMenu.add( exitMenuItem );
	}

	/**
	 * Sets up the View menu in the Control Panel's menu bar.
	 */
	private void initializeViewMenu() {

		// View menu creation
		viewMenu = new JMenu( "Tools" );
		viewMenu.setMnemonic( 'T' );

		// 1) "Show Basic Tools" menu item
		JMenuItem showBasicToolsMenuItem = new JMenuItem( "Show Basic Tools" );
		showBasicToolsMenuItem.setMnemonic( 'B' );
		showBasicToolsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				Simulation sim = Simulation.getInstance();
				InputAgent.applyBoolean(sim, "ShowModelBuilder", true);
				InputAgent.applyBoolean(sim, "ShowObjectSelector", true);
				InputAgent.applyBoolean(sim, "ShowInputEditor", true);
				InputAgent.applyBoolean(sim, "ShowOutputViewer", true);
			}
		} );
		viewMenu.add( showBasicToolsMenuItem );

		// 2) "Close All Tools" menu item
		JMenuItem closeAllToolsMenuItem = new JMenuItem( "Close All Tools" );
		closeAllToolsMenuItem.setMnemonic( 'C' );
		closeAllToolsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				Simulation sim = Simulation.getInstance();
				InputAgent.applyBoolean(sim, "ShowModelBuilder", false);
				InputAgent.applyBoolean(sim, "ShowObjectSelector", false);
				InputAgent.applyBoolean(sim, "ShowInputEditor", false);
				InputAgent.applyBoolean(sim, "ShowOutputViewer", false);
				InputAgent.applyBoolean(sim, "ShowPropertyViewer", false);
				InputAgent.applyBoolean(sim, "ShowLogViewer", false);
			}
		} );
		viewMenu.add( closeAllToolsMenuItem );

		// 3) "Model Builder" menu item
		JMenuItem objectPalletMenuItem = new JMenuItem( "Model Builder" );
		objectPalletMenuItem.setMnemonic( 'O' );
		objectPalletMenuItem.addActionListener(new SimulationMenuAction("ShowModelBuilder", "TRUE"));
		viewMenu.addSeparator();
		viewMenu.add( objectPalletMenuItem );

		// 4) "Object Selector" menu item
		JMenuItem objectSelectorMenuItem = new JMenuItem( "Object Selector" );
		objectSelectorMenuItem.setMnemonic( 'S' );
		objectSelectorMenuItem.addActionListener(new SimulationMenuAction("ShowObjectSelector", "TRUE"));
		viewMenu.add( objectSelectorMenuItem );

		// 5) "Input Editor" menu item
		JMenuItem inputEditorMenuItem = new JMenuItem( "Input Editor" );
		inputEditorMenuItem.setMnemonic( 'I' );
		inputEditorMenuItem.addActionListener(new SimulationMenuAction("ShowInputEditor", "TRUE"));
		viewMenu.add( inputEditorMenuItem );

		// 6) "Output Viewer" menu item
		JMenuItem outputMenuItem = new JMenuItem( "Output Viewer" );
		outputMenuItem.setMnemonic( 'U' );
		outputMenuItem.addActionListener(new SimulationMenuAction("ShowOutputViewer", "TRUE"));
		viewMenu.add( outputMenuItem );

		// 7) "Property Viewer" menu item
		JMenuItem propertiesMenuItem = new JMenuItem( "Property Viewer" );
		propertiesMenuItem.setMnemonic( 'P' );
		propertiesMenuItem.addActionListener(new SimulationMenuAction("ShowPropertyViewer", "TRUE"));
		viewMenu.add( propertiesMenuItem );

		// 8) "Log Viewer" menu item
		JMenuItem logMenuItem = new JMenuItem( "Log Viewer" );
		logMenuItem.setMnemonic( 'L' );
		logMenuItem.addActionListener(new SimulationMenuAction("ShowLogViewer", "TRUE"));
		viewMenu.add( logMenuItem );

		// 9) "Event Viewer" menu item
		JMenuItem eventsMenuItem = new JMenuItem( "Event Viewer" );
		eventsMenuItem.setMnemonic( 'E' );
		eventsMenuItem.addActionListener(new SimulationMenuAction("ShowEventViewer", "TRUE"));
		viewMenu.add( eventsMenuItem );

		// 10) "Reset Positions and Sizes" menu item
		JMenuItem resetItem = new JMenuItem( "Reset Positions and Sizes" );
		resetItem.setMnemonic( 'R' );
		resetItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				Simulation.resetWindowPositionsAndSizes();
			}
		} );
		viewMenu.addSeparator();
		viewMenu.add( resetItem );
	}

	private static final class SimulationMenuAction implements ActionListener {
		final String keyword;
		final String args;

		SimulationMenuAction(String k, String a) {
			keyword = k;
			args = a;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			InputAgent.applyArgs(Simulation.getInstance(), keyword, args);
		}
	}

	/**
	 * Sets up the Window menu in the Control Panel's menu bar.
	 */
	private void initializeWindowMenu() {

		// Window menu creation
		windowMenu = new NewRenderWindowMenu("Views");
		windowMenu.setMnemonic( 'V' );

		// Initialize list of windows
		windowList = new WindowMenu("Select Window");
		windowList.setMnemonic( 'S' );
	}

	/**
	 * Sets up the Options menu in the Control Panel's menu bar.
	 */
	private void initializeOptionsMenu() {

		optionMenu = new JMenu( "Options" );
		optionMenu.setMnemonic( 'O' );

		// 1) "Always on top" check box
		alwaysTop = new JCheckBoxMenuItem( "Always on top", false );
		alwaysTop.setMnemonic( 'A' );
		optionMenu.add( alwaysTop );
		alwaysTop.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if( GUIFrame.this.isAlwaysOnTop() ) {
					GUIFrame.this.setAlwaysOnTop( false );
				}
				else {
					GUIFrame.this.setAlwaysOnTop( true );
				}
			}
		} );

		// 2) "Graphics Debug Info" check box
		graphicsDebug = new JCheckBoxMenuItem( "Graphics Debug Info", false );
		graphicsDebug.setMnemonic( 'D' );
		optionMenu.add( graphicsDebug );
		graphicsDebug.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				RenderManager.setDebugInfo(((JCheckBoxMenuItem)e.getSource()).getState());
			}
		});
	}

	/**
	 * Sets up the Units menu in the Control Panel's menu bar.
	 */
	private void initializeUnitsMenu() {

		unitsMenu = new JMenu( "Units" );
		unitsMenu.setMnemonic( 'U' );

		unitsMenu.addMenuListener( new MenuListener() {

			@Override
			public void menuCanceled(MenuEvent arg0) {}

			@Override
			public void menuDeselected(MenuEvent arg0) {
				unitsMenu.removeAll();
			}

			@Override
			public void menuSelected(MenuEvent arg0) {
				UnitsSelector.populateMenu(unitsMenu);
				unitsMenu.setVisible(true);
			}
		});
	}

	/**
	 * Sets up the Help menu in the Control Panel's menu bar.
	 */
	private void initializeHelpMenu() {

		// Help menu creation
		helpMenu = new JMenu( "Help" );
		helpMenu.setMnemonic( 'H' );

		// 1) "About" menu item
		JMenuItem aboutMenu = new JMenuItem( "About" );
		aboutMenu.setMnemonic( 'A' );
		aboutMenu.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				AboutBox.instance().setVisible(true);
			}
		} );
		helpMenu.add( aboutMenu );
	}

	/**
	 * Returns the pixel length of the string with specified font
	 */
	private static int getPixelWidthOfString_ForFont(String str, Font font) {
		FontMetrics metrics = new FontMetrics(font) {};
		Rectangle2D bounds = metrics.getStringBounds(str, null);
		return (int)bounds.getWidth();
	}

	// ******************************************************************************************************
	// BUTTON BAR
	// ******************************************************************************************************

	/**
	 * Sets up the Control Panel's button bar.
	 */
	public void initializeButtonBar() {

		Insets noMargin = new Insets( 0, 0, 0, 0 );
		Insets smallMargin = new Insets( 1, 1, 1, 1 );
		Dimension separatorDim = new Dimension(11, 20);
		Dimension gapDim = new Dimension(5, separatorDim.height);

		JToolBar buttonBar = new JToolBar();
		buttonBar.setMargin( smallMargin );
		buttonBar.setFloatable(false);
		buttonBar.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );

		getContentPane().add( buttonBar, BorderLayout.NORTH );

		// File new, open, and save buttons
		buttonBar.add(Box.createRigidArea(gapDim));
		addFileNewButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addFileOpenButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addFileSaveButton(buttonBar, noMargin);

		// Undo and redo buttons
		buttonBar.addSeparator(separatorDim);
		addUndoButtons(buttonBar, noMargin);

		// 2D, axes, and grid buttons
		buttonBar.addSeparator(separatorDim);
		add2dButton(buttonBar, smallMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addShowAxesButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addShowGridButton(buttonBar, noMargin);

		// Snap-to-grid button and field
		buttonBar.addSeparator(separatorDim);
		addSnapToGridButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addSnapToGridField(buttonBar, noMargin);

		// Show and create links buttons
		buttonBar.addSeparator(separatorDim);
		addShowLinksButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addCreateLinksButton(buttonBar, noMargin);

		// Font selector and text height field
		buttonBar.addSeparator(separatorDim);
		addFontSelector(buttonBar, smallMargin);
		addTextHeightField(buttonBar, noMargin);

		// Larger and smaller text height buttons
		buttonBar.add(Box.createRigidArea(gapDim));
		addTextHeightButtons(buttonBar, noMargin);

		// Bold and Italic buttons
		buttonBar.add(Box.createRigidArea(gapDim));
		addFontStyleButtons(buttonBar, noMargin);

		// Font colour button
		buttonBar.add(Box.createRigidArea(gapDim));
		addFontColourButton(buttonBar, noMargin);

		// Text alignment buttons
		buttonBar.add(Box.createRigidArea(gapDim));
		addTextAlignmentButtons(buttonBar, noMargin);

		// Z-coordinate buttons
		buttonBar.addSeparator(separatorDim);
		addZButtons(buttonBar, noMargin);

		// Line buttons
		buttonBar.addSeparator(separatorDim);
		addOutlineButton(buttonBar, noMargin);
		addLineWidthSpinner(buttonBar, noMargin);
		addLineColourButton(buttonBar, noMargin);

		// Fill buttons
		buttonBar.addSeparator(separatorDim);
		addFillButton(buttonBar, noMargin);
		addFillColourButton(buttonBar, noMargin);
	}

	private void addFileNewButton(JToolBar buttonBar, Insets margin) {
		JButton fileNew = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/New-16.png")) );
		fileNew.setMargin(margin);
		fileNew.setFocusPainted(false);
		fileNew.setToolTipText(formatToolTip("New", "Starts a new model."));
		fileNew.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.newModel();
			}
		} );
		buttonBar.add( fileNew );
	}

	private void addFileOpenButton(JToolBar buttonBar, Insets margin) {
		JButton fileOpen = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Open-16.png")) );
		fileOpen.setMargin(margin);
		fileOpen.setFocusPainted(false);
		fileOpen.setToolTipText(formatToolTip("Open...", "Opens a model."));
		fileOpen.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.load();
			}
		} );
		buttonBar.add( fileOpen );
	}

	private void addFileSaveButton(JToolBar buttonBar, Insets margin) {
		fileSave = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Save-16.png")) );
		fileSave.setMargin(margin);
		fileSave.setFocusPainted(false);
		fileSave.setToolTipText(formatToolTip("Save", "Saves the present model."));
		fileSave.setEnabled(false);
		fileSave.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.save();
			}
		} );
		buttonBar.add( fileSave );
	}

	private void addUndoButtons(JToolBar buttonBar, Insets margin) {

		// Undo button
		undo = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Undo-16.png")) );
		undo.setMargin(margin);
		undo.setFocusPainted(false);
		undo.setRequestFocusEnabled(false);
		undo.setToolTipText(formatToolTip("Undo", "Reverses the last change to the model."));
		undo.setEnabled(InputAgent.hasUndo());
		undo.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.undo();
			}
		} );
		buttonBar.add( undo );

		// Undo Dropdown Menu
		undoDropdown = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/dropdown.png")));
		undoDropdown.setMargin(margin);
		undoDropdown.setFocusPainted(false);
		undoDropdown.setRequestFocusEnabled(false);
		undoDropdown.setEnabled(InputAgent.hasUndo());
		undoDropdown.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				ScrollablePopupMenu menu = new ScrollablePopupMenu("UndoMenu");
				ArrayList<Command> list = InputAgent.getUndoList();
				for (int i = 1; i <= list.size(); i++) {
					Command cmd = list.get(list.size() - i);
					final int num = i;
					JMenuItem item = new JMenuItem(cmd.toString());
					item.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed( ActionEvent event ) {
							InputAgent.undo(num);
						}
					} );
					menu.add(item);
				}
				menu.show(undoDropdown, 0, undoDropdown.getHeight());
			}
		} );
		buttonBar.add( undoDropdown );

		// Redo button
		redo = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Redo-16.png")) );
		redo.setMargin(margin);
		redo.setFocusPainted(false);
		redo.setRequestFocusEnabled(false);
		redo.setToolTipText(formatToolTip("Redo", "Re-performs the last change to the model that was undone."));
		redo.setEnabled(InputAgent.hasRedo());
		redo.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.redo();
			}
		} );
		buttonBar.add( redo );

		// Redo Dropdown Menu
		redoDropdown = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/dropdown.png")));
		redoDropdown.setMargin(margin);
		redoDropdown.setFocusPainted(false);
		redoDropdown.setRequestFocusEnabled(false);
		redoDropdown.setEnabled(InputAgent.hasRedo());
		redoDropdown.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				ScrollablePopupMenu menu = new ScrollablePopupMenu("RedoMenu");
				ArrayList<Command> list = InputAgent.getRedoList();
				for (int i = 1; i <= list.size(); i++) {
					Command cmd = list.get(list.size() - i);
					final int num = i;
					JMenuItem item = new JMenuItem(cmd.toString());
					item.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed( ActionEvent event ) {
							InputAgent.redo(num);
						}
					} );
					menu.add(item);
				}
				menu.show(redoDropdown, 0, redoDropdown.getHeight());
			}
		} );
		buttonBar.add( redoDropdown );
	}

	private void add2dButton(JToolBar buttonBar, Insets margin) {
		lockViewXYPlane = new JToggleButton( "2D" );
		lockViewXYPlane.setMargin(margin);
		lockViewXYPlane.setFocusPainted(false);
		lockViewXYPlane.setRequestFocusEnabled(false);
		lockViewXYPlane.setToolTipText(formatToolTip("2D View",
				"Sets the camera position to show a bird's eye view of the 3D scene."));
		lockViewXYPlane.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				boolean bLock2D = (((JToggleButton)event.getSource()).isSelected());

				if (RenderManager.isGood()) {
					View currentView = RenderManager.inst().getActiveView();
					if (currentView != null) {
						currentView.setLock2D(bLock2D);
					}
				}
				fileSave.requestFocusInWindow();
			}
		} );
		buttonBar.add( lockViewXYPlane );
	}

	private void addShowAxesButton(JToolBar buttonBar, Insets margin) {
		xyzAxis = new JToggleButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Axes-16.png")) );
		xyzAxis.setMargin(margin);
		xyzAxis.setFocusPainted(false);
		xyzAxis.setRequestFocusEnabled(false);
		xyzAxis.setToolTipText(formatToolTip("Show Axes",
				"Shows the unit vectors for the x, y, and z axes."));
		xyzAxis.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				DisplayEntity ent = (DisplayEntity) Entity.getNamedEntity("XYZ-Axis");
				if (ent != null) {
					InputAgent.applyBoolean(ent, "Show", xyzAxis.isSelected());
				}
				fileSave.requestFocusInWindow();
			}
		} );
		buttonBar.add( xyzAxis );
	}

	private void addShowGridButton(JToolBar buttonBar, Insets margin) {
		grid = new JToggleButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Grid-16.png")) );
		grid.setMargin(margin);
		grid.setFocusPainted(false);
		grid.setRequestFocusEnabled(false);
		grid.setToolTipText(formatToolTip("Show Grid",
				"Shows the coordinate grid on the x-y plane."));
		grid.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				DisplayEntity ent = (DisplayEntity) Entity.getNamedEntity("XY-Grid");
				if (ent == null && Entity.getNamedEntity("Grid100x100") != null) {
					InputAgent.storeAndExecute(new DefineCommand(DisplayEntity.class, "XY-Grid"));
					ent = (DisplayEntity) Entity.getNamedEntity("XY-Grid");
					KeywordIndex dmKw = InputAgent.formatArgs("DisplayModel", "Grid100x100");
					KeywordIndex sizeKw = InputAgent.formatArgs("Size", "100", "100", "0", "m");
					InputAgent.storeAndExecute(new KeywordCommand(ent, dmKw, sizeKw));
					grid.setSelected(true);
				}
				if (ent != null) {
					InputAgent.applyBoolean(ent, "Show", grid.isSelected());
				}
				fileSave.requestFocusInWindow();
			}
		} );
		buttonBar.add( grid );
	}

	private void addSnapToGridButton(JToolBar buttonBar, Insets margin) {
		snapToGrid = new JToggleButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Snap-16.png")) );
		snapToGrid.setMargin(margin);
		snapToGrid.setFocusPainted(false);
		snapToGrid.setRequestFocusEnabled(false);
		snapToGrid.setToolTipText(formatToolTip("Snap to Grid",
				"During repositioning, objects are forced to the nearest grid point."));
		snapToGrid.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.applyBoolean(Simulation.getInstance(), "SnapToGrid", snapToGrid.isSelected());
				gridSpacing.setEnabled(snapToGrid.isSelected());
				fileSave.requestFocusInWindow();
			}
		} );

		buttonBar.add( snapToGrid );
	}

	private void addSnapToGridField(JToolBar buttonBar, Insets margin) {

		gridSpacing = new JTextField("1000000 m") {
			@Override
			protected void processFocusEvent(FocusEvent fe) {
				if (fe.getID() == FocusEvent.FOCUS_LOST) {
					GUIFrame.this.setSnapGridSpacing(this.getText().trim());
				}
				else if (fe.getID() == FocusEvent.FOCUS_GAINED) {
					gridSpacing.selectAll();
				}
				super.processFocusEvent( fe );
			}
		};

		gridSpacing.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				GUIFrame.this.setSnapGridSpacing(gridSpacing.getText().trim());
				fileSave.requestFocusInWindow();
			}
		});

		gridSpacing.setMaximumSize(gridSpacing.getPreferredSize());
		int hght = snapToGrid.getPreferredSize().height;
		gridSpacing.setPreferredSize(new Dimension(gridSpacing.getPreferredSize().width, hght));

		gridSpacing.setHorizontalAlignment(JTextField.RIGHT);
		gridSpacing.setToolTipText(formatToolTip("Snap Grid Spacing",
				"Distance between adjacent grid points, e.g. 0.1 m, 10 km, etc."));

		gridSpacing.setEnabled(snapToGrid.isSelected());

		buttonBar.add(gridSpacing);
	}

	private void addShowLinksButton(JToolBar buttonBar, Insets margin) {
		showLinks = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/ShowLinks-16.png")));
		showLinks.setToolTipText(formatToolTip("Show Entity Flow",
				"When selected, arrows are shown between objects to indicate the flow of entities."));
		showLinks.setMargin(margin);
		showLinks.setFocusPainted(false);
		showLinks.setRequestFocusEnabled(false);
		showLinks.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {

				boolean bShow = (((JToggleButton)event.getSource()).isSelected());
				if (RenderManager.isGood()) {
					RenderManager.inst().setShowLinks(bShow);
					RenderManager.redraw();
				}
				fileSave.requestFocusInWindow();
			}

		});
		buttonBar.add( showLinks );
	}

	private void addCreateLinksButton(JToolBar buttonBar, Insets margin) {
		createLinks = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/MakeLinks-16.png")));
		createLinks.setToolTipText(formatToolTip("Create Entity Links",
				"When this is enabled, entities are linked when selection is changed."));
		createLinks.setMargin(margin);
		createLinks.setFocusPainted(false);
		createLinks.setRequestFocusEnabled(false);
		createLinks.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {

				boolean bCreate = (((JToggleButton)event.getSource()).isSelected());
				if (RenderManager.isGood()) {
					if (bCreate) {
						FrameBox.setSelectedEntity(null, false);
					}
					RenderManager.inst().setCreateLinks(bCreate);
					RenderManager.redraw();
				}
				fileSave.requestFocusInWindow();
			}

		});
		buttonBar.add( createLinks );
	}

	private void addTextAlignmentButtons(JToolBar buttonBar, Insets margin) {

		ActionListener alignListener = new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof TextBasics))
					return;
				TextBasics textEnt = (TextBasics) selectedEntity;
				Vec3d align = textEnt.getAlignment();
				double prevAlign = align.x;
				align.x = alignLeft.isSelected() ? -0.5d : align.x;
				align.x = alignCentre.isSelected() ? 0.0d : align.x;
				align.x = alignRight.isSelected() ? 0.5d : align.x;
				if (align.x == textEnt.getAlignment().x)
					return;
				KeywordIndex kw = InputAgent.formatVec3dInput("Alignment", align, DimensionlessUnit.class);

				Vec3d pos = textEnt.getPosition();
				Vec3d size = textEnt.getSize();
				pos.x += (align.x - prevAlign) * size.x;
				KeywordIndex posKw = InputAgent.formatVec3dInput("Position", pos, DistanceUnit.class);

				InputAgent.storeAndExecute(new KeywordCommand(textEnt, kw, posKw));
				fileSave.requestFocusInWindow();
			}
		};

		alignLeft = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/AlignLeft-16.png")));
		alignLeft.setMargin(margin);
		alignLeft.setFocusPainted(false);
		alignLeft.setRequestFocusEnabled(false);
		alignLeft.setToolTipText(formatToolTip("Align Left",
				"Aligns the text to the left margin."));
		alignLeft.addActionListener( alignListener );

		alignCentre = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/AlignCentre-16.png")));
		alignCentre.setMargin(margin);
		alignCentre.setFocusPainted(false);
		alignCentre.setRequestFocusEnabled(false);
		alignCentre.setToolTipText(formatToolTip("Align Centre",
				"Centres the text between the right and left margins."));
		alignCentre.addActionListener( alignListener );

		alignRight = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/AlignRight-16.png")));
		alignRight.setMargin(margin);
		alignRight.setFocusPainted(false);
		alignRight.setRequestFocusEnabled(false);
		alignRight.setToolTipText(formatToolTip("Align Right",
				"Aligns the text to the right margin."));
		alignRight.addActionListener( alignListener );

		ButtonGroup alignmentGroup = new ButtonGroup();
		alignmentGroup.add(alignLeft);
		alignmentGroup.add(alignCentre);
		alignmentGroup.add(alignRight);

		buttonBar.add( alignLeft );
		buttonBar.add( alignCentre );
		buttonBar.add( alignRight );
	}

	private void addFontStyleButtons(JToolBar buttonBar, Insets margin) {

		ActionListener alignmentListener = new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof TextEntity))
					return;
				TextEntity textEnt = (TextEntity) selectedEntity;
				if (textEnt.isBold() == bold.isSelected()
						&& textEnt.isItalic() && italic.isSelected())
					return;
				ArrayList<String> stylesList = new ArrayList<>(2);
				if (bold.isSelected())
					stylesList.add("BOLD");
				if (italic.isSelected())
					stylesList.add("ITALIC");
				String[] styles = stylesList.toArray(new String[stylesList.size()]);
				KeywordIndex kw = InputAgent.formatArgs("FontStyle", styles);
				InputAgent.storeAndExecute(new KeywordCommand((Entity)textEnt, kw));
				fileSave.requestFocusInWindow();
			}
		};

		bold = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Bold-16.png")));
		bold.setMargin(margin);
		bold.setFocusPainted(false);
		bold.setRequestFocusEnabled(false);
		bold.setToolTipText(formatToolTip("Bold", "Makes the text bold."));
		bold.addActionListener( alignmentListener );

		italic = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Italic-16.png")));
		italic.setMargin(margin);
		italic.setFocusPainted(false);
		italic.setRequestFocusEnabled(false);
		italic.setToolTipText(formatToolTip("Italic", "Italicizes the text."));
		italic.addActionListener( alignmentListener );

		buttonBar.add( bold );
		buttonBar.add( italic );
	}

	private void addFontSelector(JToolBar buttonBar, Insets margin) {

		fontSelector = new JButton("");
		fontSelector.setMargin(margin);
		fontSelector.setFocusPainted(false);
		fontSelector.setRequestFocusEnabled(false);
		fontSelector.setPreferredSize(new Dimension(100, fileSave.getPreferredSize().height));
		fontSelector.setToolTipText(formatToolTip("Font", "Sets the font for the text."));
		fontSelector.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof TextEntity))
					return;
				final TextEntity textEnt = (TextEntity) selectedEntity;
				final String presentFontName = textEnt.getFontName();
				ScrollablePopupMenu fontMenu = new ScrollablePopupMenu();

				ActionListener fontActionListener = new ActionListener() {
					@Override
					public void actionPerformed( ActionEvent event ) {
						if (!(event.getSource() instanceof JMenuItem))
							return;
						JMenuItem item = (JMenuItem) event.getSource();
						String fontName = item.getText();
						if (!fontName.equals(textEnt.getFontName())) {
							String name = Parser.addQuotesIfNeeded(fontName);
							KeywordIndex kw = InputAgent.formatInput("FontName", name);
							InputAgent.storeAndExecute(new KeywordCommand((Entity)textEnt, kw));
						}
						fileSave.requestFocusInWindow();
					}
				};

				MouseListener fontMouseListener = new MouseListener() {
					@Override
					public void mouseClicked(MouseEvent e) {}
					@Override
					public void mousePressed(MouseEvent e) {}
					@Override
					public void mouseReleased(MouseEvent e) {}
					@Override
					public void mouseEntered(MouseEvent e) {
						if (!(e.getSource() instanceof JMenuItem))
							return;
						JMenuItem item = (JMenuItem) e.getSource();
						String fontName = item.getText();
						if (!fontName.equals(textEnt.getFontName())) {
							String name = Parser.addQuotesIfNeeded(fontName);
							KeywordIndex kw = InputAgent.formatInput("FontName", name);
							InputAgent.storeAndExecute(new KeywordCommand((Entity)textEnt, kw));
						}
					}
					@Override
					public void mouseExited(MouseEvent e) {
						if (!presentFontName.equals(textEnt.getFontName())) {
							String name = Parser.addQuotesIfNeeded(presentFontName);
							KeywordIndex kw = InputAgent.formatInput("FontName", name);
							InputAgent.storeAndExecute(new KeywordCommand((Entity)textEnt, kw));
						}
					}
				};

				// Fonts already in use
				JMenuItem selectedItem = null;
				int selectedIndex = -1;
				int ind = 0;
				for (final String fontName : TextEntity.getFontsInUse()) {
					JMenuItem item = new JMenuItem(fontName);
					if (selectedItem == null && fontName.equals(textEnt.getFontName())) {
						selectedItem = item;
						selectedIndex = ind;
					}
					ind++;
					item.addActionListener(fontActionListener);
					item.addMouseListener(fontMouseListener);
					fontMenu.add(item);
				}
				fontMenu.addSeparator();

				// All possible fonts
				for (final String fontName : TextModel.validFontNames) {
					JMenuItem item = new JMenuItem(fontName);
					item.addActionListener(fontActionListener);
					item.addMouseListener(fontMouseListener);
					fontMenu.add(item);
				}

				fontMenu.show(fontSelector, 0, fontSelector.getPreferredSize().height);
				if (selectedItem != null) {
					fontMenu.ensureIndexIsVisible(selectedIndex);
					selectedItem.setArmed(true);
				}
			}
		});

		buttonBar.add(fontSelector);
	}

	private void addTextHeightField(JToolBar buttonBar, Insets margin) {

		textHeight = new JTextField("1000000 m") {
			@Override
			protected void processFocusEvent(FocusEvent fe) {
				if (fe.getID() == FocusEvent.FOCUS_LOST) {
					GUIFrame.this.setTextHeight(this.getText().trim());
				}
				super.processFocusEvent( fe );
			}
		};

		textHeight.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				GUIFrame.this.setTextHeight(textHeight.getText().trim());
				fileSave.requestFocusInWindow();
			}
		});

		textHeight.setMaximumSize(textHeight.getPreferredSize());
		textHeight.setPreferredSize(new Dimension(textHeight.getPreferredSize().width,
				fontSelector.getPreferredSize().height));

		textHeight.setHorizontalAlignment(JTextField.RIGHT);
		textHeight.setToolTipText(formatToolTip("Text Height",
				"Sets the height of the text, e.g. 0.1 m, 200 cm, etc."));

		buttonBar.add(textHeight);
	}

	private void addTextHeightButtons(JToolBar buttonBar, Insets margin) {

		ActionListener textHeightListener = new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof TextEntity))
					return;
				TextEntity textEnt = (TextEntity) selectedEntity;

				double height = textEnt.getTextHeight();
				double spacing = Simulation.getSnapGridSpacing();
				if (textEnt instanceof OverlayText || textEnt instanceof BillboardText)
					spacing = 1.0d;
				height = Math.round(height/spacing) * spacing;

				if (event.getActionCommand().equals("LargerText")) {
					height += spacing;
				}
				else if (event.getActionCommand().equals("SmallerText")) {
					height -= spacing;
					height = Math.max(spacing, height);
				}

				String format = "%.1f  m";
				if (textEnt instanceof OverlayText || textEnt instanceof BillboardText)
					format = "%.0f";
				String str = String.format(format, height);
				KeywordIndex kw = InputAgent.formatInput("TextHeight", str);
				InputAgent.storeAndExecute(new KeywordCommand((Entity)textEnt, kw));
				fileSave.requestFocusInWindow();
			}
		};

		largerText = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/LargerText-16.png")));
		largerText.setMargin(margin);
		largerText.setFocusPainted(false);
		largerText.setRequestFocusEnabled(false);
		largerText.setToolTipText(formatToolTip("Larger Text",
				"Increases the text height to the next higher multiple of the snap grid spacing."));
		largerText.setActionCommand("LargerText");
		largerText.addActionListener( textHeightListener );

		smallerText = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/SmallerText-16.png")));
		smallerText.setMargin(margin);
		smallerText.setFocusPainted(false);
		smallerText.setRequestFocusEnabled(false);
		smallerText.setToolTipText(formatToolTip("Smaller Text",
				"Decreases the text height to the next lower multiple of the snap grid spacing."));
		smallerText.setActionCommand("SmallerText");
		smallerText.addActionListener( textHeightListener );

		buttonBar.add( largerText );
		buttonBar.add( smallerText );
	}

	private void addFontColourButton(JToolBar buttonBar, Insets margin) {

		colourIcon = new ColorIcon(16, 16);
		colourIcon.setFillColor(Color.LIGHT_GRAY);
		colourIcon.setOutlineColor(Color.LIGHT_GRAY);
		fontColour = new JButton(colourIcon);
		fontColour.setMargin(margin);
		fontColour.setFocusPainted(false);
		fontColour.setRequestFocusEnabled(false);
		fontColour.setToolTipText(formatToolTip("Font Colour", "Sets the colour of the text."));
		fontColour.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof TextEntity))
					return;
				final TextEntity textEnt = (TextEntity) selectedEntity;
				final Color4d presentColour = textEnt.getFontColor();
				ScrollablePopupMenu fontMenu = new ScrollablePopupMenu();

				ActionListener fontActionListener = new ActionListener() {
					@Override
					public void actionPerformed( ActionEvent event ) {
						if (!(event.getSource() instanceof JMenuItem))
							return;
						JMenuItem item = (JMenuItem) event.getSource();
						setFontColour(textEnt, item.getText());
						fileSave.requestFocusInWindow();
					}
				};

				MouseListener fontMouseListener = new MouseListener() {
					@Override
					public void mouseClicked(MouseEvent e) {}
					@Override
					public void mousePressed(MouseEvent e) {}
					@Override
					public void mouseReleased(MouseEvent e) {}
					@Override
					public void mouseEntered(MouseEvent e) {
						if (!(e.getSource() instanceof JMenuItem))
							return;
						JMenuItem item = (JMenuItem) e.getSource();
						setFontColour(textEnt, item.getText());
					}
					@Override
					public void mouseExited(MouseEvent e) {
						setFontColour(textEnt, presentColour);
					}
				};

				final ActionListener chooserActionListener = new ActionListener() {
					@Override
					public void actionPerformed( ActionEvent event ) {
						Color clr = ColorEditor.getColorChooser().getColor();
						Color4d newColour = new Color4d(clr.getRed(), clr.getGreen(),
								clr.getBlue(), clr.getAlpha());
						setFontColour(textEnt, newColour);
						fileSave.requestFocusInWindow();
					}
				};

				// Font colours already in use
				JMenuItem selectedItem = null;
				int selectedIndex = -1;
				int ind = 0;
				for (Color4d col : TextEntity.getFontColoursInUse()) {
					String colourName = ColourInput.toString(col);
					JMenuItem item = new JMenuItem(colourName);
					ColorIcon icon = new ColorIcon(16, 16);
					icon.setFillColor(
							new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
					icon.setOutlineColor(Color.DARK_GRAY);
					item.setIcon(icon);
					if (selectedItem == null && col.equals(textEnt.getFontColor())) {
						selectedItem = item;
						selectedIndex = ind;
					}
					ind++;
					item.addActionListener(fontActionListener);
					item.addMouseListener(fontMouseListener);
					fontMenu.add(item);
				}
				fontMenu.addSeparator();

				// Colour chooser
				JMenuItem chooserItem = new JMenuItem(ColorEditor.OPTION_COLOUR_CHOOSER);
				chooserItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						JColorChooser chooser = ColorEditor.getColorChooser();
						JDialog dialog = JColorChooser.createDialog(null,
								ColorEditor.DIALOG_NAME,
								true,  //modal
								chooser,
								chooserActionListener,  //OK button listener
								null); //no CANCEL button listener
						dialog.setIconImage(GUIFrame.getWindowIcon());
						dialog.setAlwaysOnTop(true);
						Color4d col = textEnt.getFontColor();
						chooser.setColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
						dialog.setVisible(true);
					}
				});
				fontMenu.add(chooserItem);
				fontMenu.addSeparator();

				// All possible fonts
				for (Color4d col : ColourInput.namedColourList) {
					String colourName = ColourInput.toString(col);
					JMenuItem item = new JMenuItem(colourName);
					ColorIcon icon = new ColorIcon(16, 16);
					icon.setFillColor(
							new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
					icon.setOutlineColor(Color.DARK_GRAY);
					item.setIcon(icon);
					item.addActionListener(fontActionListener);
					item.addMouseListener(fontMouseListener);
					fontMenu.add(item);
				}

				fontMenu.show(fontColour, 0, fontColour.getPreferredSize().height);
				if (selectedItem != null) {
					fontMenu.ensureIndexIsVisible(selectedIndex);
					selectedItem.setArmed(true);
				}
			}
		});

		buttonBar.add( fontColour );
	}

	private static void setFontColour(TextEntity textEnt, String colName) {
		KeywordIndex kw = InputAgent.formatInput("FontColour", colName);
		Color4d col = Input.parseColour(kw);
		setFontColour(textEnt, col);
	}

	private static void setFontColour(TextEntity textEnt, Color4d col) {
		if (col.equals(textEnt.getFontColor()))
			return;
		String colName = ColourInput.toString(col);
		KeywordIndex kw = InputAgent.formatInput("FontColour", colName);
		InputAgent.storeAndExecute(new KeywordCommand((Entity)textEnt, kw));
	}

	private void addZButtons(JToolBar buttonBar, Insets margin) {

		ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof DisplayEntity)
						|| selectedEntity instanceof OverlayEntity)
					return;
				DisplayEntity dispEnt = (DisplayEntity) selectedEntity;

				double delta = Simulation.getSnapGridSpacing()/100.0d;
				Vec3d pos = dispEnt.getPosition();
				ArrayList<Vec3d> points = dispEnt.getPoints();
				Vec3d offset = new Vec3d();

				if (event.getActionCommand().equals("Up")) {
					pos.z += delta;
					offset.z += delta;
				}
				else if (event.getActionCommand().equals("Down")) {
					pos.z -= delta;
					offset.z -= delta;
				}

				KeywordIndex posKw = InputAgent.formatVec3dInput("Position", pos, DistanceUnit.class);
				KeywordIndex ptsKw = InputAgent.formatPointsInputs("Points", points, offset);
				InputAgent.storeAndExecute(new KeywordCommand(dispEnt, posKw, ptsKw));
				fileSave.requestFocusInWindow();
			}
		};

		increaseZ = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/PlusZ-16.png")));
		increaseZ.setMargin(margin);
		increaseZ.setFocusPainted(false);
		increaseZ.setRequestFocusEnabled(false);
		increaseZ.setToolTipText(formatToolTip("Move Up",
				"Increases the selected object's z-coordinate by one hundredth of the snap-grid "
				+ "spacing. By moving the object closer to the camera, it will appear on top of "
				+ "other objects with smaller z-coordinates."));
		increaseZ.setActionCommand("Up");
		increaseZ.addActionListener( actionListener );

		decreaseZ = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/MinusZ-16.png")));
		decreaseZ.setMargin(margin);
		decreaseZ.setFocusPainted(false);
		decreaseZ.setRequestFocusEnabled(false);
		decreaseZ.setToolTipText(formatToolTip("Move Down",
				"Decreases the selected object's z-coordinate by one hundredth of the snap-grid "
				+ "spacing. By moving the object farther from the camera, it will appear below "
				+ "other objects with larger z-coordinates."));
		decreaseZ.setActionCommand("Down");
		decreaseZ.addActionListener( actionListener );

		buttonBar.add( increaseZ );
		buttonBar.add( decreaseZ );
	}

	private void addOutlineButton(JToolBar buttonBar, Insets margin) {
		outline = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Outline-16.png")));
		outline.setMargin(margin);
		outline.setFocusPainted(false);
		outline.setRequestFocusEnabled(false);
		outline.setToolTipText(formatToolTip("Show Outline", "Shows the outline."));
		outline.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof LineEntity))
					return;
				LineEntity lineEnt = (LineEntity) selectedEntity;
				lineWidth.setEnabled(outline.isSelected());
				lineColour.setEnabled(outline.isSelected());
				if (lineEnt.isOutlined() == outline.isSelected())
					return;
				KeywordIndex kw = InputAgent.formatBoolean("Outlined", outline.isSelected());
				InputAgent.storeAndExecute(new KeywordCommand((Entity)lineEnt, kw));
				fileSave.requestFocusInWindow();
			}
		});

		buttonBar.add( outline );
	}

	private void addLineWidthSpinner(JToolBar buttonBar, Insets margin) {
		SpinnerNumberModel numberModel = new SpinnerNumberModel(1, 1, 10, 1);
		lineWidth = new JSpinner(numberModel);
		lineWidth.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged( ChangeEvent e ) {
				if (!(selectedEntity instanceof LineEntity))
					return;
				LineEntity lineEnt = (LineEntity) selectedEntity;
				int val = (int) lineWidth.getValue();
				if (val == lineEnt.getLineWidth())
					return;
				InputAgent.applyIntegers((Entity)lineEnt, "LineWidth", val);
			}
		});

		lineWidth.setToolTipText(formatToolTip("Line Width",
				"Sets the width of the line in pixels."));

		buttonBar.add( lineWidth );
	}

	private void addLineColourButton(JToolBar buttonBar, Insets margin) {

		lineColourIcon = new ColorIcon(16, 16);
		lineColourIcon.setFillColor(Color.LIGHT_GRAY);
		lineColourIcon.setOutlineColor(Color.LIGHT_GRAY);
		lineColour = new JButton(lineColourIcon);
		lineColour.setMargin(margin);
		lineColour.setFocusPainted(false);
		lineColour.setRequestFocusEnabled(false);
		lineColour.setToolTipText(formatToolTip("Line Colour",
				"Sets the colour of the line."));
		lineColour.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof LineEntity))
					return;
				final LineEntity lineEnt = (LineEntity) selectedEntity;
				final Color4d presentColour = lineEnt.getLineColour();
				ScrollablePopupMenu menu = new ScrollablePopupMenu();

				ActionListener actionListener = new ActionListener() {
					@Override
					public void actionPerformed( ActionEvent event ) {
						if (!(event.getSource() instanceof JMenuItem))
							return;
						JMenuItem item = (JMenuItem) event.getSource();
						setLineColour(lineEnt, item.getText());
						fileSave.requestFocusInWindow();
					}
				};

				MouseListener mouseListener = new MouseListener() {
					@Override
					public void mouseClicked(MouseEvent e) {}
					@Override
					public void mousePressed(MouseEvent e) {}
					@Override
					public void mouseReleased(MouseEvent e) {}
					@Override
					public void mouseEntered(MouseEvent e) {
						if (!(e.getSource() instanceof JMenuItem))
							return;
						JMenuItem item = (JMenuItem) e.getSource();
						setLineColour(lineEnt, item.getText());
					}
					@Override
					public void mouseExited(MouseEvent e) {
						setLineColour(lineEnt, presentColour);
					}
				};

				final ActionListener chooserActionListener = new ActionListener() {
					@Override
					public void actionPerformed( ActionEvent event ) {
						Color clr = ColorEditor.getColorChooser().getColor();
						Color4d newColour = new Color4d(clr.getRed(), clr.getGreen(),
								clr.getBlue(), clr.getAlpha());
						setLineColour(lineEnt, newColour);
						fileSave.requestFocusInWindow();
					}
				};

				// Line colours already in use
				JMenuItem selectedItem = null;
				int selectedIndex = -1;
				int ind = 0;
				for (Color4d col : LineEntity.getLineColoursInUse()) {
					String colourName = ColourInput.toString(col);
					JMenuItem item = new JMenuItem(colourName);
					ColorIcon icon = new ColorIcon(16, 16);
					icon.setFillColor(
							new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
					icon.setOutlineColor(Color.DARK_GRAY);
					item.setIcon(icon);
					if (selectedItem == null && col.equals(lineEnt.getLineColour())) {
						selectedItem = item;
						selectedIndex = ind;
					}
					ind++;
					item.addActionListener(actionListener);
					item.addMouseListener(mouseListener);
					menu.add(item);
				}
				menu.addSeparator();

				// Colour chooser
				JMenuItem chooserItem = new JMenuItem(ColorEditor.OPTION_COLOUR_CHOOSER);
				chooserItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						JColorChooser chooser = ColorEditor.getColorChooser();
						JDialog dialog = JColorChooser.createDialog(null,
								ColorEditor.DIALOG_NAME,
								true,  //modal
								chooser,
								chooserActionListener,  //OK button listener
								null); //no CANCEL button listener
						dialog.setIconImage(GUIFrame.getWindowIcon());
						dialog.setAlwaysOnTop(true);
						Color4d col = lineEnt.getLineColour();
						chooser.setColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
						dialog.setVisible(true);
					}
				});
				menu.add(chooserItem);
				menu.addSeparator();

				// All possible colours
				for (Color4d col : ColourInput.namedColourList) {
					String colourName = ColourInput.toString(col);
					JMenuItem item = new JMenuItem(colourName);
					ColorIcon icon = new ColorIcon(16, 16);
					icon.setFillColor(
							new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
					icon.setOutlineColor(Color.DARK_GRAY);
					item.setIcon(icon);
					item.addActionListener(actionListener);
					item.addMouseListener(mouseListener);
					menu.add(item);
				}

				menu.show(lineColour, 0, lineColour.getPreferredSize().height);
				if (selectedItem != null) {
					menu.ensureIndexIsVisible(selectedIndex);
					selectedItem.setArmed(true);
				}
			}
		});

		buttonBar.add( lineColour );
	}

	private static void setLineColour(LineEntity lineEnt, String colName) {
		KeywordIndex kw = InputAgent.formatInput("LineColour", colName);
		Color4d col = Input.parseColour(kw);
		setLineColour(lineEnt, col);
	}

	private static void setLineColour(LineEntity lineEnt, Color4d col) {
		if (col.equals(lineEnt.getLineColour()))
			return;
		String colName = ColourInput.toString(col);
		KeywordIndex kw = InputAgent.formatInput("LineColour", colName);
		InputAgent.storeAndExecute(new KeywordCommand((Entity)lineEnt, kw));
	}

	private void addFillButton(JToolBar buttonBar, Insets margin) {
		fill = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Fill-16.png")));
		fill.setMargin(margin);
		fill.setFocusPainted(false);
		fill.setRequestFocusEnabled(false);
		fill.setToolTipText(formatToolTip("Show Fill",
				"Fills the entity with the selected colour."));
		fill.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof FillEntity))
					return;
				FillEntity fillEnt = (FillEntity) selectedEntity;
				fillColour.setEnabled(fill.isSelected());
				if (fillEnt.isFilled() == fill.isSelected())
					return;
				KeywordIndex kw = InputAgent.formatBoolean("Filled", fill.isSelected());
				InputAgent.storeAndExecute(new KeywordCommand((Entity)fillEnt, kw));
				fileSave.requestFocusInWindow();
			}
		});

		buttonBar.add( fill );
	}

	private void addFillColourButton(JToolBar buttonBar, Insets margin) {

		fillColourIcon = new ColorIcon(16, 16);
		fillColourIcon.setFillColor(Color.LIGHT_GRAY);
		fillColourIcon.setOutlineColor(Color.LIGHT_GRAY);
		fillColour = new JButton(fillColourIcon);
		fillColour.setMargin(margin);
		fillColour.setFocusPainted(false);
		fillColour.setRequestFocusEnabled(false);
		fillColour.setToolTipText(formatToolTip("Fill Colour",
				"Sets the colour of the fill."));
		fillColour.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof FillEntity))
					return;
				final FillEntity fillEnt = (FillEntity) selectedEntity;
				final Color4d presentColour = fillEnt.getFillColour();
				ScrollablePopupMenu menu = new ScrollablePopupMenu();

				ActionListener actionListener = new ActionListener() {
					@Override
					public void actionPerformed( ActionEvent event ) {
						if (!(event.getSource() instanceof JMenuItem))
							return;
						JMenuItem item = (JMenuItem) event.getSource();
						setFillColour(fillEnt, item.getText());
						fileSave.requestFocusInWindow();
					}
				};

				MouseListener mouseListener = new MouseListener() {
					@Override
					public void mouseClicked(MouseEvent e) {}
					@Override
					public void mousePressed(MouseEvent e) {}
					@Override
					public void mouseReleased(MouseEvent e) {}
					@Override
					public void mouseEntered(MouseEvent e) {
						if (!(e.getSource() instanceof JMenuItem))
							return;
						JMenuItem item = (JMenuItem) e.getSource();
						setFillColour(fillEnt, item.getText());
					}
					@Override
					public void mouseExited(MouseEvent e) {
						setFillColour(fillEnt, presentColour);
					}
				};

				final ActionListener chooserActionListener = new ActionListener() {
					@Override
					public void actionPerformed( ActionEvent event ) {
						Color clr = ColorEditor.getColorChooser().getColor();
						Color4d newColour = new Color4d(clr.getRed(), clr.getGreen(),
								clr.getBlue(), clr.getAlpha());
						setFillColour(fillEnt, newColour);
						fileSave.requestFocusInWindow();
					}
				};

				// Fill colours already in use
				JMenuItem selectedItem = null;
				int selectedIndex = -1;
				int ind = 0;
				for (Color4d col : FillEntity.getFillColoursInUse()) {
					String colourName = ColourInput.toString(col);
					JMenuItem item = new JMenuItem(colourName);
					ColorIcon icon = new ColorIcon(16, 16);
					icon.setFillColor(
							new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
					icon.setOutlineColor(Color.DARK_GRAY);
					item.setIcon(icon);
					if (selectedItem == null && col.equals(fillEnt.getFillColour())) {
						selectedItem = item;
						selectedIndex = ind;
					}
					ind++;
					item.addActionListener(actionListener);
					item.addMouseListener(mouseListener);
					menu.add(item);
				}
				menu.addSeparator();

				// Colour chooser
				JMenuItem chooserItem = new JMenuItem(ColorEditor.OPTION_COLOUR_CHOOSER);
				chooserItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						JColorChooser chooser = ColorEditor.getColorChooser();
						JDialog dialog = JColorChooser.createDialog(null,
								ColorEditor.DIALOG_NAME,
								true,  //modal
								chooser,
								chooserActionListener,  //OK button listener
								null); //no CANCEL button listener
						dialog.setIconImage(GUIFrame.getWindowIcon());
						dialog.setAlwaysOnTop(true);
						Color4d col = fillEnt.getFillColour();
						chooser.setColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
						dialog.setVisible(true);
					}
				});
				menu.add(chooserItem);
				menu.addSeparator();

				// All possible colours
				for (Color4d col : ColourInput.namedColourList) {
					String colourName = ColourInput.toString(col);
					JMenuItem item = new JMenuItem(colourName);
					ColorIcon icon = new ColorIcon(16, 16);
					icon.setFillColor(
							new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
					icon.setOutlineColor(Color.DARK_GRAY);
					item.setIcon(icon);
					item.addActionListener(actionListener);
					item.addMouseListener(mouseListener);
					menu.add(item);
				}

				menu.show(fillColour, 0, fillColour.getPreferredSize().height);
				if (selectedItem != null) {
					menu.ensureIndexIsVisible(selectedIndex);
					selectedItem.setArmed(true);
				}
			}
		});

		buttonBar.add( fillColour );
	}

	private static void setFillColour(FillEntity fillEnt, String colName) {
		KeywordIndex kw = InputAgent.formatInput("FillColour", colName);
		Color4d col = Input.parseColour(kw);
		setFillColour(fillEnt, col);
	}

	private static void setFillColour(FillEntity fillEnt, Color4d col) {
		if (col.equals(fillEnt.getFillColour()))
			return;
		String colName = ColourInput.toString(col);
		KeywordIndex kw = InputAgent.formatInput("FillColour", colName);
		InputAgent.storeAndExecute(new KeywordCommand((Entity)fillEnt, kw));
	}

	// ******************************************************************************************************
	// TOOL BAR
	// ******************************************************************************************************

	/**
	 * Sets up the Control Panel's main tool bar.
	 */
	public void initializeMainToolBars() {

		// Insets used in setting the tool bar components
		Insets noMargin = new Insets( 0, 0, 0, 0 );
		Insets smallMargin = new Insets( 1, 1, 1, 1 );

		// Initialize the main tool bar
		JToolBar mainToolBar = new JToolBar();
		mainToolBar.setMargin( smallMargin );
		mainToolBar.setFloatable(false);
		mainToolBar.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );

		// Add the main tool bar to the display
		getContentPane().add( mainToolBar, BorderLayout.SOUTH );

		// Run/pause button
		addRunButton(mainToolBar, noMargin);

		Dimension separatorDim = new Dimension(11, controlStartResume.getPreferredSize().height);
		Dimension gapDim = new Dimension(5, separatorDim.height);

		// Reset button
		mainToolBar.add(Box.createRigidArea(gapDim));
		addResetButton(mainToolBar, noMargin);

		// Real time button
		mainToolBar.addSeparator(separatorDim);
		addRealTimeButton(mainToolBar, smallMargin);

		// Speed multiplier spinner
		mainToolBar.add(Box.createRigidArea(gapDim));
		addSpeedMultiplier(mainToolBar, noMargin);

		// Pause time field
		mainToolBar.addSeparator(separatorDim);
		mainToolBar.add(new JLabel("Pause Time:"));
		mainToolBar.add(Box.createRigidArea(gapDim));
		addPauseTime(mainToolBar, noMargin);

		// Simulation time display
		mainToolBar.addSeparator(separatorDim);
		addSimulationTime(mainToolBar, noMargin);

		// Run progress bar
		mainToolBar.add(Box.createRigidArea(gapDim));
		addRunProgress(mainToolBar, noMargin);

		// Remaining time display
		mainToolBar.add(Box.createRigidArea(gapDim));
		addRemainingTime(mainToolBar, noMargin);

		// Achieved speed multiplier
		mainToolBar.addSeparator(separatorDim);
		mainToolBar.add(new JLabel("Speed:"));
		addAchievedSpeedMultiplier(mainToolBar, noMargin);

		// Cursor position
		mainToolBar.addSeparator(separatorDim);
		mainToolBar.add(new JLabel("Position:"));
		addCursorPosition(mainToolBar, noMargin);
	}

	private void addRunButton(JToolBar mainToolBar, Insets margin) {
		runPressedIcon = new ImageIcon(GUIFrame.class.getResource("/resources/images/run-pressed-24.png"));
		pausePressedIcon = new ImageIcon(GUIFrame.class.getResource("/resources/images/pause-pressed-24.png"));

		controlStartResume = new RoundToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/run-24.png")));
		controlStartResume.setRolloverEnabled(true);
		controlStartResume.setRolloverIcon(new ImageIcon(GUIFrame.class.getResource("/resources/images/run-rollover-24.png")));
		controlStartResume.setPressedIcon(runPressedIcon);
		controlStartResume.setSelectedIcon(
				new ImageIcon(GUIFrame.class.getResource("/resources/images/pause-24.png")));
		controlStartResume.setRolloverSelectedIcon(
				new ImageIcon(GUIFrame.class.getResource("/resources/images/pause-rollover-24.png")));
		controlStartResume.setToolTipText(RUN_TOOLTIP);
		controlStartResume.setMargin(margin);
		controlStartResume.setEnabled( false );
		controlStartResume.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JToggleButton startResume = (JToggleButton)event.getSource();
				startResume.setEnabled(false);
				if(startResume.isSelected()) {
					boolean bool = GUIFrame.this.startSimulation();
					if (bool) {
						controlStartResume.setPressedIcon(pausePressedIcon);
					}
					else {
						startResume.setSelected(false);
						startResume.setEnabled(true);
					}
				}
				else {
					GUIFrame.this.pauseSimulation();
					controlStartResume.setPressedIcon(runPressedIcon);
				}
				controlStartResume.requestFocusInWindow();
			}
		} );
		mainToolBar.add( controlStartResume );
	}

	private void addResetButton(JToolBar mainToolBar, Insets margin) {
		controlStop = new RoundToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/reset-16.png")));
		controlStop.setToolTipText(formatToolTip("Reset",
				"Resets the simulation run time to zero."));
		controlStop.setPressedIcon(new ImageIcon(GUIFrame.class.getResource("/resources/images/reset-pressed-16.png")));
		controlStop.setRolloverEnabled( true );
		controlStop.setRolloverIcon(new ImageIcon(GUIFrame.class.getResource("/resources/images/reset-rollover-16.png")));
		controlStop.setMargin(margin);
		controlStop.setEnabled( false );
		controlStop.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if( getSimState() == SIM_STATE_RUNNING ) {
					GUIFrame.this.pauseSimulation();
				}
				boolean confirmed = GUIFrame.showConfirmStopDialog();
				if (!confirmed)
					return;

				GUIFrame.this.stopSimulation();
				initSpeedUp(0.0d);
			}
		} );
		mainToolBar.add( controlStop );
	}

	private void addRealTimeButton(JToolBar mainToolBar, Insets margin) {
		controlRealTime = new JToggleButton( " Real Time " );
		controlRealTime.setToolTipText(formatToolTip("Real Time Mode",
				"When selected, the simulation runs at a fixed multiple of wall clock time."));
		controlRealTime.setMargin(margin);
		controlRealTime.setFocusPainted(false);
		controlRealTime.setRequestFocusEnabled(false);
		controlRealTime.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				boolean bool = ((JToggleButton)event.getSource()).isSelected();
				InputAgent.applyBoolean(Simulation.getInstance(), "RealTime", bool);
			}
		});
		mainToolBar.add( controlRealTime );
	}

	private void addSpeedMultiplier(JToolBar mainToolBar, Insets margin) {
		SpinnerNumberModel numberModel =
				new SpinnerModel(Simulation.DEFAULT_REAL_TIME_FACTOR,
				   Simulation.MIN_REAL_TIME_FACTOR, Simulation.MAX_REAL_TIME_FACTOR, 1);
		spinner = new JSpinner(numberModel);
		// show up to 6 decimal places
		JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(spinner,"0.######");
		spinner.setEditor(numberEditor);

		// make sure spinner TextField is no wider than 9 digits
		int diff =
			((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().getPreferredSize().width -
			getPixelWidthOfString_ForFont("9", spinner.getFont()) * 9;
		Dimension dim = spinner.getPreferredSize();
		dim.width -= diff;
		spinner.setPreferredSize(dim);
		spinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged( ChangeEvent e ) {
				Double val = (Double)((JSpinner)e.getSource()).getValue();
				NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
				DecimalFormat df = (DecimalFormat)nf;
				df.applyPattern("0.######");
				InputAgent.applyArgs(Simulation.getInstance(), "RealTimeFactor", df.format(val));
			}
		});

		spinner.setToolTipText(formatToolTip("Speed Multiplier (up/down key)",
				"Target ratio of simulation time to wall clock time when Real Time mode is selected."));
		spinner.setEnabled(false);
		mainToolBar.add( spinner );
	}

	private void addPauseTime(JToolBar mainToolBar, Insets margin) {
		pauseTime = new JTextField("0000-00-00T00:00:00") {
			@Override
			protected void processFocusEvent(FocusEvent fe) {
				if (fe.getID() == FocusEvent.FOCUS_LOST) {
					GUIFrame.this.setPauseTime(this.getText());
				}
				else if (fe.getID() == FocusEvent.FOCUS_GAINED) {
					pauseTime.selectAll();
				}
				super.processFocusEvent( fe );
			}
		};

		pauseTime.setPreferredSize(new Dimension(pauseTime.getPreferredSize().width,
				pauseTime.getPreferredSize().height));

		pauseTime.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				GUIFrame.this.setPauseTime(pauseTime.getText());
				controlStartResume.requestFocusInWindow();
			}
		});

		pauseTime.setText("");
		pauseTime.setHorizontalAlignment(JTextField.RIGHT);
		pauseTime.setToolTipText(formatToolTip("Pause Time",
				"Time at which to pause the run, e.g. 3 h, 10 s, etc."));

		mainToolBar.add(pauseTime);
	}

	private void addSimulationTime(JToolBar mainToolBar, Insets margin) {
		clockDisplay = new JLabel( "", JLabel.CENTER );
		clockDisplay.setPreferredSize( new Dimension( 110, 16 ) );
		clockDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		clockDisplay.setToolTipText(formatToolTip("Simulation Time",
				"The present simulation time"));
		mainToolBar.add( clockDisplay );
	}

	private void addRunProgress(JToolBar mainToolBar, Insets margin) {
		progressBar = new JProgressBar( 0, 100 );
		progressBar.setPreferredSize( new Dimension( 120, controlRealTime.getPreferredSize().height ) );
		progressBar.setValue( 0 );
		progressBar.setStringPainted( true );
		progressBar.setToolTipText(formatToolTip("Run Progress",
				"Percent of the present simulation run that has been completed."));
		mainToolBar.add( progressBar );
	}

	private void addRemainingTime(JToolBar mainToolBar, Insets margin) {
		remainingDisplay = new JLabel( "", JLabel.CENTER );
		remainingDisplay.setPreferredSize( new Dimension( 110, 16 ) );
		remainingDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		remainingDisplay.setToolTipText(formatToolTip("Remaining Time",
				"The remaining time required to complete the present simulation run."));
		mainToolBar.add( remainingDisplay );
	}

	private void addAchievedSpeedMultiplier(JToolBar mainToolBar, Insets margin) {
		speedUpDisplay = new JLabel( "", JLabel.CENTER );
		speedUpDisplay.setPreferredSize( new Dimension( 110, 16 ) );
		speedUpDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		speedUpDisplay.setToolTipText(formatToolTip("Achieved Speed Multiplier",
				"The ratio of elapsed simulation time to elasped wall clock time that was achieved."));
		mainToolBar.add( speedUpDisplay );
	}

	private void addCursorPosition(JToolBar mainToolBar, Insets margin) {
		locatorPos = new JLabel( "", JLabel.CENTER );
		locatorPos.setPreferredSize( new Dimension( 140, 16 ) );
		locatorPos.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		locatorPos.setToolTipText(formatToolTip("Cursor Position",
				"The coordinates of the cursor on the x-y plane."));
		mainToolBar.add( locatorPos );
	}

	// ******************************************************************************************************
	// VIEW WINDOWS
	// ******************************************************************************************************

	private static class WindowMenu extends JMenu implements MenuListener {

		WindowMenu(String text) {
			super(text);
			this.addMenuListener(this);
		}

		@Override
		public void menuCanceled(MenuEvent arg0) {}

		@Override
		public void menuDeselected(MenuEvent arg0) {
			this.removeAll();
		}

		@Override
		public void menuSelected(MenuEvent arg0) {
			if (!RenderManager.isGood()) { return; }

			ArrayList<Integer> windowIDs = RenderManager.inst().getOpenWindowIDs();
			for (int id : windowIDs) {
				String windowName = RenderManager.inst().getWindowName(id);
				this.add(new WindowSelector(id, windowName));
			}
		}
	}

	private static class WindowSelector extends JMenuItem implements ActionListener {
		private final int windowID;

		WindowSelector(int windowID, String windowName) {
			this.windowID = windowID;
			this.setText(windowName);
			this.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!RenderManager.isGood()) { return; }

			RenderManager.inst().focusWindow(windowID);
		}
	}

	private static class NewRenderWindowMenu extends JMenu implements MenuListener {

		NewRenderWindowMenu(String text) {
			super(text);
			this.addMenuListener(this);
		}

		@Override
		public void menuSelected(MenuEvent e) {

			// 1) Select from the available view windows
			for (View view : View.getAll()) {
				this.add(new NewRenderWindowLauncher(view));
			}

			// 2) "Define New View" menu item
			this.addSeparator();
			this.add(new ViewDefiner());

			// 3) "Reset Positions and Sizes" menu item
			JMenuItem resetItem = new JMenuItem( "Reset Positions and Sizes" );
			resetItem.setMnemonic( 'R' );
			resetItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent e ) {
					for (View v : View.getAll()) {
						InputAgent.applyArgs(v, "WindowPosition");
						InputAgent.applyArgs(v, "WindowSize");
					}
				}
			} );
			this.addSeparator();
			this.add(resetItem);
		}

		@Override
		public void menuCanceled(MenuEvent arg0) {
		}

		@Override
		public void menuDeselected(MenuEvent arg0) {
			this.removeAll();
		}
	}
	private static class NewRenderWindowLauncher extends JMenuItem implements ActionListener {
		private final View view;

		NewRenderWindowLauncher(View v) {
			view = v;
			this.setText(view.getName());
			this.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!RenderManager.isGood()) {
				if (RenderManager.canInitialize()) {
					RenderManager.initialize(SAFE_GRAPHICS);
				} else {
					// A fatal error has occurred, don't try to initialize again
					return;
				}
			}
			KeywordIndex kw = InputAgent.formatArgs("ShowWindow", "TRUE");
			InputAgent.storeAndExecute(new KeywordCommand(view, kw));
			FrameBox.setSelectedEntity(view, false);
		}
	}

	private static class ViewDefiner extends JMenuItem implements ActionListener {
		ViewDefiner() {} {
			this.setText("Define New View");
			this.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!RenderManager.isGood()) {
				if (RenderManager.canInitialize()) {
					RenderManager.initialize(SAFE_GRAPHICS);
				} else {
					// A fatal error has occurred, don't try to initialize again
					return;
				}
			}

			String name = InputAgent.getUniqueName("View", "");
			IntegerVector winPos = null;
			Vec3d pos = null;
			Vec3d center = null;
			ArrayList<View> viewList = View.getAll();
			if (!viewList.isEmpty()) {
				View lastView = viewList.get(viewList.size()-1);
				winPos = lastView.getWindowPos();
				winPos.set(0, winPos.get(0) + VIEW_OFFSET);
				pos = lastView.getViewPosition();
				center = lastView.getViewCenter();
			}
			InputAgent.storeAndExecute(new DefineViewCommand(name, pos, center, winPos));
		}
	}

	public static void setActiveView(View activeView) {
		boolean lock2D = activeView.is2DLocked();
		instance.lockViewXYPlane.setSelected(lock2D);
	}

	// ******************************************************************************************************
	// RUN STATUS UPDATES
	// ******************************************************************************************************

	private long resumeSystemTime;
	private long lastSystemTime;
	private double lastSimTime;
	private double speedUp;

	public void initSpeedUp(double simTime) {
		resumeSystemTime = System.currentTimeMillis();
		lastSystemTime = resumeSystemTime;
		lastSimTime = simTime;
	}

	/**
	 * Sets the values for the simulation time, run progress, speedup factor,
	 * and remaining run time in the Control Panel's status bar.
	 *
	 * @param simTime - the present simulation time in seconds.
	 */
	void setClock(double simTime) {

		// Set the simulation time display
		String unit = Unit.getDisplayedUnit(TimeUnit.class);
		double factor = Unit.getDisplayedUnitFactor(TimeUnit.class);
		clockDisplay.setText(String.format("%,.2f  %s", simTime/factor, unit));

		// Set the run progress bar display
		long cTime = System.currentTimeMillis();
		double duration = Simulation.getRunDuration() + Simulation.getInitializationTime();
		double timeElapsed = simTime - Simulation.getStartTime();
		int progress = (int)(timeElapsed * 100.0d / duration);
		this.setProgress(progress);

		// Do nothing further if the simulation is not executing events
		if (getSimState() != SIM_STATE_RUNNING)
			return;

		// Set the speedup factor display
		if (cTime - lastSystemTime > 5000L || cTime - resumeSystemTime < 5000L) {
			long elapsedMillis = cTime - lastSystemTime;
			double elapsedSimTime = timeElapsed - lastSimTime;

			// Determine the speed-up factor
			speedUp = (elapsedSimTime * 1000.0d) / elapsedMillis;
			setSpeedUp(speedUp);
			if (elapsedMillis > 5000L) {
				lastSystemTime = cTime;
				lastSimTime = timeElapsed;
			}
		}

		// Set the remaining time display
		setRemaining( (duration - timeElapsed)/speedUp );
	}

	/**
	 * Displays the given value on the Control Panel's progress bar.
	 *
	 * @param val - the percent of the run that has completed.
	 */
	public void setProgress( int val ) {
		if (lastValue == val)
			return;

		progressBar.setValue( val );
		progressBar.repaint(25);
		lastValue = val;

		if (getSimState() >= SIM_STATE_CONFIGURED) {
			String title = String.format("%d%% %s - %s", val, Simulation.getModelName(), InputAgent.getRunName());
			setTitle(title);
		}
	}

	/**
	 * Write the given value on the Control Panel's speed up factor box.
	 *
	 * @param val - the speed up factor to write.
	 */
	public void setSpeedUp( double val ) {
		if (val == 0.0) {
			speedUpDisplay.setText("-");
		}
		else if (val >= 0.99) {
			speedUpDisplay.setText(String.format("%,.0f", val));
		}
		else {
			speedUpDisplay.setText(String.format("%,.6f", val));
		}
	}

	/**
	 * Write the given value on the Control Panel's remaining run time box.
	 *
	 * @param val - the remaining run time in seconds.
	 */
	public void setRemaining( double val ) {
		if (val == 0.0)
			remainingDisplay.setText("-");
		else if (val < 60.0)
			remainingDisplay.setText(String.format("%.0f seconds left", val));
		else if (val < 3600.0)
			remainingDisplay.setText(String.format("%.1f minutes left", val/60.0));
		else if (val < 3600.0*24.0)
			remainingDisplay.setText(String.format("%.1f hours left", val/3600.0));
		else if (val < 3600.0*8760.0)
			remainingDisplay.setText(String.format("%.1f days left", val/(3600.0*24.0)));
		else
			remainingDisplay.setText(String.format("%.1f years left", val/(3600.0*8760.0)));
	}

	// ******************************************************************************************************
	// SIMULATION CONTROLS
	// ******************************************************************************************************

	/**
	 * Starts or resumes the simulation run.
	 * @return true if the simulation was started or resumed; false if cancel or close was selected
	 */
	public boolean startSimulation() {
		if( getSimState() <= SIM_STATE_CONFIGURED ) {
		
		//
//		if(getSimState() == SIM_STATE_RUNNING ) return true; // Added by Jalal, if StartSimulation runs multiple time then nothing willl happen
			
			boolean confirmed = true;
			if (InputAgent.isSessionEdited()) {
				confirmed = GUIFrame.showSaveChangesDialog(this);
			}
			if (confirmed) {
				Simulation.start(currentEvt);
			}
			return confirmed;
		}
		else if( getSimState() == SIM_STATE_PAUSED ) {
			initSpeedUp(EventManager.ticksToSecs(simTicks));
			currentEvt.resume(currentEvt.secondsToNearestTick(Simulation.getPauseTime()));
			return true;
		}
		else
			throw new ErrorException( "Invalid Simulation State for Start/Resume" );
	}

	/**
	 * Pauses the simulation run.
	 */
	public void pauseSimulation() {
		if(getSimState() == SIM_STATE_PAUSED ) return;
		// Added by Jalal, if PauseSimulation runs multiple time then nothing willl happen
		if( getSimState() == SIM_STATE_RUNNING )
			currentEvt.pause();
		else
			throw new ErrorException( "Invalid Simulation State for pause" );
	}

	/**
	 * Stops the simulation run.
	 */
	public void stopSimulation() {
		if( getSimState() == SIM_STATE_RUNNING ||
		    getSimState() == SIM_STATE_PAUSED ||
		    getSimState() == SIM_STATE_ENDED) {
			Simulation.stop(currentEvt);
			FrameBox.stop();
			this.updateForSimulationState(GUIFrame.SIM_STATE_CONFIGURED);
		}
		else
			throw new ErrorException( "Invalid Simulation State for stop" );
	}

	/** model was executed, but no configuration performed */
	public static final int SIM_STATE_LOADED = 0;
	/** essential model elements created, no configuration performed */
	public static final int SIM_STATE_UNCONFIGURED = 1;
	/** model has been configured, not started */
	public static final int SIM_STATE_CONFIGURED = 2;
	/** model is presently executing events */
	public static final int SIM_STATE_RUNNING = 3;
	/** model has run, but presently is paused */
	public static final int SIM_STATE_PAUSED = 4;
	/** model is paused but cannot be resumed */
	public static final int SIM_STATE_ENDED = 5;

	private int simState;
	public int getSimState() {
		return simState;
	}

	EventManager currentEvt;
	public void setEventManager(EventManager e) {
		currentEvt = e;
	}

	public EventManager getEventManager() {
		return currentEvt;
	}

	public static void updateForSimState(int state) {
		GUIFrame inst = GUIFrame.getInstance();
		if (inst == null)
			return;

		inst.updateForSimulationState(state);
	}

	/**
	 * Sets the state of the simulation run to the given state value.
	 *
	 * @param state - an index that designates the state of the simulation run.
	 */
	void updateForSimulationState(int state) {
		simState = state;

		switch( getSimState() ) {
			case SIM_STATE_LOADED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < viewMenu.getItemCount(); i++ ) {
					if (viewMenu.getItem(i) == null)
						continue;
					viewMenu.getItem(i).setEnabled(true);
				}

				windowList.setEnabled( true );
				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				setSpeedUp(0);
				setRemaining(0);
				setProgress(0);
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText(RUN_TOOLTIP);
				controlStop.setEnabled( false );
				controlStop.setSelected( false );
				lockViewXYPlane.setEnabled( true );
				progressBar.setEnabled( false );
				break;

			case SIM_STATE_UNCONFIGURED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < viewMenu.getItemCount(); i++ ) {
					if (viewMenu.getItem(i) == null)
						continue;
					viewMenu.getItem(i).setEnabled(true);
				}

				windowList.setEnabled( true );
				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				setSpeedUp(0);
				setRemaining(0);
				setProgress(0);
				controlStartResume.setEnabled( false );
				controlStartResume.setSelected( false );
				controlStop.setSelected( false );
				controlStop.setEnabled( false );
				lockViewXYPlane.setEnabled( true );
				progressBar.setEnabled( false );
				break;

			case SIM_STATE_CONFIGURED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < viewMenu.getItemCount(); i++ ) {
					if (viewMenu.getItem(i) == null)
						continue;
					viewMenu.getItem(i).setEnabled(true);
				}

				windowList.setEnabled( true );
				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				setSpeedUp(0);
				setRemaining(0);
				setProgress(0);
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText(RUN_TOOLTIP);
				controlStop.setSelected( false );
				controlStop.setEnabled( false );
				lockViewXYPlane.setEnabled( true );
				progressBar.setEnabled( true );
				break;

			case SIM_STATE_RUNNING:
				speedUpDisplay.setEnabled( true );
				remainingDisplay.setEnabled( true );
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( true );
				controlStartResume.setToolTipText(PAUSE_TOOLTIP);
				controlStop.setEnabled( true );
				controlStop.setSelected( false );
				break;

			case SIM_STATE_PAUSED:
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText(RUN_TOOLTIP);
				controlStop.setEnabled( true );
				controlStop.setSelected( false );
				break;

			case SIM_STATE_ENDED:
				controlStartResume.setEnabled( false );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText(RUN_TOOLTIP);
				controlStop.setEnabled( true );
				controlStop.setSelected( false );
				break;

			default:
				throw new ErrorException( "Unrecognized Graphics State" );
		}
		fileMenu.setEnabled( true );
	}

	public void updateSaveButton() {
		fileSave.setEnabled(InputAgent.isSessionEdited());
	}

	public static synchronized void updateForRealTime(boolean executeRT, double factorRT) {
		GUIFrame inst = GUIFrame.getInstance();
		if (inst == null)
			return;

		inst.updateForRT(executeRT, factorRT);
	}

	/**
	 * updates RealTime button and Spinner
	 */
	private void updateForRT(boolean executeRT, double factorRT) {
		currentEvt.setExecuteRealTime(executeRT, factorRT);
		controlRealTime.setSelected(executeRT);
		spinner.setValue(factorRT);
		if (executeRT)
			spinner.setEnabled(true);
		else
			spinner.setEnabled(false);
	}

	/**
	 * updates PauseTime entry
	 */
	public void updateForPauseTime(String str) {
		pauseTime.setText(str);
	}

	/**
	 * Sets the PauseTime keyword for Simulation.
	 * @param str - value to assign.
	 */
	private void setPauseTime(String str) {
		Input<?> pause = Simulation.getInstance().getInput("PauseTime");
		String prevVal = pause.getValueString();
		if (prevVal.equals(str))
			return;

		// If the time is in RFC8601 format, enclose in single quotes
		if (str.contains("-") || str.contains(":"))
			if (Parser.needsQuoting(str) && !Parser.isQuoted(str))
				str = Parser.addQuotes(str);

		ArrayList<String> tokens = new ArrayList<>();
		Parser.tokenize(tokens, str, true);
		// if we only got one token, and it isn't RFC8601 - add a unit
		if (tokens.size() == 1 && !tokens.get(0).contains("-") && !tokens.get(0).contains(":"))
			tokens.add(Unit.getDisplayedUnit(TimeUnit.class));

		try {
			// Parse the keyword inputs
			KeywordIndex kw = new KeywordIndex("PauseTime", tokens, null);
			InputAgent.apply(Simulation.getInstance(), kw);
		}
		catch (InputErrorException e) {
			pauseTime.setText(prevVal);
			GUIFrame.showErrorDialog("Input Error", e.getMessage());
		}
	}

	public void updateForUndo() {
		undo.setEnabled(InputAgent.hasUndo());
		undoDropdown.setEnabled(InputAgent.hasUndo());
		redo.setEnabled(InputAgent.hasRedo());
		redoDropdown.setEnabled(InputAgent.hasRedo());
		GUIFrame.updateUI();
	}

	public void updateForSnapGridSpacing(String str) {
		gridSpacing.setText(str);
	}

	private void setSnapGridSpacing(String str) {
		Input<?> in = Simulation.getInstance().getInput("SnapGridSpacing");
		String prevVal = in.getValueString();
		if (prevVal.equals(str))
			return;

		if (str.isEmpty()) {
			gridSpacing.setText(prevVal);
			return;
		}

		try {
			KeywordIndex kw = InputAgent.formatInput("SnapGridSpacing", str);
			InputAgent.storeAndExecute(new KeywordCommand(Simulation.getInstance(), kw));
		}
		catch (InputErrorException e) {
			gridSpacing.setText(prevVal);
			GUIFrame.showErrorDialog("Input Error", e.getMessage());
		}
	}

	public void updateForSnapToGrid() {
		snapToGrid.setSelected(Simulation.isSnapToGrid());
		gridSpacing.setEnabled(Simulation.isSnapToGrid());
	}

	public static void setSelectedEntity(Entity ent) {
		if (instance == null)
			return;
		instance.setSelectedEnt(ent);
	}

	public void setSelectedEnt(Entity ent) {
		selectedEntity = ent;
		updateTextButtons();
		updateZButtons();
		updateLineButtons();
		updateFillButtons();
	}

	public void updateTextButtons() {
		boolean bool = selectedEntity instanceof TextEntity;

		boolean isAlignable = bool && !(selectedEntity instanceof OverlayText)
				&& !(selectedEntity instanceof BillboardText);
		alignLeft.setEnabled(isAlignable);
		alignCentre.setEnabled(isAlignable);
		alignRight.setEnabled(isAlignable);

		bold.setEnabled(bool);
		italic.setEnabled(bool);
		fontSelector.setEnabled(bool);
		textHeight.setEnabled(bool);
		largerText.setEnabled(bool);
		smallerText.setEnabled(bool);
		fontColour.setEnabled(bool);
		if (!bool) {
			fontSelector.setText("-");
			textHeight.setText(null);
			colourIcon.setFillColor(Color.LIGHT_GRAY);
			colourIcon.setOutlineColor(Color.LIGHT_GRAY);
			return;
		}

		TextEntity textEnt = (TextEntity) selectedEntity;
		int val = (int) Math.signum(((DisplayEntity) textEnt).getAlignment().x);
		alignLeft.setSelected(val == -1);
		alignCentre.setSelected(val == 0);
		alignRight.setSelected(val == 1);

		bold.setSelected(textEnt.isBold());
		italic.setSelected(textEnt.isItalic());
		fontSelector.setText(textEnt.getFontName());
		textHeight.setText(textEnt.getTextHeightString());

		Color4d col = textEnt.getFontColor();
		colourIcon.setFillColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
		colourIcon.setOutlineColor(Color.DARK_GRAY);
		fontColour.repaint();
	}

	public void updateZButtons() {
		boolean bool = selectedEntity instanceof DisplayEntity;
		bool = bool && !(selectedEntity instanceof OverlayEntity);
		bool = bool && !(selectedEntity instanceof BillboardText);
		increaseZ.setEnabled(bool);
		decreaseZ.setEnabled(bool);
	}

	public void updateLineButtons() {
		boolean bool = selectedEntity instanceof LineEntity;
		outline.setEnabled(bool && selectedEntity instanceof FillEntity);
		lineWidth.setEnabled(bool);
		lineColour.setEnabled(bool);
		if (!bool) {
			lineWidth.setValue(1);
			lineColourIcon.setFillColor(Color.LIGHT_GRAY);
			lineColourIcon.setOutlineColor(Color.LIGHT_GRAY);
			return;
		}

		LineEntity lineEnt = (LineEntity) selectedEntity;
		outline.setSelected(lineEnt.isOutlined());
		lineWidth.setEnabled(lineEnt.isOutlined());
		lineColour.setEnabled(lineEnt.isOutlined());

		lineWidth.setValue(Integer.valueOf(lineEnt.getLineWidth()));

		Color4d col = lineEnt.getLineColour();
		lineColourIcon.setFillColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
		lineColourIcon.setOutlineColor(Color.DARK_GRAY);
		lineColour.repaint();
	}

	public void updateFillButtons() {
		boolean bool = selectedEntity instanceof FillEntity;
		fill.setEnabled(bool);
		fillColour.setEnabled(bool);
		if (!bool) {
			fillColourIcon.setFillColor(Color.LIGHT_GRAY);
			fillColourIcon.setOutlineColor(Color.LIGHT_GRAY);
			return;
		}

		FillEntity fillEnt = (FillEntity) selectedEntity;
		fill.setSelected(fillEnt.isFilled());
		fillColour.setEnabled(fillEnt.isFilled());

		Color4d col = fillEnt.getFillColour();
		fillColourIcon.setFillColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
		fillColourIcon.setOutlineColor(Color.DARK_GRAY);
		fillColour.repaint();
	}

	private void setTextHeight(String str) {
		if (!(selectedEntity instanceof TextEntity))
			return;
		TextEntity textEnt = (TextEntity) selectedEntity;
		if (str.equals(textEnt.getTextHeightString()))
			return;

		try {
			KeywordIndex kw = InputAgent.formatInput("TextHeight", str);
			InputAgent.storeAndExecute(new KeywordCommand((Entity)textEnt, kw));
		}
		catch (InputErrorException e) {
			textHeight.setText(textEnt.getTextHeightString());
			GUIFrame.showErrorDialog("Input Error", e.getMessage());
		}
	}

	public static Image getWindowIcon() {
		return iconImage;
	}

	public void copyLocationToClipBoard(Vec3d pos) {
		String data = String.format("(%.3f, %.3f, %.3f)", pos.x, pos.y, pos.z);
		StringSelection stringSelection = new StringSelection(data);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents( stringSelection, null );
	}

	public static void showLocatorPosition(Vec3d pos) {
		GUIFrame inst = GUIFrame.getInstance();
		if (inst == null)
			return;

		inst.showLocator(pos);
	}

	private void showLocator(Vec3d pos) {
		if( pos == null ) {
			locatorPos.setText( "-" );
			return;
		}

		String unit = Unit.getDisplayedUnit(DistanceUnit.class);
		double factor = Unit.getDisplayedUnitFactor(DistanceUnit.class);
		locatorPos.setText(String.format((Locale)null, "%.3f  %.3f  %.3f  %s",
				pos.x/factor, pos.y/factor, pos.z/factor, unit));
	}

	public void enableSave(boolean bool) {
		saveConfigurationMenuItem.setEnabled(bool);
	}

	/**
	 * Sets variables used to determine the position and size of various
	 * windows based on the size of the computer display being used.
	 */
	private void calcWindowDefaults() {
		Dimension guiSize = this.getSize();
		Rectangle winSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

		COL1_WIDTH = 220;
		COL2_WIDTH = Math.min(520, (winSize.width - COL1_WIDTH) / 2);
		COL3_WIDTH = Math.min(420, winSize.width - COL1_WIDTH - COL2_WIDTH);
		VIEW_WIDTH = COL2_WIDTH + COL3_WIDTH;
		COL4_WIDTH = 520;

		COL1_START = this.getX();
		COL2_START = COL1_START + COL1_WIDTH;
		COL3_START = COL2_START + COL2_WIDTH;
		COL4_START = Math.min(COL3_START + COL3_WIDTH, winSize.width - COL4_WIDTH);

		HALF_TOP = (winSize.height - guiSize.height) / 2;
		HALF_BOTTOM = (winSize.height - guiSize.height - HALF_TOP);
		LOWER_HEIGHT = (winSize.height - guiSize.height) / 3;
		VIEW_HEIGHT = winSize.height - guiSize.height - LOWER_HEIGHT;

		TOP_START = this.getY() + guiSize.height;
		BOTTOM_START = TOP_START + HALF_TOP;
		LOWER_START = TOP_START + VIEW_HEIGHT;
	}

	/**
	 * Displays the view windows and tools on startup.
	 */
	public static void displayWindows() {

		// Show the view windows specified in the configuration file
		for (View v : View.getAll()) {
			if (v.showWindow())
				RenderManager.inst().createWindow(v);
		}

		// Set the initial state for the "Show Axes" check box
		DisplayEntity ent = (DisplayEntity) Entity.getNamedEntity("XYZ-Axis");
		if (ent == null) {
			xyzAxis.setEnabled(false);
			xyzAxis.setSelected(false);
		}
		else {
			xyzAxis.setEnabled(true);
			xyzAxis.setSelected(ent.getShow());
		}

		// Set the initial state for the "Show Grid" check box
		ent = (DisplayEntity) Entity.getNamedEntity("XY-Grid");
		grid.setSelected(ent != null && ent.getShow());
	}

	// ******************************************************************************************************
	// MAIN
	// ******************************************************************************************************


	public static Federate federate;
    
	public static void main(String args[] ) throws Exception
	{
		create(args);
	}
	
	public static GUIFrame create( String args[] ) throws Exception
	{
		// Process the input arguments and filter out directives
		ArrayList<String> configFiles = new ArrayList<>(args.length);
		boolean batch = false;
		boolean minimize = false;
		boolean quiet = false;
		boolean scriptMode = false;
		boolean headless = false;

//		int port = 0;
		
		for (String each : args) {
			// Batch mode
			if(each.startsWith("-fn:"))  // java ..... -w::c:/...
			{
				continue;
			}
			
			if (each.equalsIgnoreCase("-b") ||
			    each.equalsIgnoreCase("-batch")) {
				batch = true;
				continue;
			}
			// Script mode (command line I/O)
			if (each.equalsIgnoreCase("-s") ||
			    each.equalsIgnoreCase("-script")) {
				scriptMode = true;
				continue;
			}
			// z-buffer offset
			if (each.equalsIgnoreCase("-z") ||
			    each.equalsIgnoreCase("-zbuffer")) {
				// Parse the option, but do nothing
				continue;
			}
			// Minimize model window
			if (each.equalsIgnoreCase("-m") ||
			    each.equalsIgnoreCase("-minimize")) {
				minimize = true;
				continue;
			}
			// Minimize model window
			if (each.equalsIgnoreCase("-h") ||
			    each.equalsIgnoreCase("-headless")) {
				headless = true;
				batch = true;
				continue;
			}
			// Do not open default windows
			if (each.equalsIgnoreCase("-q") ||
			    each.equalsIgnoreCase("-quiet")) {
				quiet = true;
				continue;
			}
			if (each.equalsIgnoreCase("-sg") ||
			    each.equalsIgnoreCase("-safe_graphics")) {
				SAFE_GRAPHICS = true;
				continue;
			}
			// Not a program directive, add to list of config files
			configFiles.add(each);
		}

		InputAgent.setScriptMode(scriptMode);

		// If not running in batch mode, create the splash screen
		JWindow splashScreen = null;
		if (!batch) {
			URL splashImage = GUIFrame.class.getResource("/resources/images/splashscreen.png");
			ImageIcon imageIcon = new ImageIcon(splashImage);
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			int splashX = (screen.width - imageIcon.getIconWidth()) / 2;
			int splashY = (screen.height - imageIcon.getIconHeight()) / 2;

			// Set the window's bounds, centering the window
			splashScreen = new JWindow();
			splashScreen.setAlwaysOnTop(true);
			splashScreen.setBounds(splashX, splashY, imageIcon.getIconWidth(), imageIcon.getIconHeight());

			// Build the splash screen
			splashScreen.getContentPane().add(new JLabel(imageIcon));

			// Display it
			splashScreen.setVisible(true);
		}

		// create a graphic simulation
		LogBox.logLine("Loading Simulation Environment ... ");

		EventManager evt = new EventManager("DefaultEventManager");
		GUIFrame gui = null;
		if (!headless) {
			gui = GUIFrame.createInstance();
			gui.setEventManager(evt);
			gui.updateForSimulationState(SIM_STATE_LOADED);
			evt.setTimeListener(gui);
			evt.setErrorListener(gui);

			if (minimize)
				gui.setExtendedState(JFrame.ICONIFIED);
		}

		if (!batch && !headless) {
			// Begin initializing the rendering system
			RenderManager.initialize(SAFE_GRAPHICS);
		}

		LogBox.logLine("Simulation Environment Loaded");

		if (batch)
			InputAgent.setBatch(true);

		// Load the autoload file
		InputAgent.setRecordEdits(false);
		InputAgent.readResource("<res>/inputs/autoload.cfg");
		InputAgent.setPreDefinedEntityCount( Entity.getAll().get( Entity.getAll().size() - 1 ).getEntityNumber());

		// Show the Control Panel
		if (gui != null) {
			gui.setTitle(Simulation.getModelName());
			gui.setVisible(true);
			gui.calcWindowDefaults();
			gui.setLocation(gui.getX(), gui.getY());  //FIXME remove when setLocation is fixed for Windows 10
			Simulation.setWindowDefaults();
		}

		// Resolve all input arguments against the current working directory
		File user = new File(System.getProperty("user.dir"));
		// Process any configuration files passed on command line
		// (Multiple configuration files are not supported at present)
		for (int i = 0; i < configFiles.size(); i++) {
			//InputAgent.configure(gui, new File(configFiles.get(i)));
			File abs = new File((File)null, configFiles.get(i));
			File loadFile;
			if (abs.exists())
				loadFile = abs.getAbsoluteFile();
			else
				loadFile = new File(user, configFiles.get(i));

			Throwable t = GUIFrame.configure(loadFile);
			if (t != null) {
				// Hide the splash screen
				if (splashScreen != null) {
					splashScreen.dispose();
					splashScreen = null;
				}
				handleConfigError(t, loadFile);
			}
		}

		// If in script mode, load a configuration file from standard in
		if (scriptMode) {
			BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
			InputAgent.readBufferedStream(buf, null, "");
		}

		// If no configuration files were specified on the command line, then load the default configuration file
		if (configFiles.size() == 0 && !scriptMode) {
			InputAgent.setRecordEdits(true);
			InputAgent.loadDefault();
			GUIFrame.updateForSimState(GUIFrame.SIM_STATE_CONFIGURED);
		}

		// Show the view windows
		if(!quiet && !batch) {
			displayWindows();
		}

		// If in batch or quiet mode, close the any tools that were opened
		if (quiet || batch)
			Simulation.closeAllTools();

		// Set RecordEdits mode (if it has not already been set in the configuration file)
		InputAgent.setRecordEdits(true);

		// Start the model if in batch mode
		if (batch) {
			if (InputAgent.numErrors() > 0)
				GUIFrame.shutdown(0);
			Simulation.start(evt);
		}

		// Hide the splash screen
		if (splashScreen != null) {
			splashScreen.dispose();
			splashScreen = null;
		}

		// Bring the Control Panel to the front (along with any open Tools)
		if (gui != null)
			gui.toFront();

		// Set the selected entity to the Simulation object
		FrameBox.setSelectedEntity(Simulation.getInstance(), false);
		//
		//added by Jalal Possik
//		if(watchFile != null) new WatchChange(watchFile, gui).start();
//		new HTTPServer(port, gui);
		return gui;
	}

	/*
	 * this class is created so the next value will be value * 2 and the
	 * previous value will be value / 2
	 */
	public static class SpinnerModel extends SpinnerNumberModel {
		private double value;
		public SpinnerModel( double val, double min, double max, double stepSize) {
			super(val, min, max, stepSize);
		}

		@Override
		public Object getPreviousValue() {
			value = this.getNumber().doubleValue() / 2.0;
			if (value >= 1.0)
				value = Math.floor(value);

			// Avoid going beyond limit
			Double min = (Double)this.getMinimum();
			if (min.doubleValue() > value) {
				return min;
			}
			return value;
		}

		@Override
		public Object getNextValue() {
			value = this.getNumber().doubleValue() * 2.0;
			if (value >= 1.0)
				value = Math.floor(value);

			// Avoid going beyond limit
			Double max = (Double)this.getMaximum();
			if (max.doubleValue() < value) {
				return max;
			}
			return value;
		}
	}

	public static boolean getShuttingDownFlag() {
		return shuttingDown.get();
	}

	public static void shutdown(int errorCode) {

		shuttingDown.set(true);
		if (RenderManager.isGood()) {
			RenderManager.inst().shutdown();
		}

		System.exit(errorCode);
	}

	volatile long simTicks;

	private static class UIUpdater implements Runnable {
		private final GUIFrame frame;

		UIUpdater(GUIFrame gui) {
			frame = gui;
		}

		@Override
		public void run() {
			double callBackTime = EventManager.ticksToSecs(frame.simTicks);

			frame.setClock(callBackTime);
			FrameBox.updateEntityValues(callBackTime);
		}
	}

	@Override
	public void tickUpdate(long tick) {
		if (tick == simTicks)
			return;

		simTicks = tick;
		RenderManager.updateTime(tick);
		GUIFrame.updateUI();
	}

	@Override
	public void timeRunning(boolean running) {
		if (running) {
			updateForSimulationState(SIM_STATE_RUNNING);
		}
		else {
			int state = SIM_STATE_PAUSED;
			if (!Simulation.canResume(simTicks))
				state = SIM_STATE_ENDED;
			updateForSimulationState(state);
		}
	}

	@Override
	public void handleError(EventManager evt, Throwable t, long currentTick) {
		if (t instanceof OutOfMemoryError) {
			OutOfMemoryError e = (OutOfMemoryError)t;
			InputAgent.logMessage("Out of Memory use the -Xmx flag during execution for more memory");
			InputAgent.logMessage("Further debug information:");
			InputAgent.logMessage("%s", e.getMessage());
			InputAgent.logStackTrace(t);
			GUIFrame.shutdown(1);
			return;
		}
		else {
			double curSec = evt.ticksToSeconds(currentTick);
			InputAgent.logMessage("EXCEPTION AT TIME: %f s", curSec);
			InputAgent.logMessage("%s", t.getMessage());
			if (t.getCause() != null) {
				InputAgent.logMessage("Call Stack of original exception:");
				InputAgent.logStackTrace(t.getCause());
			}
			InputAgent.logMessage("Thrown exception call stack:");
			InputAgent.logStackTrace(t);
		}

		GUIFrame.showErrorDialog("Runtime Error",
				"JaamSim has detected the following runtime error condition:",
				t,
				"Programmers can find more information by opening the Log Viewer.\n"
						+ "The simulation run must be reset to zero simulation time before it "
						+ "can be restarted.");
	}

	void newModel() {
		currentEvt.pause();

		// check for unsaved changes
		if (InputAgent.isSessionEdited()) {
			boolean confirmed = GUIFrame.showSaveChangesDialog(GUIFrame.this);
			if (!confirmed) {
				return;
			}
		}

		clear();
		InputAgent.setRecordEdits(true);
		InputAgent.loadDefault();
		displayWindows();
		FrameBox.setSelectedEntity(Simulation.getInstance(), false);
	}

	void load() {
		currentEvt.pause();

		// check for unsaved changes
		if (InputAgent.isSessionEdited()) {
			boolean confirmed = GUIFrame.showSaveChangesDialog(GUIFrame.this);
			if (!confirmed) {
				return;
			}
		}

		LogBox.logLine("Loading...");

		Preferences prefs = Preferences.userRoot().node(getClass().getName());

		// Create a file chooser
		final JFileChooser chooser = new JFileChooser(prefs.get(LAST_USED_FOLDER,
				new File(".").getAbsolutePath()));

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(true);
		FileNameExtensionFilter cfgFilter =
				new FileNameExtensionFilter("JaamSim Configuration File (*.cfg)", "CFG");
		chooser.addChoosableFileFilter(cfgFilter);
		chooser.setFileFilter(cfgFilter);

		// Show the file chooser and wait for selection
		int returnVal = chooser.showOpenDialog(this);

		// Load the selected file
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            File temp = chooser.getSelectedFile();
			final GUIFrame gui1 = this;
    		final File chosenfile = temp;
			new Thread(new Runnable() {
				@Override
				public void run() {
					InputAgent.setRecordEdits(false);
					gui1.clear();
					Throwable ret = GUIFrame.configure(chosenfile);
					if (ret != null)
						handleConfigError(ret, chosenfile);

					InputAgent.setRecordEdits(true);

					GUIFrame.displayWindows();
					FrameBox.setSelectedEntity(Simulation.getInstance(), false);
				}
			}).start();

			prefs.put(LAST_USED_FOLDER, chosenfile.getParent());
        }
	}

	static Throwable configure(File file) {
		InputAgent.setConfigFile(file);
		GUIFrame.updateForSimState(GUIFrame.SIM_STATE_UNCONFIGURED);

		Throwable ret = null;
		try {
			InputAgent.loadConfigurationFile(file);
		}
		catch (Throwable t) {
			ret = t;
		}

		if (ret == null)
			LogBox.logLine("Configuration File Loaded");
		else
			LogBox.logLine("Configuration File Loaded - errors found");

		// show the present state in the user interface
		GUIFrame gui = GUIFrame.getInstance();
		if (gui != null) {
			gui.setProgress(0);
			gui.setTitle( Simulation.getModelName() + " - " + InputAgent.getRunName() );
			gui.updateForSimulationState(GUIFrame.SIM_STATE_CONFIGURED);
			gui.enableSave(InputAgent.getRecordEditsFound());
		}
		return ret;
	}

	static void handleConfigError(Throwable t, File file) {
		if (t instanceof InputErrorException) {
			InputAgent.logMessage("Input Error: %s", t.getMessage());
			GUIFrame.showErrorOptionDialog("Input Error",
			                         "Input errors were detected while loading file: '%s'\n\n%s\n\n" +
			                         "Open '%s' with Log Viewer?",
			                         file.getName(), t.getMessage(), InputAgent.getRunName() + ".log");
			return;
		}

		InputAgent.logMessage("Fatal Error while loading file '%s': %s\n", file.getName(), t.getMessage());
		GUIFrame.showErrorDialog("Fatal Error",
				String.format("A fatal error has occured while loading the file '%s':", file.getName()),
				t.getMessage(),
				"");
	}

	/**
	 * Saves the configuration file.
	 * @param fileName = absolute file path and file name for the file to be saved
	 */
	private void setSaveFile(String fileName) {

		// Set root directory
		File temp = new File(fileName);

		// Save the configuration file
		try {
			InputAgent.printNewConfigurationFileWithName( fileName );
			InputAgent.setConfigFile(temp);

			// Set the title bar to match the new run name
			this.setTitle( Simulation.getModelName() + " - " + InputAgent.getRunName() );
		}
		catch (Exception e) {
			GUIFrame.showErrorDialog("File Error", e.getMessage());
		}
	}

	boolean save() {
		LogBox.logLine("Saving...");
		if( InputAgent.getConfigFile() != null ) {
			setSaveFile(InputAgent.getConfigFile().getPath());
			return true;
		}

		boolean confirmed = saveAs();
		return confirmed;
	}

	boolean saveAs() {
		LogBox.logLine("Save As...");

		Preferences prefs = Preferences.userRoot().node(getClass().getName());

		// Create a file chooser
		final JFileChooser chooser = new JFileChooser(prefs.get(LAST_USED_FOLDER,
				new File(".").getAbsolutePath()));

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(true);
		FileNameExtensionFilter cfgFilter =
				new FileNameExtensionFilter("JaamSim Configuration File (*.cfg)", "CFG");
		chooser.addChoosableFileFilter(cfgFilter);
		chooser.setFileFilter(cfgFilter);
		chooser.setSelectedFile(InputAgent.getConfigFile());

		// Show the file chooser and wait for selection
		int returnVal = chooser.showSaveDialog(this);

		if (returnVal != JFileChooser.APPROVE_OPTION)
			return false;

		File file = chooser.getSelectedFile();
		String filePath = file.getPath();

		// Add the file extension ".cfg" if needed
		filePath = filePath.trim();
		if (file.getName().trim().indexOf('.') == -1) {
			filePath = filePath.concat(".cfg");
		}

		// Confirm overwrite if file already exists
		File temp = new File(filePath);
		if (temp.exists()) {
			boolean confirmed = GUIFrame.showSaveAsDialog(file.getName());
			if (!confirmed) {
				return false;
			}
		}

		// Save the configuration file
		setSaveFile(filePath);

		prefs.put(LAST_USED_FOLDER, file.getParent());
		return true;
	}

	public static String getImageFolder() {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		String def = prefs.get(LAST_USED_FOLDER, new File(".").getAbsolutePath());
		return prefs.get(LAST_USED_IMAGE_FOLDER, def);
	}

	public static void setImageFolder(String path) {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		prefs.put(LAST_USED_IMAGE_FOLDER, path);
	}

	public static String get3DFolder() {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		String def = prefs.get(LAST_USED_FOLDER, new File(".").getAbsolutePath());
		return prefs.get(LAST_USED_3D_FOLDER, def);
	}

	public static void set3DFolder(String path) {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		prefs.put(LAST_USED_3D_FOLDER, path);
	}

	// ******************************************************************************************************
	// DIALOG BOXES
	// ******************************************************************************************************

	/**
	 * Shows the "Confirm Save As" dialog box
	 * @param fileName - name of the file to be saved
	 * @return true if the file is to be overwritten.
	 */
	public static boolean showSaveAsDialog(String fileName) {
		int userOption = JOptionPane.showConfirmDialog(null,
				String.format("The file '%s' already exists.\n" +
						"Do you want to replace it?", fileName),
				"Confirm Save As",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		return (userOption == JOptionPane.YES_OPTION);
	}

	/**
	 * Shows the "Confirm Stop" dialog box.
	 * @return true if the run is to be stopped.
	 */
	public static boolean showConfirmStopDialog() {
		int userOption = JOptionPane.showConfirmDialog( null,
				"WARNING: Are you sure you want to reset the simulation time to 0?",
				"Confirm Reset",
				JOptionPane.YES_OPTION,
				JOptionPane.WARNING_MESSAGE );
		return (userOption == JOptionPane.YES_OPTION);
	}

	/**
	 * Shows the "Save Changes" dialog box
	 * @return true for any response other than Cancel or Close.
	 */
	public static boolean showSaveChangesDialog(GUIFrame gui) {

		String message;
		if (InputAgent.getConfigFile() == null)
			message = "Do you want to save the changes you made?";
		else
			message = String.format("Do you want to save the changes you made to '%s'?", InputAgent.getConfigFile().getName());

		Object[] options = {"Save", "Don't Save", "Cancel"};
		int userOption = JOptionPane.showOptionDialog( null,
				message,
				"Save Changes",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE,
				null,
				options,
				options[0]);

		if (userOption == JOptionPane.YES_OPTION) {
			boolean confirmed = gui.save();
			return confirmed;
		}
		else if (userOption == JOptionPane.NO_OPTION)
			return true;

		return false;
	}

	private static String getErrorMessage(String source, int position, String pre, String message, String post) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");

		// Initial text prior to the message
		if (!pre.isEmpty()) {
			sb.append(html_replace(pre)).append("<br><br>");
		}

		// Error message
		sb.append(html_replace(message)).append("<br>");

		// Append the source expression and an 'arrow' pointing at the error
		if (!source.isEmpty()) {
			sb.append("<pre><font color=\"red\">");
			sb.append(html_replace(source)).append("<br>");
			for (int i = 0; i < position; ++i) {
				sb.append(" ");
			}
			sb.append("<b>^</b></font></pre>");
		}

		// Final text after the message
		if (!post.isEmpty()) {
			if (source.isEmpty()) {
				sb.append("<br>");
			}
			sb.append(html_replace(post));
		}

		sb.append("</html>");
		return sb.toString();
	}

	/**
	 * Displays the Error Message dialog box
	 * @param title - text for the dialog box name
	 * @param source - expression that cause the error (if applicable)
	 * @param position - location of the error in the expression (if applicable)
	 * @param pre - text to appear prior to the error message
	 * @param message - error message
	 * @param post - text to appear after the error message
	 */
	public static void showErrorDialog(String title, String source, int position, String pre, String message, String post) {
		if (InputAgent.getBatch()) GUIFrame.shutdown(1);
		String msg = GUIFrame.getErrorMessage(source, position, pre, message, post);
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
	}

	public static void showErrorDialog(String title, String pre, String message, String post) {
		GUIFrame.showErrorDialog(title, "", -1, pre, message, post);
	}

	public static void showErrorDialog(String title, String message) {
		GUIFrame.showErrorDialog(title, "", -1, "", message, "");
	}

	public static void showErrorDialog(String title, String pre, Throwable t, String post) {
		if (t instanceof InputErrorException) {
			InputErrorException e = (InputErrorException)t;
			GUIFrame.showErrorDialog(title, e.source, e.position, pre, e.getMessage(), post);
			return;
		}
		if (t instanceof ErrorException) {
			ErrorException e = (ErrorException)t;
			GUIFrame.showErrorDialog(title, e.source, e.position, pre, e.getMessage(), post);
			return;
		}
		String message = t.getMessage();
		if (message == null)
			message = "null";
		GUIFrame.showErrorDialog(title, "", -1, pre, message, post);
	}

	/**
	 * Shows the Error Message dialog box from a non-Swing thread
	 * @param title - text for the dialog box name
	 * @param fmt - format string for the error message
	 * @param args - inputs to the error message
	 */
	public static void invokeErrorDialog(String title, String fmt, Object... args) {
		final String msg = String.format(fmt,  args);
		SwingUtilities.invokeLater(new RunnableError(title, msg));
	}

	private static class RunnableError implements Runnable {
		private final String title;
		private final String message;

		public RunnableError(String t, String m) {
			title = t;
			message = m;
		}

		@Override
		public void run() {
			GUIFrame.showErrorDialog(title, message);
		}
	}

	/**
	 * Shows the Error Message dialog box with option to open the Log Viewer
	 * @param title - text for the dialog box name
	 * @param fmt - format string for the error message
	 * @param args - inputs to the error message
	 */
	public static void showErrorOptionDialog(String title, String fmt, Object... args) {
		if (InputAgent.getBatch()) GUIFrame.shutdown(1);

		final String msg = String.format(fmt,  args);

		Object[] options = {"Yes", "No"};
		int userOption = JOptionPane.showOptionDialog(null,
				msg,
				title,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.ERROR_MESSAGE,
				null,
				options,
				options[0]);

		if (userOption == JOptionPane.YES_OPTION) {
			InputAgent.applyBoolean(Simulation.getInstance(), "ShowLogViewer", true);
		}
	}

	/**
	 * Shows the Error Message dialog box for the Input Editor
	 * @param title - text for the dialog box name
	 * @param pre - text to appear before the error message
	 * @param e - input error object
	 * @param post - text to appear after the error message
	 * @return true if the input is to be re-edited
	 */
	public static boolean showErrorEditDialog(String title, String pre, InputErrorException e, String post) {
		String msg = GUIFrame.getErrorMessage(e.source, e.position,
				"Input error:",
				e.getMessage(),
				"Do you want to continue editing, or reset the input?");
		String[] options = { "Edit", "Reset" };
		int reply = JOptionPane.showOptionDialog(null, msg, "Input Error", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.ERROR_MESSAGE, null, options, options[0]);
		return (reply == JOptionPane.OK_OPTION);
	}

	// ******************************************************************************************************
	// TOOL TIPS
	// ******************************************************************************************************
	private static final Pattern amp = Pattern.compile("&");
	private static final Pattern lt = Pattern.compile("<");
	private static final Pattern gt = Pattern.compile(">");
	private static final Pattern br = Pattern.compile("\n");

	private static final String html_replace(String str) {
		String desc = str;
		desc = amp.matcher(desc).replaceAll("&amp;");
		desc = lt.matcher(desc).replaceAll("&lt;");
		desc = gt.matcher(desc).replaceAll("&gt;");
		desc = br.matcher(desc).replaceAll("<BR>");
		return desc;
	}

	/**
	 * Returns the HTML code for pop-up tooltips in the Model Builder and Control Panel.
	 * @param name - name of the item whose tooltip is to be generated
	 * @param description - text describing the item's function
	 * @return HTML for the tooltip
	 */
	public static String formatToolTip(String name, String desc) {
		return String.format("<html><p width=\"200px\"><b>%s</b><br>%s</p></html>",
				name, desc);
	}

	/**
	 * Returns the HTML code for a keyword's pop-up tooltip in the Input Editor.
	 * @param className - object whose keyword tooltip is to be displayed
	 * @param keyword - name of the keyword
	 * @param description - description of the keyword
	 * @param exampleList - a list of examples that show how the keyword can be used
	 * @return HTML for the tooltip
	 */
	public static String formatKeywordToolTip(String className, String keyword,
			String description, String validInputs, String... exampleList) {

		StringBuilder sb = new StringBuilder("<html><p width=\"350px\">");

		// Keyword name
		String key = html_replace(keyword);
		sb.append("<b>").append(key).append("</b><br>");

		// Description
		String desc = html_replace(description);
		sb.append(desc).append("<br><br>");

		// Valid Inputs
		if (validInputs != null) {
			sb.append(validInputs).append("<br><br>");
		}

		// Examples
		if (exampleList.length > 0) {
			sb.append("<u>Examples:</u>");
		}
		for (int i=0; i<exampleList.length; i++) {
			String item = html_replace(exampleList[i]);
			sb.append("<br>").append(item);
		}

		sb.append("</p></html>");
		return sb.toString();
	}

	/**
	 * Returns the HTML code for an output's pop-up tooltip in the Output Viewer.
	 * @param name - name of the output
	 * @param description - description of the output
	 * @return HTML for the tooltip
	 */
	public static String formatOutputToolTip(String name, String description) {
		String desc = html_replace(description);
		return String.format("<html><p width=\"250px\"><b>%s</b><br>%s</p></html>",
				name, desc);
	}

	//added by Jalal
	public void setRealTimeFactor(double val) 
	{
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		DecimalFormat df = (DecimalFormat)nf;
		df.applyPattern("0.######");
		InputAgent.applyArgs(Simulation.getInstance(), "RealTimeFactor", df.format(val));
	}
	//till here 

}
