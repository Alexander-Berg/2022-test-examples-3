package ru.yandex.market.checkout.checkouter.actualization.flow.configuration;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.actualization.flow.BaseReceivingFlowTest;
import ru.yandex.market.reservation.feature.api.config.flow.FlowNamedStage;
import ru.yandex.market.reservation.feature.api.config.flow.FlowStages;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

public class FlowConfigurationTest extends BaseReceivingFlowTest {

    @Test
    void shouldCollectNamedStages() {
        FlowConfiguration<?, ?> config =
                FlowConfigurations.configurationOf(c -> {
                        }, fetchAsync(1)
                                .whenSuccess(
                                        fetchNamed(FlowStages.makeSingleOrderNamedStage("some stage", Object.class),
                                                c -> 2)
                                                .whenSuccess(fetch(3)
                                                        .whenSuccess(fetchNamedAsync(
                                                                FlowStages.makeSingleOrderNamedStage("some stage 2",
                                                                        Object.class),
                                                                c -> 3)))
                                )
                );

        var stages = config.namedStages().stream()
                .map(FlowNamedStage::name)
                .collect(toUnmodifiableList());

        assertThat(stages, hasItems("some stage", "some stage 2"));
    }

    @Test
    void shouldInvokeConsumerOnSingleResourceResolve() throws Throwable {
        var stage1 = FlowStages.makeSingleOrderNamedStage("some stage", Object.class);
        var stage2 = FlowStages.makeSingleOrderNamedStage("some stage 2", Object.class);
        var config =
                FlowConfigurations.configurationOf(c -> {
                        }, fetchAsync(1)
                                .whenSuccess(
                                        fetchNamed(stage1, c -> 2)
                                                .whenSuccess(fetch(3)
                                                        .whenSuccess(fetchNamedAsync(stage2, c -> 3)))
                                )
                );

        var featureStage = config.subscribe(stage1, msg -> msg + " modified");

        var rootStage = config.apply(context());

        rootStage.awaitChildrenSilently();

        var msg = rootStage.session().await(featureStage).orElseThrow();

        assertThat(msg, is("2 modified"));
    }

    @Test
    void shouldInvokeConsumerOn2ResourceResolve() throws Throwable {
        var stage1 = FlowStages.makeSingleOrderNamedStage("some stage", Integer.class);
        var stage2 = FlowStages.makeSingleOrderNamedStage("some stage 2", Integer.class);
        FlowConfiguration<SimpleContext, SimpleContext> config =
                FlowConfigurations.configurationOf(c -> {
                        }, fetchAsync(1)
                                .whenSuccess(
                                        fetchNamed(stage1, c -> 2)
                                                .whenSuccess(fetch(3)
                                                        .whenSuccess(fetchNamedAsync(stage2, c -> 3)))
                                )
                );

        var featureStage = config.subscribe(stage1, stage2,
                Integer::sum);

        var rootStage = config.apply(context());

        rootStage.awaitChildrenSilently();

        var msg = rootStage.session().await(featureStage).orElseThrow();

        assertThat(msg, is(5));
    }

    @Test
    void shouldInvokeConsumerOn3ResourceResolve() throws Throwable {
        var stage1 = FlowStages.makeSingleOrderNamedStage("some stage", Integer.class);
        var stage2 = FlowStages.makeSingleOrderNamedStage("some stage 2", Integer.class);
        var stage3 = FlowStages.makeSingleOrderNamedStage("some stage 3", Integer.class);
        FlowConfiguration<SimpleContext, SimpleContext> config =
                FlowConfigurations.configurationOf(c -> {
                        }, fetchAsync(1)
                                .whenSuccess(
                                        fetchNamed(stage1, c -> 2)
                                                .whenSuccess(fetchNamed(stage2, c -> 3)
                                                        .whenSuccess(fetchNamedAsync(stage3, c -> 3)))
                                )
                );

        var featureStage = config.subscribe(stage1, stage2, stage3,
                (v1, v2, v3) -> v1 + v2 + v3);

        var rootStage = config.apply(context());

        rootStage.awaitChildrenSilently();

        var msg = rootStage.session().await(featureStage).orElseThrow();

        assertThat(msg, is(8));
    }

    @Test
    void shouldInvokeChainedSubscriber() throws Throwable {
        var stage1 = FlowStages.makeSingleOrderNamedStage("some stage", Integer.class);
        var stage2 = FlowStages.makeSingleOrderNamedStage("some stage 2", Integer.class);
        var stage3 = FlowStages.makeSingleOrderNamedStage("some stage 3", Integer.class);
        FlowConfiguration<SimpleContext, SimpleContext> config =
                FlowConfigurations.configurationOf(c -> {
                        }, fetchAsync(1)
                                .whenSuccess(
                                        fetchNamed(stage1, c -> 2)
                                                .whenSuccess(fetchNamed(stage2, c -> 3)
                                                        .whenSuccess(fetchNamedAsync(stage3, c -> 3)))
                                )
                );

        var featureStage = config.subscribe(stage1, stage2,
                Integer::sum);

        var featureStage2 = config.subscribe(featureStage, stage3,
                Integer::sum);

        var rootStage = config.apply(context());

        rootStage.awaitChildrenSilently();
        rootStage.awaitChildrenSilently();

        var msg = rootStage.session().await(featureStage2).orElseThrow();

        assertThat(msg, is(8));
    }

}
