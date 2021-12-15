package dock;

/**
 * Factory responsible to Instantiate new DetachableTabPane when a Tab is
 * detached/docked. Extend this class then implement {@link #init(DetachableTabPane)} method.
 *
 * @author amrullah
 */
public abstract class DetachableTabPaneFactory {

	DetachableTabPane create(DetachableTabPane source) {
		final DetachableTabPane tabPane = new DetachableTabPane(source.getTabPanes());
		tabPane.setSceneFactory(source.getSceneFactory());
		tabPane.setStageOwnerFactory(source.getStageOwnerFactory());
		tabPane.setScope(source.getScope());
		tabPane.setTabClosingPolicy(source.getTabClosingPolicy());
		tabPane.setCloseIfEmpty(true);
		tabPane.setDetachableTabPaneFactory(source.getDetachableTabPaneFactory());
		tabPane.setDropHint(source.getDropHint());
		init(tabPane);
		return tabPane;
	}

	/**
	 * Callback method to initialize newly created DetachableTabPane for the Tab
	 * that is being detached/docked.
	 */
	protected abstract void init(DetachableTabPane newTabPane);
}
