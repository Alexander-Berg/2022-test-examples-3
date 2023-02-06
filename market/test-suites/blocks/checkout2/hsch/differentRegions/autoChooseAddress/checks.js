import {waitPreloader} from '@self/root/src/spec/hermione/scenarios/checkout';

export const addressCardMoscowText = 'Москва, Усачёва, д. 52';
export const addressCardSamaraText =
    'Самара, Ленинская улица, д. 147, 12' +
    '\n' +
    '1 подъезд, 15 этаж, домофон 12test, "Тестирование"';

const EXPECT_MOSCOW_ADDRESS =
    'В карточке адреса должен отображаться самарский адрес';
const EXPECT_SAMARA_ADDRESS =
    'В карточке адреса должен отображаться московский адрес';

export const addressCardMoscowCheck = {
    name: 'Проверяем, что на карточке московский адрес',
    async func() {
        return this.addressCard.getText().should.eventually.to.be.equal(
            addressCardMoscowText,
            EXPECT_MOSCOW_ADDRESS
        );
    },
};

export const addressCardSamaraCheck = {
    name: 'Проверяем, что на карточке самарский адрес',
    async func() {
        return this.addressCard.getText().should.eventually.to.be.equal(
            addressCardSamaraText,
            EXPECT_SAMARA_ADDRESS
        );
    },
};

export const addressFlow = {
    name: 'Выбираем адрес, проверяем, что подставился, обновляем сртаницу, проверяем, что подставился',
    async func({addressText, addressCheck}) {
        await this.popupDeliveryTypeList.setDeliveryTypeDelivery();
        await this.addressList
            .clickAddressListItemByAddress(addressText);

        await this.editPopup.waitForChooseButtonEnabled();
        await this.editPopup.chooseButtonClick();

        await this.browser.yaScenario(this, waitPreloader);
        await this.browser.yaScenario(this, addressCheck);

        await this.browser.yaPageReload(10000, []);
        await this.browser.yaScenario(this, waitPreloader);
        await this.browser.yaScenario(this, addressCheck);
    },
};
