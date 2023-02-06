package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.cal.util.CalConsts;
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.autotests.passport.api.common.Properties;

import static ru.yandex.autotests.passport.api.common.data.PassportEnv.TEAM;

/**
 * @author crafty
 */
public class SetCorpUrlRule extends ExternalResource {
    public static SetCorpUrlRule setCorpUrlRule() {
        return new SetCorpUrlRule();
    }

    public SetCorpUrlRule() {
        if (UrlProps.urlProps().getBaseUri().contains("mail"))
            UrlProps.urlProps().setProdUri(MailConst.CORP_BASE_URL);
        else {
            UrlProps.urlProps().setProdUri(CalConsts.CORP_BASE_URL);
            UrlProps.urlProps().setBaseUri(UrlProps.urlProps().getCorpUri().toString());
        }
        Properties.props().setPassportEnv(TEAM);
    }
}
