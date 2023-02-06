package ru.yandex.direct.organizations.swagger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.asynchttp.ErrorResponseWrapperException;
import ru.yandex.direct.asynchttp.JsonParsableRequest;
import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.CallsPack;
import ru.yandex.direct.organizations.swagger.model.MetrikaData;
import ru.yandex.direct.organizations.swagger.model.PubApiCompaniesData;
import ru.yandex.direct.organizations.swagger.model.PubApiCompany;
import ru.yandex.direct.organizations.swagger.model.PubApiCompanyData;
import ru.yandex.direct.organizations.swagger.model.PubApiExtraCompanyField;
import ru.yandex.direct.organizations.swagger.model.PubApiUserAccessLevel;
import ru.yandex.direct.organizations.swagger.model.PublishingStatus;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.altay.model.language.LanguageOuterClass.Language.RU;
import static ru.yandex.direct.organizations.swagger.OrganizationInfoConverters.toCoreStatusPublish;
import static ru.yandex.direct.organizations.swagger.model.PubApiExtraCompanyField.METRIKA_DATA;

public class OrganizationsClientTest {

    private OrganizationsApi organizationsApi;
    private OrganizationsClient organizationsClient;

    @Before
    public void before() throws Exception {
        organizationsApi = mock(OrganizationsApi.class);
        Call<MetrikaData> call = getCall(new MetrikaData());
        when(organizationsApi.createCounterByPermalink(anyLong())).thenReturn(call);
        PpcProperty<Boolean> mockedProperty = mock(PpcProperty.class);
        when(mockedProperty.getOrDefault(anyBoolean())).thenReturn(true);
        organizationsClient = new OrganizationsClient(organizationsApi,
                null, //ходим без специфичных настроек для поиска по тексту
                mockedProperty,
                0 /* ходить всегда одним запросом */);
    }

    @Test
    public void getOnlineOrganization() {
        Long permalinkId = 123L;
        PubApiCompanyData company =
                createPubApiCompanyData().id(permalinkId).publishingStatus(PublishingStatus.PUBLISH).isOnline(true);
        Call<PubApiCompanyData> call = getCall(company);
        when(organizationsApi.getSingleCompanyByPermalink(eq(permalinkId),
                anyCollection(), anyMap()))
                .thenReturn(call);
        List<OrganizationInfo> results = organizationsClient.getOrganizationsInfo(List.of(permalinkId), RU, null);
        Assert.assertThat(results.size(), equalTo(1));
        compare(results.get(0), company);
    }

    @Test
    public void getNotOnlineOrganization() {
        Long permalinkId = 123L;
        PubApiCompanyData company =
                createPubApiCompanyData().id(permalinkId).publishingStatus(PublishingStatus.PUBLISH).isOnline(false);
        Call<PubApiCompanyData> call = getCall(company);
        when(organizationsApi.getSingleCompanyByPermalink(eq(permalinkId),
                anyCollection(), anyMap()))
                .thenReturn(call);
        List<OrganizationInfo> results = organizationsClient.getOrganizationsInfo(List.of(permalinkId), RU, null);
        Assert.assertThat(results.size(), equalTo(1));
        compare(results.get(0), company);
    }

    @Test
    public void getUnpublishedOrganization() {
        Long permalinkId = 123L;
        PubApiCompanyData company =
                createPubApiCompanyData().id(permalinkId).publishingStatus(PublishingStatus.UNKNOWN);
        Call<PubApiCompanyData> call = getCall(company);
        when(organizationsApi.getSingleCompanyByPermalink(eq(permalinkId),
                anyCollection(), anyMap()))
                .thenReturn(call);
        List<OrganizationInfo> results = organizationsClient.getOrganizationsInfo(List.of(permalinkId), RU, null);
        Assert.assertThat(results.size(), equalTo(1));
        compare(results.get(0), company);
    }

