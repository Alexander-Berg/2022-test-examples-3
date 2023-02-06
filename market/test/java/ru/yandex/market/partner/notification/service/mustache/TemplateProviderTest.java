package ru.yandex.market.partner.notification.service.mustache;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import ru.yandex.market.partner.notification.service.mustache.model.Template;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TemplateProviderTest {
    private final TemplateProvider provider = new TemplateProvider(getTemplates());
    private static final Template templateDummy100 = mock(Template.class);
    private static final Template templateDummy200 = mock(Template.class);

    @Test
    void getTemplate() {
        assertThat(provider.getTemplate(100L),
                equalTo(templateDummy100));
        assertThat(provider.getTemplate(200L),
                equalTo(templateDummy200));
    }

    @Test
    void getTemplateNotFound() {
        assertThrows(RuntimeException.class, () -> provider.getTemplate(101L));
    }


    private static Map<Long, Template> getTemplates() {
        Objects.requireNonNull(templateDummy100);
        Objects.requireNonNull(templateDummy200);
        return Map.of(
                100L, templateDummy100,
                200L, templateDummy200
        );
    }
}
