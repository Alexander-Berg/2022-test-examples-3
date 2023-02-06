package ru.yandex.market.abo.clch.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.clch.model.OrganizationInfo;
import ru.yandex.market.abo.clch.provider.DataProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.abo.clch.checker.OrganizationInfoChecker.FIFTY_PERCENT;
import static ru.yandex.market.abo.clch.checker.OrganizationInfoChecker.HUNDRED_PERCENT;

/**
 * @author kukabara@yandex-team.ru
 */
public class OrganizationInfoCheckerTest {
    private static final String OGRN_1 = "thirteen_size";
    private static final String OGRN_2 = "thirteenOther";

    @InjectMocks
    private OrganizationInfoChecker organizationInfoChecker;
    @Mock
    DataProvider<OrganizationInfo> dataProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        organizationInfoChecker.configure(new CheckerDescriptor(0, null));
    }

    @ParameterizedTest(name = "compare_org_data_{index}")
    @MethodSource("testOrgData")
    public void compareOrgData(OrganizationInfo first, OrganizationInfo second, double expected) {
        assertEquals(expected, organizationInfoChecker.compareData(first, second));
    }

    private static Stream<Arguments> testOrgData() {
        return Stream.of(
                Arguments.of(
                        OrganizationInfo.builder().ogrn("shortOgrn1").build(),
                        OrganizationInfo.builder().ogrn("shortOgrn1").build(),
                        FIFTY_PERCENT
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn("shortOgrn1").build(),
                        OrganizationInfo.builder().ogrn("shortOgrn2").build(),
                        0
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(OGRN_1).build(),
                        OrganizationInfo.builder().ogrn(OGRN_1).build(),
                        HUNDRED_PERCENT
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(null).build(),
                        OrganizationInfo.builder().ogrn(null).build(),
                        0
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(null).build(),
                        OrganizationInfo.builder().ogrn("foo").build(),
                        0
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn("  ").build(),
                        OrganizationInfo.builder().ogrn(" ").build(),
                        0
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(OGRN_1).build(),
                        OrganizationInfo.builder().ogrn(OGRN_2).build(),
                        0
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(OGRN_1).juridicalAddress("foobar").name("=>_<=").build(),
                        OrganizationInfo.builder().ogrn(OGRN_2).juridicalAddress("foobar").factAddress(".__.").build(),
                        HUNDRED_PERCENT
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(OGRN_1).juridicalAddress("foobar").build(),
                        OrganizationInfo.builder().ogrn(OGRN_2).juridicalAddress("baz").build(),
                        0
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(OGRN_1).factAddress("foobar").build(),
                        OrganizationInfo.builder().ogrn(OGRN_2).factAddress("foobar").build(),
                        0
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(OGRN_1).factAddress("foobar").build(),
                        OrganizationInfo.builder().ogrn(OGRN_2).factAddress("baz").build(),
                        0
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(OGRN_1).name("foobar").build(),
                        OrganizationInfo.builder().ogrn(OGRN_2).name("foobar").build(),
                        HUNDRED_PERCENT
                ),
                Arguments.of(
                        OrganizationInfo.builder().ogrn(OGRN_1).name("foobar").build(),
                        OrganizationInfo.builder().ogrn(OGRN_2).name("baz").build(),
                        0
                )
        );
    }
}
