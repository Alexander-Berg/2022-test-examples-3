import {makeCase, makeSuite} from 'ginny';
import COOKIE_CONSTANTS from '@self/root/src/constants/cookie';

/**
 * Тест на блок n-deals-terms
 *
 * @param {PageObject.DealsTerms} dealsBadge
 * @param {string} params.expectedText - ожидаемый текст бейджа (без цены)
 * @param {string} params.expectedPriceText - ожидаемый текст в цене
 */
export default makeSuite('Бейдж акции', {
    feature: 'Бейдж акции.',
    params: {
        expectedText: 'ожидаемый текст бейджа (без цены)',
        expectedPriceText: 'ожидаемый текст в цене',
    },
    defaultParams: {
        /**
         * @expFlag all_auto_apply_promocode_rev
         * @ticket MARKETFRONT-60043
         * @start
         */
        cookie: {
            [COOKIE_CONSTANTS.EXP_FLAGS]: {
                name: COOKIE_CONSTANTS.EXP_FLAGS,
                value: 'all_auto_apply_promocode_rev',
            },
        },
        /**
         * @expFlag all_auto_apply_promocode_rev
         * @ticket MARKETFRONT-60043
         * @end
         */
    },
    story: {
        'По умолчанию': {
            'должен содержать ожидаемый текст': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверяем текст бейджа', async () => {
                        await this.dealsBadge.info.getText().should.eventually.equal(this.params.expectedText);
                        if (!this.params.expectedPriceText) {
                            return null;
                        }
                        return this.dealsBadge.price.getText()
                            .should.eventually.equal(this.params.expectedPriceText);
                    });
                },
            }),
        },
    },
});
