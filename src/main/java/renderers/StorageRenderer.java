package renderers;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.VehicleState;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;

import agents.StorageAgent;
import com.github.rinde.rinsim.ui.renderers.UiSchema;
import com.github.rinde.rinsim.ui.renderers.ViewPort;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Transform;

public class StorageRenderer extends AbstractCanvasRenderer {
	static final float AT_SITE_ROTATION = 0f;
	static final float IN_CARGO_ROTATION = 20f;
	static final Point LABEL_OFFSET = new Point(-15, -40);
	static final double MAX_PERC = 100d;
	@SuppressWarnings("unused")
	private static final int ROUND_RECT_ARC_HEIGHT = 5;
	@SuppressWarnings("unused")
	private static final int X_OFFSET = -5;
	@SuppressWarnings("unused")
	private static final int Y_OFFSET = -30;

	RoadModel roadModel;
	PDPModel pdpModel;
	final UiSchema uiSchema;

	StorageRenderer(RoadModel rm, PDPModel pm) {
		roadModel = rm;
		pdpModel = pm;
		uiSchema = new UiSchema(false);
		uiSchema.add(StorageAgent.class, "/storageAgent.png");
	}

	public void renderStatic(GC gc, ViewPort vp) {
	}

	public void renderDynamic(GC gc, ViewPort vp, long time) {
		
		uiSchema.initialize(gc.getDevice());

		final Collection<Parcel> parcels = pdpModel.getParcels(ParcelState.values());
		final Image image = uiSchema.getImage(StorageAgent.class);
		checkState(image != null);

		synchronized (pdpModel) {
			final Set<Vehicle> vehicles = pdpModel.getVehicles();
			final Map<Parcel, Vehicle> mapping = new LinkedHashMap<Parcel, Vehicle>();
			for (final Vehicle v : vehicles) {
				for (final Parcel p : pdpModel.getContents(v)) {
					mapping.put(p, v);
				}
				if (pdpModel.getVehicleState(v) != VehicleState.IDLE) {
					final PDPModel.VehicleParcelActionInfo vpai = pdpModel.getVehicleActionInfo(v);
					mapping.put(vpai.getParcel(), vpai.getVehicle());
				}
			}

			for (final Parcel p : parcels) {
				drawBox(p, gc, vp, time, image, mapping);
			}
		}
		
	}

	@SuppressWarnings("unused")
	void drawBox(Parcel p, GC gc, ViewPort vp, long time, Image image, Map<Parcel, Vehicle> mapping) {
		float rotation = AT_SITE_ROTATION;
		int offsetX = 0;
		int offsetY = 0;
		@Nullable
		final ParcelState ps = pdpModel.getParcelState(p);
		if (ps == null) {
			return;
		}
		if (ps == ParcelState.AVAILABLE) {
			final Point pos = roadModel.getPosition(p);
			final int x = vp.toCoordX(pos.x);
			final int y = vp.toCoordY(pos.y);
		} else if (ps == ParcelState.PICKING_UP || ps == ParcelState.DELIVERING) {

			final Vehicle v = mapping.get(p);
			final PDPModel.VehicleParcelActionInfo vpai = pdpModel.getVehicleActionInfo(v);
			final Point pos = roadModel.getPosition(v);
			final int x = vp.toCoordX(pos.x);
			final int y = vp.toCoordY(pos.y);
			final double percentage = 1d - vpai.timeNeeded() / (double) p.getPickupDuration();
			final String text = (int) (percentage * MAX_PERC) + "%";

			final float rotFac = (float) (ps == ParcelState.PICKING_UP ? percentage : 1d - percentage);
			rotation = IN_CARGO_ROTATION * rotFac;

			final int textWidth = gc.textExtent(text).x;
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLUE));
			gc.drawText(text, (int) LABEL_OFFSET.x + x - textWidth / 2, (int) LABEL_OFFSET.y + y, true);

			if (!ps.isDelivered()) {
				if (rotation == 0f) {
					gc.drawImage(image, offsetX, offsetY);
				} else {
					final Transform oldTransform = new Transform(gc.getDevice());
					gc.getTransform(oldTransform);

					final Transform transform = new Transform(gc.getDevice());
					transform.translate(offsetX + image.getBounds().width / 2, offsetY + image.getBounds().height / 2);
					transform.rotate(rotation);
					transform.translate(-(offsetX + image.getBounds().width / 2),
							-(offsetY + image.getBounds().height / 2));
					gc.setTransform(transform);
					gc.drawImage(image, offsetX, offsetY);
					gc.setTransform(oldTransform);
					transform.dispose();
					oldTransform.dispose();
				}
			}
		}
	}

	public static StorageRendererBuilder builder() {
		return new StorageRendererBuilder();
	}
}