    @Test
    public void getMultipleOrganization() {
        Long onlinePermalinkId = 123L;
        Long offlinePermalinkId = 321L;
        List<Long> permalinkIds = List.of(onlinePermalinkId, offlinePermalinkId);
        List<PubApiCompany> companies = new ArrayList<>(List.of(
                createPubApiCompany().id(onlinePermalinkId).publishingStatus(PublishingStatus.PUBLISH).isOnline(true),
                createPubApiCompany().id(offlinePermalinkId).publishingStatus(PublishingStatus.PUBLISH).isOnline(false)
        ));
        PubApiCompaniesData result = new PubApiCompaniesData();
        result.setCompanies(companies);
        CallsPack<PubApiCompaniesData> callsPack = getCallsPack(result);
        when(organizationsApi.getCompaniesByPermalinkAndUid(anyCollection(),
                anyCollection(), anyCollection(), any(PubApiUserAccessLevel.class), anyMap()))
                .thenReturn(callsPack);
        List<OrganizationInfo> results = organizationsClient.getOrganizationsInfo(permalinkIds, RU, null);
        Assert.assertThat(results.size(), equalTo(2));
        compare(results.get(0), companies.get(0));
        compare(results.get(1), companies.get(1));
    }

    @Test
    public void getMultipleOrganization_withDifferentStatuses() {
        List<PubApiCompany> companies = new ArrayList<>(List.of(
                createPubApiCompany().id(1L).publishingStatus(PublishingStatus.PUBLISH),
                createPubApiCompany().id(2L).publishingStatus(PublishingStatus.MOVED),
                createPubApiCompany().id(3L).publishingStatus(PublishingStatus.TEMPORARILY_CLOSED),
                createPubApiCompany().id(4L).publishingStatus(PublishingStatus.CLOSED),
                createPubApiCompany().id(5L).publishingStatus(PublishingStatus.GEO_SPAM),
                createPubApiCompany().id(6L).publishingStatus(PublishingStatus.PUBLISH)
        ));
        List<Long> permalinkIds = StreamEx.of(companies).map(PubApiCompany::getId).toList();
        PubApiCompaniesData result = new PubApiCompaniesData();
        result.setCompanies(companies);
        CallsPack<PubApiCompaniesData> callsPack = getCallsPack(result);
        when(organizationsApi.getCompaniesByPermalinkAndUid(anyCollection(),
                anyCollection(), anyCollection(), any(PubApiUserAccessLevel.class), anyMap()))
                .thenReturn(callsPack);
        List<OrganizationInfo> results = organizationsClient.getOrganizationsInfo(permalinkIds, RU, null);
        Assert.assertThat(results.size(), equalTo(companies.size()));
        for (int i = 0; i < results.size(); i++) {
            compare(results.get(i), companies.get(i));
        }
    }

    @Test
    public void getSingleOrganization_Duplicate() {
        Long permalinkId = 123L;
        PubApiCompanyData company =
                createPubApiCompanyData()
                        .id(permalinkId)
                        .publishingStatus(PublishingStatus.PUBLISH)
                        .headPermalink(permalinkId + 1);
        Call<PubApiCompanyData> call = getCall(company);
        when(organizationsApi.getSingleCompanyByPermalink(eq(permalinkId),
                anyCollection(), anyMap()))
                .thenReturn(call);
        PubApiCompanyData result = organizationsClient.getSingleOrganizationInfo(
                permalinkId, List.of(PubApiExtraCompanyField.PUBLISHING_STATUS.getValue()),
                RU.name().toLowerCase(), null);
        assertThat(result.getPublishingStatus(), equalTo(PublishingStatus.DUPLICATE));
    }

