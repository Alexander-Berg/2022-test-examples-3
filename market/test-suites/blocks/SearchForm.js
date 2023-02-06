import {prepareSuite, mergeSuites, makeSuite, makeCase} from 'ginny';
import suggestConfig from '@self/platform/spec/hermione/configs/suggest';

import SuggestPopup from '@self/platform/spec/page-objects/components/SuggestPopup';

import SuggestPopupSuite from '@self/platform/spec/hermione/test-suites/blocks/SuggestPopup';

/**
 * Тесты на блок SearchForm
 * @param {PageObject.SearchForm} search
 */
export default makeSuite('Поисковая строка.', {
    feature: 'Поисковая строка',
    story: mergeSuites(
        {
            before() {
                this.setPageObjects({
                    miniSuggestPopup: () => this.createPageObject(SuggestPopup),
                });
            },

            'По-умолчанию': {
                'должна присутствовать на странице': makeCase({
                    id: 'm-touch-1821',
                    issue: 'MOBMARKET-6839',
                    test() {
                        return this.search
                            .isExist()
                            .should.eventually.to.be.equal(true, 'Поисковая строка отобразилась');
                    },
                }),
            },

            'При клике в инпут поисковой строки': {
                'должен открываться блок саджестов': makeCase({
                    id: 'm-touch-1324',
                    issue: 'MOBMARKET-5674',
                    async test() {
                        await this.search.clickSearch();
                        await this.search.suggestInputSetValue(suggestConfig.product.searchText);
                        return this.miniSuggestPopup.waitForVisibleInViewPort()
                            .should.eventually.to.be.equal(true, 'Саджесты отобразились');
                    },
                }),
            },

            'При вводе "<название магазина> отзывы"': {
                async beforeEach() {
                    const state = {
                        redirect: {
                            url: await this.browser
                                .yaBuildURL('touch:shop-reviews', {
                                    shopId: suggestConfig.shop.id,
                                    slug: suggestConfig.shop.slug,
                                }),
                        },
                    };
                    await this.browser.setState('report', state);
                },
                'должна открываться страница отзывов на магазин': makeCase({
                    id: 'm-touch-1328',
                    issue: 'MOBMARKET-5678',
                    environment: 'kadavr',
                    async test() {
                        await this.search.clickSearch();
                        await this.search.suggestInputSetValue(suggestConfig.shop.searchText);

                        const changedUrl = await this.browser
                            .yaWaitForChangeUrl(() => this.search.submitForm(), 10000);

                        const expectedUrl = await this.browser
                            .yaBuildURL('touch:shop-reviews', {
                                shopId: suggestConfig.shop.id,
                                slug: suggestConfig.shop.slug,
                            });

                        return this.browser.allure.runStep(
                            'Проверяем, что перешли на страницу магазина',
                            () => this.expect(changedUrl, 'Перешли на страницу магазина')
                                .to.be.link(expectedUrl, {
                                    skipProtocol: true,
                                    skipHostname: true,
                                    skipQuery: true,
                                })
                        );
                    },
                }),
            },

            'При клике': {
                'на кнопку очистки, инпут должен стать пустым': makeCase({
                    id: 'm-touch-2936',
                    issue: 'MOBMARKET-12952',
                    async test() {
                        await this.search.clickSearch();
                        await this.search.suggestInputSetValue('text');
                        await this.search.clickClearButton();

                        return this.browser.allure.runStep(
                            'Проверяем, что инпут пустой',
                            () => this.expect(this.search.suggestInputGetValue())
                                .to.be.equal('', 'Инпут действительно пустой')
                        );
                    },
                }),
            },

            'При клике на кнопку "Найти"': {
                'должен происходить переход на соответствующую категоричную выдачу': makeCase({
                    id: 'm-touch-3244',
                    issue: 'MARKETFRONT-7987',
                    async test() {
                        const SEARCH_TEXT = 'смартфоны';

                        await this.search.clickSearch();
                        await this.search.suggestInputSetValue(SEARCH_TEXT);

                        const changedUrl = await this.browser
                            .yaWaitForChangeUrl(() => this.search.clickSubmitButton(), 10000);

                        const expectedUrl = await this.browser
                            .yaBuildURL('touch:list', {
                                text: SEARCH_TEXT,
                                ...this.params.routeParams,
                            });

                        return this.browser.allure.runStep(
                            'Проверяем, что перешли на страницу категоричной выдачи',
                            () => this.expect(changedUrl, 'Перешли на соответствующую категоричную выдачу')
                                .to.be.link(expectedUrl, {
                                    skipProtocol: true,
                                    skipHostname: true,
                                })
                        );
                    },
                }),
            },
        },
        prepareSuite(SuggestPopupSuite)
    ),
});
