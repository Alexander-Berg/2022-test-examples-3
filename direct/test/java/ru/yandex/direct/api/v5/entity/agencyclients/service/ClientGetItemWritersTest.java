package ru.yandex.direct.api.v5.entity.agencyclients.service;

import java.util.Collections;

import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.generalclients.ClientGetItem;
import com.yandex.direct.api.v5.generalclients.ClientSettingGetEnum;
import com.yandex.direct.api.v5.generalclients.ClientSettingGetItem;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.repository.ClientMapping;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@SuppressWarnings("unchecked")
public class ClientGetItemWritersTest {
    public static final ClientId CLIENT_ID = ClientId.fromLong(1221L);
    private static final Long USER_ID = 1225L;
    private static ClientGetItemWriters writers;

    @BeforeClass
    public static void beforeClass() {
        writers = new ClientGetItemWriters();
    }

    @Test
    public void getSettingsWriter_nullClient() throws Exception {
        ClientGetItemWriter settingsWriter = writers.getSettingsWriter(Collections::emptyMap, Collections::emptyMap);
        ClientGetItem getItem = new ClientGetItem();
        settingsWriter.write(
                Collections.singleton(RequestedField.SETTINGS), getItem, new User().withClientId(CLIENT_ID));
        assertThat(getItem.getSettings(), contains(
                beanDiffer(new ClientSettingGetItem()
                        .withOption(ClientSettingGetEnum.DISPLAY_STORE_RATING).withValue(YesNoEnum.YES)),
                beanDiffer(new ClientSettingGetItem()
                        .withOption(ClientSettingGetEnum.CORRECT_TYPOS_AUTOMATICALLY).withValue(YesNoEnum.YES)),
                beanDiffer(new ClientSettingGetItem()
                        .withOption(ClientSettingGetEnum.SHARED_ACCOUNT_ENABLED).withValue(YesNoEnum.NO))
        ));
    }

    @Test
    public void getSettingsWriter_emptyClient() throws Exception {
        ClientGetItemWriter settingsWriter = writers.getSettingsWriter(
                () -> Collections.singletonMap(CLIENT_ID, ClientMapping.postProcess(new Client())),
                Collections::emptyMap);
        ClientGetItem getItem = new ClientGetItem();
        settingsWriter.write(
                Collections.singleton(RequestedField.SETTINGS), getItem, new User().withClientId(CLIENT_ID));
        assertThat(getItem.getSettings(), contains(
                beanDiffer(new ClientSettingGetItem()
                        .withOption(ClientSettingGetEnum.DISPLAY_STORE_RATING).withValue(YesNoEnum.YES)),
                beanDiffer(new ClientSettingGetItem()
                        .withOption(ClientSettingGetEnum.CORRECT_TYPOS_AUTOMATICALLY).withValue(YesNoEnum.YES)),
                beanDiffer(new ClientSettingGetItem()
                        .withOption(ClientSettingGetEnum.SHARED_ACCOUNT_ENABLED).withValue(YesNoEnum.NO))
        ));
    }

    @Test
    public void getSettingsWriter_clientWithOptions() throws Exception {
        Client client = new Client()
                .withHideMarketRating(true)
                .withNoTextAutocorrection(true);
        ClientGetItemWriter settingsWriter = writers.getSettingsWriter(
                () -> Collections.singletonMap(CLIENT_ID, client),
                () -> Collections.singletonMap(CLIENT_ID, true));
        ClientGetItem getItem = new ClientGetItem();
        settingsWriter.write(
                Collections.singleton(RequestedField.SETTINGS), getItem, new User().withClientId(CLIENT_ID));
        assertThat(getItem.getSettings(), contains(
                beanDiffer(new ClientSettingGetItem()
                        .withOption(ClientSettingGetEnum.DISPLAY_STORE_RATING).withValue(YesNoEnum.NO)),
                beanDiffer(new ClientSettingGetItem()
                        .withOption(ClientSettingGetEnum.CORRECT_TYPOS_AUTOMATICALLY).withValue(YesNoEnum.NO)),
                beanDiffer(new ClientSettingGetItem()
                        .withOption(ClientSettingGetEnum.SHARED_ACCOUNT_ENABLED).withValue(YesNoEnum.YES))
        ));
    }

    @Test
    public void getClientSubtypeWriter_success() {
        ClientGetItemWriter clientSubtypeWriter = writers.getClientSubtypeWriter(
                () -> Collections.singletonMap(USER_ID, ClientSubtype.NONE)
        );
        ClientGetItem getItem = new ClientGetItem();
        clientSubtypeWriter
                .write(Collections.singleton(RequestedField.SUBTYPE), getItem, new User().withUid(USER_ID));
        assertThat(getItem.getSubtype(), Matchers.equalTo("NONE"));
    }
}