    @Test
    public void getMultipleOrganization_withDuplicates() {
        Map<Long, PubApiCompany> expectedCompanies = StreamEx.of(
                createPubApiCompany().id(2L)
                        .publishingStatus(PublishingStatus.DUPLICATE)
                        .headPermalink(1L),
                createPubApiCompany().id(1L)
                        .publishingStatus(PublishingStatus.PUBLISH)
                        .duplicates(List.of(1L, 2L, 5L, 4L)),
                createPubApiCompany().id(3L)
                        .publishingStatus(PublishingStatus.PUBLISH),
                createPubApiCompany().id(4L)
                        .publishingStatus(PublishingStatus.DUPLICATE)
                        .headPermalink(1L)
        ).toMap(
                PubApiCompany::getId,
                c -> c
        );
        List<PubApiCompany> companies = StreamEx.of(expectedCompanies.values())
                .map(OrganizationInfoConverters::copyCompanyData)
                .filter(c -> c.getHeadPermalink() == null)
                .map(c -> c.publishingStatus(PublishingStatus.PUBLISH))
                .toList();
        Collection<Long> permalinkIds = expectedCompanies.keySet();
        PubApiCompaniesData result = new PubApiCompaniesData();
        result.setCompanies(companies);
        CallsPack<PubApiCompaniesData> callsPack = getCallsPack(result);
        when(organizationsApi.getCompaniesByPermalinkAndUid(anyCollection(),
                anyCollection(), anyCollection(), any(PubApiUserAccessLevel.class), anyMap()))
                .thenReturn(callsPack);
        List<OrganizationInfo> results = organizationsClient.getOrganizationsInfo(permalinkIds, RU, null);
        Assert.assertThat(results.size(), equalTo(expectedCompanies.size()));
        for (OrganizationInfo info : results) {
            compare(info, expectedCompanies.get(info.getPermalinkId()));
        }
    }

    @Test(expected = OrganizationsClientException.class)
    public void getMultipleOrganization_withErrorResponse() {
        Long publishedPermalinkId = 123L;
        Long unpublishedPermalinkId = 321L;
        List<Long> permalinkIds = List.of(publishedPermalinkId, unpublishedPermalinkId);
        CallsPack<PubApiCompaniesData> callsPack = getErrorCallsPack();
        when(organizationsApi.getCompaniesByPermalinkAndUid(anyCollection(),
                anyCollection(), anyCollection(), any(PubApiUserAccessLevel.class), anyMap()))
                .thenReturn(callsPack);
        organizationsClient.getOrganizationsInfo(permalinkIds, RU, null);
    }

    @Test
    public void getSingleOrganizationInfo_withLkLink() {
        Long permalinkId = 123L;
        String lkLink = "http://ya.ru";
        String expectedLink = lkLink + "?source=direct";
        PubApiCompanyData company =
                createPubApiCompanyData().id(permalinkId).publishingStatus(PublishingStatus.PUBLISH).lkLink(lkLink);
        Call<PubApiCompanyData> call = getCall(company);
        when(organizationsApi.getSingleCompanyByPermalink(eq(permalinkId),
                anyCollection(), anyMap()))
                .thenReturn(call);
        PubApiCompanyData result = organizationsClient.getSingleOrganizationInfo(
                permalinkId, List.of("lk_link"), RU.name().toLowerCase(), null);
        assertThat(result.getLkLink(), equalTo(expectedLink));
    }

    @Test
    public void getSingleOrganizationInfo_withoutLkLink() {
        Long permalinkId = 123L;
        String lkLink = "http://ya.ru";
        String expectedLink = lkLink;
        PubApiCompanyData company =
                createPubApiCompanyData().id(permalinkId).publishingStatus(PublishingStatus.PUBLISH).lkLink(lkLink);
        Call<PubApiCompanyData> call = getCall(company);
        when(organizationsApi.getSingleCompanyByPermalink(eq(permalinkId),
                anyCollection(), anyMap()))
                .thenReturn(call);
        PubApiCompanyData result = organizationsClient.getSingleOrganizationInfo(
                permalinkId, Collections.emptyList(), RU.name().toLowerCase(), null);
        assertThat(result.getLkLink(), equalTo(expectedLink));
    }

    @Test(expected = OrganizationsClientException.class)
    public void getSingleOrganizationInfo_withErrorResponse() {
        Long permalinkId = 123L;
        Call<PubApiCompanyData> call = getErrorCall();
        when(organizationsApi.getSingleCompanyByPermalink(eq(permalinkId),
                anyCollection(), anyMap()))
                .thenReturn(call);
        organizationsClient.getSingleOrganizationInfo(
                permalinkId, List.of("lk_link"), RU.name().toLowerCase(), null);
    }

