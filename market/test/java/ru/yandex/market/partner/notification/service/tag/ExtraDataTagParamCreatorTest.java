package ru.yandex.market.partner.notification.service.tag;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notification.model.context.FilterParamCreatorContext;
import ru.yandex.market.notification.model.context.FilterParamCreatorContextImpl;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.MbiNotificationData;
import ru.yandex.market.notification.simple.model.context.NotificationContextImpl;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.service.tag.model.NotificationTags;

public class ExtraDataTagParamCreatorTest extends AbstractFunctionalTest {
    @Autowired
    ExtraDataTagParamCreator extraDataTagParamCreator;

    @Test
    public void shouldCreateNotificationTags() {
        ArrayListNotificationData<Element> templateData = new ArrayListNotificationData<>();
        Element top = new Element("top");
        Element middle = new Element("middle");
        Element bottom = new Element("bottom");
        bottom.addContent("testTagValue");
        top.addContent(middle);
        middle.addContent(bottom);
        templateData.add(middle);
        FilterParamCreatorContext context = new FilterParamCreatorContextImpl(getNotificationContext(100200300400500L, templateData), List.of());
        extraDataTagParamCreator.addValueProvider(
                "testTag1",
                new TagContextValueProvider(
                        "prefix_", "_postfix", "top.middle.bottom", "testTag1", Map.of("testTagValue","correctValue")
                )
        );
        extraDataTagParamCreator.addValueProvider(
                "testTag2",
                new TagContextValueProvider("prefix_", "_postfix", "middle.bottom", "testTag2", null)
        );
        List<NotificationTags> tags = extraDataTagParamCreator.provide(context);
        NotificationTags expected = new NotificationTags(
                Map.of(
                        "testTag1", "prefix_correctValue_postfix",
                        "fixedTag1", "fixedValue1"
                ),
                "TEST_SCOPE",
                true
        );
        Assertions.assertThat(List.of(expected)).isEqualTo(tags);
    }

    @Nonnull
    private NotificationContext getNotificationContext(long id, ArrayListNotificationData<Element> templateData) {
        return new NotificationContextImpl(
                new CodeNotificationType(id),
                NotificationPriority.HIGH,
                NotificationTransport.MOBILE_PUSH,
                List.of(),
                Instant.now(),
                new MbiNotificationData(templateData, null, null),
                null,
                false
        );
    }

    @Test
    public void shouldFindEmptyIfNoFiltrationSettings() {
        ArrayListNotificationData<Element> templateData = new ArrayListNotificationData<>();
        FilterParamCreatorContext context = new FilterParamCreatorContextImpl(getNotificationContext(1L, templateData), List.of());
        List<NotificationTags> tags = extraDataTagParamCreator.provide(context);
        Assertions.assertThat(List.of()).isEqualTo(tags);
    }

    @Test
    public void shouldFindEmptyIfNoExtraData() {
        ArrayListNotificationData<Element> templateData = new ArrayListNotificationData<>();
        FilterParamCreatorContext context = new FilterParamCreatorContextImpl(getNotificationContext(2L, templateData), List.of());
        List<NotificationTags> tags = extraDataTagParamCreator.provide(context);
        Assertions.assertThat(List.of()).isEqualTo(tags);
    }
}
