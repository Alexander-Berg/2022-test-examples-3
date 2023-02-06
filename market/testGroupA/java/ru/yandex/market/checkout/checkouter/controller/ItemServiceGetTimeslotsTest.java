package ru.yandex.market.checkout.checkouter.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.offer.AbstractOfferCategorizeTestBase;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServicePartnerInfoViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceTimeSlotViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceTimeslotResultViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceTimeslotResultsViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceTimeslotsRequest;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServicePartnerDescriptionDto;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceTimeSlotDto;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceTimeSlotsResponse;
import ru.yandex.market.checkout.common.util.ItemServiceDefaultPartnerInfo;
import ru.yandex.market.checkout.common.util.ItemServiceDefaultTimeInterval;
import ru.yandex.market.checkout.helpers.ItemServiceTestHelper;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.checkout.util.yauslugi.YaUslugiServiceTestConfigurer;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.OfferService;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ItemServiceGetTimeslotsTest extends AbstractOfferCategorizeTestBase {

    @Autowired
    private ItemServiceTestHelper itemServiceTestHelper;
    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private YaUslugiServiceTestConfigurer yaUslugiServiceTestConfigurer;
    @Autowired
    private CheckouterProperties checkouterProperties;
    @Autowired
    private CheckouterFeatureReader checkouterFeatureReader;

    @BeforeEach
    public void setup() {
        checkouterProperties.setEnableItemServiceTimeslotsEndpoint(true);
    }

    @AfterEach
    public void resetMocks() {
        yaUslugiServiceTestConfigurer.getYaUslugiMock().resetAll();
    }

    @Disabled("MARKETCHECKOUT-24398")
    @Test
    public void testGetTimeslots() throws Exception {
        yaUslugiServiceTestConfigurer.getYaUslugiMock().resetAll();
        //mock report
        var foundOffer1 = buildFoundOffer(1L, "1", "offer_1",
                List.of(
                        buildOfferService(1L, "s1"),
                        buildOfferService(2L, "s2")
                )
        );
        var foundOffer2 = buildFoundOffer(2L, "2", "offer_2",
                List.of(
                        buildOfferService(3L, "s3"),
                        buildOfferService(4L, "s4")
                )
        );
        mockReport(213L, List.of(foundOffer1, foundOffer2));

        //mock yaUslugi
        var yaTimeslots = buildYaServiceResponse("1", "2");
        yaUslugiServiceTestConfigurer.mockGetTimeslots(yaTimeslots);

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(2L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_2");
        requestInfo2.setServiceId(3L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var response = testSerializationService.deserializeCheckouterObject(
                itemServiceTestHelper.getTimeslots(request)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ItemServiceTimeslotResultsViewModel.class
        );

        //check timeslot results
        assertNotNull(response);
        assertThat(response.getTimeslotsResult(), hasSize(2));
        var ts1 = getTimeSlotResultByKey("key_1", response);
        checkTimeslotResult(ts1, "key_1", List.of(
                buildItemServiceTimeslotViewModel("Partner 1", "INN Partner 1",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_1"),
                buildItemServiceTimeslotViewModel("Partner 2", "INN Partner 2",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_2")
        ), false);

        var ts2 = getTimeSlotResultByKey("key_2", response);
        checkTimeslotResult(ts2, "key_2", List.of(
                buildItemServiceTimeslotViewModel("Partner 1", "INN Partner 1",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_1"),
                buildItemServiceTimeslotViewModel("Partner 2", "INN Partner 2",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_2")
        ), false);

        //check defaults
        checkDefaults(response);
    }


    @Test
    public void testGetTimeslotsForOffersWithSameServices() throws Exception {
        yaUslugiServiceTestConfigurer.getYaUslugiMock().resetAll();
        //mock report
        OfferService commonService = buildOfferService(1L, "s1");
        var foundOffer1 = buildFoundOffer(1L, "1", "offer_1",
                Collections.singletonList(commonService));
        var foundOffer2 = buildFoundOffer(2L, "2", "offer_2",
                Collections.singletonList(commonService));
        mockReport(213L, List.of(foundOffer1, foundOffer2));

        //mock yaUslugi
        var yaTimeslots = buildYaServiceResponse("1");
        yaUslugiServiceTestConfigurer.mockGetTimeslots(yaTimeslots);

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(1L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_2");
        requestInfo2.setServiceId(1L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var resonse = itemServiceTestHelper.getTimeslots(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var actualTimeslots = testSerializationService.deserializeCheckouterObject(
                resonse,
                ItemServiceTimeslotResultsViewModel.class);

        //check timeslot results
        assertNotNull(actualTimeslots);
        assertThat(actualTimeslots.getTimeslotsResult(), hasSize(2));

        List<ItemServiceTimeSlotViewModel> expectedResult = Collections.singletonList(
                buildItemServiceTimeslotViewModel("Partner 1", "INN Partner 1",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_1"));

        var ts1 = getTimeSlotResultByKey("key_1", actualTimeslots);
        checkTimeslotResult(ts1, "key_1", expectedResult, false);

        var ts2 = getTimeSlotResultByKey("key_2", actualTimeslots);
        checkTimeslotResult(ts2, "key_2", expectedResult, false);

        //check defaults
        checkDefaults(actualTimeslots);
    }

    @Test
    public void testGetTimeslotsReturnsReqId() throws Exception {
        yaUslugiServiceTestConfigurer.getYaUslugiMock().resetAll();
        //mock report
        OfferService commonService = buildOfferService(1L, "s1");
        var foundOffer1 = buildFoundOffer(1L, "1", "offer_1",
                Collections.singletonList(commonService));
        mockReport(213L, List.of(foundOffer1));

        //mock yaUslugi
        var yaTimeslots = buildYaServiceResponse("1");
        String reqId = "someReqId";
        yaUslugiServiceTestConfigurer.mockGetTimeslots(yaTimeslots, reqId);

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(1L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        request.setServices(List.of(requestInfo1));

        var resonse = itemServiceTestHelper.getTimeslots(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var actualTimeslots = testSerializationService.deserializeCheckouterObject(
                resonse,
                ItemServiceTimeslotResultsViewModel.class);

        //check timeslot results
        assertNotNull(actualTimeslots);
        assertThat(actualTimeslots.getTimeslotsResult(), hasSize(1));
        assertThat(actualTimeslots.getTimeslotsResult().get(0).getYaUslugiTimeslotReqId(), equalTo(reqId));
    }

    @Test
    public void testGetTimeslotsReportReturnsEmptyResult() throws Exception {
        yaUslugiServiceTestConfigurer.getYaUslugiMock().resetAll();
        //mock report, empty result
        mockReport(213L, Collections.emptyList());

        //mock yaUslugi
        var yaTimeslots = buildYaServiceResponse("1", "2");
        yaUslugiServiceTestConfigurer.mockGetTimeslots(yaTimeslots);

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(2L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_2");
        requestInfo2.setServiceId(3L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var response = testSerializationService.deserializeCheckouterObject(
                itemServiceTestHelper.getTimeslots(request)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ItemServiceTimeslotResultsViewModel.class
        );

        //check timeslot results
        assertNotNull(response);
        assertThat(response.getTimeslotsResult(), hasSize(2));
        var ts1 = getTimeSlotResultByKey("key_1", response);
        checkEmptyTimeslotResult(ts1, "key_1");

        var ts2 = getTimeSlotResultByKey("key_2", response);
        checkEmptyTimeslotResult(ts2, "key_2");

        //check defaults
        checkDefaults(response);
    }

    @Test
    public void testGetTimeslotsReportReturnsNotAllResults() throws Exception {
        yaUslugiServiceTestConfigurer.getYaUslugiMock().resetAll();
        //mock report
        var foundOffer1 = buildFoundOffer(1L, "1", "offer_1",
                List.of(
                        buildOfferService(1L, "s1"),
                        buildOfferService(2L, "s2")
                )
        );
        mockReport(213L, Collections.singletonList(foundOffer1));

        //mock yaUslugi
        var yaTimeslots = buildYaServiceResponse("1", "2");
        yaUslugiServiceTestConfigurer.mockGetTimeslots(yaTimeslots);

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(2L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_2");
        requestInfo2.setServiceId(3L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var response = testSerializationService.deserializeCheckouterObject(
                itemServiceTestHelper.getTimeslots(request)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ItemServiceTimeslotResultsViewModel.class
        );

        //check timeslot results
        assertNotNull(response);
        assertThat(response.getTimeslotsResult(), hasSize(2));
        var ts1 = getTimeSlotResultByKey("key_1", response);
        checkEmptyTimeslotResult(ts1, "key_1");

        var ts2 = getTimeSlotResultByKey("key_2", response);
        checkEmptyTimeslotResult(ts2, "key_2");

        //check defaults
        checkDefaults(response);
    }

    @Disabled("MARKETCHECKOUT-24398")
    @Test
    public void testDefaultsFromYaUslugi() throws Exception {
        yaUslugiServiceTestConfigurer.getYaUslugiMock().resetAll();
        //mock report
        var foundOffer1 = buildFoundOffer(1L, "1", "offer_1",
                List.of(
                        buildOfferService(1L, "s1"),
                        buildOfferService(2L, "s2")
                )
        );
        var foundOffer2 = buildFoundOffer(2L, "2", "offer_2",
                List.of(
                        buildOfferService(3L, "s3"),
                        buildOfferService(4L, "s4")
                )
        );
        mockReport(213L, List.of(foundOffer1, foundOffer2));

        //mock yaUslugi
        var yaTimeslots = buildYaServiceResponse("1", "2");
        yaTimeslots.getPartnerDescription().remove("partner_1");
        yaUslugiServiceTestConfigurer.mockGetTimeslots(yaTimeslots);

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(2L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_2");
        requestInfo2.setServiceId(3L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var response = testSerializationService.deserializeCheckouterObject(
                itemServiceTestHelper.getTimeslots(request)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ItemServiceTimeslotResultsViewModel.class
        );

        //check timeslot results
        assertNotNull(response);
        assertThat(response.getTimeslotsResult(), hasSize(2));
        var ts1 = getTimeSlotResultByKey("key_1", response);
        checkTimeslotResult(ts1, "key_1", List.of(
                buildItemServiceTimeslotViewModel("Partner Default", "INN Partner Default",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_Default"),
                buildItemServiceTimeslotViewModel("Partner 2", "INN Partner 2",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_2")
        ), false);

        var ts2 = getTimeSlotResultByKey("key_2", response);
        checkTimeslotResult(ts2, "key_2", List.of(
                buildItemServiceTimeslotViewModel("Partner Default", "INN Partner Default",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_Default"),
                buildItemServiceTimeslotViewModel("Partner 2", "INN Partner 2",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_2")
        ), false);

        //check defaults
        checkDefaults(response);
    }

    @Disabled("MARKETCHECKOUT-24398")
    @Test
    public void testResponseTimeslotsSorting() throws Exception {
        yaUslugiServiceTestConfigurer.getYaUslugiMock().resetAll();
        //mock report
        var foundOffer1 = buildFoundOffer(1L, "1", "offer_1",
                List.of(
                        buildOfferService(1L, "s1"),
                        buildOfferService(2L, "s2")
                )
        );
        var foundOffer2 = buildFoundOffer(2L, "2", "offer_2",
                List.of(
                        buildOfferService(3L, "s3"),
                        buildOfferService(4L, "s4")
                )
        );
        mockReport(213L, List.of(foundOffer1, foundOffer2));

        //mock yaUslugi
        var yaTimeslots = buildYaServiceUnsortedResponse("1", "2");
        yaUslugiServiceTestConfigurer.mockGetTimeslots(yaTimeslots);

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(2L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_2");
        requestInfo2.setServiceId(3L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var response = testSerializationService.deserializeCheckouterObject(
                itemServiceTestHelper.getTimeslots(request)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ItemServiceTimeslotResultsViewModel.class
        );

        //check timeslot results
        assertNotNull(response);
        assertThat(response.getTimeslotsResult(), hasSize(2));
        var ts1 = getTimeSlotResultByKey("key_1", response);
        checkTimeslotResult(ts1, "key_1", List.of(
                buildItemServiceTimeslotViewModel("Partner 1", "INN Partner 1",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_1"),
                buildItemServiceTimeslotViewModel("Partner 2", "INN Partner 2",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_2"),
                buildItemServiceTimeslotViewModel("Partner 1", "INN Partner 1",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay().plusHours(4), 100,
                        "OGRN_1"),
                buildItemServiceTimeslotViewModel("Partner 2", "INN Partner 2",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay().plusHours(4), 100,
                        "OGRN_2")
        ), true);

        var ts2 = getTimeSlotResultByKey("key_2", response);
        checkTimeslotResult(ts2, "key_2", List.of(
                buildItemServiceTimeslotViewModel("Partner 1", "INN Partner 1",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_1"),
                buildItemServiceTimeslotViewModel("Partner 2", "INN Partner 2",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay(), 100,
                        "OGRN_2"),
                buildItemServiceTimeslotViewModel("Partner 1", "INN Partner 1",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay().plusHours(4), 100,
                        "OGRN_1"),
                buildItemServiceTimeslotViewModel("Partner 2", "INN Partner 2",
                        LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay().plusHours(4), 100,
                        "OGRN_2")
        ), true);

        //check defaults
        checkDefaults(response);
    }

    @Test
    public void testItemServiceNotFound() throws Exception {
        //mock report
        var foundOffer1 = buildFoundOffer(1L, "1", "offer_1",
                List.of(
                        buildOfferService(1L, "s1"),
                        buildOfferService(2L, "s2")
                )
        );
        var foundOffer2 = buildFoundOffer(2L, "2", "offer_2",
                List.of(
                        buildOfferService(3L, "s3"),
                        buildOfferService(4L, "s4")
                )
        );
        mockReport(213L, List.of(foundOffer1, foundOffer2));

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(2L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_3");
        requestInfo2.setServiceId(3L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var response = testSerializationService.deserializeCheckouterObject(
                itemServiceTestHelper.getTimeslots(request)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ItemServiceTimeslotResultsViewModel.class
        );

        //check timeslot results
        assertNotNull(response);
        assertThat(response.getTimeslotsResult(), hasSize(2));
        var ts1 = getTimeSlotResultByKey("key_1", response);
        checkEmptyTimeslotResult(ts1, "key_1");

        var ts2 = getTimeSlotResultByKey("key_2", response);
        checkEmptyTimeslotResult(ts2, "key_2");

        //check defaults
        checkDefaults(response);
    }

    @Test
    public void testYaUslugiReturns5xx() throws Exception {
        //mock report
        var foundOffer1 = buildFoundOffer(1L, "1", "offer_1",
                List.of(
                        buildOfferService(1L, "s1"),
                        buildOfferService(2L, "s2")
                )
        );
        var foundOffer2 = buildFoundOffer(2L, "2", "offer_2",
                List.of(
                        buildOfferService(3L, "s3"),
                        buildOfferService(4L, "s4")
                )
        );
        mockReport(213L, List.of(foundOffer1, foundOffer2));

        //mock yaUslugi
        yaUslugiServiceTestConfigurer.getYaUslugiMock().stubFor(
                post(urlPathEqualTo("/get_cached_slots"))
                        .willReturn(serverError()));

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(2L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_2");
        requestInfo2.setServiceId(3L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var response = testSerializationService.deserializeCheckouterObject(
                itemServiceTestHelper.getTimeslots(request)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ItemServiceTimeslotResultsViewModel.class
        );

        //check timeslot results
        assertNotNull(response);
        assertThat(response.getTimeslotsResult(), hasSize(2));
        var ts1 = getTimeSlotResultByKey("key_1", response);
        assertNull(ts1.getTimeslots());

        var ts2 = getTimeSlotResultByKey("key_2", response);
        assertNull(ts2.getTimeslots());

        //check defaults
        checkDefaults(response);
    }

    @Test
    public void testYaUslugiReturns4xx() throws Exception {
        //mock report
        var foundOffer1 = buildFoundOffer(1L, "1", "offer_1",
                List.of(
                        buildOfferService(1L, "s1"),
                        buildOfferService(2L, "s2")
                )
        );
        var foundOffer2 = buildFoundOffer(2L, "2", "offer_2",
                List.of(
                        buildOfferService(3L, "s3"),
                        buildOfferService(4L, "s4")
                )
        );
        mockReport(213L, List.of(foundOffer1, foundOffer2));

        //mock yaUslugi
        yaUslugiServiceTestConfigurer.getYaUslugiMock().stubFor(
                post(urlPathEqualTo("/get_cached_slots"))
                        .willReturn(badRequest()));

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(2L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_2");
        requestInfo2.setServiceId(3L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var response = testSerializationService.deserializeCheckouterObject(
                itemServiceTestHelper.getTimeslots(request)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ItemServiceTimeslotResultsViewModel.class
        );

        //check timeslot results
        assertNotNull(response);
        assertThat(response.getTimeslotsResult(), hasSize(2));
        var ts1 = getTimeSlotResultByKey("key_1", response);
        assertNull(ts1.getTimeslots());

        var ts2 = getTimeSlotResultByKey("key_2", response);
        assertNull(ts2.getTimeslots());

        //check defaults
        checkDefaults(response);
    }

    @Test
    public void testDisabledGettingTimeslots() throws Exception {
        checkouterProperties.setEnableItemServiceTimeslotsEndpoint(false);

        //build test request
        var request = new ItemServiceTimeslotsRequest();
        request.setGps("12,10");
        request.setRegionId(213L);

        var requestInfo1 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo1.setKey("key_1");
        requestInfo1.setWareMd5Id("offer_1");
        requestInfo1.setServiceId(2L);
        var testDate1 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo1.setDate(testDate1);

        var requestInfo2 = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        requestInfo2.setKey("key_2");
        requestInfo2.setWareMd5Id("offer_2");
        requestInfo2.setServiceId(3L);
        var testDate2 = LocalDate.of(2021, Month.DECEMBER, 1);
        requestInfo2.setDate(testDate2);

        request.setServices(List.of(requestInfo1, requestInfo2));

        var response = testSerializationService.deserializeCheckouterObject(
                itemServiceTestHelper.getTimeslots(request)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                ItemServiceTimeslotResultsViewModel.class
        );

        //check timeslot results
        assertNotNull(response);
        assertThat(response.getTimeslotsResult(), hasSize(0));

        //check defaults
        checkDefaults(response);
    }

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(new Object[][]{
                {
                        buildItemServiceTimeslotsRequest("12, 10", 213L, null),
                },
                {
                        buildItemServiceTimeslotsRequest("12, 10", 213L, Collections.emptyList()),
                },
                {
                        buildItemServiceTimeslotsRequest("12, 10", null, List.of(
                                buildItemServiceInfo("key_1", "ware1", 1L, LocalDate.now())
                        ))
                },
                {
                        buildItemServiceTimeslotsRequest(null, 213L, List.of(
                                buildItemServiceInfo("key_1", "ware1", 1L, LocalDate.now())
                        ))
                },
                {
                        buildItemServiceTimeslotsRequest("12, 10", 213L, List.of(
                                buildItemServiceInfo(null, "ware1", 1L, LocalDate.now())
                        ))
                },
                {
                        buildItemServiceTimeslotsRequest("12, 10", 213L, List.of(
                                buildItemServiceInfo("key_1", null, 1L, LocalDate.now())
                        ))
                },
                {
                        buildItemServiceTimeslotsRequest("12, 10", 213L, List.of(
                                buildItemServiceInfo("key_1", "ware1", null, LocalDate.now())
                        ))
                },
                {
                        buildItemServiceTimeslotsRequest("12, 10", 213L, List.of(
                                buildItemServiceInfo("key_1", "ware1", 1L, null)
                        ))
                }
        }).map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void testRequiredRequestParameters(ItemServiceTimeslotsRequest request) throws Exception {
        itemServiceTestHelper.getTimeslots(request).andExpect(status().isBadRequest());
    }

    private void checkTimeslotResult(ItemServiceTimeslotResultViewModel timeslotResult,
                                     String expectedKey,
                                     List<ItemServiceTimeSlotViewModel> expectedTimeslots,
                                     boolean strictOrder) {
        assertNotNull(timeslotResult);
        assertEquals(expectedKey, timeslotResult.getKey());

        assertThat(timeslotResult.getTimeslots(),
                strictOrder ? contains(expectedTimeslots.toArray(new ItemServiceTimeSlotViewModel[0]))
                        : containsInAnyOrder(expectedTimeslots.toArray(new ItemServiceTimeSlotViewModel[0])));
    }

    private void checkEmptyTimeslotResult(ItemServiceTimeslotResultViewModel timeslotResult,
                                          String expectedKey) {
        assertNotNull(timeslotResult);
        assertEquals(expectedKey, timeslotResult.getKey());
        assertNull(timeslotResult.getTimeslots());
    }

    private FoundOffer buildFoundOffer(Long feedId, String offerId, String wareMd5Id,
                                       List<OfferService> offerServices) {
        return FoundOfferBuilder.create()
                .feedId(feedId)
                .offerId(offerId)
                .wareMd5(wareMd5Id)
                .services(offerServices)
                .build();
    }

    private OfferService buildOfferService(Long serviceId, String yaServiceId) {
        var offerService = new OfferService();
        offerService.setServiceId(serviceId);
        offerService.setYaServiceId(yaServiceId);
        return offerService;
    }

    private YaServiceTimeSlotsResponse buildYaServiceResponse(String... ids) {
        var yaTimeslots = new YaServiceTimeSlotsResponse();
        List<YaServiceTimeSlotDto> timeslots = Arrays.stream(ids)
                .map(id -> {
                    var timeslot = new YaServiceTimeSlotDto();
                    timeslot.setId("p" + id);
                    timeslot.setPartnerId("partner_" + id);
                    var date = LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay();
                    timeslot.setDate(date);
                    return timeslot;
                }).collect(Collectors.toList());
        yaTimeslots.setTimeslots(timeslots);

        Map<String, YaServicePartnerDescriptionDto> descriptions = Arrays.stream(ids)
                .map(id -> {
                    var description = buildYaServicePartnerDescription("Partner " + id,
                            "INN Partner " + id, "OGRN_" + id);
                    return Map.entry("partner_" + id, description);
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        descriptions.put("default", buildYaServicePartnerDescription("Partner Default",
                "INN Partner Default", "OGRN_Default"));
        yaTimeslots.setPartnerDescription(descriptions);

        return yaTimeslots;
    }

    private YaServiceTimeSlotsResponse buildYaServiceUnsortedResponse(String... ids) {
        var yaTimeslots = new YaServiceTimeSlotsResponse();
        List<YaServiceTimeSlotDto> timeslots = Arrays.stream(ids)
                .flatMap(id -> {
                    var date = LocalDate.of(2021, Month.DECEMBER, 1).atStartOfDay();
                    return Stream.of(
                            buildTimeslot("p1" + id, "partner_" + id, date.plusHours(4)),
                            buildTimeslot("p2" + id, "partner_" + id, date)
                    );
                })
                .sorted(Comparator.comparing(YaServiceTimeSlotDto::getDate).reversed())
                .collect(Collectors.toList());
        yaTimeslots.setTimeslots(timeslots);

        Map<String, YaServicePartnerDescriptionDto> descriptions = Arrays.stream(ids)
                .map(id -> {
                    var description = buildYaServicePartnerDescription("Partner " + id,
                            "INN Partner " + id, "OGRN_" + id);
                    return Map.entry("partner_" + id, description);
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        yaTimeslots.setPartnerDescription(descriptions);

        return yaTimeslots;
    }

    private YaServiceTimeSlotDto buildTimeslot(String id, String partnerId, LocalDateTime dateTime) {
        var timeslot = new YaServiceTimeSlotDto();
        timeslot.setId(id);
        timeslot.setPartnerId(partnerId);
        timeslot.setDate(dateTime);
        return timeslot;
    }

    private YaServicePartnerDescriptionDto buildYaServicePartnerDescription(String name, String inn, String ogrn) {
        var description = new YaServicePartnerDescriptionDto();
        description.setName(name);
        description.setDuration(100);
        description.setInn(inn);
        description.setVat(VatType.VAT_20.getTrustId());
        description.setFullName("Full " + name);
        description.setOgrn(ogrn);
        description.setScheduleText("Пн-Вс 9:00-10:00");
        return description;
    }

    private ItemServiceTimeslotResultViewModel getTimeSlotResultByKey(String key,
                                                                      ItemServiceTimeslotResultsViewModel response) {
        return response.getTimeslotsResult().stream()
                .filter(it -> Objects.equals(it.getKey(), key))
                .findFirst()
                .orElse(null);
    }

    private ItemServiceTimeSlotViewModel buildItemServiceTimeslotViewModel(String name, String inn,
                                                                           LocalDateTime date, Integer duration,
                                                                           String ogrn) {
        var model = new ItemServiceTimeSlotViewModel();
        model.setName(name);
        model.setInn(inn);
        model.setDate(date);
        model.setDuration(duration);
        model.setFullName("Full " + name);
        model.setOgrn(ogrn);
        model.setScheduleText("Пн-Вс 9:00-10:00");
        return model;
    }

    private void checkDefaults(ItemServiceTimeslotResultsViewModel response) {
        assertNotNull(response.getDefaults());
        var defaultPartner = checkouterProperties.getItemServiceDefaultPartnerInfo();
        assertEquals(defaultPartner.getName(), response.getDefaults().getName());
        assertEquals(defaultPartner.getFullName(), response.getDefaults().getFullName());
        assertEquals(defaultPartner.getInn(), response.getDefaults().getInn());
        assertEquals(defaultPartner.getOgrn(), response.getDefaults().getOgrn());
        assertEquals(defaultPartner.getScheduleText(), response.getDefaults().getScheduleText());

        var actualAddress = response.getDefaults().getAddress();
        assertNotNull(actualAddress);
        checkAddress(actualAddress.getPost(), defaultPartner.getAddress().getPost());
        checkAddress(actualAddress.getLegal(), defaultPartner.getAddress().getLegal());

        assertThat(response.getDefaults().getTimeIntervals(),
                hasSize(checkouterFeatureReader.getList(CollectionFeatureType.ITEM_SERVICE_DEFAULT_TIME_INTERVALS,
                        ItemServiceDefaultTimeInterval.class).size()));
    }

    private void checkAddress(ItemServicePartnerInfoViewModel.Address actual,
                              ItemServiceDefaultPartnerInfo.AddressModel expected) {
        assertEquals(expected.getCountry(), actual.getCountry());
        assertEquals(expected.getStreet(), actual.getStreet());
        assertEquals(expected.getCity(), actual.getCity());
        assertEquals(expected.getZip(), actual.getZip());
        assertEquals(expected.getHome(), actual.getHome());
    }

    private static ItemServiceTimeslotsRequest buildItemServiceTimeslotsRequest(
            String gps,
            Long regionId,
            List<ItemServiceTimeslotsRequest.ItemServiceInfo> services
    ) {
        var request = new ItemServiceTimeslotsRequest();
        request.setGps(gps);
        request.setRegionId(regionId);
        request.setServices(services);
        return request;
    }

    private static ItemServiceTimeslotsRequest.ItemServiceInfo buildItemServiceInfo(String key, String wareMd5Id,
                                                                                    Long serviceId, LocalDate date) {
        var serviceInfo = new ItemServiceTimeslotsRequest.ItemServiceInfo();
        serviceInfo.setKey(key);
        serviceInfo.setWareMd5Id(wareMd5Id);
        serviceInfo.setServiceId(serviceId);
        serviceInfo.setDate(date);
        return serviceInfo;
    }
}