    @Test
    public void getMultiplePublishedOrganizationInfo_withLkLink() {
        Long permalinkId = 123L;
        String lkLink = "http://ya.ru";
        String expectedLink = lkLink + "?source=direct";
        PubApiCompany company =
                createPubApiCompany().id(permalinkId).publishingStatus(PublishingStatus.PUBLISH).lkLink(lkLink);
        PubApiCompaniesData result = new PubApiCompaniesData()
                .addCompaniesItem(company);
        CallsPack<PubApiCompaniesData> callsPack = getCallsPack(result);
        when(organizationsApi.getCompaniesByPermalinkAndUid(anyCollection(),
                anyCollection(), anyCollection(), any(PubApiUserAccessLevel.class), anyMap()))
                .thenReturn(callsPack);
        PubApiCompaniesData results = organizationsClient.getMultiplePublishedOrganizationsInfo(
                List.of(permalinkId), List.of("lk_link"), RU.name().toLowerCase(), null);
        Assert.assertThat(results.getCompanies().size(), equalTo(1));
        Assert.assertThat(results.getCompanies().get(0).getLkLink(), equalTo(expectedLink));
    }

    @Test
    public void getMultiplePublishedOrganizationInfo_withoutLkLink() {
        Long permalinkId = 123L;
        String lkLink = "http://ya.ru";
        String expectedLink = lkLink;
        PubApiCompany company =
                createPubApiCompany().id(permalinkId).publishingStatus(PublishingStatus.PUBLISH).lkLink(lkLink);
        PubApiCompaniesData result = new PubApiCompaniesData()
                .addCompaniesItem(company);
        CallsPack<PubApiCompaniesData> callsPack = getCallsPack(result);
        when(organizationsApi.getCompaniesByPermalinkAndUid(anyCollection(),
                anyCollection(), anyCollection(), any(PubApiUserAccessLevel.class), anyMap()))
                .thenReturn(callsPack);
        PubApiCompaniesData results = organizationsClient.getMultiplePublishedOrganizationsInfo(
                List.of(permalinkId), Collections.emptyList(), RU.name().toLowerCase(), null);
        Assert.assertThat(results.getCompanies().size(), equalTo(1));
        Assert.assertThat(results.getCompanies().get(0).getLkLink(), equalTo(expectedLink));
    }

    @Test
    public void getMultiplePublishedOrganizationInfo_withMergeResults() {
        Long firstPermalinkId = 123L;
        Long secondPermalinkId = 321L;
        List<Long> permalinkIds = List.of(firstPermalinkId, secondPermalinkId);
        PubApiCompany firstCompany =
                createPubApiCompany().id(firstPermalinkId).publishingStatus(PublishingStatus.PUBLISH);
        PubApiCompany secondCompany =
                createPubApiCompany().id(secondPermalinkId).publishingStatus(PublishingStatus.PUBLISH);
        PubApiCompaniesData firstResponse = new PubApiCompaniesData().addCompaniesItem(firstCompany);
        PubApiCompaniesData secondResponse = new PubApiCompaniesData().addCompaniesItem(secondCompany);
        CallsPack<PubApiCompaniesData> callsPack = getCallsPack(Map.of(
                1L, firstResponse,
                2L, secondResponse)
        );
        when(organizationsApi.getCompaniesByPermalinkAndUid(anyCollection(),
                anyCollection(), anyCollection(), any(PubApiUserAccessLevel.class), anyMap()))
                .thenReturn(callsPack);
        List<OrganizationInfo> results = organizationsClient.getOrganizationsInfo(permalinkIds, RU, null);
        Assert.assertThat(results.size(), equalTo(2));
        if (results.get(0).getPermalinkId() == firstPermalinkId) {
            compare(results.get(0), firstCompany);
            compare(results.get(1), secondCompany);
        } else {
            compare(results.get(1), firstCompany);
            compare(results.get(0), secondCompany);
        }
    }

    @Test
    public void getSingleOrganizationInfo_withMetrikaData() {
        Long permalinkId = 123L;
        var metrikaDataWithCounter = new MetrikaData().permalink(permalinkId).counter("123456");
        Call<MetrikaData> createCounterCall = getCall(metrikaDataWithCounter);
        when(organizationsApi.createCounterByPermalink(anyLong())).thenReturn(createCounterCall);

        var metrikaDataWithoutCounter = new MetrikaData().permalink(permalinkId);
        PubApiCompanyData company = createPubApiCompanyData()
                .id(permalinkId)
                .publishingStatus(PublishingStatus.PUBLISH)
                .metrikaData(metrikaDataWithoutCounter);
        Call<PubApiCompanyData> getSingleCompanyCall = getCall(company);
        when(organizationsApi.getSingleCompanyByPermalink(eq(permalinkId),
                anyCollection(), anyMap()))
                .thenReturn(getSingleCompanyCall);

        PubApiCompanyData result = organizationsClient.getSingleOrganizationInfo(
                permalinkId, List.of(METRIKA_DATA.getValue()), RU.name().toLowerCase(), null);
        assertThat(result.getMetrikaData(), equalTo(metrikaDataWithCounter));
    }

