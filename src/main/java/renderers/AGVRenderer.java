package renderers;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;

import agents.AGVAgent;
import agents.DualStorageAgent;
import agents.StorageAgent;
import agents.StorageAgentConnector;
import tasks.BasicTask;
import tasks.HoldingTask;

import com.github.rinde.rinsim.ui.renderers.ViewPort;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.jetbrains.annotations.NotNull;


public class AGVRenderer extends AbstractCanvasRenderer {

    private static final int ROUND_RECT_ARC_HEIGHT = 5;
    private final RoadModel roadModel;
    private Font battery;
    Font Bold;
    Font standard;

    AGVRenderer(RoadModel r) {
        roadModel = r;
    }

    public static AGVRendererBuilder builder() {
        return new AGVRendererBuilder();
    }

    @Override
	public void renderDynamic(@NotNull GC gc, @NotNull ViewPort vp, long time) {
    	if(battery==null) {
    		battery = new Font(gc.getDevice(),"Arial",6, 0);
    		Bold = new Font(gc.getDevice(),"Arial",10, SWT.BOLD);
    		standard = new Font(gc.getDevice(),"Arial",10, 0);
    	}
		for (AGVAgent agv : this.roadModel.getObjectsOfType(AGVAgent.class)) {
			if (!agv.getPosition().isPresent()) {
				continue;
			}

			final Point p = agv.getPosition().get();

			final int x = vp.toCoordX(p.x);
			final int y = vp.toCoordY(p.y);

			drawBattery(x, y, gc, agv.getRemainingBatteryCapacityPercentage()/100);
			drawPackage(agv, x, y, gc, agv.allocatedTask);
			
			gc.setFont(standard);


			if(agv.allocatedTask!= null && agv.allocatedTask.beginPosition.equals(agv.getPosition().get())) {
					 	gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_MAGENTA));
						gc.drawText("Loading", x - 20, y+5, true);
			}else if(agv.waiting) {
					gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_MAGENTA));
					gc.drawText("Waiting", x - 20, y+5, true);
				 }
			else if (agv.isIdle()) {
					// Draw robot's idle time
					gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_BLUE));
					gc.drawText("Idle", x - 20, y+5, true);
			} else {
				// Draw robot's estimated time of arrival
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_GREEN));
				gc.drawText("Driving to " + (int) agv.endPoint.get().x + "." + (int) agv.endPoint.get().y, x - 40, y+5, true);
			}
			 
			gc.setFont(Bold);

			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
			gc.drawText(""+agv.ID%1000, x - 30, y-10, true);
			
			gc.setFont(standard);

		}
	}

	private void drawPackage(AGVAgent agv, int x, int y, GC gc, BasicTask bt) {
		gc.setFont(Bold);
		if(!agv.hasParcel()) {
			return;
		}
		// Draw waiting for ants.
		String carId = agv.getCarId();
		if (carId != null) {
			if(bt instanceof HoldingTask) {
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_MAGENTA));
				if(!((HoldingTask) bt).returnAllowed) 
					gc.drawText("TRANSFER", x +5, y - 12, true);
				else
					gc.drawText("RETURN", x +5, y - 12, true);
			}
			else {
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_CYAN));
				gc.drawText("DELIVERY", x +5, y - 12, true);
			}
		}
		
	}

	private void drawBattery(int x, int y, @NotNull GC gc, double currentBattery) {
		x += -5;
		y += -15;

		gc.setFont(battery);
		
		final org.eclipse.swt.graphics.Point extent = gc.textExtent(Integer.toString(100) + "%");
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

		gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2, extent.x + 2, extent.y + 2, ROUND_RECT_ARC_HEIGHT,
				ROUND_RECT_ARC_HEIGHT);

		if (currentBattery > 0.50) {
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));

		} else if (currentBattery > 0.25) {
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));

		} else {
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
		}

		gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2,
				(int) Math.max((extent.x + 2) * currentBattery, 0), extent.y + 2, ROUND_RECT_ARC_HEIGHT,
				ROUND_RECT_ARC_HEIGHT);

		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
		//gc.drawText( "%", x - extent.x / 2 + 1,y - extent.y / 2 + 1, true);		
	}

	@Override
	public void renderStatic(GC gc, ViewPort vp) {		
	}
}

