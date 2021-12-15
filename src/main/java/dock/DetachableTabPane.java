package dock;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import tornadofx.NodesKt;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author amrullah
 */
public class DetachableTabPane extends TabPane {

	/**
	 * Hold reference to the source of drag event. We can't use
	 * event.getGestureSource() because it is null when the target is on a
	 * different stage.
	 */
	private static DetachableTabPane DRAG_SOURCE;
	public static Tab DRAGGED_TAB;
	private final StringProperty scope = new SimpleStringProperty("");
	private TabDropHint dropHint = new TabDropHint();
	private Pos pos;
	private int dropIndex;
	private final List<Double> lstTabPoint = new ArrayList<>();
	private boolean closeIfEmpty;
	private ObservableList<DetachableTabPane> tabPanes;

	public DetachableTabPane(ObservableList<DetachableTabPane> tabPanes) {
		this.tabPanes = tabPanes;
		getStyleClass().add("detachable-tab-pane");
		attachListeners();
		tabPanes.add(this);
	}
	public TabSplitPane parentSplitPane;
	private StackPane btnBottom;
	private StackPane btnLeft;
	private StackPane btnTop;
	private StackPane btnRight;
	private StackPane dockPosIndicator;

	private void initDropButton() {
		btnTop = new StackPane();
		btnTop.getStyleClass().addAll("adjacent-drop", "drop-top");
		btnRight = new StackPane();
		btnRight.getStyleClass().addAll("adjacent-drop", "drop-right");

		btnLeft = new StackPane();
		btnLeft.getStyleClass().addAll("adjacent-drop", "drop-left");

		btnBottom = new StackPane();
		btnBottom.getStyleClass().addAll("adjacent-drop", "drop-bottom");

		StackPane.setAlignment(btnTop, Pos.TOP_CENTER);
		StackPane.setAlignment(btnRight, Pos.CENTER_RIGHT);
		StackPane.setAlignment(btnBottom, Pos.BOTTOM_CENTER);
		StackPane.setAlignment(btnLeft, Pos.CENTER_LEFT);

		// Required to paint the docking indicator properly, at least on Linux.
		StackPane wrapper = new StackPane();
		wrapper.getStyleClass().setAll("dock-pos-indicator");
		wrapper.getChildren().addAll(btnBottom, btnLeft, btnTop, btnRight);

		dockPosIndicator = new StackPane();
		dockPosIndicator.getChildren().add(wrapper);
	}

	/**
	 * Get drag scope id
	 */
	public String getScope() {
		return scope.get();
	}

	/**
	 * Set scope id. Only TabPane having the same scope that could be drop
	 * target. Default is empty string. So the default behavior is this TabPane
	 * could receive tab from empty scope DragAwareTabPane
	 */
	public void setScope(String scope) {
		this.scope.set(scope);
	}

	/**
	 * Scope property. Only TabPane having the same scope that could be drop
	 * target.
	 */
	public StringProperty scopeProperty() {
		return scope;
	}

	/**
	 * Helper method to add a new {@link DetachableTab} to the list of tabs.
	 *
	 * @param tabName The tab's name, presented to the user.
	 * @param content The tab's content, displayed within the tab.
	 * @return The tab instance that was created.
	 */
	public DetachableTab addTab( final String tabName, final Node content ) {
		final var tab = new DetachableTab( tabName, content );
		getTabs().add( tab );
		return tab;
	}

