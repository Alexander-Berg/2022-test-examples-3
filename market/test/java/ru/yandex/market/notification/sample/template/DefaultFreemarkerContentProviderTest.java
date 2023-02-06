package ru.yandex.market.notification.sample.template;

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.notification.common.model.TextNotificationContent;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.sample.service.provider.DefaultFreemarkerContentProvider;
import ru.yandex.market.notification.service.content.NotificationTemplateModelComposer;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для проверки корректной работы {@link DefaultFreemarkerContentProvider}.
 *
 * @author avetokhin 16/06/16.
 */
public class DefaultFreemarkerContentProviderTest extends AbstractBaseFreemarkerContentProviderTest {

    private static final String TEXT = "Content: key1:value1, key2:25";

    private final static Map<String, Object> MODEL = ImmutableMap.<String, Object>builder()
            .put("key1", "value1")
            .put("key2", 25)
            .build();

    /**
     * {@link DefaultFreemarkerContentProvider} должен отрисовать шаблон и вернуть корректный контент.
     *
     * @throws IOException если произошла ошибка IO
     */
    @Test
    public void provideValidContentTest() throws IOException {
        // Init model composer mock.
        final NotificationTemplateModelComposer modelComposer = mock(NotificationTemplateModelComposer.class);
        when(modelComposer.compose(any())).thenReturn(MODEL);

        // Init template provider mock.
        final NotificationTemplateProvider templateProvider = mock(NotificationTemplateProvider.class);
        when(templateProvider.provide(any()))
            .thenReturn(new FreemarkerTemplate(getTemplate("base-freemarker-content-provider-test.ftl")));

        // Init context mock.
        final NotificationContentProviderContext context = mock(NotificationContentProviderContext.class);
        when(context.getTemplateProvider()).thenReturn(templateProvider);

        final DefaultFreemarkerContentProvider provider = new DefaultFreemarkerContentProvider(modelComposer);

        final NotificationContent content = provider.provide(context);
        assertThat(content, instanceOf(TextNotificationContent.class));

        final TextNotificationContent defaultContent = (TextNotificationContent) content;
        assertThat(defaultContent.getText(), equalTo(TEXT));
    }

}
