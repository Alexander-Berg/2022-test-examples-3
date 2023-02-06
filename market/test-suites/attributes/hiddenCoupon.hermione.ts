import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import {TabsWrapper} from '../../../src/controls/TabsControl/__pageObject__';
import {Coupon} from '../../../src/components/jmf/HiddenAttribute/__pageObject__';
import {HIDDEN_VALUE} from '../../../src/components/jmf/HiddenAttribute/constants';

/**
 * Ссылка на заказ, в котором есть купон на <= 300 руб.
 * Если ссылка протухнет, то надо найти любой заказ, создать в нем купон и обновить ссылку.
 */
const PAGE_URL = '/entity/order@2106T32608189';

/**
 * План теста:
 * 1. Найти блок вкладок и в нем вкладку "Бонусы"
 * 2. Клацнуть по вкладке "Бонусы"
 * 3. Дождаться загрузки данных (п. 4 станет видимым)
 * 4. Найти блок "Выданные купоны" [data-ow-test-content="table-loyaltyCoupon-forList"]
 * 5. Считать первый атрибут с '[data-ow-test-hidden="true"]'
 * 5. Значение должно соответствовать ${HIDDEN_VALUE} и быть кликабельным
 * 6. Клацнуть по атрибуту - появится спиннер
 * 7. Дождаться когда загрузится контент, button заменится span'ом
 * 8. Сверить значение внутри span, оно должно содержать буквы-цифры длиной >= 8 зн. и не должно содержать ${HIDDEN_VALUE}.
 */
describe(`ocrm-1220: Проверка скрытого кода купона на кликабельность и загрузку значения.`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`Код подгружается, имеет длину >= 8 зн. и не содержит ${HIDDEN_VALUE}`, async function() {
        const tabsWrapper = new TabsWrapper(this.browser, 'body', '[data-ow-test-attribute-container="tabsWrapper"]');
        const coupon = new Coupon(
            this.browser,
            '[data-ow-test-content="table-loyaltyCoupon-forList"]',
            '[data-ow-test-hidden="true"]'
        );

        await tabsWrapper.isDisplayed();
        await tabsWrapper.clickTab('Бонусы');
        await coupon.isDisplayed();

        const buttonContents = await (await coupon.button).getText();
        const buttonEnabled = await (await coupon.button).isEnabled();

        expect(buttonEnabled).to.equal(true, 'Контрол оказался не кликабельным.');
        expect(buttonContents).to.equal(HIDDEN_VALUE, 'Начальное значение контрола отличается от заданного.');

        await (await coupon.button).click();
        await coupon.isContentsDisplayed();

        const contents = await (await coupon.contents).getText();

        expect(contents).to.have.lengthOf.above(7, 'Длина значения кода оказалась меньше 8 зн.');
        expect(contents).not.to.equal(HIDDEN_VALUE, 'Значение после клика не должно соответствовать начальному.');
    });
});
