import {makeSuite, makeCase} from '@yandex-market/ginny';

import RegionSelector from '@self/platform/spec/page-objects/widgets/content/RegionSelector';

const COOKIE_LR = 'lr';
const COOKIE_CURRENT_REGION = 'currentRegionId';
const LOCAL_STORAGE_HISTORY = 'regionHistory';

/**
 * Тесты на виджет RegionSelector.
 * @param {PageObject.RegionSelector} regionSelector
 */
export default makeSuite('Попап выбора региона', {
    issue: 'MOBMARKET-11116',
    story: {
        async beforeEach() {
            await this.browser.yaOpenPage('touch:blank');
            await this.browser.localStorage('DELETE', LOCAL_STORAGE_HISTORY);
            await this.browser.deleteCookie(COOKIE_CURRENT_REGION);
            await this.browser.deleteCookie(COOKIE_LR);

            const {baseUrl} = this.browser.options;
            const retPath = `${baseUrl}?lol=kek`;

            await this.browser.yaOpenPage('touch:my-region', {retPath});
        },

        'Кнопка автоматического определения региона.': {
            'По-умолчанию': {
                'корректно отображается': makeCase({
                    id: 'm-touch-2674',
                    async test() {
                        return this.browser.assertView('plain', RegionSelector.root);
                    },
                }),
            },
        },
    },
});
