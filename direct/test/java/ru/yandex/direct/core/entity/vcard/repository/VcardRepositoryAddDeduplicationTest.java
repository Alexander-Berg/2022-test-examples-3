package ru.yandex.direct.core.entity.vcard.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.AddedModelId;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;

/**
 * Проверки уникализации при добавлении визиток
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VcardRepositoryAddDeduplicationTest {

    @Autowired
    private VcardRepository vcardRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private UserSteps userSteps;

    private int shard;
    private long clientUid;
    private ClientId clientId;
    private long campaignId;

    @Before
    public void before() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign();
        shard = campaignInfo.getShard();
        clientUid = campaignInfo.getUid();
        clientId = campaignInfo.getClientId();
        campaignId = campaignInfo.getCampaignId();
    }

    // addVcards - проверка уникализации (когда карточка присутствует в БД)

    @Test
    public void addVcards_OneMatchingItemWithAllFields_ReturnsExistingId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        checkMatchingItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneMatchingItemWithoutPhoneExtension_ReturnsExistingId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        vcard1.getPhone().withExtension(null);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard2.getPhone().withExtension(null);
        checkMatchingItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneMatchingItemWithoutPhone_ReturnsExistingId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        vcard1.withPhone(null);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard2.withPhone(null);
        checkMatchingItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneMatchingItemWithoutOgrn_ReturnsExistingId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        vcard1.withOgrn(null);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard2.withOgrn(null);
        checkMatchingItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneMatchingItemWithoutManualPoint_ReturnsExistingId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        vcard1.withManualPoint(null);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard2.withManualPoint(null);
        checkMatchingItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneMatchingItemWithoutAutoPoint_ReturnsExistingId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        vcard1.withAutoPoint(null);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard2.withAutoPoint(null);
        checkMatchingItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_TwoDifferentItemsMatchingToExistingInDatabase_ReturnsExistingIds() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId).withApart("999");
        Vcard vcard11 = fullVcard(clientUid, campaignId);
        Vcard vcard21 = fullVcard(clientUid, campaignId).withApart("999");

        List<AddedModelId> ids1 = vcardRepository.addVcards(shard, clientUid, clientId, asList(vcard1, vcard2));
        checkState(ids1.size() == 2,
                "количество возвращенных id не соответствует количеству сохраняемых объектов");

        List<AddedModelId> idsToCheck = vcardRepository.addVcards(shard, clientUid, clientId, asList(vcard11, vcard21));
        checkState(idsToCheck.size() == 2,
                "количество возвращенных id не соответствует количеству сохраняемых объектов");

        assertThat("при добавлении нескольких существующих в базе карточек "
                        + "должны возвращаться id существующих карточек",
                idsToCheck, equalTo(ids1));
    }

    @Test
    public void addVcards_TwoEqualItemsMatchingToExistingInDatabase_ReturnsExistingId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard11 = fullVcard(clientUid, campaignId);
        Vcard vcard12 = fullVcard(clientUid, campaignId);

        List<AddedModelId> ids1 = vcardRepository.addVcards(shard, clientUid, clientId, singletonList(vcard1));
        checkState(ids1.size() == 1,
                "количество возвращенных id не соответствует количеству сохраняемых объектов");

        List<AddedModelId> idsToCheck = vcardRepository.addVcards(shard, clientUid, clientId, asList(vcard11, vcard12));
        checkState(idsToCheck.size() == 2,
                "количество возвращенных id не соответствует количеству сохраняемых объектов");

        assertThat("при добавлении нескольких одинаковых карточек, существующих в базе,"
                        + "должны возвращаться id существующей карточки",
                idsToCheck.get(0), equalTo(ids1.get(0)));
        assertThat("при добавлении нескольких одинаковых карточек, существующих в базе,"
                        + "должны возвращаться id существующей карточки",
                idsToCheck.get(1), equalTo(ids1.get(0)));
    }

    @Test
    public void addVcards_OneNewItemAndOneMatchingToExistingInDatabase_ReturnsExistingId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard11 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId).withApart("999");

        List<AddedModelId> ids1 = vcardRepository.addVcards(shard, clientUid, clientId, singletonList(vcard1));
        checkState(ids1.size() == 1,
                "количество возвращенных id не соответствует количеству сохраняемых объектов");

        List<AddedModelId> idsToCheck = vcardRepository.addVcards(shard, clientUid, clientId, asList(vcard11, vcard2));
        checkState(idsToCheck.size() == 2,
                "количество возвращенных id не соответствует количеству сохраняемых объектов");

        assertThat("при добавлении карточки, существующей в базе, должен возвращаться id существующей карточки",
                idsToCheck.get(0), equalTo(ids1.get(0)));
        assertThat("при добавлении карточки, не существующей в базе, должен возвращаться новый id",
                idsToCheck.get(1), not(equalTo(ids1.get(0))));
    }

    // addVcards - проверка уникализации (когда карточка отсутствует в БД)

    // проверка, что уникализация работает в рамках одного UID
    @Test
    public void addVcards_OneNewItemDifferentByClientUid_ReturnsNewId() {
        CampaignInfo campaignInfo2 = campaignSteps.createActiveTextCampaign();
        int shard2 = campaignInfo2.getShard();
        ClientId clientId2 = campaignInfo2.getClientId();
        long clientUid2 = campaignInfo2.getUid();
        long campaignId2 = campaignInfo2.getCampaignId();

        checkState(shard2 == shard, "шарды обоих клиентов должны совпадать");

        Vcard vcard1 = fullVcard(clientUid, campaignId);
        List<AddedModelId> ids1 = vcardRepository.addVcards(shard, clientUid, clientId, singletonList(vcard1));
        checkState(ids1.size() == 1, "количество возвращенных id не соответствует количеству сохраняемых объектов");

        Vcard vcard2 = fullVcard(clientUid2, campaignId2);
        List<AddedModelId> ids2 = vcardRepository
                .addVcards(shard, clientUid2, clientId2, singletonList(vcard2));
        checkState(ids2.size() == 1, "количество возвращенных id не соответствует количеству сохраняемых объектов");

        assertThat("при добавлении карточки, совпадающей с карточкой другого пользователя, должна быть создана новая",
                ids1.get(0), not(ids2.get(0)));
    }

    // проверка, что уникализация работает в рамках одного CID
    @Test
    public void addVcards_OneNewItemDifferentByCampaignId_ReturnsNewId() {
        CampaignInfo campaignInfo2 = campaignSteps.createActiveTextCampaign();
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignInfo2.getCampaignId());
        checkDifferentItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneNewItemDifferentByHouse_ReturnsNewId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId).withHouse("98");
        Vcard vcard2 = fullVcard(clientUid, campaignId).withHouse("99");
        checkDifferentItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneNewItemDifferentByPhoneCountryCode_ReturnsNewId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard1.getPhone().withCountryCode("+91");
        vcard2.getPhone().withCountryCode("+92");
        checkDifferentItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneNewItemDifferentByPhoneExtension_ReturnsNewId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard1.getPhone().withExtension("998");
        vcard2.getPhone().withExtension("999");
        checkDifferentItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneNewItemDifferentByNullPhoneExtension_ReturnsNewId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard1.getPhone().withExtension("998");
        vcard2.getPhone().withExtension(null);
        checkDifferentItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneNewItemDifferentByOgrn_ReturnsNewId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard1.withOgrn("43848");
        vcard2.withOgrn("43849");
        checkDifferentItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_OneNewItemDifferentByNullOgrn_ReturnsNewId() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId);
        vcard1.withOgrn("43849");
        vcard2.withOgrn(null);
        checkDifferentItemId(vcard1, vcard2);
    }

    @Test
    public void addVcards_TwoNewEqualItems_ReturnsEqualIds() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId);

        List<AddedModelId> ids = vcardRepository.addVcards(shard, clientUid, clientId, asList(vcard1, vcard2));
        checkState(ids.size() == 2,
                "количество возвращенных id не соответствует количеству сохраняемых объектов");

        assertThat("при добавлении нескольких новых одинаковых карточек их id должны быть равны",
                ids, equalTo(nCopies(2, ids.get(0))));
    }


    private void checkMatchingItemId(Vcard vcard1, Vcard vcard2) {
        List<AddedModelId> ids1 = vcardRepository.addVcards(shard, clientUid, clientId, singletonList(vcard1));
        checkState(ids1.size() == 1, "количество возвращенных id не соответствует количеству сохраняемых объектов");

        List<AddedModelId> ids2 = vcardRepository.addVcards(shard, clientUid, clientId, singletonList(vcard2));
        checkState(ids2.size() == 1, "количество возвращенных id не соответствует количеству сохраняемых объектов");

        assertThat("при добавлении карточки, существующей в базе, должен возвращаться id существующей",
                ids1.get(0), equalTo(ids2.get(0)));
    }

    private void checkDifferentItemId(Vcard vcard1, Vcard vcard2) {
        List<AddedModelId> ids1 = vcardRepository.addVcards(shard, clientUid, clientId, singletonList(vcard1));
        checkState(ids1.size() == 1, "количество возвращенных id не соответствует количеству сохраняемых объектов");

        List<AddedModelId> ids2 = vcardRepository.addVcards(shard, clientUid, clientId, singletonList(vcard2));
        checkState(ids2.size() == 1, "количество возвращенных id не соответствует количеству сохраняемых объектов");

        assertThat("при добавлении карточки, отличающейся от тех, которые есть в базе, должен возвращаться новый id",
                ids1.get(0), not(ids2.get(0)));
    }
}
