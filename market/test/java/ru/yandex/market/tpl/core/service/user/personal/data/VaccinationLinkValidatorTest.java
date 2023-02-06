package ru.yandex.market.tpl.core.service.user.personal.data;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.user.partner.PartnerUserPersonalDataRequestDto;
import ru.yandex.market.tpl.common.covid.VaccinationValidator;
import ru.yandex.market.tpl.common.covid.external.TplVaccinationInfo;
import ru.yandex.market.tpl.common.covid.external.TplVaccinationInfoSource;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccinationLinkValidatorTest {

    public static final String LINK = "https://www.gosuslugi.ru/covid-cert/12123?adfads";
    public static final long USER_ID = 777L;
    public static final LocalDate EXPECTED_BIRTHDAY_DATE = LocalDate.of(1967, 6, 23);
    public static final String EXPECTED_FIO = "Р**** М**** П***";
    public static final String EXPECTED_PASSPORT = "61** **230";

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private VaccinationValidator validator;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private VaccinationLinkValidator linkValidator;

    @BeforeEach
    void setUp() {
        Mockito.reset(userRepository);
        User mockedUser = Mockito.mock(User.class);
        when(mockedUser.getFirstName()).thenReturn("Марат");
        when(mockedUser.getLastName()).thenReturn("Рязанов");

        when(userRepository.findByIdOrThrow(USER_ID)).thenReturn(mockedUser);
    }

    @ParameterizedTest
    @MethodSource("provideObjectsForIsBlank")
    void validateLink_Fail(
            TplVaccinationInfo actualInfo,
            String expectedError
    ) {
        //when
        TplInvalidParameterException exception = assertThrows(
                TplInvalidParameterException.class,
                () -> linkValidator.validatePersonalData(
                        PartnerUserPersonalDataRequestDto.builder()
                                .link(LINK)
                                .passport("6112456230")
                                .birthdayDate(EXPECTED_BIRTHDAY_DATE)
                                .build(),
                        USER_ID,
                        actualInfo
                )
        );
        //then
        assertThat(exception.getMessage()).isEqualTo(expectedError);
    }

    @Test
    void validateLink_Success() {
        //when
        assertDoesNotThrow(
                () -> linkValidator.validatePersonalData(
                        PartnerUserPersonalDataRequestDto.builder()
                                .link(LINK)
                                .passport("6112456230")
                                .birthdayDate(EXPECTED_BIRTHDAY_DATE)
                                .build(),
                        USER_ID,
                        TplVaccinationInfo
                                .builder()
                                .fio(EXPECTED_FIO)
                                .birthdayDate(EXPECTED_BIRTHDAY_DATE)
                                .source(TplVaccinationInfoSource.GOSUSLUGI)
                                .passport(EXPECTED_PASSPORT)
                                .build()
                )
        );
    }

    private static Stream<Arguments> provideObjectsForIsBlank() {
        return Stream.of(
                Arguments.of(
                        TplVaccinationInfo
                                .builder()
                                .fio("Р**** А**** П***")
                                .birthdayDate(EXPECTED_BIRTHDAY_DATE)
                                .source(TplVaccinationInfoSource.GOSUSLUGI)
                                .passport(EXPECTED_PASSPORT)
                                .build(),
                        VaccinationLinkValidator.FIO_VALIDATION_ERROR
                ),
                Arguments.of(
                        TplVaccinationInfo
                                .builder()
                                .fio(EXPECTED_FIO)
                                .birthdayDate(EXPECTED_BIRTHDAY_DATE)
                                .source(TplVaccinationInfoSource.GOSUSLUGI)
                                .passport("71** **230")
                                .build(),
                        VaccinationLinkValidator.PASSPORT_VALIDATION_ERROR
                ),
                Arguments.of(
                        TplVaccinationInfo
                                .builder()
                                .fio(EXPECTED_FIO)
                                .birthdayDate(LocalDate.of(2020,10,10))
                                .source(TplVaccinationInfoSource.GOSUSLUGI)
                                .passport(EXPECTED_PASSPORT)
                                .build(),
                        VaccinationLinkValidator.BIRTH_DATE_VALIDATE_ERROR
                )
        );
    }
}