	/**
	 * This listener detects when the TabPane is shown. Then it will call
	 * initiateDragGesture. It because the lookupAll call in that method only
	 * works if the stage containing this instance is already shown.
	 */
	private void attachListeners() {
		sceneProperty().addListener((ObservableValue<? extends Scene> ov, Scene t, Scene t1) -> {
			if (t == null && t1 != null) {
				if (getScene().getWindow() != null) {
					Platform.runLater(() -> initiateDragGesture(true) );
				} else {
					getScene().windowProperty().addListener((ObservableValue<? extends Window> ov1, Window t2, Window t3) -> {
						if (t2 == null && t3 != null) {
							t3.addEventHandler(WindowEvent.WINDOW_SHOWN, (t4) ->
									initiateDragGesture(true) );
						}
					});
				}
			}
		});

		this.addEventFilter(DragEvent.ANY, (DragEvent event) -> {
			if (DRAG_SOURCE == null) {
				return;
			}
			if (event.getEventType() == DragEvent.DRAG_OVER) {
				if (DetachableTabPane.this.scope.get().equals(DRAG_SOURCE.getScope())) {
					event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
					repaintPath(event);
				}
				event.consume();
			} else if (event.getEventType() == DragEvent.DRAG_EXITED) {
				if (DetachableTabPane.this.getSkin() instanceof TabPaneSkin) {
					TabPaneSkin sp = (TabPaneSkin) getSkin();
					sp.getChildren().remove(dropHint.getPath());
					sp.getChildren().remove(dockPosIndicator);
					DetachableTabPane.this.requestLayout();
				}
			} else if (event.getEventType() == DragEvent.DRAG_ENTERED) {
				if (!DetachableTabPane.this.scope.get().equals(DRAG_SOURCE.getScope())) {
					return;
				}
				calculateTabPoints();
				if (dockPosIndicator == null) {
					initDropButton();
				}
				double layoutX = DetachableTabPane.this.getWidth() / 2;
				double layoutY = DetachableTabPane.this.getHeight() / 2;
				dockPosIndicator.setLayoutX(layoutX);
				dockPosIndicator.setLayoutY(layoutY);
				if (DetachableTabPane.this.getSkin() instanceof TabPaneSkin) {
					TabPaneSkin sp = (TabPaneSkin) getSkin();
					if (!sp.getChildren().contains(dropHint.getPath())) {
						if (!getTabs().isEmpty()) {
							sp.getChildren().add(dockPosIndicator);
						}
						repaintPath(event);
						sp.getChildren().add(dropHint.getPath());
					}
				}
			} else if (event.getEventType() == DragEvent.DRAG_DROPPED) {
				if (pos != null) {
					placeTab(DRAGGED_TAB, pos);
					event.setDropCompleted(true);
					event.consume();
					return;
				}
				if ( DRAG_SOURCE != DetachableTabPane.this ) {
					final Tab selectedtab = DRAGGED_TAB;
					DetachableTabPane.this.getTabs().add(dropIndex, selectedtab);
					Platform.runLater(
							() -> {
								DetachableTabPane.this.getSelectionModel().select(selectedtab);
								requestFocus();
							}
					);
					event.setDropCompleted(true);
				} else {
					event.setDropCompleted(true);
					final Tab selectedtab = DRAGGED_TAB;
					int currentSelectionIndex = getTabs().indexOf(selectedtab);
					if (dropIndex == currentSelectionIndex) {
						return;
					}
					getTabs().add(dropIndex, selectedtab);
					Platform.runLater(
							() -> {
								DetachableTabPane.this.getSelectionModel().select(selectedtab);
								requestFocus();
							}
					);
				}
				if (event.isDropCompleted()) {
					event.getDragboard().setContent(null);
				}
				event.consume();
			}
		});

		getTabs().addListener((ListChangeListener.Change<? extends Tab> change) -> {
			while (change.next()) {
				if (change.wasAdded()) {
					if (getScene() != null && getScene().getWindow() != null) {
						if (getScene().getWindow().isShowing()) {
							Platform.runLater(() -> {
								clearGesture();
								initiateDragGesture(true);
								/*
								 * We need to use timer to wait until the
								 * tab-add-animation finish
								 */
								futureCalculateTabPoints();
							});
						}
					}
				} else if (change.wasRemoved()) {
					/*
					 * We need to use timer to give the system some time to remove
					 * the tab from TabPaneSkin.
					 */
					futureCalculateTabPoints();

					if (DRAG_SOURCE == null) {
						//it means we are not dragging
						if (getScene() != null && getScene().getWindow() instanceof TabStage) {
							TabStage stage = (TabStage) getScene().getWindow();
							closeStageIfNeeded(stage);
						}

						if (getTabs().isEmpty()) {
							System.out.println("Removing " + this);
							removeTabPaneFromParent(DetachableTabPane.this);
						}
					}
				}
			}
		});

	}

	private SplitPane findParentSplitPane(Node control) {
		if (control instanceof DetachableTabPane) return ((DetachableTabPane) control).parentSplitPane;
		if (control.getParent() == null) return null;
		final Set<Node> lstSplitpane = control.getScene().getRoot().lookupAll(".split-pane");
		SplitPane parentSplitpane = null;
		for (final Node node : lstSplitpane) {
			if (node instanceof SplitPane) {
				final SplitPane splitpane = (SplitPane) node;
				if (splitpane.getItems().contains(control)) {
					parentSplitpane = splitpane;
					break;
				}
			}
		}
		return parentSplitpane;
	}


