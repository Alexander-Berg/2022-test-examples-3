import {makeCase, makeSuite} from 'ginny';
import COOKIE_CONSTANTS from '@self/root/src/constants/cookie';

/**
 * Тест на блок DealsBadge
 *
 * @param {PageObject.DealsBadge | PageObject.DealsBadgeReact} dealsBadge
 */
export default makeSuite('Бейдж акции.', {
    feature: 'Бейдж акции.',
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
                        await this.dealsBadge.scrollToBadge();

                        const dealsBadgeText = await this.dealsBadge.root.getText();

                        return this.expect(dealsBadgeText).to.equal(
                            this.params.expectedText,
                            'Бейдж акции содержит ожидаемый текст'
                        );
                    });
                },
            }),
        },
    },
});
