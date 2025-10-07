package guru.nicks.test;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Builder nested in another one. For Lombok builders, {@link #build()} is auto-generated.
 * <p>
 * Copy-pasted from Core module because this module cannot depend on Core (otherwise a circular dependency would be
 * introduced).
 *
 * @param <T> type produced by this builder
 * @param <P> parent builder type
 */
@RequiredArgsConstructor
abstract class NestedBuilder<T, P> {

    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final P parentBuilder;

    /**
     * Builds and returns the configured object.
     *
     * @return object just built
     */
    public abstract T build();

    /**
     * Returns to the parent builder to continue building the parent object.
     *
     * @return parent builder instance
     */
    public P and() {
        return parentBuilder;
    }

}
