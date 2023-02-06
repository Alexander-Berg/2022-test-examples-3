import _ from 'lodash';
import {makeSuite, makeCase, mergeSuites, prepareSuite} from 'ginny';

import ISliderSuite from '@self/platform/spec/hermione/test-suites/blocks/i-slider';
import ISliderArrowSuite from '@self/platform/spec/hermione/test-suites/blocks/i-slider/__arrow';
import Slider from '@self/platform/spec/page-objects/i-slider';

/**
 * Тест на блок n-recipes-hubs.
 * @param {PageObject.RecipesHubs} recipesHubs
 */
export default makeSuite('Блок с рецептами Лучших отзывов.', {
    environment: 'kadavr',
    params: {
        itemsCount: 'Количество пупырей в блоке',
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    slider: () => this.createPageObject(Slider, {
                        root: this.recipesHubs,
                    }),
                });
            },
        },

        {
            'По умолчанию.': {
                'Виден на странице': makeCase({
                    id: 'marketfront-2457',
                    issue: 'MARKETVERSTKA-28502',
                    test() {
                        return this.browser.allure.runStep('Смотрим, блок с пупырями отображается', () =>
                            this.recipesHubs.isVisible()
                                .should.eventually.be.equal(true, 'блок отображается на странице')
                        );
                    },
                }),

                'Содержит заданное количество элементов': makeCase({
                    id: 'marketfront-2458',
                    issue: 'MARKETVERSTKA-28502',
                    test() {
                        const expectedCnt = this.params.itemsCount;

                        return this.recipesHubs.getItemsCount()
                            .should.eventually.be.equal(expectedCnt, 'Количество элементов корректное');
                    },
                }),

                'Каждый элемент это ссылка на хаб отзывов': makeCase({
                    id: 'marketfront-2459',
                    issue: 'MARKETVERSTKA-28502',
                    test() {
                        return this.recipesHubs.getItemsUrls()
                            .then(links => this.browser.allure.runStep(
                                'Проверяем ссылки пупырей',
                                () => Promise.all(links.map(
                                    link => this.expect(link).to.be.link({
                                        pathname: '/catalog--.*/\\d+/list',
                                        query: {
                                            'show-reviews': '1',
                                            'onstock': '0',
                                        },
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }))
                                ))
                            );
                    },
                }),
            },
        },

        prepareSuite(ISliderSuite, {
            hooks: {
                beforeEach() {
                    return this.slider.getActiveItemsCount()
                        .then(size => _.assign(this.params, {size}));
                },
            },
        }),
        prepareSuite(ISliderArrowSuite)
    ),
});
