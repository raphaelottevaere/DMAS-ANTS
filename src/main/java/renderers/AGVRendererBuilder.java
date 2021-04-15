package renderers;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import org.jetbrains.annotations.NotNull;

public class AGVRendererBuilder extends ModelBuilder.AbstractModelBuilder<AGVRenderer, Void> {

    private static final long serialVersionUID = -1772420262312399130L;

    AGVRendererBuilder() {
        setDependencies(RoadModel.class);
    }

    @Override
    public AGVRenderer build(@NotNull DependencyProvider dependencyProvider) {
        final RoadModel rm = dependencyProvider.get(RoadModel.class);
        return new AGVRenderer(rm);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else {
            //RobotRendererBuilder that = (RobotRendererBuilder)o;
            return o instanceof AGVRendererBuilder;
        }
    }

    public int hashCode() {
        int h = 1;
        h = h * 1000003;
        return h;
    }
}
