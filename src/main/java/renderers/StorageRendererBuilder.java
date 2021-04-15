package renderers;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import org.jetbrains.annotations.NotNull;

public class StorageRendererBuilder extends ModelBuilder.AbstractModelBuilder<StorageRenderer, Void> {

    private static final long serialVersionUID = -1772420262312399130L;

    StorageRendererBuilder() {
        setDependencies(RoadModel.class,PDPModel.class);
    }

    public StorageRenderer build(@NotNull DependencyProvider dependencyProvider) {
        final RoadModel rm = dependencyProvider.get(RoadModel.class);
        final PDPModel pm = dependencyProvider.get(PDPModel.class);
        return new StorageRenderer(rm, pm);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else {
            return o instanceof StorageRendererBuilder;
        }
    }

    public int hashCode() {
        int h = 3;
        h = h * 1000003;
        return h;
    }
}