	public void placeTab(Tab tab, Pos pos) {
		boolean addToLast = pos == Pos.CENTER_RIGHT || pos == Pos.BOTTOM_CENTER;
		DetachableTabPane dt = detachableTabPaneFactory.create(this);
		dt.getTabs().add(tab);

		Orientation requestedOrientation = Orientation.HORIZONTAL;
		if (pos == Pos.BOTTOM_CENTER || pos == Pos.TOP_CENTER) {
			requestedOrientation = Orientation.VERTICAL;
		}

		TabSplitPane targetSplitPane = parentSplitPane;

		int requestedIndex = 0;
		if (targetSplitPane != null && requestedOrientation == targetSplitPane.getOrientation()) {
			requestedIndex = targetSplitPane.getItems().indexOf(DetachableTabPane.this);
		}
		if (pos == Pos.CENTER_RIGHT || pos == Pos.BOTTOM_CENTER) {
			requestedIndex++;
		}

		// If there is no splitPane parent... Create one!!
		if (targetSplitPane == null) {
			targetSplitPane = new TabSplitPane();
			targetSplitPane.setOrientation(requestedOrientation);

				Pane parent = (Pane) getParent();
				int indexInParent = parent.getChildren().indexOf(DetachableTabPane.this);
				parent.getChildren().remove(DetachableTabPane.this);
				if (addToLast) {
					targetSplitPane.getItems().addAll(DetachableTabPane.this, dt);
				} else {
					targetSplitPane.getItems().addAll(dt, DetachableTabPane.this);
				}
				parent.getChildren().add(indexInParent, targetSplitPane);
			}
		//  If the orientation is changed... create a new split pane.
		else {
			if (targetSplitPane.getItems().size() == 1) {
				targetSplitPane.setOrientation(requestedOrientation);
			}
			if (targetSplitPane.getOrientation() != requestedOrientation) {
				TabSplitPane parent = targetSplitPane;
				int indexInParent = parent.getItems().indexOf(DetachableTabPane.this);
				parent.getItems().remove(DetachableTabPane.this);

				targetSplitPane = new TabSplitPane();
				targetSplitPane.setOrientation(requestedOrientation);
				if (addToLast) {
					targetSplitPane.getItems().addAll(DetachableTabPane.this, dt);
				} else {
					targetSplitPane.getItems().addAll(dt, DetachableTabPane.this);
				}
				parent.getItems().add(indexInParent, targetSplitPane);
			} else {
				targetSplitPane.getItems().add(requestedIndex, dt);
				int itemCount = targetSplitPane.getItems().size();
				double[] dividerPos = new double[itemCount];
				dividerPos[0] = 1d / itemCount;
				for (int i = 1; i < dividerPos.length; i++) {
					dividerPos[i] = dividerPos[i - 1] + dividerPos[0];
				}
				targetSplitPane.setDividerPositions(dividerPos);
			}
		}
		Platform.runLater(dt::requestFocus);
	}

