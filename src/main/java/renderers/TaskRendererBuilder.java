package renderers;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import org.jetbrains.annotations.NotNull;

public class TaskRendererBuilder extends ModelBuilder.AbstractModelBuilder<TaskRenderer, Void> {

    private static final long serialVersionUID = -1772420262312399130L;

    TaskRendererBuilder() {
        setDependencies(RoadModel.class);
    }

    public TaskRenderer build(@NotNull DependencyProvider dependencyProvider) {
        final RoadModel rm = dependencyProvider.get(RoadModel.class);
        return new TaskRenderer(rm);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else {
            return o instanceof TaskRendererBuilder;
        }
    }

    public int hashCode() {
        int h = 1;
        h = h * 1000003;
        return h;
    }

}