    @Test
    public void getSingleOrganizationInfo_withoutMetrikaData() {
        Long permalinkId = 123L;
        var metrikaDataWithCounter = new MetrikaData().permalink(permalinkId).counter("123456");
        Call<MetrikaData> createCounterCall = getCall(metrikaDataWithCounter);
        when(organizationsApi.createCounterByPermalink(anyLong())).thenReturn(createCounterCall);

        var metrikaDataWithoutCounter = new MetrikaData().permalink(permalinkId);
        PubApiCompanyData company = createPubApiCompanyData()
                .id(permalinkId)
                .publishingStatus(PublishingStatus.PUBLISH)
                .metrikaData(metrikaDataWithoutCounter);
        Call<PubApiCompanyData> getSingleCompanyCall = getCall(company);
        when(organizationsApi.getSingleCompanyByPermalink(eq(permalinkId),
                anyCollection(), anyMap()))
                .thenReturn(getSingleCompanyCall);

        PubApiCompanyData result = organizationsClient.getSingleOrganizationInfo(
                permalinkId, List.of(), RU.name().toLowerCase(), null);
        assertThat(result.getMetrikaData(), equalTo(metrikaDataWithoutCounter));
    }

    @Test
    public void getMultipleOrganizationInfo_withMetrikaData() {
        Long permalinkId = 123L;
        var metrikaDataWithCounter = new MetrikaData().permalink(permalinkId).counter("123456");
        Call<MetrikaData> createCounterCall = getCall(metrikaDataWithCounter);
        when(organizationsApi.createCounterByPermalink(anyLong())).thenReturn(createCounterCall);

        var metrikaDataWithoutCounter = new MetrikaData().permalink(permalinkId);
        PubApiCompany company = createPubApiCompany()
                .id(permalinkId)
                .publishingStatus(PublishingStatus.PUBLISH)
                .metrikaData(metrikaDataWithoutCounter);
        PubApiCompaniesData result = new PubApiCompaniesData()
                .addCompaniesItem(company);
        CallsPack<PubApiCompaniesData> callsPack = getCallsPack(result);
        when(organizationsApi.getCompaniesByPermalinkAndUid(anyCollection(),
                anyCollection(), anyCollection(), any(PubApiUserAccessLevel.class), anyMap()))
                .thenReturn(callsPack);

        PubApiCompaniesData results = organizationsClient.getMultipleOrganizationsInfo(
                List.of(permalinkId), List.of(METRIKA_DATA.getValue()), RU.name().toLowerCase(), null);
        assertThat(results.getCompanies().size(), equalTo(1));
        assertThat(results.getCompanies().get(0).getMetrikaData(), equalTo(metrikaDataWithCounter));
    }

    @Test
    public void getMultipleOrganizationInfo_withoutMetrikaData() {
        Long permalinkId = 123L;
        var metrikaDataWithCounter = new MetrikaData().permalink(permalinkId).counter("123456");
        Call<MetrikaData> createCounterCall = getCall(metrikaDataWithCounter);
        when(organizationsApi.createCounterByPermalink(anyLong())).thenReturn(createCounterCall);

        var metrikaDataWithoutCounter = new MetrikaData().permalink(permalinkId);
        PubApiCompany company = createPubApiCompany()
                .id(permalinkId)
                .publishingStatus(PublishingStatus.PUBLISH)
                .metrikaData(metrikaDataWithoutCounter);
        PubApiCompaniesData result = new PubApiCompaniesData()
                .addCompaniesItem(company);
        CallsPack<PubApiCompaniesData> callsPack = getCallsPack(result);
        when(organizationsApi.getCompaniesByPermalinkAndUid(anyCollection(),
                anyCollection(), anyCollection(), any(PubApiUserAccessLevel.class), anyMap()))
                .thenReturn(callsPack);

        PubApiCompaniesData results = organizationsClient.getMultipleOrganizationsInfo(
                List.of(permalinkId), List.of(), RU.name().toLowerCase(), null);
        assertThat(results.getCompanies().size(), equalTo(1));
        assertThat(results.getCompanies().get(0).getMetrikaData(), equalTo(metrikaDataWithoutCounter));
    }

