import {makeCase} from 'ginny';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {openCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {fillAddressForm}
    from '@self/root/src/spec/hermione/scenarios/checkout/deliveryEditor';

import {samaraAddress} from './mocks';


const EXPECT_FIRST_AUTOFILL_MOSCOW =
    'Первое открытие страницы чекаута, регион Москва,' +
    'в предыдущих заказах есть адрес из пресетов,' +
    'в карточке адреса должен отображаться московский адрес';

const EXPECT_ADD_SAMARA = 'Добавление самарского адреса,' +
    'в карточке адреса должен отображаться самарский адрес';

const EXPECT_RELOAD_AUTOFILL_MOSCOW = 'Перезагрузка, регион Москва,' +
    'в карточке адреса должен отображаться московский адрес';

const EXPECT_CHANGE_MOSCOW_ON_SAMARA = 'Смена региона с Москвы на Самару,' +
    'в карточке адреса должен отображаться самарский адрес';

const EXPECT_CHANGE_SAMARA_ON_MOSCOW = 'Смена региона с Самары на Москву,' +
    'в карточке адреса должен отображаться московский адрес';

const addressCardSamaraText =
    'Самара, Ленинская улица, д. 147, 12' +
    '\n' +
    '1 подъезд, 15 этаж, домофон 12test, "Тестирование"';

const addressCardMoscowText = 'Москва, Усачева, д. 52';


module.exports = makeCase({
    issue: 'MARKETFRONT-50676',
    id: 'marketfront-4878',
    async test() {
        await this.addressCard.getText().should.eventually.to.be.equal(
            addressCardMoscowText,
            EXPECT_FIRST_AUTOFILL_MOSCOW
        );

        await this.editableCard.changeButtonClick();
        await this.deliveryTypeList.setDeliveryTypeDelivery();
        await this.editPopup.addButtonClick();

        await this.browser
            .yaScenario(this, fillAddressForm, samaraAddress);

        await this.addressCard.getText().should.eventually.to.be.equal(
            addressCardSamaraText,
            EXPECT_ADD_SAMARA
        );

        await this.browser.yaPageReload(5000, ['state']);
        await this.addressCard.getText().should.eventually.to.be.equal(
            addressCardMoscowText,
            EXPECT_RELOAD_AUTOFILL_MOSCOW
        );

        await this.browser.yaScenario(this, openCheckoutPage, {
            lr: region['Самара'],
        });
        await this.addressCard.getText().should.eventually.to.be.equal(
            addressCardSamaraText,
            EXPECT_CHANGE_MOSCOW_ON_SAMARA
        );

        await this.browser.yaScenario(this, openCheckoutPage, {
            lr: region['Москва'],
        });
        await this.addressCard.getText().should.eventually.to.be.equal(
            addressCardMoscowText,
            EXPECT_CHANGE_SAMARA_ON_MOSCOW
        );
    },
});
