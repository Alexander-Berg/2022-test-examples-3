import {makeSuite, makeCase, prepareSuite, mergeSuites} from 'ginny';

import PopularRecipesSuite from '@self/platform/spec/hermione/test-suites/blocks/n-popular-recipes';
/**
 * Тест на блок n-popular-recipes.
 * @param {PageObject.PopularRecipes} popularRecipes
 */
export default makeSuite('Популярные рецепты для лучших отзывов.', {
    feature: 'SEO',
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(PopularRecipesSuite, {
            params: {
                title: 'Почитайте отзывы покупателей на товары',
            },
        }),

        {
            'По умолчанию': {
                'содержит корректные ссылки': makeCase({
                    id: 'marketfront-1681',
                    issue: 'MARKETVERSTKA-28997',
                    test() {
                        return this.popularRecipes.getItemsUrls()
                            .then(links => this.browser.allure.runStep(
                                'Проверяем ссылки рецептов',
                                () => Promise.all(
                                    links.map(link => this.expect(link).to.be.link({
                                        pathname: '/catalog--.*/\\d+/list',
                                        query: {
                                            'show-reviews': '1',
                                            'onstock': '0',
                                        },
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }))))
                            );
                    },
                }),
            },
        }
    ),
});
