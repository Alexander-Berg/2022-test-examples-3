package ru.yandex.direct.web.entity.cashback.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import javax.annotation.ParametersAreNonnullByDefault;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.common.mds.MdsHolder;
import ru.yandex.direct.core.entity.cashback.model.CashbackCardsProgram;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgram;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgramDetails;
import ru.yandex.direct.core.entity.cashback.model.CashbackRewardsDetails;
import ru.yandex.direct.core.entity.cashback.service.CashbackClientsService;
import ru.yandex.direct.core.entity.cashback.service.CashbackProgramsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.web.entity.cashback.CashbackCsvDetailsTranslations;
import ru.yandex.direct.web.entity.cashback.model.CashbackDetailsGroupMode;
import ru.yandex.direct.web.entity.cashback.model.CashbackDetailsRequest;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.inside.mds.MdsHosts;
import ru.yandex.inside.mds.MdsNamespace;
import ru.yandex.inside.mds.MdsPostResponse;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.ip.HostPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.cashback.CashbackConstants.DETALIZATION_MAX_LENGTH;
import static ru.yandex.direct.core.entity.cashback.CashbackConstants.DETALIZATION_MIN_LENGTH;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.entity.cashback.service.CashbackWebService.BOM_HEADER;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class CashbackWebServiceMockTest {
    private static final LocalDate DEFAULT_DATE = LocalDate.of(2021, 2, 1);
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final User USER = new User().withClientId(CLIENT_ID).withLogin("some login");
    private static final String DEFAULT_MDS_HOST = "mds.host";
    private static final int DEFAULT_MDS_PORT = 80;
    private static final String DEFAULT_MDS_NAMESPACE = "mdsNamespace";
    private static final BigDecimal TEN_WITHOUT_NDS = new BigDecimal(8);

    private static final String CSV_HEADER = BOM_HEADER
            + "Идентификатор акции,Название акции,Начисленный кешбэк,Дата начисления\n";
    private static final String SUCCESS_RESULT_NO_GROUP_MODE = CSV_HEADER
            + "1,Программа 1,8,2021-01\n" +
            "2,Программа 2,8,2020-12\n";
    private static final String SUCCESS_GROUP_BY_MONTH = BOM_HEADER
            + "Дата начисления,Название акции,Начисленный кешбэк\n" +
            "2020-12,Программа 2,8\n" +
            "2021-01,Программа 1,8\n";
    private static final String SUCCESS_GROUP_BY_PROGRAM = BOM_HEADER
            + "Название акции,Дата начисления,Начисленный кешбэк\n" +
            "Программа 1,2021-01,8\n" +
            "Программа 2,2020-12,8\n";

    @Mock
    private CashbackProgramsService programsService;

    @Mock
    private CashbackClientsService clientsService;

    @Mock
    private TranslationService translationService;

    @Mock
    private MdsHolder mdsHolder;

    @Spy
    @InjectMocks
    private CashbackWebService service;

    private String uploadedContents;

    @Before
    public void init() {
        var mdsHosts = mock(MdsHosts.class);
        var hostPort = new HostPort(DEFAULT_MDS_HOST, DEFAULT_MDS_PORT);
        doReturn(hostPort).when(mdsHosts).getHostPortForRead();
        doReturn(mdsHosts).when(mdsHolder).getHosts();
        var mdsNamespace = new MdsNamespace(DEFAULT_MDS_NAMESPACE, null, 1);
        doReturn(mdsNamespace).when(mdsHolder).getNamespace();

        var mdsKey = mock(MdsFileKey.class);
        doReturn("the key").when(mdsKey).serialize();
        var uploadResponse = mock(MdsPostResponse.class);
        doReturn(mdsKey).when(uploadResponse).getKey();
        doAnswer(invocation -> {
            var inputStreamSource = (InputStreamSource) invocation.getArgument(1);
            uploadedContents = inputStreamSource.readText();
            return uploadResponse;
        }).when(mdsHolder).upload(anyString(), any(InputStreamSource.class), any(Duration.class));

        initTranslations();

        doReturn(getRewardsDetails()).when(clientsService).getClientCashbackRewardsDetails(CLIENT_ID, DETALIZATION_MAX_LENGTH);

        service = new CashbackWebService(programsService, clientsService, translationService, mdsHolder);
    }

    private void initTranslations() {
        doReturn(Locale.forLanguageTag("ru")).when(translationService).getLocale();

        doReturn("Идентификатор акции").when(translationService)
                .translate(CashbackCsvDetailsTranslations.INSTANCE.programId());
        doReturn("Название акции").when(translationService)
                .translate(CashbackCsvDetailsTranslations.INSTANCE.programName());
        doReturn("Начисленный кешбэк").when(translationService)
                .translate(CashbackCsvDetailsTranslations.INSTANCE.reward());
        doReturn("Дата начисления").when(translationService)
                .translate(CashbackCsvDetailsTranslations.INSTANCE.date());
    }

    @Test
    public void getCashbackDetailsCsv_invalidPeriod() {
        var request = new CashbackDetailsRequest()
                .withPeriod(DETALIZATION_MAX_LENGTH + 1);
        var result = service.getCashbackDetailsCsv(USER, request);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(path(field(CashbackDetailsRequest.PERIOD_FIELD)),
                                NumberDefects.inInterval(DETALIZATION_MIN_LENGTH, DETALIZATION_MAX_LENGTH)))));
    }

    @Test
    public void getCashbackDetailsCsv_clientHasNoPrograms() {
        doReturn(getRewardDetailsWithoutPrograms()).when(clientsService).getClientCashbackRewardsDetails(CLIENT_ID, DETALIZATION_MAX_LENGTH);

        var request = new CashbackDetailsRequest().withPeriod(DETALIZATION_MAX_LENGTH);
        var result = service.getCashbackDetailsCsv(USER, request);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(uploadedContents).isNotBlank().isEqualTo(CSV_HEADER);
    }

    @Test
    public void getCashbackDetailsCsv_clientHasNoRewards() {
        doReturn(getRewardsDetails().withTotalByPrograms(List.of()))
                .when(clientsService).getClientCashbackRewardsDetails(CLIENT_ID, DETALIZATION_MAX_LENGTH);

        var request = new CashbackDetailsRequest().withPeriod(DETALIZATION_MAX_LENGTH);
        var result = service.getCashbackDetailsCsv(USER, request);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(uploadedContents).isNotBlank().isEqualTo(CSV_HEADER);
    }

    @Test
    public void getCashbackDetailsCsv_successNoGroupMode() {
        var request = new CashbackDetailsRequest().withPeriod(DETALIZATION_MAX_LENGTH);
        var result = service.getCashbackDetailsCsv(USER, request);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(uploadedContents).isNotBlank().isEqualTo(SUCCESS_RESULT_NO_GROUP_MODE);
    }

    @Test
    public void getCashbackDetailsCsv_successNoGroupByMonth() {
        var request = new CashbackDetailsRequest()
                .withGroupBy(CashbackDetailsGroupMode.BY_MONTH)
                .withPeriod(DETALIZATION_MAX_LENGTH);
        var result = service.getCashbackDetailsCsv(USER, request);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(uploadedContents).isNotBlank().isEqualTo(SUCCESS_GROUP_BY_MONTH);
    }

    @Test
    public void getCashbackDetailsCsv_successNoGroupByProgram() {
        var request = new CashbackDetailsRequest()
                .withGroupBy(CashbackDetailsGroupMode.BY_PROGRAM)
                .withPeriod(DETALIZATION_MAX_LENGTH);
        var result = service.getCashbackDetailsCsv(USER, request);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(uploadedContents).isNotBlank().isEqualTo(SUCCESS_GROUP_BY_PROGRAM);
    }

    private static CashbackProgram getProgram1() {
        return new CashbackProgram()
                .withId(1L)
                .withCategoryNameRu("Программа 1")
                .withCategoryNameEn("Program 1")
                .withCategoryDescriptionRu("Программа 1")
                .withCategoryDescriptionEn("Program 1")
                .withCategoryId(1L)
                .withPercent(BigDecimal.TEN)
                .withIsEnabled(true)
                .withIsPublic(true);
    }

    private static CashbackProgram getProgram2() {
        return new CashbackProgram()
                .withId(2L)
                .withCategoryNameRu("Программа 2")
                .withCategoryNameEn("Program 2")
                .withCategoryDescriptionRu("Программа 2")
                .withCategoryDescriptionEn("Program 2")
                .withCategoryId(2L)
                .withPercent(BigDecimal.TEN)
                .withIsEnabled(true)
                .withIsPublic(false);
    }

    private static CashbackCardsProgram getCardsProgram1() {
        return new CashbackCardsProgram()
                .withId(1L)
                .withNameRu("Программа 1")
                .withNameEn("Program 1")
                .withPercent(BigDecimal.TEN)
                .withIsGeneral(true);
    }

    private static CashbackCardsProgram getCardsProgram2() {
        return new CashbackCardsProgram()
                .withId(2L)
                .withNameRu("Программа 2")
                .withNameEn("Program 2")
                .withPercent(BigDecimal.TEN)
                .withIsGeneral(false);
    }

    private static CashbackRewardsDetails getRewardsDetails() {
        return new CashbackRewardsDetails()
                .withTotalByPrograms(List.of(
                        new CashbackProgramDetails()
                                .withProgramId(getProgram1().getId())
                                .withReward(BigDecimal.TEN)
                                .withRewardWithoutNds(TEN_WITHOUT_NDS)
                                .withDate(DEFAULT_DATE.minusMonths(1L))
                                .withProgram(getCardsProgram1()),
                        new CashbackProgramDetails()
                                .withProgramId(getProgram2().getId())
                                .withReward(BigDecimal.TEN)
                                .withRewardWithoutNds(TEN_WITHOUT_NDS)
                                .withDate(DEFAULT_DATE.minusMonths(2L))
                                .withProgram(getCardsProgram2())
                ));
    }

    private static CashbackRewardsDetails getRewardDetailsWithoutPrograms() {
        return new CashbackRewardsDetails()
                .withTotalByPrograms(List.of(
                        new CashbackProgramDetails()
                                .withProgramId(getProgram1().getId())
                                .withReward(BigDecimal.TEN)
                                .withRewardWithoutNds(TEN_WITHOUT_NDS)
                                .withDate(DEFAULT_DATE.minusMonths(1L)),
                        new CashbackProgramDetails()
                                .withProgramId(getProgram2().getId())
                                .withReward(BigDecimal.TEN)
                                .withRewardWithoutNds(TEN_WITHOUT_NDS)
                                .withDate(DEFAULT_DATE.minusMonths(2L))
                ));
    }
}
