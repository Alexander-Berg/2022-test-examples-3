package ru.yandex.market.tpl.api.advice;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalRetrieveApi;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.GpsCoord;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveRequestItem;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalAddressKeys;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveRequest;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveResponse;
import ru.yandex.market.tpl.common.personal.client.tpl.EnrichPersonalDataService;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ContextConfiguration(classes = {
        EnrichPersonalDataService.class,
        PersonalExternalService.class,
        PersonalDataTestController.class
})
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Transactional(propagation = Propagation.NOT_SUPPORTED) //обогащение ПД должно проходить вне транзакции
public class AdviceEnrichmentTest extends BaseApiIntTest {

    private static final String PHONE = "+71112223344";
    private static final String EMAIL = "testEmail@mail.ru";
    private static final String SURNAME = "Pupkin";
    private static final String FORENAME = "Vasiliy";
    private static final BigDecimal LONGITUDE = BigDecimal.valueOf(12);
    private static final BigDecimal LATITUDE = BigDecimal.valueOf(11);

    private final DefaultPersonalRetrieveApi personalRetrieveApi;

    private final TestUserHelper testUserHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final ObjectMapper tplObjectMapper;


    @BeforeEach
    void setUp() {

        PersonalMultiTypeRetrieveRequest request = new PersonalMultiTypeRetrieveRequest().items(
                List.of(new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.EMAIL).id("4321"),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.PHONE).id("1234"),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.FULL_NAME).id("5678"),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.ADDRESS).id("90"),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.GPS_COORD).id("09"))
        );
        PersonalMultiTypeRetrieveResponse response = new PersonalMultiTypeRetrieveResponse().items(
                List.of(new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.PHONE).id("1234")
                                .value(new CommonType().phone(PHONE)),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.EMAIL).id("4321")
                                .value(new CommonType().email(EMAIL)),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.FULL_NAME).id("5678")
                                .value(new CommonType().fullName(new FullName().forename(FORENAME).surname(SURNAME))),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.ADDRESS).id("90")
                                .value(new CommonType().address(
                                        Map.of(
                                                "test", "test",
                                                PersonalAddressKeys.LOCALITY.getName(), "city",
                                                PersonalAddressKeys.STREET.getName(), "street",
                                                PersonalAddressKeys.HOUSE.getName(), "house",
                                                PersonalAddressKeys.BUILDING.getName(), "building",
                                                PersonalAddressKeys.HOUSING.getName(), "housing",
                                                PersonalAddressKeys.ROOM.getName(), "room",
                                                PersonalAddressKeys.FLOOR.getName(), "floor",
                                                PersonalAddressKeys.PORCH.getName(), "porch",
                                                PersonalAddressKeys.INTERCOM.getName(), "intercom"
                                        )
                                )),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.GPS_COORD).id("09")
                                .value(new CommonType().gpsCoord(
                                        new GpsCoord().latitude(LATITUDE).longitude(LONGITUDE)
                                ))
                )
        );
        doReturn(response).when(personalRetrieveApi).v1MultiTypesRetrievePost(request);

        testUserHelper.findOrCreateUser(1L);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.PERSONAL_DATA_ENRICH_FROM_ADVICE_API_ENABLED,
                true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.ENRICH_ADDRESS_ENABLED, true);

    }

    @Test
    void personalDataEnriched_WhenHasPersonalData() throws Exception {
        //given
        HasPersonalDataTestImpl testbj = new HasPersonalDataTestImpl();
        testbj.setPersonalDataDtos(List.of(new HasPersonalFieldTestImpl("1234", "4321", "5678", "90", "09")));

        //when
        var res = mockMvc.perform(get("/test/pd/enrich")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(testbj)))
                .andReturn()
                .getResponse();

        //then
        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());

        tplObjectMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        var responseDto = tplObjectMapper.readValue(res.getContentAsString(), HasPersonalDataTestImpl.class);

        HasPersonalFieldTestImpl personalDataDto = responseDto.personalDataDtos.get(0);
        assertThat(personalDataDto.getEmail()).isEqualTo(EMAIL);
        assertThat(personalDataDto.getPhone()).isEqualTo(PHONE);
        assertThat(personalDataDto.getName()).isEqualTo(SURNAME + " " + FORENAME);
        assertThat(new String(personalDataDto.getAddress().getBytes("Cp1252"), StandardCharsets.UTF_8))
                .isEqualTo("г. city");
        assertThat(personalDataDto.getLatitude()).isEqualTo(LATITUDE);
        assertThat(personalDataDto.getLongitude()).isEqualTo(LONGITUDE);
    }

    @Test
    void personalDataNotEnriched_WhenPersonalDataFieldsAreNull() throws Exception {
        //given
        HasPersonalDataTestImpl testbj = new HasPersonalDataTestImpl();
        HasPersonalFieldTestImpl pdFieldDto = new HasPersonalFieldTestImpl(null, null, null, null, null);
        String email = "some random email";
        pdFieldDto.setEmail(email);
        String phone = "+1234567890";
        pdFieldDto.setPhone(phone);
        String name = "some name";
        pdFieldDto.setName(name);
        String address = "address";
        pdFieldDto.setAddress(address);
        BigDecimal longitude = BigDecimal.valueOf(40);
        pdFieldDto.setLongitude(longitude);
        testbj.setPersonalDataDtos(List.of(pdFieldDto));

        //when
        var res = mockMvc.perform(get("/test/pd/enrich")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(testbj)))
                .andReturn()
                .getResponse();

        //then
        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());

        tplObjectMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        var responseDto = tplObjectMapper.readValue(res.getContentAsString(), HasPersonalDataTestImpl.class);

        HasPersonalFieldTestImpl personalDataDto = responseDto.personalDataDtos.get(0);
        assertThat(personalDataDto.getEmail()).isEqualTo(email);
        assertThat(personalDataDto.getPhone()).isEqualTo(phone);
        assertThat(personalDataDto.getName()).isEqualTo(name);
        assertThat(personalDataDto.getAddress()).isEqualTo(address);
        assertThat(personalDataDto.getLongitude()).isEqualTo(longitude);
        assertThat(personalDataDto.getLatitude()).isNull();
    }

    @Test
    void doesNotThrow_WhenPersonalDataIsNull() throws Exception {
        //given
        HasPersonalDataTestImpl testbj = new HasPersonalDataTestImpl();

        //when
        var res = mockMvc.perform(get("/test/pd/enrich")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(testbj)))
                .andReturn()
                .getResponse();

        //then
        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void personalDataEnriched_WhenHasPersonalDataIndented() throws Exception {
        //given
        HasPersonalDataTestImpl personalDataTest = new HasPersonalDataTestImpl();
        personalDataTest.setPersonalDataDtos(List.of(new HasPersonalFieldTestImpl("1234", "4321", "5678", "90", "09")));
        HasPersonalDataIndented testbj = new HasPersonalDataIndented();
        testbj.setPersonalDataItems(List.of(personalDataTest));

        //when
        var res = mockMvc.perform(get("/test/pd/enrich/indented")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(testbj)))
                .andReturn()
                .getResponse();

        //then
        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());

        tplObjectMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        var responseDto = tplObjectMapper.readValue(res.getContentAsString(), HasPersonalDataIndented.class);

        HasPersonalDataTestImpl personalDataResult = responseDto.personalDataItems.get(0);
        HasPersonalFieldTestImpl personalFieldResult = personalDataResult.getPersonalDataDtos().get(0);
        assertThat(personalFieldResult.getPhone()).isEqualTo(PHONE);
        assertThat(personalFieldResult.getEmail()).isEqualTo(EMAIL);
        assertThat(personalFieldResult.getName()).isEqualTo(SURNAME + " " + FORENAME);
        assertThat(new String(personalFieldResult.getAddress().getBytes("Cp1252"), StandardCharsets.UTF_8))
                .isEqualTo("г. city");
        assertThat(personalFieldResult.getLatitude()).isEqualTo(LATITUDE);
        assertThat(personalFieldResult.getLongitude()).isEqualTo(LONGITUDE);
    }

    @Test
    void doesNotThrow_WhenNoHasPersonalData() throws Exception {
        //given
        HasNoPersonalDataImpl hasNoPersonalData = new HasNoPersonalDataImpl();
        hasNoPersonalData.setPersonaTestFields(List.of(new HasPersonalFieldTestImpl("1234", "4321", "5678", "90", "09"
        )));

        //when
        var res = mockMvc.perform(get("/test/pd/enrich/nopd")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(hasNoPersonalData)))
                .andReturn()
                .getResponse();

        //then
        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void personalDataEnriched_WhenAbstractClassReturnType() throws Exception {
        //given
        HasPersonalDataTestImpl hasNoPersonalData = new HasPersonalDataTestImpl();
        hasNoPersonalData.setPersonalDataDtos(List.of(new HasPersonalFieldTestImpl("1234", "4321", "5678", "90",
                "09")));

        //when
        var res = mockMvc.perform(get("/test/pd/enrich/abstract")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(hasNoPersonalData)))
                .andReturn()
                .getResponse();

        //then
        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());

        tplObjectMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        var responseDto = tplObjectMapper.readValue(res.getContentAsString(), HasPersonalDataTestImpl.class);

        HasPersonalFieldTestImpl personalDataDto = responseDto.getPersonalDataDtos().get(0);
        assertThat(personalDataDto.getEmail()).isEqualTo(EMAIL);
        assertThat(personalDataDto.getPhone()).isEqualTo(PHONE);
        assertThat(personalDataDto.getName()).isEqualTo(SURNAME + " " + FORENAME);
        assertThat(new String(personalDataDto.getAddress().getBytes("Cp1252"), StandardCharsets.UTF_8))
                .isEqualTo("г. city");
        assertThat(personalDataDto.getLatitude()).isEqualTo(LATITUDE);
        assertThat(personalDataDto.getLongitude()).isEqualTo(LONGITUDE);
    }
}
