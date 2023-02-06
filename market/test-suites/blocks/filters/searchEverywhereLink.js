import {makeSuite, makeCase} from '@yandex-market/ginny';

import {
    reportState,
} from '@self/platform/spec/hermione2/test-suites/blocks/filters/fixtures/productWithOffers';

import {routes} from '@self/platform/spec/hermione/configs/routes';

const isSearchLinkExistingCheck = async ({searchHeader, browser, isExisting}) => {
    const isSearchLinkExisting = await searchHeader.isSearchLinkExisting();
    return browser.expect(isSearchLinkExisting)
        .to.be.equal(
            isExisting
        );
};

export default makeSuite('Ссылка "Искать везде" и плитки категорий', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-64247',
    story: {
        async beforeEach() {
            await this.browser.setState('report', reportState);
            await this.browser.yaOpenPage('touch:list', {...routes.catalog.phones, was_redir: 1, text: this.params.text});
        },
        'По поисковому запросу "Мобильные телефоны"': {
            'На категорийной выдаче': {
                'ссылка "Искать везде" отображается': makeCase({
                    id: 'm-touch-3820',
                    async test() {
                        await this.browser.yaWaitForPageReady();
                        return isSearchLinkExistingCheck({
                            searchHeader: this.searchHeader,
                            browser: this.browser,
                            isExisting: true,
                        });
                    },
                }),
                'При переходе по ссылке "Искать везде"': {
                    'осуществился переход на поисковую выдачу /search': makeCase({
                        id: 'm-touch-3820',
                        async test() {
                            await this.browser.yaWaitForPageReady();
                            await this.searchHeader.clickSearchLink();

                            const url = await this.browser.getUrl();

                            return this.browser.expect(url.includes('/search'))
                                .to.be.equal(
                                    true
                                );
                        },
                    }),
                    'в поисковой строке присутствует поисковой запрос': makeCase({
                        id: 'm-touch-3820',
                        async test() {
                            await this.browser.yaWaitForPageReady();
                            await this.searchHeader.clickSearchLink();

                            const url = await this.browser.getUrl();

                            return this.browser.expect(url.includes(`text=${encodeURI(this.params.text)}`))
                                .to.be.equal(
                                    true
                                );
                        },
                    }),
                    'отображаются уточняющие категории': makeCase({
                        id: 'm-touch-3820',
                        async test() {
                            await this.browser.yaWaitForPageReady();
                            await this.searchHeader.clickSearchLink();

                            const isClarifyingCategoryVisible = await this.сlarifyingCategories.getItem().isVisible();

                            return this.browser.expect(isClarifyingCategoryVisible)
                                .to.be.equal(
                                    true
                                );
                        },
                    }),
                },
                'При клике на плитку уточняющей категории (в виде баблов)': {
                    'ссылка "Искать везде" отсутствует': makeCase({
                        id: 'm-touch-3821',
                        async test() {
                            await this.browser.yaWaitForPageReady();
                            await this.сlarifyingCategories.getItem().click();
                            return isSearchLinkExistingCheck({
                                searchHeader: this.searchHeader,
                                browser: this.browser,
                                isExisting: false,
                            });
                        },
                    }),
                    'произошел переход в соответствующую категорию': makeCase({
                        id: 'm-touch-3821',
                        async test() {
                            await this.browser.yaWaitForPageReady();
                            await this.сlarifyingCategories.getItem().click();

                            const url = await this.browser.getUrl();
                            const isUrlValid = url.includes(`/catalog--${this.params.slug}/${this.params.nid}/list`);

                            return this.browser.expect(isUrlValid)
                                .to.be.equal(
                                    true
                                );
                        },
                    }),
                },
            },
            'На поисковой выдаче': {
                'при клике на плитку уточняющей категории': {
                    'ссылка "Искать везде" отсутствует': makeCase({
                        id: 'm-touch-3820',
                        async test() {
                            await this.browser.yaOpenPage('touch:search', {text: this.params.text});
                            await this.browser.yaWaitForPageReady();
                            await this.сlarifyingCategories.getItem().click();
                            return isSearchLinkExistingCheck({
                                searchHeader: this.searchHeader,
                                browser: this.browser,
                                isExisting: false,
                            });
                        },
                    }),
                    'произошел переход в соответствующую категорию': makeCase({
                        id: 'm-touch-3820',
                        async test() {
                            await this.browser.yaOpenPage('touch:search', {text: this.params.text});
                            await this.browser.yaWaitForPageReady();
                            await this.сlarifyingCategories.getItem().click();
                            const url = await this.browser.getUrl();
                            const isUrlValid = url.includes(`/catalog--${this.params.slug}/${this.params.nid}/list`);
                            return this.browser.expect(isUrlValid)
                                .to.be.equal(
                                    true
                                );
                        },
                    }),
                },
            },
        },
    },
});
