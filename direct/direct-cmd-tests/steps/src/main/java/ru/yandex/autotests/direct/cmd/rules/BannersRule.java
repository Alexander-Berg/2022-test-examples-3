package ru.yandex.autotests.direct.cmd.rules;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.cmd.util.NullAwareBeanUtilsBean;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

public abstract class BannersRule extends CampaignRule {

    protected Long bannerId;
    protected Long groupId;
    private String groupTemplate;
    private Group group;
    private boolean convertCurrency = false;
    protected CampaignRule campaignRule;

    protected BannersRule(String groupTemplate) {
        this.groupTemplate = groupTemplate;
    }

    public <T extends BannersRule> T withGroupTemplate(String groupTemplate) {
        this.groupTemplate = groupTemplate;
        return (T) this;
    }

    public <T extends BannersRule> T convertCurrency(boolean convertCurrency) {
        this.convertCurrency = convertCurrency;
        return (T) this;
    }

    public <T extends BannersRule> T overrideBannerTemplate(Banner banner) {
        try {
            new NullAwareBeanUtilsBean().copyProperties(getBanner(), banner);
            return (T) this;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("ошибка при записи полей бина группы", e);
        }
    }

    public <T extends BannersRule> T overrideGroupTemplate(Group group) {
        try {
            new NullAwareBeanUtilsBean().copyProperties(getGroup(), group);
            return (T) this;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("ошибка при записи полей бина группы", e);
        }
    }

    public <T extends BannersRule> T overrideVCardTemplate(ContactInfo vcard) {
        try {
            ContactInfo existingVCard = getBanner().getContactInfo();
            if (existingVCard == null) {
                existingVCard = new ContactInfo();
                getBanner().withContactInfo(existingVCard);
            }
            new NullAwareBeanUtilsBean().copyProperties(existingVCard, vcard);
            getBanner().withHasVcard(1);
            return (T) this;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("ошибка при записи полей бина группы", e);
        }
    }

    public Banner getBanner() {
        return getGroup().getBanners().get(0);
    }


    public Group getGroup() {
        if (group == null) {
            group = BeanLoadHelper.loadCmdBean(groupTemplate, Group.class);
        }
        if (convertCurrency) {
            convertToUserCurrency(group);
        }
        return group;
    }

    public Long getBannerId() {
        return bannerId;
    }

    public Long getGroupId() {
        return groupId;
    }

    private void convertToUserCurrency(Group group) {
        MoneyCurrency mc = MoneyCurrency.get(User.get(ulogin).getCurrency());
        group.getPhrases().forEach(
                phrase ->
                        phrase.withPrice(convertPriceSafely(phrase.getPrice(), mc))
                                .withMinPrice(convertPriceSafely(Double.valueOf(phrase.getMinPrice()), mc)
                                        .toString())
                                .withPriceContext(convertPriceSafely(phrase.getPriceContext(), mc)));
    }

    private Double convertPriceSafely(Double price, MoneyCurrency userCurrency) {
        Double result = Money.valueOf(price, Currency.RUB).convert(userCurrency.getCurrency()).doubleValue();
        return Math.max(userCurrency.getMinPrice().doubleValue(), result);
    }

    @Override
    protected void start() {
        if (campaignRule == null) {
            super.start();
        } else {
            campaignId = campaignRule.campaignId;
        }
        createGroup();
        getCreatedIds();
    }

    @Override
    protected void finish() {
        if (campaignRule == null) {
            super.finish();
        }
    }

    public abstract void createGroup();

    public abstract void saveGroup(GroupsParameters request);

    public abstract Group getCurrentGroup();

    public <T extends BannersRule, V extends CampaignRule> T forExistingCampaign(V campaignRule) {
        this.campaignRule = campaignRule;
        this.overrideCampTemplate(campaignRule.saveCampRequest)
                .withMediaType(campaignRule.mediaType)
                .withUlogin(campaignRule.ulogin)
                .withCampStrategy(campaignRule.campStrategy)
                .withMinusWords(campaignRule.minusWords);
        return (T) this;
    }

    public void updateCurrentGroupWith(Group group) {
        Group currentGroup = getCurrentGroup();
        try {
            new NullAwareBeanUtilsBean().copyProperties(currentGroup, group);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("ошибка при записи полей бина группы", e);
        }
        getDirectCmdSteps().groupsSteps().prepareGroupForUpdate(currentGroup, mediaType);
        saveGroup(GroupsParameters.forCamp(ulogin, campaignId, currentGroup, "0"));
    }

    public void updateCurrentGroupBy(Function<Group, Group> modifier) {
        Group currentGroup = getCurrentGroup();
        currentGroup = modifier.apply(currentGroup);
        getDirectCmdSteps().groupsSteps().prepareGroupForUpdate(currentGroup, mediaType);
        saveGroup(GroupsParameters.forExistingCamp(ulogin, campaignId, currentGroup));
    }

    private void getCreatedIds() {
        ShowCampResponse showCamp = getDirectCmdSteps().campaignSteps().getShowCamp(ulogin, campaignId.toString());
        groupId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"))
                .getAdGroupId();
        bannerId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть баннер"))
                .getBid();

    }

    /**
     * Возвращает группу, которую можно передавать в saveAdGroups для обновления существующей группы объявлений
     */
    public Group getGroupForUpdate() {
        Group group = getCurrentGroup();
        getDirectCmdSteps().groupsSteps().prepareGroupForUpdate(group, getMediaType());

        return group;
    }
}
