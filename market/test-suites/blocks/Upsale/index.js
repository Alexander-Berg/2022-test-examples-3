/**
 * @expFlag touch_km_upsale_popup
 * @ticket MARKETFRONT-82563
 * @start
 */
import {makeSuite, makeCase} from '@yandex-market/ginny';
import UpsaleMixContent from '@self/root/src/widgets/content/UpsaleMixContent/__pageObject';
import COOKIE_CONSTANTS from '@self/root/src/constants/cookie';
import StickyOffer from '@self/platform/widgets/parts/StickyOffer/__pageObject';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Попап апсейла', {
    defaultParams: {
        cookie: {
            [COOKIE_CONSTANTS.EXP_FLAGS]: {
                name: COOKIE_CONSTANTS.EXP_FLAGS,
                value: 'touch_km_upsale_popup',
            },
            [COOKIE_CONSTANTS.FORCE_AT_EXP]: {
                name: COOKIE_CONSTANTS.FORCE_AT_EXP,
                value: 'true',
            },
        },
    },
    story: {
        'По-умолчанию попап отображается': {
            'При добавлении товара из ДО': makeCase({
                id: 'MARKETFRONT-85112',
                async test() {
                    await this.browser.allure.runStep(
                        'Дожидаемся загрузки блока с описанием товара',
                        () => this.defaultOffer.waitForVisible()
                    );

                    await this.browser.yaRemoveElement(StickyOffer.root);

                    await this.browser.allure.runStep(
                        'Добавляем товар в корзину',
                        () => this.defaultOffer.cartButton.click()
                    );

                    await this.browser.allure.runStep(
                        'Проверяем что открылся попап корзины',
                        async () => {
                            const isCartPopupOpen = await this.cartPopup.isOpen();

                            await this.browser.expect(isCartPopupOpen).to.be.equal(true);
                        }
                    );

                    await this.browser.allure.runStep('Ждем появления апсейла', () =>
                        this.browser.waitForVisible(UpsaleMixContent.root, 5000)
                    );
                },
            }),
        },
    },
});
/**
 * @expFlag touch_km_upsale_popup
 * @end
 */
