package org.bbop.swing.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.text.JTextComponent;

import org.bbop.swing.autocomplete.AutocompleteModel;
import org.bbop.swing.autocomplete.MatchPair;
import org.bbop.swing.autocomplete.StringListAutocompleteModel;
import org.bbop.util.StringUtil;
import org.bbop.util.TaskDelegate;

import org.apache.log4j.*;

public class AutocompleteBox<T> extends JComboBox {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(AutocompleteBox.class);

	protected class AutoTextField extends JTextField implements ComboBoxEditor {

		protected boolean updating = false;

		protected Runnable updateRunnable = new Runnable() {
			public void run() {
				update();
			}
		};

		protected void updateLater() {
			SwingUtilities.invokeLater(updateRunnable);
		}

		DocumentListener docListener = new DocumentListener() {

			public void changedUpdate(DocumentEvent e) {
				updateLater();
			}

			public void insertUpdate(DocumentEvent e) {
				updateLater();
			}

			public void removeUpdate(DocumentEvent e) {
				updateLater();
			}
		};

		/*
		 * KeyListener keyListener = new KeyListener() {
		 * 
		 * public void keyPressed(KeyEvent e) { if (e.getKeyCode() ==
		 * KeyEvent.VK_ENTER) commit(); else if (!e.isActionKey()) update(); }
		 * 
		 * public void keyReleased(KeyEvent e) { if (e.getKeyCode() ==
		 * KeyEvent.VK_ENTER) return; else if (!e.isActionKey()) update(); }
		 * 
		 * public void keyTyped(KeyEvent e) { } };
		 */

