package ru.yandex.market.crm;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.crm.client.YaCrmSpaceClient;
import ru.yandex.market.crm.client.model.YaCrmAccount;
import ru.yandex.market.crm.client.model.YaCrmAccountKik;
import ru.yandex.market.crm.client.model.YaCrmContactRequest;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link YaCrmSpaceSyncContactsExecutor}
 */
@DbUnitDataSet(before = "YaCrmSpaceSyncContactsExecutor.before.csv")
class YaCrmSpaceSyncContactsExecutorTest extends FunctionalTest {

    @Autowired
    private YaCrmSpaceSyncContactsExecutor executor;

    @Autowired
    private YaCrmSpaceClient yaCrmSpaceClient;

    @Test
    @DbUnitDataSet(
            before = "YaCrmSpaceSyncContactsExecutor.testSyncWithFilterByPartnerType.before.csv",
            after = "YaCrmSpaceSyncContactsExecutor.testSyncWithFilterByPartnerType.after.csv")
    @DisplayName("Проверка экспорта контактов поставщиков в CRM")
    void testSyncWithFilterByPartnerType() {
        final YaCrmAccount account = mockCrmSpaceService();
        executor.doJob(null);
        verifyContactInfoSync(account, List.of(
                YaCrmContactRequest.builder()
                        .withAccountId(account.getId())
                        .withFirstName("Иван")
                        .withMiddleName(null)
                        .withLastName("Петров")
                        .withComment("somemail@ya.ru, +7 4956680647")
                        .withEmail("somemail@ya.ru")
                        .withPhone("+7 4956680647")
                        .withPhoneExt(null)
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "YaCrmSpaceSyncContactsExecutor.testNotSyncUnchanged.before.csv",
            after = "YaCrmSpaceSyncContactsExecutor.testNotSyncUnchanged.after.csv")
    @DisplayName("Проверка отсутствия экспорта уже отправленных в CRM данных")
    void testNotSyncUnchanged() {
        executor.doJob(null);
        verifyZeroInteractions(yaCrmSpaceClient);
    }

    @Test
    @DbUnitDataSet(
            before = "YaCrmSpaceSyncContactsExecutor.testIncorrectContactInfo.before.csv",
            after = "YaCrmSpaceSyncContactsExecutor.testIncorrectContactInfo.after.csv")
    @DisplayName("Проверка фильтрации некорректной контактной информации")
    void testIncorrectContactInfo() {
        final YaCrmAccount account = mockCrmSpaceService();
        executor.doJob(null);
        verifyContactInfoSync(account, List.of(
                YaCrmContactRequest.builder()
                        .withAccountId(account.getId())
                        .withFirstName("Петр")
                        .withMiddleName(null)
                        .withLastName("Петров")
                        .withComment("petrov@ya.ru, +7213")
                        .withEmail("petrov@ya.ru")
                        .withPhone(null)
                        .withPhoneExt(null)
                        .build(),
                YaCrmContactRequest.builder()
                        .withAccountId(account.getId())
                        .withFirstName("Сидр")
                        .withMiddleName(null)
                        .withLastName("Сидоров")
                        .withComment("я тоже не email@ya.ru, +7913-472-9401 (8891)")
                        .withEmail(null)
                        .withPhone("+7913-472-9401")
                        .withPhoneExt("8891")
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "YaCrmSpaceSyncContactsExecutor.testDefaultContactInfo.before.csv",
            after = "YaCrmSpaceSyncContactsExecutor.testDefaultContactInfo.after.csv")
    @DisplayName("Проверка отсутствия ФИО")
    void testDefaultContactInfo() {
        final YaCrmAccount account = mockCrmSpaceService();
        executor.doJob(null);
        verifyContactInfoSync(account, List.of(
                YaCrmContactRequest.builder()
                        .withAccountId(account.getId())
                        .withFirstName("Имя")
                        .withMiddleName(null)
                        .withLastName("Фамилия")
                        .withComment("mail@ya.ru, +79134729401")
                        .withEmail("mail@ya.ru")
                        .withPhone("+79134729401")
                        .withPhoneExt(null)
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "YaCrmSpaceSyncContactsExecutor.testBusinessContacts.before.csv",
            after = "YaCrmSpaceSyncContactsExecutor.testBusinessContacts.after.csv")
    @DisplayName("Проверка экспорта контактов бизнеса")
    void testBusinessContacts() {
        final YaCrmAccount account = mockCrmSpaceService();
        executor.doJob(null);
        verifyContactInfoSync(account, List.of(
                YaCrmContactRequest.builder()
                        .withAccountId(account.getId())
                        .withFirstName("John")
                        .withMiddleName(null)
                        .withLastName("Doe")
                        .withComment("doe@yandex-team.ru, john@yandex-team.ru, +79134729494 (13)")
                        .withEmails(List.of("doe@yandex-team.ru", "john@yandex-team.ru"))
                        .withPhone("+79134729494")
                        .withPhoneExt("13")
                        .build()
        ));
    }

    private YaCrmAccount mockCrmSpaceService() {
        final YaCrmAccount account = new YaCrmAccount(1, 256, "contact", "some login", "some domain");
        when(yaCrmSpaceClient.findAccountByClient(account.getClientId())).thenReturn(Optional.of(account));
        when(yaCrmSpaceClient.findContactsByAccount(account.getId())).thenReturn(Optional.empty());
        return account;
    }

    private void verifyContactInfoSync(final YaCrmAccount account, final List<YaCrmContactRequest> expectedValues) {
        verify(yaCrmSpaceClient, times(1)).findAccountByClient(account.getClientId());
        verify(yaCrmSpaceClient, times(1)).findContactsByAccount(account.getId());

        final ArgumentCaptor<YaCrmContactRequest> captor = ArgumentCaptor.forClass(YaCrmContactRequest.class);
        verify(yaCrmSpaceClient, times(expectedValues.size())).createContact(captor.capture());

        final List<YaCrmContactRequest> actualValues = captor.getAllValues();
        assertThat(actualValues).hasSameSizeAs(expectedValues);

        for (int i = 0; i < actualValues.size(); i++) {
            final YaCrmContactRequest actual = actualValues.get(i);
            final YaCrmContactRequest expected = expectedValues.get(i);

            assertThat(actual)
                    .returns(account.getId(), from(YaCrmContactRequest::getAccountId))
                    .returns(expected.getFirstName(), from(YaCrmContactRequest::getFirstName))
                    .returns(expected.getMiddleName(), from(YaCrmContactRequest::getMiddleName))
                    .returns(expected.getLastName(), from(YaCrmContactRequest::getLastName))
                    .returns(expected.getComment(), from(YaCrmContactRequest::getComment));

            final List<YaCrmAccountKik> actualKiks = actual.getAccountKiks();
            final List<YaCrmAccountKik> expectedKiks = expected.getAccountKiks();
            assertThat(actualKiks).hasSameSizeAs(expectedKiks);

            final YaCrmAccountKik actualKik = Iterables.getLast(actualKiks);
            final YaCrmAccountKik expectedKik = Iterables.getLast(expectedKiks);

            assertThat(actualKik).extracting(YaCrmAccountKik::getEmail).isEqualTo(expectedKik.getEmail());
            assertThat(actualKik).extracting(YaCrmAccountKik::getPhone).isEqualTo(expectedKik.getPhone());
            assertThat(actualKik).extracting(YaCrmAccountKik::getPhoneExt).isEqualTo(expectedKik.getPhoneExt());
        }

        verifyNoMoreInteractions(yaCrmSpaceClient);
    }

}
