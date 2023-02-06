package ru.yandex.market.deepmind.common.hiding.configuration;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.util.ReflectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;

public class HidingConfigurationsTest {

    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(1).build();
    }

    @Test
    public void readAllIsSuccessful() {
        var hidingConfigurations = HidingConfigurations.readAllFromResourceFile();
        Assertions.assertThat(hidingConfigurations).isNotNull();
    }

    @Test
    public void toCreateTicket() {
        var config = random.nextObject(HidingConfiguration.class);

        var actualConfig = config.toCreateTicket("", "", List.of(), List.of(), List.of(), List.of());

        Assertions.assertThat(actualConfig)
            .isEqualToComparingOnlyGivenFields(config, getFields(actualConfig, "catman", "sskus", "catTeams"));
    }

    @Test
    public void toCloseTicket() {
        var config = random.nextObject(HidingConfiguration.class);

        var actualConfig = config.toCloseTicket();

        Assertions.assertThat(actualConfig)
            .isEqualToComparingOnlyGivenFields(config, getFields(actualConfig));
    }

    @Test
    public void realConfigsCanBeRendered() {
        for (var hidingConfiguration : HidingConfigurations.readAllFromResourceFile()) {
            try {
                var createConfig = hidingConfiguration.toCreateTicket("http://url", "catman", List.of(
                    new ServiceOfferReplica().setSupplierId(1).setShopSku("ssku1"),
                    new ServiceOfferReplica().setSupplierId(1).setShopSku("ssku2"),
                    new ServiceOfferReplica().setSupplierId(2).setShopSku("sku")
                    ),
                    List.of(
                        new Supplier().setId(1).setName("supplier"),
                        new Supplier().setId(2).setName("supplier2")
                    ),
                    List.of(),
                    List.of());
                var closeConfig = hidingConfiguration.toCloseTicket();
            } catch (Exception e) {
                Assertions.fail(String.format("Fail to render config:\n%s", hidingConfiguration), e);
            }
        }
    }

    @Test
    public void assigneeWillBeNullIfCatmanIsMissing() {
        var config = random.nextObject(HidingConfiguration.class);
        config.setAssignee(" {{catman}}");

        var actualConfig = config.toCreateTicket("url", null, List.of(), List.of(), List.of(), List.of());

        Assertions.assertThat(actualConfig.getAssignee()).isNull();
    }

    @Test
    public void followerWillBeSkippedIfCatmanIsMissing() {
        var config = random.nextObject(HidingConfiguration.class);
        config.setFollowers(List.of(" {{catman}}", "@not_real_user"));

        var actualConfig = config.toCreateTicket("url", null, List.of(), List.of(), List.of(), List.of());

        Assertions.assertThat(actualConfig.getFollowers())
            .containsExactly("@not_real_user");
    }

    private static String[] getFields(Object obj, String... ignoreFields) {
        return Stream.concat(
            ReflectionUtils.getDeclaredFields(obj).stream(),
            ReflectionUtils.getInheritedFields(obj.getClass()).stream()
        )
            .map(Field::getName)
            .filter(field -> !Arrays.asList(ignoreFields).contains(field))
            .toArray(String[]::new);
    }
}
