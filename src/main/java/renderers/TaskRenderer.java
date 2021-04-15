package renderers;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;

import agents.ChargingStation;
import agents.DualStorageAgent;
import agents.ExportStorage;
import agents.ImportStorage;
import agents.StorageAgent;
import agents.StorageAgentConnector;

import com.github.rinde.rinsim.ui.renderers.ViewPort;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;

public class TaskRenderer extends AbstractCanvasRenderer {

	private static final int ROUND_RECT_ARC_HEIGHT = 5;
	private static final int X_OFFSET = 5;
	private static final int Y_OFFSET = -30;
	private RoadModel roadModel;

	public TaskRenderer(RoadModel rm) {
		roadModel = rm;
	}

	public void renderStatic(GC gc, ViewPort vp) {
		drawLegende(gc, vp);
	}

	public void renderDynamic(GC gc, ViewPort vp, long time) {
		/*
		 * Font font = new Font(gc.getDevice(),"Arial",10, 0); gc.setFont(font);
		 */

		Set<Point> handled = new HashSet<Point>();
		for (StorageAgent sa : roadModel.getObjectsOfType(StorageAgent.class)) {
			if (sa instanceof DualStorageAgent) {
				DualStorageAgent dsa = (DualStorageAgent) sa;
				StorageAgentConnector conn = dsa.returnConnector();
				if (!handled.contains(conn.getPosition().get())) {
					drawDualStorage(gc, conn, vp, time);
				}
			} else {
				drawStandard(gc, sa, vp, time);
			}
		}
	}

	private void drawLegende(GC gc, ViewPort vp) {
		Point p = new Point(228, 60);
		int x = vp.toCoordX(p.x);
		int y = vp.toCoordY(p.y);

		String text = "Legend:";
		org.eclipse.swt.graphics.Point extent = gc.textExtent("Storage has a return");
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		gc.drawText(text, x - extent.x / 2, y - extent.y / 2 + 1, true);

		y += extent.y;
		text = "Storage has task";
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_CYAN));
		gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2, extent.x - 1, extent.y + 1, ROUND_RECT_ARC_HEIGHT,
				ROUND_RECT_ARC_HEIGHT);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		gc.drawText(text, x - extent.x / 2, y - extent.y / 2 + 1, true);

		y += extent.y;

		text = "Storage has a return";
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_MAGENTA));
		gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2, extent.x - 1, extent.y + 1, ROUND_RECT_ARC_HEIGHT,
				ROUND_RECT_ARC_HEIGHT);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.drawText(text, x - extent.x / 2, y - extent.y / 2 + 1, true);

		y += extent.y;

		text = "Storage is idle";
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2, extent.x - 1, extent.y + 1, ROUND_RECT_ARC_HEIGHT,
				ROUND_RECT_ARC_HEIGHT);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.drawText(text, x - extent.x / 2, y - extent.y / 2 + 1, true);

		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		p = new Point(110, 10);
		x = vp.toCoordX(p.x);
		y = vp.toCoordY(p.y);
		text = "Importnodes";
		gc.drawText(text, x - extent.x / 2, y - extent.y / 2 + 1, true);
		
		p = new Point(110,125);
		x = vp.toCoordX(p.x);
		y = vp.toCoordY(p.y);
		text = "Exportnodes";
		gc.drawText(text, x - extent.x / 2, y - extent.y / 2 + 1, true);

	}

	private void drawDualStorage(GC gc, StorageAgentConnector conn, ViewPort vp, long time) {
		final Point p = conn.getPosition().get();
		int x = vp.toCoordX(p.x);
		int y = vp.toCoordY(p.y);
		int size = conn.da1.getCarIds().size();
		String text;
		if (size < 10)
			text = " " + size + " ";
		else
			text = "" + size + " ";

		final org.eclipse.swt.graphics.Point extent = gc.textExtent(text);

		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_GRAY));

		gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2, extent.x - 1, extent.y + 1, ROUND_RECT_ARC_HEIGHT,
				ROUND_RECT_ARC_HEIGHT);

		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

		gc.drawText(text, x - extent.x / 2, y - extent.y / 2 + 1, true);

		drawSA(gc, conn.da1, vp, time);
		drawSA(gc, conn.da2, vp, time);

	}

	private void drawSA(GC gc, DualStorageAgent sa, ViewPort vp, long time) {
		final Point p = sa.position;
		int x = vp.toCoordX(p.x);
		int y = vp.toCoordY(p.y);
		String text = "  ";
		final org.eclipse.swt.graphics.Point extent = gc.textExtent(text);
		if (sa.hasTask()) {
			if (sa.tasks.size() > 1)
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_CYAN));
		} else {

			if (sa.hasReturn()) {
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_MAGENTA));
			} else {
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_GRAY));
			}
		}
		gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2, extent.x - 1, extent.y + 1, ROUND_RECT_ARC_HEIGHT,
				ROUND_RECT_ARC_HEIGHT);

		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

		gc.drawText(text, x - extent.x / 2, y - extent.y / 2 + 1, true);
	}

	private void drawStandard(GC gc, StorageAgent sa, ViewPort vp, long time) {
		final Point p = sa.position;
		int x = vp.toCoordX(p.x);
		int y = vp.toCoordY(p.y);
		// x += X_OFFSET;
		int size = sa.getCarIds().size();
		String text = "";
		if (sa instanceof ImportStorage) {
			y += Y_OFFSET;
			text = " IM:" + sa.getTaskAmmount() + " ";
		} else {
			if (sa instanceof ExportStorage) {
				text = " EX ";
			} else {
				text = " " + Integer.toString(size) + " ";
				y -= 15;
			}
		}

		final org.eclipse.swt.graphics.Point extent = gc.textExtent(text);
		y += extent.y;

		if (sa.hasTask()) {
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_CYAN));
		} else {

			if (sa.hasReturn()) {
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_MAGENTA));
			} else {
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_GRAY));
			}
		}
		gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2, extent.x - 1, extent.y + 1, ROUND_RECT_ARC_HEIGHT,
				ROUND_RECT_ARC_HEIGHT);

		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

		gc.drawText(text, x - extent.x / 2, y - extent.y / 2 + 1, true);
	}

	public static TaskRendererBuilder builder() {
		return new TaskRendererBuilder();
	}

}
