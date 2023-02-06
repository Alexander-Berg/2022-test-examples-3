package ru.yandex.market.core.agency.program.purchase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.program.purchase.model.FullArpAgencyLinkInfo;
import ru.yandex.market.core.agency.program.purchase.model.OnBoardingRewardType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArpAgencyPartnerLinkServiceFunctionalTest extends FunctionalTest {

    private static final LocalDate NOW = LocalDate.parse("2020-01-01");

    @Autowired
    private ArpAgencyPartnerLinkServiceImpl arpAgencyPartnerLinkService;

    @Test
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testInsert.before.csv",
            after = "ArpAgencyPartnerLinkServiceFunctionalTest.testInsert.after.csv")
    @DisplayName("Создание связки агентство - партнер")
    public void testInsert() {
        LocalDate bindingDate = LocalDate.parse("2020-01-01");
        arpAgencyPartnerLinkService.linkPartnerToAgency(1, 1, 2, OnBoardingRewardType.PARTIAL, bindingDate);
    }

    @Test
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testInsertWhenLinkExists.before.csv",
            after = "ArpAgencyPartnerLinkServiceFunctionalTest.testInsertWhenLinkExists.after.csv")
    @DisplayName("Создание связки агентство - партнер, когда есть связка с другим агентством")
    public void testInsertWhenLinkExists() {
        LocalDate bindingDate = LocalDate.parse("2020-05-01");
        arpAgencyPartnerLinkService.linkPartnerToAgency(1, 2, 1,
                OnBoardingRewardType.PARTIAL, bindingDate);
    }

    @Test
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testFindLinkByPartner.before.csv")
    @DisplayName("Поиск связки агентство - партнер")
    public void testFindLinkByPartner() {
        Optional<FullArpAgencyLinkInfo> existingLinkOpt = arpAgencyPartnerLinkService.loadFullLinkInfoByPartner(1L);

        assertThat(existingLinkOpt).isPresent();

        FullArpAgencyLinkInfo existingLink = existingLinkOpt.get();
        assertThat(existingLink.getAgencyId()).isEqualTo(1L);
        assertThat(existingLink.getAgencyName()).isEqualTo("Агентство 1");
        assertThat(existingLink.getPartnerId()).isEqualTo(1L);
        assertThat(existingLink.getOnboardingRewardType()).isEqualTo(OnBoardingRewardType.FULL);
        assertThat(existingLink.getLinkDate()).isEqualTo(LocalDate.parse("2020-01-01"));
        assertThat(existingLink.isOnboardingRewardPaid()).isTrue();
    }

    @Test
    @DisplayName("Поиск несуществующей связки агентство - партнер")
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testFindNonExistingLinkByPartner.before.csv")
    public void testFindNonExistingLinkByPartner() {
        Optional<FullArpAgencyLinkInfo> notExistingLinkOpt =
                arpAgencyPartnerLinkService.loadFullLinkInfoByPartner(2L);

        assertThat(notExistingLinkOpt).isEmpty();
    }

    @Test
    @DisplayName("Редактирование связки агентство-партнер")
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testUpdateLink.before.csv",
            after = "ArpAgencyPartnerLinkServiceFunctionalTest.testUpdateLink.after.csv")
    public void testUpdateLink() {
        int updatedRows = arpAgencyPartnerLinkService.updateLinkData(1, 1,
                LocalDate.parse("2021-01-01"), OnBoardingRewardType.PARTIAL);

        assertThat(updatedRows).isEqualTo(1);
    }

    @Test
    @DisplayName("Поиск несуществующей связки агентство - партнер")
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testUpdateOnPaid.before.csv")
    public void testUpdateOnPaidLink() {
        assertThatThrownBy(() -> arpAgencyPartnerLinkService.updateLinkData(1, 1,
                LocalDate.parse("2021-01-01"), OnBoardingRewardType.PARTIAL))
                .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testNewLinkOnReactivation.before.csv",
            after = "ArpAgencyPartnerLinkServiceFunctionalTest.testNewLinkOnReactivation.after.csv")
    @DisplayName("Если есть деактивированная связка агентство-партнер - создаем новую ")
    public void testNewLinkOnReactivation() {
        LocalDate bindingDate = LocalDate.parse("2020-01-02");
        arpAgencyPartnerLinkService.linkPartnerToAgency(1, 1, 1, OnBoardingRewardType.PARTIAL, bindingDate);
    }

    @Test
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testInsertIgnoredIfRecordExists.before.csv",
            after = "ArpAgencyPartnerLinkServiceFunctionalTest.testInsertIgnoredIfRecordExists.after.csv")
    @DisplayName("Если активная связка уже есть, оставляем ее как есть, не портя данные")
    public void testInsertIgnoredIfRecordExists() {
        LocalDate bindingDate = LocalDate.parse("2020-01-02");
        arpAgencyPartnerLinkService.linkPartnerToAgency(1, 1, 1, OnBoardingRewardType.PARTIAL, bindingDate);
    }

    @Test
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testDelete.before.csv",
            after = "ArpAgencyPartnerLinkServiceFunctionalTest.testDelete.after.csv")
    @DisplayName("Удаление связки агентство - партнер")
    public void testDelete() {
        arpAgencyPartnerLinkService.unlinkPartnerFromCurrentAgency(2, NOW);
    }

    @Test
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testFindPreviousLinks.before.csv")
    @DisplayName("Поиск предыдущих связок агентств")
    public void testFindPreviousLinks() {
        List<FullArpAgencyLinkInfo> result = arpAgencyPartnerLinkService.loadPreviousAgenciesByPartner(2);

        assertThat(result)
                .hasSize(2);

        FullArpAgencyLinkInfo firstAgency = result.get(0);
        assertThat(firstAgency.getAgencyId()).isEqualTo(4);

        FullArpAgencyLinkInfo secondAgency = result.get(1);
        assertThat(secondAgency.getAgencyId()).isEqualTo(3);
    }

    @Test
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testDeleteHistoryLink.before.csv",
            after = "ArpAgencyPartnerLinkServiceFunctionalTest.testDeleteHistoryLink.after.csv")
    @DisplayName("Удаление записи с открепленным агентством")
    public void testDeleteHistoryLink() {
        arpAgencyPartnerLinkService.deleteHistoryLink(1, 2, 3,
                LocalDate.parse("2020-01-01"));
    }

    @Test
    //before и after совпадают, т.к. удаления не должно произойти
    @DbUnitDataSet(before = "ArpAgencyPartnerLinkServiceFunctionalTest.testDeleteHistoryLink.before.csv",
            after = "ArpAgencyPartnerLinkServiceFunctionalTest.testDeleteHistoryLink.before.csv")
    @DisplayName("Удаление записи с открепленным агентством, которому выплачено вознаграждение - не должно сработать")
    public void testDeleteHistoryLinkWithAlreadyPaidReward() {
        assertThatThrownBy(() -> arpAgencyPartnerLinkService.deleteHistoryLink(1, 1, 3,
                LocalDate.parse("2020-01-01"))
        ).isInstanceOf(IllegalArgumentException.class);

    }


}
