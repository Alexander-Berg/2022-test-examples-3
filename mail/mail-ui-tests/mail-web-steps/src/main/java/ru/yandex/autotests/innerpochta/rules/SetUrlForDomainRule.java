package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.util.props.HostRootProperties;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;

import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_BASE_URL;

/**
 * @author crafty
 */

public class SetUrlForDomainRule extends ExternalResource {

    public static SetUrlForDomainRule setUrlForDomainRule() {
        return new SetUrlForDomainRule();
    }

    public SetUrlForDomainRule() {
        HostRootProperties.hostrootProps().setTestHost(UrlProps.urlProps().getBaseUri());
        UrlProps.urlProps().setBaseUri(MAIL_BASE_URL);
    }
}