    @Test
    public void getOrganizationsCountersData() {
        Long permalinkId = 123L;
        var metrikaDataWithCounter = new MetrikaData().permalink(permalinkId).counter("123456");
        Call<MetrikaData> createCounterCall = getCall(metrikaDataWithCounter);
        when(organizationsApi.createCounterByPermalink(anyLong())).thenReturn(createCounterCall);

        var metrikaDataWithoutCounter = new MetrikaData().permalink(permalinkId);
        Call<List<MetrikaData>> call = getCall(List.of(metrikaDataWithoutCounter));
        when(organizationsApi.getOrganizationsCountersData(anyCollection(), anyMap())).thenReturn(call);

        List<MetrikaData> metrikaDataList = organizationsClient.getOrganizationsCountersData(
                List.of(permalinkId), RU.name().toLowerCase(), null);
        assertThat(metrikaDataList.size(), equalTo(1));
        assertThat(metrikaDataList.get(0), equalTo(metrikaDataWithCounter));
    }

    private PubApiCompanyData createPubApiCompanyData() {
        return new PubApiCompanyData();
    }

    private PubApiCompany createPubApiCompany() {
        return new PubApiCompany();
    }

    private void compare(OrganizationInfo data, PubApiCompanyData expected) {
        assertThat(data.getPermalinkId(), equalTo(expected.getId()));
        assertThat(data.getIsOnline(), equalTo(expected.isIsOnline()));
    }

    private void compare(OrganizationInfo data, PubApiCompany expected) {
        assertThat(data.getPermalinkId(), equalTo(expected.getId()));
        assertThat(data.getIsOnline(), equalTo(expected.isIsOnline()));
        assertThat(data.getStatusPublish(), equalTo(toCoreStatusPublish(expected.getPublishingStatus())));
    }

    private <R> Call<R> getCall(R result) {
        Result<R> response = mock(Result.class);
        when(response.getSuccess()).thenReturn(result);
        return getCall(response);
    }

    private <R> Call<R> getErrorCall() {
        Result<R> response = mock(Result.class);
        when(response.getErrors()).thenReturn(List.of(
                new ErrorResponseWrapperException("org_client_test", null, new RuntimeException())));
        return getCall(response);
    }

    private <R> Call<R> getCall(Result<R> response) {
        Call<R> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(call.getRequest()).thenReturn(new JsonParsableRequest(0L, null, null));
        return call;
    }

    private <R> CallsPack<R> getCallsPack(R result) {
        return getCallsPack(Map.of(1L /* requestId */, result));
    }

    private <R> CallsPack<R> getCallsPack(Map<Long, R> responses) {
        Map<Long, Result<R>> results = EntryStream.of(responses)
                .mapValues(response -> {
                    Result<R> result = mock(Result.class);
                    when(result.getSuccess()).thenReturn(response);
                    return result;
                })
                .toMap();
        return getMockedCallsPack(results);
    }

    private <R> CallsPack<R> getErrorCallsPack() {
        Result<R> response = mock(Result.class);
        when(response.getErrors()).thenReturn(List.of(
                new ErrorResponseWrapperException("org_client_test", null, new RuntimeException())));
        return getMockedCallsPack(Map.of(1L /* requestId */, response));
    }

    private <R> CallsPack<R> getMockedCallsPack(Map<Long, Result<R>> results) {
        CallsPack<R> callsPack = mock(CallsPack.class);
        when(callsPack.execute(anyInt())).thenReturn(results);
        List requests = List.of(new JsonParsableRequest(0L, null, null));
        when(callsPack.getRequests(anyInt())).thenReturn(requests);
        return callsPack;
    }

}
