package ru.yandex.direct.core.testing.stub;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.lang.math.RandomUtils;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.organizations.swagger.OrganizationsClient;
import ru.yandex.direct.organizations.swagger.OrganizationsClientException;
import ru.yandex.direct.organizations.swagger.model.CompanyPhone;
import ru.yandex.direct.organizations.swagger.model.MetrikaData;
import ru.yandex.direct.organizations.swagger.model.PubApiCompaniesData;
import ru.yandex.direct.organizations.swagger.model.PubApiCompany;
import ru.yandex.direct.organizations.swagger.model.PubApiCompanyData;
import ru.yandex.direct.organizations.swagger.model.PubApiUserAccessLevel;
import ru.yandex.direct.organizations.swagger.model.UpdateOrganizationRequest;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.tvm.TvmIntegration;

import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils.getUniqPhone;
import static ru.yandex.direct.organizations.swagger.model.PublishingStatus.PUBLISH;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class OrganizationsClientStub extends OrganizationsClient {
    private final ConcurrentHashMap<Long, MetrikaData> metrikaDataByPermalink = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, PubApiCompanyData> pubApiCompanyDataByPermalinkIds =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, PubApiCompany> pubApiCompanyByPermalinkIds = new ConcurrentHashMap<>();

    public OrganizationsClientStub() {
        super("http://sprav-api-test.n.yandex-team.ru/v1", mock(ParallelFetcherFactory.class),
                mock(TvmIntegration.class), null, null, mock(PpcPropertiesSupport.class), false, 0);
    }

    public void removePermalinks(Long... permalinks) {
        for (Long permalink : permalinks) {
            this.pubApiCompanyByPermalinkIds.remove(permalink);
            this.pubApiCompanyDataByPermalinkIds.remove(permalink);
        }
    }

    public void addUidsByPermalinkId(Long permalink, List<Long> uids) {
        addDataByPermalinkIds(
                permalink,
                uids,
                RandomUtils.nextLong(),
                List.of(toCompanyPhone(getUniqPhone()))
        );
    }

    public void addUidsAndCounterIdsByPermalinkId(Long permalink, List<Long> uids, Long counterId) {
        addDataByPermalinkIds(
                permalink,
                uids,
                counterId,
                List.of(toCompanyPhone(getUniqPhone()))
        );
    }

    public void addUidsWithPhonesByPermalinkId(Long permalink, List<Long> uids, @Nullable List<String> phones) {
        addDataByPermalinkIds(
                permalink,
                uids,
                RandomUtils.nextLong(),
                phones == null ? null : mapList(phones, OrganizationsClientStub::toCompanyPhone)
        );
    }

    public void addUidsWithHiddenPhoneByPermalinkId(Long permalink, List<Long> uids) {
        addDataByPermalinkIds(
                permalink,
                uids,
                RandomUtils.nextLong(),
                List.of(toCompanyPhoneWithHideTrue(getUniqPhone()))
        );
    }

    private void addDataByPermalinkIds(
            Long permalink,
            List<Long> uids,
            Long counterId,
            @Nullable List<CompanyPhone> phones
    ) {
        var metrikaData = new MetrikaData()
                .id(RandomNumberUtils.nextPositiveLong())
                .counter(String.valueOf(counterId))
                .permalink(permalink);
        this.pubApiCompanyDataByPermalinkIds.put(
                permalink,
                new PubApiCompanyData().id(permalink)
                        .publishingStatus(PUBLISH)
                        .metrikaData(metrikaData)
                        .phones(phones)
                        .uidsWithModifyPermission(uids)
        );
        this.pubApiCompanyByPermalinkIds.put(
                permalink,
                new PubApiCompany().id(permalink)
                        .publishingStatus(PUBLISH)
                        .metrikaData(metrikaData)
                        .phones(phones)
                        .uidsWithModifyPermission(uids)
        );
    }

    @Override
    public void updateOrganization(String tvmUserTicket, Long permalink, UpdateOrganizationRequest request) {
    }

    @Override
    public List<MetrikaData> getOrganizationsCountersData(
            Collection<Long> permalinkIds,
            String language,
            @Nullable String tvmUserTicket
    ) {
        return StreamEx.ofValues(pubApiCompanyDataByPermalinkIds)
                .map(PubApiCompanyData::getMetrikaData)
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    @Override
    public PubApiCompanyData getSingleOrganizationInfo(
            Long permalinkId,
            Collection<String> fields,
            String language,
            @Nullable String tvmUserTicket
    ) throws OrganizationsClientException {
        return pubApiCompanyDataByPermalinkIds.get(permalinkId);
    }

    @Nonnull
    @Override
    public PubApiCompaniesData getMultipleOrganizationsInfo(
            Collection<Long> permalinkIds,
            Collection<Long> uids,
            Collection<String> fields,
            String language,
            @Nullable String tvmUserTicket,
            PubApiUserAccessLevel accessLevel
    ) throws OrganizationsClientException {
        List<PubApiCompany> companies = EntryStream.of(pubApiCompanyByPermalinkIds)
                .filterKeys(permalinkIds::contains)
                .values()
                .toList();
        PubApiCompaniesData result = new PubApiCompaniesData();
        result.setCompanies(companies);
        return result;
    }

    public List<CompanyPhone> getCompanyPhones(Long permalink) {
        PubApiCompany pubApiCompany = pubApiCompanyByPermalinkIds.get(permalink);
        if (pubApiCompany == null) {
            return Collections.emptyList();
        }
        return pubApiCompany.getPhones();
    }

    public void changeCompanyPhones(Long permalink, List<String> phones) {
        List<CompanyPhone> companyPhones = mapList(phones, OrganizationsClientStub::toCompanyPhone);
        pubApiCompanyByPermalinkIds.computeIfPresent(permalink, (k, v) -> {
            v.setPhones(companyPhones);
            return v;
        });

        pubApiCompanyDataByPermalinkIds.computeIfPresent(permalink, (k, v) -> {
            v.setPhones(companyPhones);
            return v;
        });
    }

    /**
     * Мимикрируем под настоящий ответ Справочника
     * {@code phoneE164} в формате +79991234567 или +79991234567,889900 с добавочным номером
     */
    private static CompanyPhone toCompanyPhone(String phoneE164) {
        // убираем ведущий "+" и разбиваем номер по группам N-NNN-NNNNNNN-NNNNNN
        // последняя группа есть только в случае добавочного номера
        String country = phoneE164.substring(1, 2);
        String region = phoneE164.substring(2, 5);
        int numberEndPos = getNumberEndPos(phoneE164);
        String number = phoneE164.substring(5, numberEndPos);
        String ext = phoneE164.length() > 12 && phoneE164.charAt(12) == ','
                ? phoneE164.substring(13)
                : null;
        return new CompanyPhone()
                .countryCode(country)
                .regionCode(region)
                .number(number)
                .ext(ext);
    }

    private static CompanyPhone toCompanyPhoneWithHideTrue(String phoneE164) {
        return toCompanyPhone(phoneE164).hide(true);
    }

    private static int getNumberEndPos(String phoneE164) {
        if (phoneE164.length() <= 12) {
            return phoneE164.length();
        }
        return phoneE164.charAt(12) == ',' ? 12 : phoneE164.length();
    }

}
