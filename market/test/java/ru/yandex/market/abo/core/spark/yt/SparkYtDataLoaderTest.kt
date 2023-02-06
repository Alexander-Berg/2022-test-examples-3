package ru.yandex.market.abo.core.spark.yt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.spark.SparkManager
import ru.yandex.market.abo.core.spark.dao.SparkService
import ru.yandex.market.abo.core.spark.yt.affiliate.SparkYtAffiliateLoader
import ru.yandex.market.abo.core.spark.yt.checkstatus.SparkYtCheckStatusLoader
import ru.yandex.market.abo.core.spark.yt.risks.SparkYtRisksLoader
import ru.yandex.market.abo.core.spark.yt.shopdata.SparkYtShopDataLoader
import ru.yandex.market.abo.core.spark.yt.shopdata.SparkYtShopDataRow
import ru.yandex.market.abo.core.yt.YtService
import java.util.Optional

class SparkYtDataLoaderTest @Autowired constructor(
    private val sparkService: SparkService,
    private val sparkManager: SparkManager,
    private val ytService: YtService,
) : EmptyTest() {

    private val mockedYtService = mock<YtService>()

    /**
    для запуска нужно вставить свои credentials для похода в YT
    core/src/test/resources/test-application.properties строчки 83-85
     */
    @Test
    @Disabled
    fun `load from yt`() {
        val shopDataRow = SparkYtShopDataLoader.load(ytService, "1157452002762")
        assertThat(shopDataRow)
            .singleElement()
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(object {
                val charterCapital = 12000L
                val inn = "7452123280"
                val isActing = false
                val phones = listOf(
                    object {
                        val code = "982"
                        val number = "2868327"
                    }
                )
            })
    }

    @Test
    fun `convert row and save to db`() {
        val row = SparkYtShopDataRow(
            ogrn = "1",
            sparkId = 2,
            shortName = "3",
            fullName = "4",
            inn = "5",
            kpp = "6",
            jurAddress = "7",
            site = "8",
            email = "9",
            companyWithSameAddressCount = 10,
            isActing = true,
            statusCode = 12,
            charterCapital = 13,
            workersRange = "14 .. 14",
            indexOfDueDiligence = 15,
            failureScore = 16,
            paymentIndex = 17,
            consolidatedIndicator = "18",
            phones = listOf(
                SparkYtShopDataRow.Phone(
                    code = "19",
                    number = "20",
                    status = "21",
                    verificationDate = "2022-01-01",
                )
            ),
            lists = listOf(
                SparkYtShopDataRow.Lists(
                    id = "23",
                )
            ),
            personsWithoutWarrantActualDate = "2024-01-01",
            pwwPerson = listOf(
                SparkYtShopDataRow.PwwPerson(
                    fio = "25",
                    inn = "26",
                    position = "27",
                )
            ),
            leaders = listOf(
                SparkYtShopDataRow.Leader(
                    actualDate = "2028-01-01",
                    position = "29",
                    fio = "30",
                    inn = "31",
                    managementCompany = "32",
                    managementCompanyINN = "33",
                )
            ),
            arbitrageStat = listOf(
                SparkYtShopDataRow.ArbitrationCasesYear(
                    year = "2034",
                    defendant = Optional.of(
                        SparkYtShopDataRow.ArbitrationCasesYear.Defendant(
                            casesNumber = "35",
                            sum = "36",
                        )
                    ),
                    plaintiff = Optional.of(
                        SparkYtShopDataRow.ArbitrationCasesYear.Plaintiff(
                            casesNumber = "37",
                            sum = "38",
                        )
                    ),
                    thirdOrOtherPerson = Optional.of(
                        SparkYtShopDataRow.ArbitrationCasesYear.ThirdOrOtherPerson(
                            casesNumber = "39",
                            sum = "40",
                        )
                    )
                )
            ),
            federalLaw94 = listOf(
                SparkYtShopDataRow.FederalLaw(
                    year = "2041",
                    contracts = Optional.of(
                        SparkYtShopDataRow.FederalLaw.Contracts(
                            signedNumber = "42",
                            sum = "43",
                        )
                    ),
                    tenders = Optional.of(
                        SparkYtShopDataRow.FederalLaw.Tenders(
                            admittedNumber = "44",
                            notAdmittedNumber = "45",
                            winnerNumber = "46",
                        )
                    )
                )
            ),
            federalLaw223 = listOf(
                SparkYtShopDataRow.FederalLaw(
                    year = "2047",
                    contracts = Optional.of(
                        SparkYtShopDataRow.FederalLaw.Contracts(
                            signedNumber = "48",
                            sum = "49",
                        )
                    ),
                    tenders = Optional.of(
                        SparkYtShopDataRow.FederalLaw.Tenders(
                            admittedNumber = "50",
                            notAdmittedNumber = "51",
                            winnerNumber = "52",
                        )
                    )
                )
            ),
            okved2list = listOf(
                SparkYtShopDataRow.OKVED2List(
                    code = "53",
                    isMain = "54",
                    isMainERGUL = "55",
                    isMainRosstat = "56",
                    name = "57",
                )
            ),
            changesInNameAndLegalForm = listOf(
                SparkYtShopDataRow.ChangeInNameAndLegalForm(
                    changeDate = "2058-01-01",
                    inn = "59",
                    ogrn = "60",
                )
            ),
            bankruptcyMessages = listOf(
                SparkYtShopDataRow.BankruptcyMessage(
                    caseId = "61",
                    date = "2062-01-01",
                    decisionDate = "2063-01-01",
                    idType = "64",
                )
            ),
            bankruptcyArbitrationCases = listOf(
                SparkYtShopDataRow.BankruptcyArbitrationCase(
                    acceptanceDate = "2065-01-01",
                    bankruptcyProceedingsDate = "2066-01-01",
                    completeDate = "2077-01-01",
                    id = "78",
                    number = "79",
                    registrationDate = "2080-01-01",
                    statusId = "81",
                    supervisionDate = "2082-01-01",
                )
            ),
            executionProceedingsActive = "82",
            executionProceedingsExecuted = "83",
            pledgerCeased = 84,
            pledgerActive = 85,
            pledgeeCeased = 111,
            pledgeeActive = 112,
            financeAndTax = listOf(
                SparkYtShopDataRow.FinanceAndTax(
                    endDate = "2086-01-01",
                    expenses = "87",
                    income = "88",
                    taxArrears = Optional.of(
                        SparkYtShopDataRow.FinanceAndTax.Tax(
                            sum = "89"
                        )
                    ),
                    taxes = Optional.of(
                        SparkYtShopDataRow.FinanceAndTax.Tax(
                            sum = "90",
                        )
                    ),
                    taxPenalties = Optional.of(
                        SparkYtShopDataRow.FinanceAndTax.Tax(
                            sum = "91",
                        )
                    )
                )
            ),
            frozenAccounts = listOf(
                SparkYtShopDataRow.FrozenAccount(
                    date = "2092-01-01",
                    number = "93",
                    bank = Optional.of(
                        SparkYtShopDataRow.FrozenAccount.Bank(
                            bik = "94",
                            name = "95",
                            sparkId = "96",
                        )
                    ),
                    taxAuthority = Optional.of(
                        SparkYtShopDataRow.FrozenAccount.TaxAuthority(
                            code = "97",
                            name = "98",
                        )
                    )
                )
            ),
            accessibleFinData = listOf(
                SparkYtShopDataRow.AccessibleFinData(
                    endDate = "2099-01-01",
                    idPeriod = "100",
                    name = "101",
                )
            ),
            finPeriod = listOf(
                SparkYtShopDataRow.FinPeriod(
                    dateBegin = "2102-01-01",
                    dateEnd = "2103-01-01",
                    periodName = "104",
                    stringList = Optional.of(
                        SparkYtShopDataRow.FinPeriod.StringList(
                            stringListInfo = Optional.of(
                                listOf(
                                    SparkYtShopDataRow.FinPeriod.StringList.StringListInfo(
                                        code = "105",
                                        form = "106",
                                        idFinPok = "107",
                                        name = "108",
                                        section = "109",
                                        value = "110",
                                    ),
                                )
                            )
                        )
                    )
                ),
            ),
            factors = listOf(
                SparkYtShopDataRow.Factor("101")
            ),
            executionProceedingsActiveSum = "19026819,54",
            linkedOGRNIPs = emptyList()
        )

        assertSaveAndLoad(row)
    }

    @Test
    fun `convert row with nulls`() {
        val row = SparkYtShopDataRow(
            ogrn = "1",
            sparkId = 2,
            shortName = null,
            fullName = "Рога и Копыта",
            inn = null,
            kpp = null,
            jurAddress = null,
            site = null,
            email = null,
            companyWithSameAddressCount = null,
            isActing = false,
            statusCode = null,
            charterCapital = null,
            workersRange = null,
            indexOfDueDiligence = null,
            failureScore = null,
            paymentIndex = null,
            consolidatedIndicator = null,
            phones = listOf(
                SparkYtShopDataRow.Phone(
                    code = null,
                    number = null,
                    status = null,
                    verificationDate = null,
                )
            ),
            lists = listOf(
                SparkYtShopDataRow.Lists(
                    id = null,
                )
            ),
            personsWithoutWarrantActualDate = null,
            pwwPerson = listOf(
                SparkYtShopDataRow.PwwPerson(
                    fio = null,
                    inn = null,
                    position = null,
                )
            ),
            leaders = listOf(
                SparkYtShopDataRow.Leader(
                    actualDate = null,
                    position = null,
                    fio = null,
                    inn = null,
                    managementCompany = null,
                    managementCompanyINN = null,
                )
            ),
            arbitrageStat = listOf(
                SparkYtShopDataRow.ArbitrationCasesYear(
                    year = "2034",
                    defendant = Optional.of(
                        SparkYtShopDataRow.ArbitrationCasesYear.Defendant(
                            casesNumber = "35",
                            sum = null,
                        )
                    ),
                    plaintiff = Optional.of(
                        SparkYtShopDataRow.ArbitrationCasesYear.Plaintiff(
                            casesNumber = "37",
                            sum = null,
                        )
                    ),
                    thirdOrOtherPerson = Optional.of(
                        SparkYtShopDataRow.ArbitrationCasesYear.ThirdOrOtherPerson(
                            casesNumber = "39",
                            sum = null,
                        )
                    )
                ),
                SparkYtShopDataRow.ArbitrationCasesYear(
                    year = "2040",
                    defendant = Optional.empty(),
                    plaintiff = Optional.empty(),
                    thirdOrOtherPerson = Optional.empty(),
                )
            ),
            federalLaw94 = listOf(
                SparkYtShopDataRow.FederalLaw(
                    year = "2041",
                    contracts = Optional.of(
                        SparkYtShopDataRow.FederalLaw.Contracts(
                            signedNumber = "42",
                            sum = null,
                        )
                    ),
                    tenders = Optional.of(
                        SparkYtShopDataRow.FederalLaw.Tenders(
                            admittedNumber = "44",
                            notAdmittedNumber = "45",
                            winnerNumber = "46",
                        )
                    )
                ),
                SparkYtShopDataRow.FederalLaw(
                    year = "2049",
                    contracts = Optional.empty(),
                    tenders = Optional.empty(),
                )
            ),
            federalLaw223 = listOf(
                SparkYtShopDataRow.FederalLaw(
                    year = "2047",
                    contracts = Optional.of(
                        SparkYtShopDataRow.FederalLaw.Contracts(
                            signedNumber = "48",
                            sum = null,
                        )
                    ),
                    tenders = Optional.of(
                        SparkYtShopDataRow.FederalLaw.Tenders(
                            admittedNumber = "50",
                            notAdmittedNumber = "51",
                            winnerNumber = "52",
                        )
                    )
                ),
                SparkYtShopDataRow.FederalLaw(
                    year = "2053",
                    contracts = Optional.empty(),
                    tenders = Optional.empty(),
                )
            ),
            okved2list = listOf(
                SparkYtShopDataRow.OKVED2List(
                    code = "54",
                    isMain = null,
                    isMainERGUL = null,
                    isMainRosstat = null,
                    name = "55",
                )
            ),
            changesInNameAndLegalForm = listOf(
                SparkYtShopDataRow.ChangeInNameAndLegalForm(
                    changeDate = null,
                    inn = null,
                    ogrn = null,
                )
            ),
            bankruptcyMessages = listOf(
                SparkYtShopDataRow.BankruptcyMessage(
                    caseId = null,
                    date = null,
                    decisionDate = null,
                    idType = null,
                )
            ),
            bankruptcyArbitrationCases = listOf(
                SparkYtShopDataRow.BankruptcyArbitrationCase(
                    acceptanceDate = null,
                    bankruptcyProceedingsDate = null,
                    completeDate = null,
                    id = null,
                    number = null,
                    registrationDate = null,
                    statusId = null,
                    supervisionDate = null,
                )
            ),
            executionProceedingsActive = null,
            executionProceedingsExecuted = null,
            pledgerCeased = null,
            pledgerActive = null,
            pledgeeCeased = null,
            pledgeeActive = null,
            financeAndTax = listOf(
                SparkYtShopDataRow.FinanceAndTax(
                    endDate = null,
                    expenses = null,
                    income = null,
                    taxArrears = Optional.of(
                        SparkYtShopDataRow.FinanceAndTax.Tax(
                            sum = null
                        )
                    ),
                    taxes = Optional.of(
                        SparkYtShopDataRow.FinanceAndTax.Tax(
                            sum = null,
                        )
                    ),
                    taxPenalties = Optional.of(
                        SparkYtShopDataRow.FinanceAndTax.Tax(
                            sum = null,
                        )
                    )
                ),
                SparkYtShopDataRow.FinanceAndTax(
                    endDate = null,
                    expenses = null,
                    income = null,
                    taxArrears = Optional.empty(),
                    taxes = Optional.empty(),
                    taxPenalties = Optional.empty(),
                )
            ),
            frozenAccounts = listOf(
                SparkYtShopDataRow.FrozenAccount(
                    date = null,
                    number = null,
                    bank = Optional.of(
                        SparkYtShopDataRow.FrozenAccount.Bank(
                            bik = null,
                            name = null,
                            sparkId = null,
                        )
                    ),
                    taxAuthority = Optional.of(
                        SparkYtShopDataRow.FrozenAccount.TaxAuthority(
                            code = null,
                            name = null,
                        )
                    )
                ),
                SparkYtShopDataRow.FrozenAccount(
                    date = null,
                    number = null,
                    bank = Optional.empty(),
                    taxAuthority = Optional.empty(),
                )
            ),
            accessibleFinData = listOf(
                SparkYtShopDataRow.AccessibleFinData(
                    endDate = null,
                    idPeriod = null,
                    name = null,
                )
            ),
            finPeriod = listOf(
                SparkYtShopDataRow.FinPeriod(
                    dateBegin = null,
                    dateEnd = null,
                    periodName = null,
                    stringList = Optional.of(
                        SparkYtShopDataRow.FinPeriod.StringList(
                            stringListInfo = Optional.of(
                                listOf(
                                    SparkYtShopDataRow.FinPeriod.StringList.StringListInfo(
                                        code = null,
                                        form = null,
                                        idFinPok = null,
                                        name = null,
                                        section = null,
                                        value = null,
                                    ),
                                )
                            )
                        )
                    )
                ),
                SparkYtShopDataRow.FinPeriod(
                    dateBegin = null,
                    dateEnd = null,
                    periodName = null,
                    stringList = Optional.empty()
                ),
                SparkYtShopDataRow.FinPeriod(
                    dateBegin = null,
                    dateEnd = null,
                    periodName = null,
                    stringList = Optional.of(
                        SparkYtShopDataRow.FinPeriod.StringList(
                            stringListInfo = Optional.empty(),
                        )
                    )
                )
            ),
            factors = listOf(
                SparkYtShopDataRow.Factor(null)
            ),
            executionProceedingsActiveSum = null,
            linkedOGRNIPs = listOf(
                SparkYtShopDataRow.LinkedOGRNIP(
                    text = null,
                    isActing = null,
                )
            )
        )

        assertSaveAndLoad(row)
    }

    @Test
    fun `row with empty collections`() {
        val row = SparkYtShopDataRow(
            ogrn = "1",
            sparkId = 2,
            shortName = "3",
            fullName = "4",
            inn = "5",
            kpp = "6",
            jurAddress = "7",
            site = "8",
            email = "9",
            companyWithSameAddressCount = 10,
            isActing = true,
            statusCode = 12,
            charterCapital = 13,
            workersRange = "14 .. 14",
            indexOfDueDiligence = 15,
            failureScore = 16,
            paymentIndex = 17,
            consolidatedIndicator = "18",
            phones = emptyList(),
            lists = emptyList(),
            personsWithoutWarrantActualDate = "2024-01-01",
            pwwPerson = emptyList(),
            leaders = emptyList(),
            arbitrageStat = emptyList(),
            federalLaw94 = emptyList(),
            federalLaw223 = emptyList(),
            okved2list = emptyList(),
            changesInNameAndLegalForm = emptyList(),
            bankruptcyMessages = emptyList(),
            bankruptcyArbitrationCases = emptyList(),
            executionProceedingsActive = "82",
            executionProceedingsExecuted = "83",
            pledgerCeased = 84,
            pledgerActive = 85,
            pledgeeCeased = 111,
            pledgeeActive = 112,
            financeAndTax = emptyList(),
            frozenAccounts = emptyList(),
            accessibleFinData = emptyList(),
            finPeriod = emptyList(),
            factors = emptyList(),
            executionProceedingsActiveSum = "19026819,54",
            linkedOGRNIPs = emptyList(),
        )

        assertSaveAndLoad(row)
    }

    private fun assertSaveAndLoad(row: SparkYtShopDataRow) {
        val result = SparkYtShopDataLoader.convertToSparkShopData(mockedYtService, row)

        sparkService.save(result)
        flushAndClear()

        val loaded = sparkManager.getSparkShopDataFromDb("1")
        assertEquals(result, loaded)
    }

    @Test
    fun `test serialization`() {
        SparkYtRisksLoader.javaClass
        SparkYtShopDataLoader.javaClass
        SparkYtCheckStatusLoader.javaClass
        SparkYtAffiliateLoader.javaClass
    }
}