	private void futureCalculateTabPoints() {
		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				calculateTabPoints();
				timer.cancel();
				timer.purge();
			}
		}, 1000);
	}

	/**
	 * The lookupAll call in this method only works if the stage containing this
	 * instance is already shown.
	 */
	private void initiateDragGesture(boolean retryOnFailed) {
		final Node tabheader = getTabHeaderArea();
		if (tabheader == null) {
			if (retryOnFailed) {
				final Timer timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						initiateDragGesture(false);
						timer.cancel();
						timer.purge();
					}
				}, 500);

			}
			return;
		}
		final Set<Node> tabs = tabheader.lookupAll(".tab");

		for (final Node node : tabs) {
			addGesture(this, node);
		}
	}

	private Node getTabHeaderArea() {
		Node tabheader = null;
		for (final Node node : this.getChildrenUnmodifiable()) {
			if (node.getStyleClass().contains("tab-header-area")) {
				tabheader = node;
				break;
			}
		}
		return tabheader;
	}

	private void calculateTabPoints() {
		lstTabPoint.clear();
		lstTabPoint.add(0d);
		final Node tabheader = getTabHeaderArea();
		if (tabheader == null) return;
		final Set<Node> tabs = tabheader.lookupAll(".tab");
		final Point2D inset = DetachableTabPane.this.localToScene(0, 0);
		for (final Node node : tabs) {
			final Point2D point = node.localToScene(0, 0);
			final Bounds bound = node.getLayoutBounds();
			lstTabPoint.add(point.getX() + bound.getWidth() - inset.getX());
		}
	}

	private void repaintPath(DragEvent event) {
		boolean hasTab = !getTabs().isEmpty();
		if (hasTab && btnLeft.contains(btnLeft.screenToLocal(event.getScreenX(), event.getScreenY()))) {
			dropHint.refresh(0, 0, DetachableTabPane.this.getWidth() / 2, DetachableTabPane.this.getHeight());
			pos = Pos.CENTER_LEFT;
		} else if (hasTab && btnRight.contains(btnRight.screenToLocal(event.getScreenX(), event.getScreenY()))) {
			double pathWidth = DetachableTabPane.this.getWidth() / 2;
			dropHint.refresh(pathWidth, 0, pathWidth, DetachableTabPane.this.getHeight());
			pos = Pos.CENTER_RIGHT;
		} else if (hasTab && btnTop.contains(btnTop.screenToLocal(event.getScreenX(), event.getScreenY()))) {
			dropHint.refresh(0, 0, getWidth(), getHeight() / 2);
			pos = Pos.TOP_CENTER;
		} else if (hasTab && btnBottom.contains(btnBottom.screenToLocal(event.getScreenX(), event.getScreenY()))) {
			double pathHeight = getHeight() / 2;
			dropHint.refresh(0, pathHeight, getWidth(), getHeight() - pathHeight);
			pos = Pos.BOTTOM_CENTER;
		} else {
			pos = null;
			double tabpos = -1;
			for (int i = 1; i < lstTabPoint.size(); i++) {
				if (event.getX() < lstTabPoint.get(i)) {
					tabpos = lstTabPoint.get(i - 1);
					dropIndex = i - 1;
					break;
				}
			}
			if (tabpos == -1) {
				final int index = lstTabPoint.size() - 1;
				dropIndex = getTabs().size();
				if (index > -1) {
					tabpos = lstTabPoint.get(index);
				}
			}
			dropHint.refresh(tabpos, DetachableTabPane.this.getWidth(), DetachableTabPane.this.getHeight());
		}
	}

	private void clearGesture() {
		final Node tabheader = getTabHeaderArea();
		if (tabheader == null) return;
		final Set<Node> tabs = tabheader.lookupAll(".tab");
		for (final Node node : tabs) {
			node.setOnDragDetected(null);
			node.setOnDragDone(null);
		}
	}

	private static final DataFormat DATA_FORMAT = new DataFormat("dragAwareTab");

	private void addGesture(final TabPane tabPane, final Node node) {

		node.lookup(".tab-container").lookup(".tab-close-button").setOnMousePressed(null);
		node.lookup(".tab-container").lookup(".tab-close-button").setOnMouseReleased((event) -> {
			Tab tab = tabPane.getSelectionModel().getSelectedItem();
			tabPane.getTabs().remove(tab);
		});


		node.setOnDragDetected((MouseEvent e) -> {
			Tab tab = tabPane.getSelectionModel().getSelectedItem();
			if (tab instanceof DetachableTab && !((DetachableTab) tab).isDetachable()) {
				return;
			}
			final Dragboard db = node.startDragAndDrop(TransferMode.ANY);
			db.setDragView(node.snapshot(null, null), -20, 0);
			Map<DataFormat, Object> dragContent = new HashMap<>();
			dragContent.put(DATA_FORMAT, "test");
			DetachableTabPane.DRAG_SOURCE = DetachableTabPane.this;
			DRAGGED_TAB = tab;

			var cmp = NodesKt.uiComponent(DRAGGED_TAB.getContent());
			var listener = (ChangeListener<Parent>) cmp.getRootParentChangeListener();
			var tabsListener = (ListChangeListener<Tab>) this.getProperties().get("tabsListener");
			this.getTabs().removeListener(tabsListener);
			cmp.getRoot().parentProperty().removeListener(listener);


			var selectedTabListener = (ChangeListener<Tab>) this.getProperties().get("selectedTabListener");
			this.getSelectionModel().selectedItemProperty().removeListener(selectedTabListener);

			getTabs().remove(DRAGGED_TAB);
			db.setContent(dragContent);
			e.consume();
		});

		node.setOnDragDone((DragEvent event) -> {
			if (DRAGGED_TAB != null && DRAGGED_TAB.getTabPane() == null) {
				Tab tab = DRAGGED_TAB;
				DRAG_SOURCE.getTabs().add(tab);
				DRAG_SOURCE.getSelectionModel().select(tab);
				var cmp = NodesKt.uiComponent(DRAGGED_TAB.getContent());
				var listener = (ChangeListener<Parent>) cmp.getRootParentChangeListener();
				cmp.getRoot().parentProperty().addListener(listener);
			}
			if (DRAG_SOURCE.getTabs().isEmpty()) {
				removeTabPaneFromParent(DRAG_SOURCE);
			} else {
				var tabsListener = (ListChangeListener<Tab>) this.getProperties().get("tabsListener");
				DRAG_SOURCE.getTabs().addListener(tabsListener);

				var selectedTabListener = (ChangeListener<Tab>) this.getProperties().get("selectedTabListener");
				DRAG_SOURCE.getSelectionModel().selectedItemProperty().addListener(selectedTabListener);
			}

			DetachableTabPane.DRAG_SOURCE = null;
			DRAGGED_TAB = null;
			event.consume();
		});

	}

	private void closeStageIfNeeded(TabStage stage) {
		final Set<Node> setNode = stage.getScene().getRoot().lookupAll(".tab-pane");
		boolean empty = true;
		for (final Node nodeTabpane : setNode) {
			if (nodeTabpane instanceof DetachableTabPane) {
				if (!((DetachableTabPane) nodeTabpane).getTabs().isEmpty()) {
					empty = false;
					break;
				}
			}
		}

		if (empty) {
			//there is a case where lookup .tab-pane style doesn't return all TabPane. So we need to lookup by SplitPane and scan through it
			final Set<Node> setSplitpane = stage.getScene().getRoot().lookupAll(".split-pane");
			for (final Node nodeSplitpane : setSplitpane) {
				if (nodeSplitpane instanceof SplitPane) {
					final SplitPane asplitpane = (SplitPane) nodeSplitpane;
					for (final Node child : asplitpane.getItems()) {
						if (child instanceof DetachableTabPane) {
							DetachableTabPane dtp = (DetachableTabPane) child;
							if (!dtp.getTabs().isEmpty()) {
								empty = false;
								break;
							}
						}
					}
				}
				if (!empty) {
					break;
				}
			}
		}
		if (empty) {
			stage.close();
		}
	}

	public void removeTabPaneFromParent(DetachableTabPane tabPaneToRemove) {
		final SplitPane sp = findParentSplitPane(tabPaneToRemove);
		System.out.println("parent splitpane is " + sp);
		if (sp == null) {
			return;
		}
		if (!tabPaneToRemove.isCloseIfEmpty()) {
			final DetachableTabPane sibling = findSibling(sp, tabPaneToRemove);
			if (sibling == null) {
				return;
			}
			sibling.setCloseIfEmpty(false);
			if (siblingProvider != null) {
				siblingProvider.accept(sibling);
				sibling.setOnClosedPassSibling(siblingProvider);
			}
		}
		sp.getItems().remove(tabPaneToRemove);
		tabPanes.remove(tabPaneToRemove);
		simplifySplitPane(sp);
	}

	private Consumer<DetachableTabPane> siblingProvider;

	/**
	 * The siblingProvider consumer works only if {@link #isCloseIfEmpty()} is
	 * true. It is called right before removing tabpane. It sends closest sibling
	 * as parameter. This callback is useful when other class has a variable to
	 * this tabpane. When this tabpane is removed, the variable should be
	 * reassigned with passed sibling.
	 *
	 * <p>
	 * The following example code reassigns the tabPane variable with its closest
	 * sibling when the pane is removed from scene due to empty:
	 * </p>
	 * <pre>
	 * tabPane.setOnClosedPassSibling((sibling) -&#62; tabPane = sibling);
	 * </pre>
	 *
	 * @param siblingProvider is a callback that sends sibling tabpane when current tabpane is removed due to empty.
	 */
	public void setOnClosedPassSibling(Consumer<DetachableTabPane> siblingProvider) {
		this.siblingProvider = siblingProvider;
	}

	private DetachableTabPane findSibling(SplitPane sp, DetachableTabPane tabPaneToRemove) {
		for (final Node sibling : sp.getItems()) {
			if (tabPaneToRemove != sibling
					  && sibling instanceof DetachableTabPane
					  && tabPaneToRemove.getScope().equals(((DetachableTabPane) sibling).getScope())) {
				return (DetachableTabPane) sibling;
			}
		}
		for (final Node sibling : sp.getItems()) {
			if (sibling instanceof SplitPane) {
				return findSibling((SplitPane) sibling, tabPaneToRemove);
			}
		}
		return null;
	}

	private void simplifySplitPane(SplitPane sp) {
		if (sp.getItems().size() != 1) {
			return;
		}
		final Node content = sp.getItems().get(0);
		final SplitPane parent = findParentSplitPane(sp);
		if (parent != null) {
			int index = parent.getItems().indexOf(sp);
			parent.getItems().remove(sp);
			parent.getItems().add(index, content);
			simplifySplitPane(parent);
		}
	}

	public ObservableList<DetachableTabPane> getTabPanes() {
		return tabPanes;
	}

	/**
	 * Set factory to generate the Scene. Default SceneFactory is provided and it
	 * will generate a scene with TabPane as root node. Call this method if you
	 * need to have a custom scene
	 */
	public void setSceneFactory(Callback<DetachableTabPane, Scene> sceneFactory) {
		this.sceneFactory = sceneFactory;
	}

	/**
	 * Getter for {@link #setSceneFactory(Callback)}
	 */
	public Callback<DetachableTabPane, Scene> getSceneFactory() {
		return this.sceneFactory;
	}

	/**
	 * By default, the stage owner is the stage that own the first TabPane. For
	 * example, detaching a Tab will open a new Stage. The new stage owner is the
	 * stage of the TabPane. Detaching a tab from the new stage will open another
	 * stage. Their owner are the same which is the stage of the first TabPane.
	 */
	public void setStageOwnerFactory(Callback<Stage, Window> stageOwnerFactory) {
		this.stageOwnerFactory = stageOwnerFactory;
	}

	/**
	 * Getter for {@link #setStageOwnerFactory(Callback)}
	 */
	public Callback<Stage, Window> getStageOwnerFactory() {
		return stageOwnerFactory;
	}

	/**
	 * Remove tabpane if it doesn't have any tabs. Default false.
	 */
	public boolean isCloseIfEmpty() {
		return closeIfEmpty;
	}

	/**
	 * Pass true to close the tabpane if it is empty. Default false.
	 */
	public void setCloseIfEmpty(boolean closeIfEmpty) {
		this.closeIfEmpty = closeIfEmpty;
	}

	private static final int STAGE_WIDTH = 400;

	private Callback<DetachableTabPane, Scene> sceneFactory = p ->
			new Scene(p, STAGE_WIDTH, STAGE_WIDTH);

	private DetachableTabPaneFactory detachableTabPaneFactory = new DetachableTabPaneFactory(){
		@Override
		protected void init(DetachableTabPane a) {}

	};

	public DetachableTabPaneFactory getDetachableTabPaneFactory() {
		return detachableTabPaneFactory;
	}

	/**
	 * Factory object to create new DetachableTabPane. We can extends
	 * {@link DetachableTabPaneFactory} and set it to this method when custom
	 * initialization is needed. For example when we want to set different
	 * TabClosingPolicy.
	 */
	public void setDetachableTabPaneFactory(DetachableTabPaneFactory detachableTabPaneFactory) {
		if (detachableTabPaneFactory == null) {
			throw new NullPointerException("detachableTabPaneFactory");
		}
		this.detachableTabPaneFactory = detachableTabPaneFactory;
	}

	private Callback<Stage, Window> stageOwnerFactory = p -> {
		if (DetachableTabPane.this.getScene() == null) {
			return null;
		}
		return DetachableTabPane.this.getScene().getWindow();
	};

	private class TabStage extends Stage {

		public TabStage(final Tab tab) {
			final DetachableTabPane tabPane = detachableTabPaneFactory.create(
					DetachableTabPane.this );
			initOwner(stageOwnerFactory.call(this));
			Scene scene = sceneFactory.call(tabPane);

			scene.getStylesheets().addAll(DetachableTabPane.this.getScene().getStylesheets());
			setScene(scene);

			final Point p = MouseInfo.getPointerInfo().getLocation();
			setX( p.x - (STAGE_WIDTH >> 1) );
			setY( p.y );
			show();
			tabPane.getTabs().add(tab);
			tabPane.getSelectionModel().select(tab);
			if (tab.getContent() instanceof Parent) {
				((Parent) tab.getContent()).requestLayout();
			}
		}
	}

	@Override
	public String getUserAgentStylesheet() {
		return DetachableTabPane.class.getResource("tiwulfx-dock.css").toExternalForm();
	}

	/**
	 * Use this method to create custom drop hint by extending {@link TabDropHint} class.
	 */
	public void setDropHint(TabDropHint dropHint) {
		this.dropHint = dropHint;
	}

	public TabDropHint getDropHint() {
		return this.dropHint;
	}
}
