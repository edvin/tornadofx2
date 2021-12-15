package dock;

import javafx.scene.Node;
import javafx.scene.shape.*;

/**
 * Class responsible to draw tab drop hint.
 *
 * @author amrullah
 */
public class TabDropHint {

	private double tabPos;
	private double width;
	private double height;
	private double startX;
	private double startY;
	private final Path path;

	public TabDropHint() {
		this.path = new Path();
		this.path.getStyleClass().add("drop-path");
	}
	
	void refresh(double startX, double startY, double width, double height) {
		boolean regenerate = this.tabPos != -1
				  || this.width != width
				  || this.height != height
				  || this.startX != startX
				  || this.startY != startY;
		this.tabPos = -1;
		this.width = width;
		this.height = height;
		this.startX = startX;
		this.startY = startY;
		if (regenerate) {
			generateAdjacentPath(path, startX + 2, startY + 2, width - 4, height - 4);
		}
	}
	
	void refresh(double tabPos, double width, double height) {
		boolean regenerate  = this.tabPos != tabPos
				  || this.width != width
				  || this.height != height;
		this.tabPos = tabPos;
		this.width = width;
		this.height = height;
		startX = 0;
		startY = 0;
		if (regenerate) {
			generateInsertionPath(path, tabPos, width - 2, height - 2);
		}
	}
	
	protected void generateAdjacentPath(Path path, double startX, double startY, double width, double height) {
		path.getElements().clear();
		MoveTo moveTo = new MoveTo();
		moveTo.setX(startX);
		moveTo.setY(startY);
		path.getElements().add(moveTo);//start
		path.getElements().add(new HLineTo(startX + width));//path width
		path.getElements().add(new VLineTo(startY + height));//path height
		path.getElements().add(new HLineTo(startX));//path bottom left
		path.getElements().add(new VLineTo(startY));//back to start
	}

	protected void generateInsertionPath(Path path, double tabPos, double width, double height) {
		int tabHeight = 28;
		int start = 2;
		tabPos = Math.max(start, tabPos);
		path.getElements().clear();
		MoveTo moveTo = new MoveTo();
		moveTo.setX(start);
		moveTo.setY(tabHeight);
		path.getElements().add(moveTo);//start

		path.getElements().add(new HLineTo(width));//path width
		path.getElements().add(new VLineTo(height));//path height
		path.getElements().add(new HLineTo(start));//path bottom left
		path.getElements().add(new VLineTo(tabHeight));//back to start

		if (tabPos > 20) {
			path.getElements().add(new MoveTo(tabPos, tabHeight + 5));
			path.getElements().add(new LineTo(Math.max(start, tabPos - 10), tabHeight + 15));
			path.getElements().add(new HLineTo(tabPos + 10));
			path.getElements().add(new LineTo(tabPos, tabHeight + 5));
		} else {
			double tip = Math.max(tabPos, start + 5);
			path.getElements().add(new MoveTo(tip, tabHeight + 5));
			path.getElements().add(new LineTo(tip + 10, tabHeight + 5));
			path.getElements().add(new LineTo(tip, tabHeight + 15));
			path.getElements().add(new VLineTo(tabHeight + 5));
		}
	}
	
	public Node getPath() {
		return path;
	}
}