		protected KeyListener keyListener = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE
						&& ((e.getModifiers() & Toolkit.getDefaultToolkit()
								.getMenuShortcutKeyMask()) > 0
						&& e.isShiftDown() || e.isControlDown())) {
					autocompleteSingleWord();
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					boolean isVisible = isPopupVisible();
					int index = list.getSelectedIndex();
					if (isVisible && index >= 0)
						commit(false);
					else
						AutocompleteBox.super.fireActionEvent();

				} else if (isPopupVisible()) {
					if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						int newIndex = list.getSelectedIndex();
						if (newIndex + 1 < list.getModel().getSize())
							newIndex++;
						list.setSelectedIndex(newIndex);
						list.ensureIndexIsVisible(newIndex);
					} else if (e.getKeyCode() == KeyEvent.VK_UP) {
						int newIndex = list.getSelectedIndex();
						if (newIndex - 1 >= 0)
							newIndex--;
						list.setSelectedIndex(newIndex);
						list.ensureIndexIsVisible(newIndex);
					}
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					showPopup();
				}
			}
		};

		
		protected FocusListener focusListener = new FocusListener() {
			public void focusLost(FocusEvent e) {
			    //				logger.debug("focus lost");
				if (allowNonModelValues
						|| (AutocompleteBox.this.getSelectedItem() == null
								&& getText().length() >= getMinLength() && getItemCount() > 0)) {
					//setSelectedIndex(0);
				    //				    logger.debug("AutocompleteBox.this.getSelectedItem() = " + AutocompleteBox.this.getSelectedItem() + "; getText = " + getText() + ", getSelectedItem = " + getSelectedItem()); // DEL
					commit(true);
				} else if (getSelectedItem() != null) {
					String s = autocompleteModel.toString(getSelectedItem());
					//					logger.debug("AutocompleteBox: setText(" + s + ")"); // DEL
					setText(s);
					//					commit(true);  // I thought this might fix the disappearing-genus problem, but it didn't totally.
				}
			}

			public void focusGained(FocusEvent e) {
			}
		};

		public AutoTextField() {
			getDocument().addDocumentListener(docListener);
			addKeyListener(keyListener);
			addFocusListener(focusListener);

			/*
			 * getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
			 * KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "commit");
			 * getActionMap().put("commit", new AbstractAction() {
			 * 
			 * public void actionPerformed(ActionEvent e) { commit(); } });
			 */
		}

		/**
		 * Autocompletebox.commit() called on term completion and selection of term from autocomplete options list
		 * */
		public void commit(boolean focusCommit) {
			killPendingTasks();
			//			logger.debug("committing.. getText()="+getText());
			if (getText().length() == 0) {
				setSelectedItem(null);
			}
			else if ((!focusCommit || !allowNonModelValues) && lastHits != null && lastHits.size() > 0) {
			    //			    logger.debug("AutocompleteBox.commit: setSelectedItem(" + list.getSelectedValue() + ")"); // DEL
				setSelectedItem(list.getSelectedValue());
			} else if (allowNonModelValues) {
			    //			    			    logger.debug("AutocompleteBox.commit: allowNonModelValues; setSelectedItem(" + list.getSelectedValue() + ")"); // DEL
			    try {
				setSelectedItem(autocompleteModel.createValue(getText()));
			    } catch (Exception e) {
				// We get a not implemented exception when CrossProductEditor sets allowNonModelValues=true for the genus and parent fields,
				// but it doesn't actually seem to matter that the setSelectedItem above failed.
				logger.debug("Exception while calling setSelectedItem(autocompleteModel.createValue(" + getText() + ")--oh well; doesn't seem to matter.");
				// Do something else instead?
				// setSelectedItem(list.getSelectedValue()); //?
			    }
			}
			// What's this big random number for??
			ActionEvent e = new ActionEvent(AutocompleteBox.this, (int) (Math.random()* Integer.MAX_VALUE), "commit");
			for (ActionListener listener : commitListeners) {
				listener.actionPerformed(e);
			}
			fireUpdateEvent();
			hidePopup();
			list.setSelectedIndex(-1); // Why is this needed?
		}

		protected void update() {
			updating = true;
			// removeKeyListener(keyListener);
			getDocument().removeDocumentListener(docListener);
			try {
				String text = getDocument().getText(0,
						getDocument().getLength());
				updateSearchText(text);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			fireUpdateEvent();
			// addKeyListener(keyListener);
			getDocument().addDocumentListener(docListener);
			updating = false;
		}

		public Component getEditorComponent() {
			return this;
		}

		public Object getItem() {
			Object out = null;
                        // This was ALWAYS RETURNING THE FIRST HIT!  No wonder it didn't work right when
                        // the user moused down the list of autocomplete hits and then hit return to try to
                        // select one.
                        // --NH, 26 Jan 2011
//				out = lastHits.size() == 0 ? null : lastHits.get(0).getVal();
			if (lastHits != null && lastHits.size() > 0 && list.getSelectedIndex() >= 0)
                          out = lastHits.get(list.getSelectedIndex()).getVal();
			//			logger.debug("AutocompleteBox: getItem = " + out); // DEL
			return out;
		}

		@Override
		public void setText(String t) {
//			logger.debug(getId() + "**setText:"+t);
			if (((AutocompleteListModel) getModel()).isUpdating() || updating)
				return;
			updating = true;
			super.setText(t);
			updating = false;
		}

		public void setItem(Object anObject) {
//			logger.debug(getId() + "**setItem:" + anObject == null ? "NULL" : anObject);
			getDocument().removeDocumentListener(docListener);
			if (anObject == null)
				setText("");
			else
				setText(autocompleteModel.toString(anObject));
			getDocument().addDocumentListener(docListener);
		}

	}

	protected FocusListener focusListener = new FocusListener() {

		public void focusGained(FocusEvent e) {
		}

		public void focusLost(FocusEvent e) {
			hidePopup();
		}

	};

	protected AutocompleteModel autocompleteModel;

	public void setAutocompleteModel(AutocompleteModel<?, T> model) {
		this.autocompleteModel = model;
	}

	public AutocompleteModel getAutocompleteModel() {
		return autocompleteModel;
	}

	protected ListCellRenderer renderer = new BasicComboBoxRenderer() {

		protected JLabel label = new JLabel();

		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			if (index < 0)
				return label;
			if (isSelected) {
				label.setOpaque(true);
				label.setBackground(selectionBackground);
			} else
				label.setOpaque(false);

			MatchPair pair = lastHits.get(index);
			String s = "<html>" + formatHTML(pair) + "</html>";
			label.setText(s);
			return label;
		}

	};

	protected Color selectionBackground = Color.blue;

	public void setSelectionBackground(Color selectionBackground) {
		this.selectionBackground = selectionBackground;
	}

	protected String formatHTML(MatchPair<?> hit) {
		StringBuffer buffer = new StringBuffer(hit.getString());
		List<int[]> indexList = new ArrayList<int[]>();
		for (String s : hit.getMatch().keySet()) {
			int[] indices = hit.getMatch().get(s);
			for (int index : indices) {
				int[] indexPair = new int[2];
				indexPair[0] = index;
				indexPair[1] = s.length();
				indexList.add(indexPair);
			}
		}
		Collections.sort(indexList, new Comparator<int[]>() {
			public int compare(int[] o1, int[] o2) {
				return o2[0] - o1[0];
			}
		});
		for (int[] strHit : indexList) {
			int startIndex = strHit[0];
			int endIndex = startIndex + strHit[1];
			if (endIndex >= buffer.length())
				buffer.append("</b>");
			else
				buffer.insert(endIndex, "</b>");
			buffer.insert(startIndex, "<b>");
		}
		return buffer.toString();
	}

	protected DropTargetListener dropListener = new DropTargetListener() {
		LineBorder border = new LineBorder(Color.black, 2);

		Border oldBorder;

		public void dragEnter(DropTargetDragEvent dtde) {
			if (!allowDrop(dtde)) {
				oldBorder = null;
				dtde.rejectDrag();
			}
			oldBorder = getBorder();
			setBorder(border);
		}

		public void dragExit(DropTargetEvent dte) {
			if (oldBorder != null)
				setBorder(oldBorder);
		}

		public void dragOver(DropTargetDragEvent dtde) {
			if (!allowDrop(dtde)) {
				oldBorder = null;
				dtde.rejectDrag();
			}
		}

		public void drop(DropTargetDropEvent dtde) {
		}

		public void dropActionChanged(DropTargetDragEvent dtde) {
		}

	};

	@Override
	protected void fireActionEvent() {
	}

	protected Collection<ActionListener> commitListeners = new LinkedList<ActionListener>();
	protected Collection<ActionListener> updateListeners = new LinkedList<ActionListener>();

	public void addCommitListener(ActionListener listener) {
		commitListeners.add(listener);
	}

	public void removeCommitListener(ActionListener listener) {
		commitListeners.remove(listener);
	}

	public void addUpdateListener(ActionListener listener) {
		updateListeners.add(listener);
	}

	public void removeUpdateListener(ActionListener listener) {
		updateListeners.remove(listener);
	}

	protected void fireUpdateEvent() {
		ActionEvent e = new ActionEvent(this, 0, "update");
		for (ActionListener listener : updateListeners) {
			listener.actionPerformed(e);
		}
	}

	protected boolean allowDrop(DropTargetDragEvent selection) {
		return false;
	}

	public void setValue(T object) {
//		logger.debug("setValue current ACBox: "+getId());
		if (object == null) {
			if (getSelectedItem() != null) {
//				logger.info("current: = " + getSelectedItem());
			}
			setSelectedItem(null);
		}
		else {
			List values = autocompleteModel.getDisplayValues(object);
//			logger.debug("setValue: values= " + values);
			if (values.size() > 0)
				setSelectedItem(values.get(0));
			else
				setSelectedItem(null);
		}
	}

	public T getValue () {
		/*
		 * at some undetermined point, the selected item in the JComboBox is being lost.
		 * to get around this we keep track of the selected item ourselves, and ensure that the JCB has the correct
		 * value prior to fetching it.
		 * -- CJM 2010-12-16
		 */
	    /* 1/22/12 This caching--which was intended to solve a problem where the genus field would mysteriously blank out when you committed-- was responsible
	       for the bug that made the genus come back if you cleared the genus field and committed.
	       Now I have properly fixed those bugs in the cross product editor, so we don't need this caching. */
	    //		if (cachedSelectedItem != null) {
//			logger.debug("reinstated:"+cachedSelectedItem);
//		    logger.debug("NOT reinstated:"+cachedSelectedItem + "; getSelectedItem = " + getSelectedItem());
		    //			setSelectedItem(cachedSelectedItem);
	    //		}
		
		
		if (getSelectedItem() == null) {
//			logger.debug("getSelectedItem() == null");
			return null;
		}
		else {
			Object selected = getSelectedItem();
			//			logger.debug("getValue: selected="+selected);
		
			if (allowNonModelValues) {
				String s = ((JTextComponent) editor).getText();

				if (!autocompleteModel.toString(getSelectedItem()).equals(s)) {
				    logger.debug("creating value " + s); // DEL
					return (T) autocompleteModel
							.getOutputValue(autocompleteModel.createValue(s));
				}
			}
			//			logger.debug("returning autocompleteModel.getOutputValue(selected)");
			return (T) autocompleteModel.getOutputValue(selected);
		}
	}

	@Override
	/* Before I added this, it was tending to select the first thing in the list even if user selected a different one. */
	public void actionPerformed(ActionEvent e) {
	}

	
	/* (non-Javadoc)
	 * @see javax.swing.JComboBox#setSelectedItem(java.lang.Object)
	 * set autocompleted object as selected item
	 */
	public void setSelectedItem(Object anObject) {
//	    if (anObject != null) {
//			logger.debug("AutocompleteBox.setSelectedItem: TRYING to select " + anObject + ", type = " + anObject.getClass());
//	    }
		if (anObject == null) {
			doSetSelectedItem(null);
		} else if (autocompleteModel.getDisplayType().isAssignableFrom(anObject.getClass())) {
			doSetSelectedItem(anObject);
			Object selected = getSelectedItem();
//			logger.debug("  selected: "+selected);
		} else if (autocompleteModel.getOutputType().isAssignableFrom(anObject.getClass())) {
			List values = autocompleteModel.getDisplayValues(anObject);
			if (values.size() > 0)
				doSetSelectedItem(values.get(0));
			else
				doSetSelectedItem(null);
		} else if (anObject instanceof String) {
			doSetSelectedItem(autocompleteModel.createValue((String) anObject));
		}
		if (isEditable() && getEditor() != null) {
			configureEditor(getEditor(), getSelectedItem());
		}
	}

	protected void doSetSelectedItem(Object o) {
		/*
		 * we keep track of the selected item ourselves as a "backup", because for some reason
		 * the JCB loses the value
		 * -- CJM 2010-12-16
		 */

		if (o == null) {
			super.setSelectedItem(null);
			//			cachedSelectedItem = null;
		}
		else if (allowNonModelValues || autocompleteModel.isLegal(o)) {
		    //		    logger.debug("doSetSelectedItem: allowNonModelValues = " + allowNonModelValues + ", o =  "+ o + ",  autocompleteModel.isLegal(o) = " +  autocompleteModel.isLegal(o)); // DEL
			super.setSelectedItem(o);
			//			cachedSelectedItem = o;
		}
		else {
		    //		    logger.debug("doSetSelectedItem: setting selected item to null. allowNonModelValues = " + allowNonModelValues + ", o =  "+ o + ",  autocompleteModel.isLegal(o) = " +  autocompleteModel.isLegal(o)); // DEL
		    //			cachedSelectedItem = null;
			super.setSelectedItem(null);
		}
	}

	protected JList list;

	protected JList createList() {
		if (list == null) {
			list = new JList(getModel()) {
				public void processMouseEvent(MouseEvent e) {
					if (e.isControlDown()) {
						// Fix for 4234053. Filter out the Control Key from the
						// list.
						// ie., don't allow CTRL key deselection.
						e = new MouseEvent((Component) e.getSource(),
								e.getID(), e.getWhen(), e.getModifiers()
										^ InputEvent.CTRL_MASK, e.getX(), e
										.getY(), e.getClickCount(), e
										.isPopupTrigger());
					}
					super.processMouseEvent(e);
				}
			};
		}
		return list;
	}

	public AutocompleteBox() {
		this(null);
	}
	public AutocompleteBox(AutocompleteModel model, String id) {
		this(model);
//		logger.debug("setting id:"+id);

		this.setId(id);
	}
		
	public AutocompleteBox(AutocompleteModel model) {
//		logger.debug("Creating new AC Box");
		setEditor(new AutoTextField());
		setAutocompleteModel(model);
		setModel(new AutocompleteListModel());
		setEditable(true);
		setRenderer(renderer);
		setValue(null);
		setDropTarget(new DropTarget(this, dropListener));
		addFocusListener(focusListener);
		super.setUI(new MetalComboBoxUI() {

			/*
			 * @Override protected void installComponents() { arrowButton =
			 * null;
			 * 
			 * if (comboBox.isEditable()) { addEditor(); }
			 * 
			 * comboBox.add(currentValuePane); }
			 */
			@Override
			protected ComboPopup createPopup() {
				return new BasicComboPopup(AutocompleteBox.this) {

					protected boolean poppingUp = false;

					@Override
					protected JList createList() {
						return AutocompleteBox.this.createList();
					}

					public void show(Component invoker, int x, int y) {
						if (poppingUp)
							return;
						poppingUp = true;
						if (userRequestedPopup) {
							JTextComponent text = (JTextComponent) AutocompleteBox.this.editor;
							if (text.getText().length() == 0) {
								ArrayList list = new ArrayList();
								list.addAll(getAutocompleteModel()
										.getAllValues());
								setDisplayResults(list);
							} else {
								TimerTask task = createTimerTask(
										text.getText(), false);
								task.run();
							}
						}
						setPopupSize((int) AutocompleteBox.this.getWidth(),
								getPopupHeightForRowCount(getMaximumRowCount()));
						super.show(invoker, x, y);
						poppingUp = false;
					}

				};
			}

			@Override
			protected JButton createArrowButton() {
				return super.createArrowButton();
			}

			public void configureArrowButton() {
				super.configureArrowButton();
				if (arrowButton != null) {
					arrowButton.removeMouseListener(popup.getMouseListener());
					arrowButton.addMouseListener(getButtonListener());
				}
			}

			public void unconfigureArrowButton() {
				super.unconfigureArrowButton();
				if (arrowButton != null) {
					arrowButton.removeMouseListener(getButtonListener());
				}
			}

			protected boolean userRequestedPopup = false;
			protected MouseListener buttonListener;

			@Override
			protected FocusListener createFocusListener() {
				return null;
			}

			protected MouseListener getButtonListener() {
				if (buttonListener == null)
					buttonListener = new MouseListener() {

						public void mouseClicked(MouseEvent e) {
							popup.getMouseListener().mouseClicked(e);
						}

						public void mouseEntered(MouseEvent e) {
							popup.getMouseListener().mouseEntered(e);
						}

						public void mouseExited(MouseEvent e) {
							popup.getMouseListener().mouseExited(e);
						}

						public void mousePressed(MouseEvent e) {
							userRequestedPopup = true;
							popup.getMouseListener().mousePressed(e);
							userRequestedPopup = false;
						}

						public void mouseReleased(MouseEvent e) {
							popup.getMouseListener().mouseReleased(e);
						}
					};
				return buttonListener;
			}

			/**
			 * This protected method is implementation specific and should be
			 * private. do not call or override.
			 * 
			 * @see #addEditor
			 */
			public void configureEditor() {
				super.configureEditor();
				editor.removeFocusListener(super.createFocusListener());
			}
		});
	}

	@Override
	public void setUI(ComboBoxUI ui) {
	}

	@Override
	protected void setUI(ComponentUI newUI) {
	}

	@Override
	public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {
		super.setFocusTraversalKeysEnabled(focusTraversalKeysEnabled);
		if (getEditor() instanceof Component)
			((Component) getEditor())
					.setFocusTraversalKeysEnabled(focusTraversalKeysEnabled);
	}

	protected String id = ""; // optional id for naming the ACBox
	
	/*
	 * we don't trust ACB to keep track of the selected item. it appears to get lost. we track this directly
	 * in the gui component rather than the model.
	 * -- CJM 2010-12-16
	 * 1/22/2012: Not doing this anymore--it was making it impossible to delete the genus in the cross product editor
	 * because it kept returning, zombie-style.
	 */
	protected Object cachedSelectedItem = null; // CJM

	protected List<MatchPair> lastHits;
	protected Timer timer = new Timer(true);
	protected TimerTask currentTask = null;
	protected int updateInterval = 100;
	protected boolean allowNonModelValues = false;
	protected boolean changing = false;
	protected int maxResults = 10;
	protected int minLength = 3;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setAllowNonModelValues(boolean allowNonModelValues) {
		this.allowNonModelValues = allowNonModelValues;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}

	public String getEditorText() {
		AutoTextField field = (AutoTextField) getEditor();
		if (field == null)
			return null;
		else
			return field.getText();
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public static void main(String[] args) {
		JDialog dialog = new JDialog((java.awt.Frame) null, true);
		final AutocompleteBox<String> box = new AutocompleteBox<String>();

		List<String> stringList = new LinkedList<String>();
		for (int i = 0; i < 200; i++) {
			stringList.add(StringUtil.createRandomString(20));
		}
		box.setAutocompleteModel(new StringListAutocompleteModel(stringList));
		dialog.getContentPane().setLayout(
				new BoxLayout(dialog.getContentPane(), BoxLayout.X_AXIS));
		dialog.getContentPane().add(box);
		dialog.getContentPane().add(Box.createHorizontalStrut(20));
		dialog.getContentPane().add(new JButton("button"));
		dialog.setVisible(true);
	}

	protected void updateSearchText(String str) {
		killPendingTasks();
		currentTask = createTimerTask(str, true);
		timer.schedule(currentTask, getUpdateInterval());
	}

	protected void killPendingTasks() {
		if (currentTask != null) {
			currentTask.cancel();
		}
		currentTask = null;
	}

	protected void autocompleteSingleWord() {
		AutoTextField field = (AutoTextField) getEditor();
		if (field == null)
			return;
		String text = getEditorText();
		int cursorPos = field.getCaretPosition() - 1;
		int[] replaceIndices = StringUtil.getWordIndicesSurrounding(text,
				cursorPos, cursorPos);
		String currentWord = text.substring(replaceIndices[0],
				replaceIndices[1]);
		for (MatchPair matchPair : lastHits) {
			int[] indices = (int[]) matchPair.getMatch().get(currentWord);
			if (indices != null) {
				int index = indices[0];
				String match = StringUtil.getWordSurrounding(matchPair
						.getString(), index, index);
				text = text.substring(0, replaceIndices[0]) + match
						+ text.substring(replaceIndices[1], text.length());
				field.setText(text);
				return;
			}
		}
		// List<MatchPair> matches = getMatches(currentWord);
		// if (matches.size() > 0) {
		// MatchPair matchPair = matches.get(0);
		// int[] indices = (int[]) matchPair.getMatch().get(currentWord);
		// int index = indices[0];
		// String match = StringUtil.getWordSurrounding(matchPair.getString(),
		// index, index);
		// text = text.substring(0,
		// replaceIndices[0])+match+text.substring(replaceIndices[1],
		// text.length());
		// field.setText(text);
		// }
	}

	protected TimerTask createTimerTask(final String str,
			final boolean invokeLater) {
		return new TimerTask() {
			protected boolean cancelled = false;
			TaskDelegate del;

			@Override
			public void run() {
				List<String> tokens = new LinkedList<String>();
				boolean longEnough = false;
				StringTokenizer st = new StringTokenizer(str);
				while (st.hasMoreTokens()) {
					if (cancelled)
						return;
					String token = st.nextToken();
					if (token.length() >= getMinLength()) {
						longEnough = true;
					}
					tokens.add(token);
				}
				if (invokeLater && !longEnough) {
					SwingUtilities.invokeLater(new Runnable() {

						public void run() {
							setResults(Collections.emptyList());
						}
					});
					return;
				}
				del = autocompleteModel.getObjects(tokens);
				if (cancelled)
					return;
				del.run();
				if (cancelled)
					return;
				Runnable r = new Runnable() {

					public void run() {
						if (!cancelled)
							setResults((List) del.getResults());
					}
				};
				if (invokeLater) {
					SwingUtilities.invokeLater(r);
				} else
					r.run();

			}

			@Override
			public boolean cancel() {
				cancelled = true;
				if (del != null) {
					del.cancel();
				}
				return super.cancel();
			}

		};
	}

	protected List getMatches(String str) {
		List tokens = new LinkedList<String>();
		boolean longEnough = false;
		StringTokenizer st = new StringTokenizer(str);
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.length() >= getMinLength()) {
				longEnough = true;
			}
			tokens.add(token);
		}
		TaskDelegate<List> del = autocompleteModel.getObjects(tokens);
		del.run();
		return del.getResults();
	}

	protected Comparator<MatchPair> alphabeticOrdering = new Comparator<MatchPair>() {

		public int compare(MatchPair o1, MatchPair o2) {
			return o1.getString().compareToIgnoreCase(o2.getString());
		}

	};

	protected void setDisplayResults(Collection results) {
		List matchList = new ArrayList(results.size());
		for (Object o : results) {
			MatchPair p = new MatchPair(o, autocompleteModel.toString(o),
					Collections.emptyList(), 1, Collections.emptyMap());
			matchList.add(p);
		}
		Collections.sort(matchList, alphabeticOrdering);
		setResults(matchList);
	}

	protected void setResults(List results) {
		lastHits = results;
		((AutocompleteListModel) getModel()).update();
		if (lastHits == null) {
//			logger.debug("setResults lastHits == null // ACID: "+getId());
		}
		else if (lastHits.size() == 0)
			super.setSelectedItem(null);
		else {
			Object val = lastHits.get(0).getVal();
//			System.out.println("setResults: SELECTING: " + val);
			super.setSelectedItem(null);
			if (isShowing() && hasMeaningfulFocus()) {
				// hidePopup();
				showPopup();
			}
		}
	}

	public boolean hasMeaningfulFocus() {
		return isFocusOwner() || ((JComponent) getEditor()).isFocusOwner();
	}

	protected class AutocompleteListModel implements ComboBoxModel {

		protected Collection<ListDataListener> listeners = new LinkedList<ListDataListener>();

		protected Object selected;

		protected boolean updating = false;

		public boolean isUpdating() {
			return updating;
		}

		public void addListDataListener(ListDataListener l) {
			listeners.add(l);
		}

		public void update() {
			updating = true;
			ListDataEvent event = new ListDataEvent(AutocompleteBox.this,
					ListDataEvent.CONTENTS_CHANGED, 0, getSize());
			for (ListDataListener listener : listeners) {
				listener.contentsChanged(event);
			}
			updating = false;
		}

		public Object getElementAt(int index) {
			return lastHits.get(index).getVal();
		}

		public int getSize() {
			if (lastHits == null)
				return 0;
			if (getMaxResults() < 0)
				return lastHits.size();
			else
				return Math.min(getMaxResults(), lastHits.size());
		}

		public void removeListDataListener(ListDataListener l) {
			listeners.remove(l);
		}

		public Object getSelectedItem() {
		    //			logger.debug("MODEL: getSelectedItem:"+ (selected == null ? "NULL" : selected));
			return selected;
		}

		public void setSelectedItem(Object anItem) {
		    //			logger.debug("MODEL: setSelectedItem:"+ (anItem == null ? "NULL" : anItem));
			selected = anItem;
		}

	}

	public int getMaxResults() {
		return maxResults;
	}

	public int getMinLength() {
		return minLength;
	}

	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}
}
