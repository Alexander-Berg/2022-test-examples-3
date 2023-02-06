import {makeCase, makeSuite} from 'ginny';
import COOKIE_CONSTANTS from '@self/root/src/constants/cookie';

/**
 * Тест на блок DealsBadge
 *
 * @param {PageObject.DealsBadge} dealsBadge
 * @param {PageObject.DealsDescriptionPopup} dealsDescriptionPopup
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
        'По наведению': {
            'должен открывать попап описания акции': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверяем открытие попапа по наведению на бейдж', async () => {
                        await this.dealsBadge.scrollToBadge();
                        await this.dealsBadge.root.click();
                        const isPopupVisible = await this.dealsDescriptionPopup.isExisting();
                        return this.expect(isPopupVisible).to.be.equal(true, 'Попап описания акции отображается');
                    });
                },
            }),
        },
    },
});
