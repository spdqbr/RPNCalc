package com.spdqbr.rpn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

public class RPNCalc extends JFrame implements KeyListener{
	private static final long serialVersionUID = 1L;
	private static final String SETTINGS_FILE = Paths.get(System.getProperty("user.home"), "rpncalc.properties").toString();
	private Properties properties = new Properties();
	
	public static final int MIN_FONT_SIZE = 8;
	
	private JTextField[] stackDisplay = new JTextField[40];
	private JTextField inputDisplay;
	private Stack<BigDecimal> stack = new Stack<>();
	StringBuilder mantissa = new StringBuilder("########");
	private DecimalFormat format = new DecimalFormat("#,##0."+mantissa.toString());
	private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	private JScrollBar vertical;
    private int currentFontSize = 16;
    private final int defaultFontSize = 16;
	
	public static void main(String[] args) {
		new RPNCalc();
	}
	
	public RPNCalc() {
		super("RPNCalc - Spdqbr");
		setSize(200,200);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	    addWindowListener(new WindowAdapter() {
	        @Override
	        public void windowClosing(WindowEvent e) {
	            saveSettings();
	            System.exit(0);
	        }
	    });
		
		JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem increaseFontSize = new JMenuItem("Increase Font Size (ctrl+'+'");
        JMenuItem decreaseFontSize = new JMenuItem("Decrease Font Size (ctrl+'-'");
        JMenuItem resetFontSize = new JMenuItem("Reset Font Size");

        increaseFontSize.addActionListener(e -> changeFontSize(currentFontSize + 4));
        decreaseFontSize.addActionListener(e -> changeFontSize(currentFontSize - 4));
        resetFontSize.addActionListener(e -> changeFontSize(defaultFontSize));
        
        settingsMenu.add(increaseFontSize);
        settingsMenu.add(decreaseFontSize);
        settingsMenu.add(resetFontSize);
        menuBar.add(settingsMenu);
        
        JMenuItem clearMenu = new JMenuItem("Clear");
        clearMenu.addActionListener(e -> clearStackAndInput());
        
        menuBar.add(clearMenu);
        
        setJMenuBar(menuBar);
		
		JPanel panel = new JPanel(new BorderLayout());
		JPanel stackPanel = new JPanel(new GridLayout(stackDisplay.length,1));
		
		for(int i = 0; i < stackDisplay.length; i++) {
			stackDisplay[i] = new JTextField();
			stackDisplay[i].setEditable(false);
			stackDisplay[i].setForeground(Color.GREEN);
			stackDisplay[i].setBackground(Color.BLACK);
			stackDisplay[i].setHorizontalAlignment(SwingConstants.RIGHT);
			stackDisplay[i].setBorder(BorderFactory.createEmptyBorder());
		}
		
		for(int i = 0; i < stackDisplay.length; i++) {
			stackPanel.add(stackDisplay[stackDisplay.length - 1 - i]);
		}
		
		JScrollPane scrollPane = new JScrollPane(stackPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		vertical = scrollPane.getVerticalScrollBar();
		vertical.setValue(vertical.getMaximum());
		
		panel.add(scrollPane, BorderLayout.CENTER);
		
		inputDisplay = new JTextField();
		inputDisplay.setBackground(Color.BLACK);
		inputDisplay.setForeground(Color.GREEN);
		inputDisplay.setHorizontalAlignment(SwingConstants.RIGHT);
		inputDisplay.addKeyListener(this);
		panel.add(inputDisplay, BorderLayout.SOUTH);
		
		add(panel);
		setVisible(true);
		inputDisplay.requestFocusInWindow();
		
		this.addComponentListener(new ComponentListener() {

			@Override
			public void componentResized(ComponentEvent e) {
				vertical.setValue(vertical.getMaximum());
				display();
			}

			@Override
			public void componentMoved(ComponentEvent e) {}
			
			@Override
			public void componentShown(ComponentEvent e) {}
			@Override
			public void componentHidden(ComponentEvent e) {}
			
		});
		
		setupHotkeys();
		
	    inputDisplay.addMouseWheelListener(new MouseWheelListener() {
	        @Override
	        public void mouseWheelMoved(MouseWheelEvent e) {
	            if (e.isControlDown()) {
	                int notches = e.getWheelRotation();
	                if (notches < 0) {
	                    changeFontSize(currentFontSize + 4);
	                } else {
	                	if(currentFontSize > MIN_FONT_SIZE + 4) {
	                		changeFontSize(currentFontSize - 4);
	                	}
	                }
	                e.consume();
	            }
	        }
	    });
	    
	    scrollPane.addMouseWheelListener(inputDisplay.getMouseWheelListeners()[0]);
		loadSettings();
	}
	
	private void clearStackAndInput() {
	    stack.clear();
	    inputDisplay.setText("");
	    display();
	}
	
	private void loadSettings() {
	    try (FileInputStream input = new FileInputStream(SETTINGS_FILE)) {
	        properties.load(input);
	        // Load font size
	        String fontSizeStr = properties.getProperty("fontSize", "12"); // Default size
	        currentFontSize = Integer.parseInt(fontSizeStr);
	        
	        // Load window size
	        String windowWidthStr = properties.getProperty("windowWidth", "200");
	        String windowHeightStr = properties.getProperty("windowHeight", "200");
	        setSize(Integer.parseInt(windowWidthStr), Integer.parseInt(windowHeightStr));
	        
	        // Load window position
	        String windowXStr = properties.getProperty("windowX", "100"); // Default x position
	        String windowYStr = properties.getProperty("windowY", "100"); // Default y position
	        setLocation(Integer.parseInt(windowXStr), Integer.parseInt(windowYStr));
	        
	        // Load stack contents
	        String stackStr = properties.getProperty("stack", "");
	        if (!stackStr.isEmpty()) {
	            String[] stackArray = stackStr.split(",");
	            for (String s : stackArray) {
	                if (!s.isEmpty()) {
	                    stack.push(new BigDecimal(s.trim()));
	                }
	            }
	        }
	        
	        // Load input display value
	        String inputValue = properties.getProperty("inputDisplay", "");
	        inputDisplay.setText(inputValue);
	        
	        // Apply font size to input display and stack display
	        changeFontSize(currentFontSize);
	    } catch (IOException e) {
	        System.err.println("Error loading settings: " + e.getMessage());
	    } catch (NumberFormatException e) {
	        System.err.println("Invalid number format in settings: " + e.getMessage());
	    }
	}

	private void saveSettings() {
	    try (FileOutputStream output = new FileOutputStream(SETTINGS_FILE)) {
	        properties.setProperty("fontSize", String.valueOf(currentFontSize));
	        properties.setProperty("windowWidth", String.valueOf(getWidth()));
	        properties.setProperty("windowHeight", String.valueOf(getHeight()));
	        properties.setProperty("windowX", String.valueOf(getX()));
	        properties.setProperty("windowY", String.valueOf(getY()));
	        
	        StringBuilder stackBuilder = new StringBuilder();
	        for (BigDecimal bd : stack) {
	            stackBuilder.append(bd.toString()).append(",");
	        }
	        properties.setProperty("stack", stackBuilder.toString());
	        
	        properties.setProperty("inputDisplay", inputDisplay.getText());
	        
	        properties.store(output, "RPNCalc Settings");
	    } catch (IOException e) {
	        System.err.println("Error saving settings: " + e.getMessage());
	    }
	}
	
    private void changeFontSize(int newSize) {
        if (newSize < MIN_FONT_SIZE) return;

        currentFontSize = newSize;

        Font newFont = new Font(inputDisplay.getFont().getName(), Font.PLAIN, currentFontSize);

        inputDisplay.setFont(newFont);

        for (JTextField display : stackDisplay) {
            display.setFont(newFont);
        }
        
        this.setSize(new Dimension(this.getWidth()+1, this.getHeight()));
        this.setSize(new Dimension(this.getWidth()-1, this.getHeight()));
    }
    
    private void setupHotkeys() {
        // Get the input map and action map of the root pane (for global hotkeys)
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // Define hotkey for "Ctrl + +" to increase font size
        KeyStroke increaseKey = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK);
        KeyStroke increaseKeyNumpad = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK);
        inputMap.put(increaseKey, "increaseFontSize");
        inputMap.put(increaseKeyNumpad, "increaseFontSize");
        actionMap.put("increaseFontSize", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(currentFontSize + 4);
            }
        });

        // Define hotkey for "Ctrl + -" to decrease font size
        KeyStroke decreaseKey = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK);
        KeyStroke decreaseKeyNumpad = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_DOWN_MASK);
        inputMap.put(decreaseKey, "decreaseFontSize");
        inputMap.put(decreaseKeyNumpad, "decreaseFontSize");
        actionMap.put("decreaseFontSize", new AbstractAction() {
        	private static final long serialVersionUID = 1L;
        	
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(currentFontSize - 4);
            }
        });
    }
	
	private void display() {
	    for (int i = 0; i < stackDisplay.length; i++) {
	        if (i < stack.size()) {
	            String formattedNumber = format.format(stack.get(stack.size() - 1 - i));

	            // Dynamically calculate visible characters
	            int visibleChars = getVisibleCharacters(stackDisplay[i]);

	            if (formattedNumber.length() > visibleChars) {
	                // Truncate and prepend the indicator "<"
	                stackDisplay[i].setText("<" + formattedNumber.substring(formattedNumber.length() - visibleChars + 1));
	            } else {
	                stackDisplay[i].setText(formattedNumber);
	            }
	        } else {
	            stackDisplay[i].setText("");
	        }
	    }
	}

	private int getVisibleCharacters(JTextField textField) {
	    return textField.getWidth() / textField.getFontMetrics(textField.getFont()).charWidth('0'); // Estimate based on '0'
	}
	
	private void push() {
		if(!inputDisplay.getText().isBlank()) {
			push(new BigDecimal(inputDisplay.getText().replaceAll(",", "")));
			inputDisplay.setText("");
			vertical.setValue(vertical.getMaximum());
		}
	}
	
	private void push(BigDecimal d) {
		stack.push(d);
		display();
	}
	
	private BigDecimal pop() {
		if(stack.size() > 0) {
			BigDecimal out = stack.pop();
			display();
			return out;
		}
		return null;
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		BigDecimal a, b;
		int key = (int) e.getKeyChar();
		switch (key) {
			case 'c':
				clearStackAndInput();
				e.consume();
				break;
			case 127: // delete
				if (inputDisplay.getText().isBlank()) {
					pop();
				}else {
					inputDisplay.setText("");
					vertical.setValue(vertical.getMaximum());
				}
				break;
			case 10: // return
				if (inputDisplay.getText().isBlank()) {
					if (stack.size() > 0) {
						a = stack.get(stack.size() - 1).add(BigDecimal.ZERO);
						push(a);
					}
				} else {
					push();
				}
				break;
			case '+':
			case '-':
			case '*':
			case '/':
			case '^':
				push();
				if(stack.size() >=2) {
					a = pop();
					b = pop();
					switch (key) {
						case '+': push(b.add(a)); break;
						case '-': push(b.subtract(a)); break;
						case '*': push(b.multiply(a)); break;
						case '/': push(b.divide(a, 16, RoundingMode.HALF_UP)); break;
						case '^': push(b.pow(a.intValue())); break; //TODO: support bigdecimal pow
					}
				}
				inputDisplay.setText("");
				vertical.setValue(vertical.getMaximum());
				e.consume();
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case '.':
				break;
			default:
				e.consume();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {
		BigDecimal a, b;
		int code = e.getKeyCode();
		switch(code) {
			case 40: // Down arrow, negate
				if (inputDisplay.getText().isBlank()) {
					if(stack.size() >= 1) {
						a = pop();
						push(BigDecimal.ZERO.subtract(a));
					}
				} else {
					a = new BigDecimal(inputDisplay.getText());
					a = BigDecimal.ZERO.subtract(a);
					inputDisplay.setText(a.toString());					
				}
				e.consume();
				break;
			case 38: // Up arrow, swap bottom two entries
				if (stack.size() >= 2) {
					a = pop();
					b = pop();
					push(a);
					push(b);
				}
				e.consume();
				break;
			case 37: // Left arrow, add decimal places
				mantissa.append("#");
				format = new DecimalFormat("#,###."+mantissa);
				display();
				e.consume();
				break;
			case 39: // Right arrow, remove decimal places
				if(mantissa.length() > 1) {
					mantissa.deleteCharAt(0);
					format = new DecimalFormat("#,###."+mantissa);
					display();
				}
				e.consume();
				break;
			case 33: // Page up
			case 34: // Page down
				break;
			case 67: // ctrl+c
				String clip;
				if(!inputDisplay.getText().isBlank()) {
					clip = inputDisplay.getText().replaceAll(",", "");
				}else if(stack.size() > 0) {
					clip = format.format(stack.get(stack.size() - 1)).replaceAll(",", "");
				}else {
					clip = "";
				}
				clipboard.setContents(new StringSelection(clip), null);
				e.consume();
				break;
		}
	}
}
