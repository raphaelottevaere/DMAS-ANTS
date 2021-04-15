package experiments;

import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.github.rinde.rinsim.scenario.generator.Parcels.ParcelGenerator;
import com.google.common.collect.ImmutableList;

public class DeliveryTaskGenerators implements ParcelGenerator {
	
    public DeliveryTaskGenerators() {}

	@Override
    public ImmutableList<AddParcelEvent> generate(long seed, @NotNull ScenarioGenerator.TravelTimes travelModel, long endTime) {
		   com.google.common.collect.ImmutableList.Builder<AddParcelEvent> eventList = ImmutableList.builder();

	            eventList.add(AddParcelEvent.create(Parcel
	                    // Just put some random positions. Not that important as they will be chosen randomly in handler.
	                    .builder(new Point(4, 2), new Point(0, 0))
	                    .neededCapacity(1)
	                    .buildDTO()
	            ));

	        return eventList.build();
    }

    @Override
    public Point getCenter() {
        return null;
    }

    @Override
    public Point getMin() {
        return null;
    }

    @Override
    public Point getMax() {
        return null;
    }
}

