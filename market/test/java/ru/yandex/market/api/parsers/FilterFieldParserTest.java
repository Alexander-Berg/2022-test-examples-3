package ru.yandex.market.api.parsers;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.controller.v2.ParametersV2;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.v2.FilterField;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.server.CapiRequestAttribute;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

@WithContext
public class FilterFieldParserTest extends ContainerTestBase {
    @Test
    public void shouldBeSortAndStatisticFieldsInVersionBeforeV2_1_3_evenIfFieldsDoesNotContainsIt() {
        ContextHolder.update(ctx -> ctx.setVersion(Version.V2_0_0));

        assertThat(
                fields(MockRequestBuilder.start().attribute(CapiRequestAttribute.CONTEXT, ContextHolder.get()).param("fields", "DESCRIPTION").build()),
            containsInAnyOrder(
                FilterField.DESCRIPTION,
                FilterField.SORTS,
                FilterField.STATISTICS
            )
        );
    }

    @Test
    public void shouldBeSortAndStatisticFieldsInVersionBeforeV2_1_3_evenIfFieldsNotSetInRequest() {
        ContextHolder.update(ctx -> ctx.setVersion(Version.V2_0_0));

        assertThat(
                fields(MockRequestBuilder.start().attribute(CapiRequestAttribute.CONTEXT, ContextHolder.get()).build()),
            containsInAnyOrder(
                FilterField.SORTS,
                FilterField.STATISTICS
            )
        );
    }

    @Test
    public void shouldNotBeSortAndStatisticFieldInVersionV2_1_3_ifFieldNotSet() {
        ContextHolder.update(ctx -> ctx.setVersion(Version.V2_1_3));

        assertThat(
                fields(MockRequestBuilder.start().attribute(CapiRequestAttribute.CONTEXT, ContextHolder.get()).param("fields", "DESCRIPTION").build()),
            contains(
                FilterField.DESCRIPTION
            )
        );
    }

    public Collection<? extends Field> fields(HttpServletRequest request) {
        return new ParametersV2.FilterFieldsParser().get(request).getValue();
    }

}
