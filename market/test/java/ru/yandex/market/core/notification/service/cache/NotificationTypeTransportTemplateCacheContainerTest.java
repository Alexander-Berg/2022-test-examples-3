package ru.yandex.market.core.notification.service.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.market.notification.model.template.MbiNotificationTemplate;
import ru.yandex.market.notification.model.template.NotificationTypeTransportTemplateLink;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты для {@link NotificationTypeTransportTemplateCacheContainer}.
 *
 * @author avetokhin 24/08/16.
 */
public class NotificationTypeTransportTemplateCacheContainerTest {

    private static final long NN_TYPE_1 = 10L;
    private static final long NN_TYPE_2 = 11L;
    private static final long NN_TYPE_3 = 12L;

    private static final NotificationTransport TRANSPORT_1 = NotificationTransport.EMAIL;
    private static final NotificationTransport TRANSPORT_2 = NotificationTransport.MBI_WEB_UI;
    private static final NotificationTransport TRANSPORT_3 = NotificationTransport.SMS;

    private static final long TEMPLATE_ID_1 = 1L;
    private static final long TEMPLATE_ID_2 = 2L;
    private static final long TEMPLATE_ID_3 = 3L;

    private static final MbiNotificationTemplate TEMPLATE_1 = new MbiNotificationTemplate(TEMPLATE_ID_1, "", null, "", null, null);
    private static final MbiNotificationTemplate TEMPLATE_2 = new MbiNotificationTemplate(TEMPLATE_ID_2, "", null, "", null, null);

    private static final NotificationTypeTransportTemplateLink LINK_1 =
            new NotificationTypeTransportTemplateLink(NN_TYPE_1, TRANSPORT_1, TEMPLATE_ID_1);
    private static final NotificationTypeTransportTemplateLink LINK_2 =
            new NotificationTypeTransportTemplateLink(NN_TYPE_1, TRANSPORT_2, TEMPLATE_ID_1);
    private static final NotificationTypeTransportTemplateLink LINK_3 =
            new NotificationTypeTransportTemplateLink(NN_TYPE_2, TRANSPORT_1, TEMPLATE_ID_2);

    @Test
    public void getByIdTest() {
        final NotificationTypeTransportTemplateCacheContainer cacheContainer = prepareCache();

        final Optional<MbiNotificationTemplate> tt1 = cacheContainer.getTemplateById(TEMPLATE_ID_1);
        final Optional<MbiNotificationTemplate> tt2 = cacheContainer.getTemplateById(TEMPLATE_ID_2);
        final Optional<MbiNotificationTemplate> tt3 = cacheContainer.getTemplateById(TEMPLATE_ID_3);

        // Проверка контракта.
        assertThat(tt1, notNullValue());
        assertThat(tt2, notNullValue());
        assertThat(tt3, notNullValue());

        // Проверка значений.
        assertThat(tt1.isPresent(), equalTo(true));
        assertThat(tt1.get(), equalTo(TEMPLATE_1));

        assertThat(tt2.isPresent(), equalTo(true));
        assertThat(tt2.get(), equalTo(TEMPLATE_2));

        assertThat(tt3.isPresent(), equalTo(false));
    }

    @Test
    public void getByNotificationTypeAndTransportTest() {
        final NotificationTypeTransportTemplateCacheContainer cacheContainer = prepareCache();

        final Optional<MbiNotificationTemplate> tt1 =
                cacheContainer.getTemplateByNotificationTypeAndTransport(NN_TYPE_1, TRANSPORT_1);
        final Optional<MbiNotificationTemplate> tt2 =
                cacheContainer.getTemplateByNotificationTypeAndTransport(NN_TYPE_1, TRANSPORT_2);
        final Optional<MbiNotificationTemplate> tt3 =
                cacheContainer.getTemplateByNotificationTypeAndTransport(NN_TYPE_2, TRANSPORT_1);

        final Optional<MbiNotificationTemplate> tt4 =
                cacheContainer.getTemplateByNotificationTypeAndTransport(NN_TYPE_1, TRANSPORT_3);
        final Optional<MbiNotificationTemplate> tt5 =
                cacheContainer.getTemplateByNotificationTypeAndTransport(NN_TYPE_2, TRANSPORT_2);
        final Optional<MbiNotificationTemplate> tt6 =
                cacheContainer.getTemplateByNotificationTypeAndTransport(NN_TYPE_3, TRANSPORT_1);

        // Проверка контракта.
        assertThat(tt1, notNullValue());
        assertThat(tt2, notNullValue());
        assertThat(tt3, notNullValue());
        assertThat(tt4, notNullValue());
        assertThat(tt5, notNullValue());
        assertThat(tt6, notNullValue());

        // Проверка значений.
        assertThat(tt1.isPresent(), equalTo(true));
        assertThat(tt1.get(), equalTo(TEMPLATE_1));

        assertThat(tt2.isPresent(), equalTo(true));
        assertThat(tt2.get(), equalTo(TEMPLATE_1));

        assertThat(tt3.isPresent(), equalTo(true));
        assertThat(tt3.get(), equalTo(TEMPLATE_2));

        assertThat(tt4.isPresent(), equalTo(false));
        assertThat(tt5.isPresent(), equalTo(false));
        assertThat(tt6.isPresent(), equalTo(false));
    }

    @Test
    public void getTransportsForNotificationTypeTest() {
        final NotificationTypeTransportTemplateCacheContainer cacheContainer = prepareCache();

        final Collection<NotificationTransport> tr1 = cacheContainer.getTransportsForNotificationType(NN_TYPE_1);
        final Collection<NotificationTransport> tr2 = cacheContainer.getTransportsForNotificationType(NN_TYPE_2);
        final Collection<NotificationTransport> tr3 = cacheContainer.getTransportsForNotificationType(NN_TYPE_3);

        // Проверка контракта.
        assertThat(tr1, notNullValue());
        assertThat(tr2, notNullValue());
        assertThat(tr3, notNullValue());

        // Проверка значений.
        assertThat(tr1, hasSize(2));
        assertThat(tr2, hasSize(1));
        assertThat(tr3, hasSize(0));

        assertThat(tr1, containsInAnyOrder(TRANSPORT_1, TRANSPORT_2));
        assertThat(tr2, containsInAnyOrder(TRANSPORT_1));

    }


    private NotificationTypeTransportTemplateCacheContainer prepareCache() {
        final Collection<MbiNotificationTemplate> templates = Arrays.asList(TEMPLATE_1, TEMPLATE_2);
        final Collection<NotificationTypeTransportTemplateLink> links = Arrays.asList(LINK_1, LINK_2, LINK_3);

        return new NotificationTypeTransportTemplateCacheContainer(templates, links);
    }
}
