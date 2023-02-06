import {makeSuite, makeCase} from '@yandex-market/ginny';

const COOKIE_LR = 'lr';
const COOKIE_MANUALLY_CHANGED = 'manuallyChangedRegion';
const COOKIE_CURRENT_REGION = 'currentRegionId';
const LOCAL_STORAGE_HISTORY = 'regionHistory';

/**
 * Тесты на виджет RegionSelector.
 * @param {PageObject.RegionSelector} regionSelector
 */
export default makeSuite('Попап выбора региона', {
    environment: 'kadavr',
    issue: 'MOBMARKET-11116',
    story: {
        async beforeEach() {
            await this.browser.yaOpenPage('touch:blank');
            await this.browser.localStorage('DELETE', LOCAL_STORAGE_HISTORY);
            await this.browser.deleteCookie(COOKIE_CURRENT_REGION);
            await this.browser.deleteCookie(COOKIE_LR);
        },

        'Кнопка автоматического определения региона.': {
            'При клике': {
                'происходит редирект на retPath': makeCase({
                    id: 'm-touch-2674',
                    async test() {
                        const {baseUrl} = this.browser.options;
                        const retPath = `${baseUrl}?lol=kek`;

                        await this.browser.yaOpenPage('touch:my-region', {retPath});

                        await this.regionSelector.buttonAutoDetectClick();
                        await this.browser.yaWaitForPageReady();
                        await this.browser.getUrl()
                            .should.eventually.to.be.equal(
                                retPath,
                                'URL соответствует ожидаемому'
                            );
                    },
                }),

                'происходит редирект на морду без retPath': makeCase({
                    id: 'm-touch-2675',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.buttonAutoDetectClick();
                        await this.browser.yaWaitForPageReady();
                        await this.browser.getUrl()
                            .should.eventually.to.be.equal(
                                this.browser.options.baseUrl,
                                'URL соответствует ожидаемому'
                            );
                    },
                }),

                'сбрасывает настройки региона': makeCase({
                    id: 'm-touch-2676',
                    async test() {
                        await this.browser.yaOpenPage('touch:my-region');
                        await this.regionSelector.buttonAutoDetectClick();
                        await this.browser.yaWaitForPageReady();

                        await this.browser.getCookie(COOKIE_LR)
                            .should.eventually.to.be.equal(
                                null,
                                'Кука региона отсутствует'
                            );

                        await this.browser.getCookie(COOKIE_MANUALLY_CHANGED)
                            .should.eventually.to.be.equal(
                                null,
                                'Кука признака ручного выставления региона отсутствует'
                            );
                    },
                }),
            },
        },
    },
});
