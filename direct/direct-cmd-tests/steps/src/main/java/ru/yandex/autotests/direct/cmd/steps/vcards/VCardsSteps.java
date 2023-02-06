package ru.yandex.autotests.direct.cmd.steps.vcards;

import java.util.Collections;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.vcards.AssignVCardRequest;
import ru.yandex.autotests.direct.cmd.data.vcards.EditVCardRequest;
import ru.yandex.autotests.direct.cmd.data.vcards.EditVCardResponse;
import ru.yandex.autotests.direct.cmd.data.vcards.SaveVCardRequest;
import ru.yandex.autotests.direct.cmd.data.vcards.SaveVCardResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class VCardsSteps extends DirectBackEndSteps {

    @Step("Модификация визитки (login = {0}, cid = {1}, bid = {2}, vcard_id = {3}")
    public SaveVCardResponse saveVCard(String login, long campaignId, long bid,
                                       long vCardId, ContactInfo contactInfo) {
        return postSaveVCard(SaveVCardRequest.fromContactInfo(contactInfo).
                withCampaignId(campaignId).
                withBannerIds(Collections.singletonList(bid)).
                withVCardId(vCardId).
                withUlogin(login));
    }

    @Step("POST cmd=saveVCard (модификация визитки)")
    public SaveVCardResponse postSaveVCard(SaveVCardRequest saveVCardRequest) {
        return post(CMD.SAVE_VCARD, saveVCardRequest, SaveVCardResponse.class);
    }

    @Step("Получение визитки (cid = {0}, bid = {1}, vcard_id = {2}")
    public ContactInfo getVCard(String login, long campaignId, long bannerId, long vCardId) {
        EditVCardResponse response = getEditVCard(new EditVCardRequest().
                withCampaignId(campaignId).
                withBannerId(bannerId).
                withVCardId(vCardId).
                withUlogin(login));
        return response.getVCard();
    }

    @Step("GET cmd=editVCard (получение визитки)")
    public EditVCardResponse getEditVCard(EditVCardRequest editVCardRequest) {
        return get(CMD.EDIT_VCARD, editVCardRequest, EditVCardResponse.class);
    }

    @Step("POST cmd=assignVCard (привязка визитки к баннерам)")
    public ErrorResponse assignVCard(AssignVCardRequest request) {
        return post(CMD.ASSIGN_VCARD, request, ErrorResponse.class);
    }

    @Step("привязка визитки {0} к баннерам {1} пользователю {2})")
    public ErrorResponse assignVCard(String vcardId, String bids, String uLogin) {
        return assignVCard(new AssignVCardRequest().withVcardId(vcardId).withBids(bids).withUlogin(uLogin));
    }
}
