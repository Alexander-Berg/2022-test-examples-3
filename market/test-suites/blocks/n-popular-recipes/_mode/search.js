import {makeSuite, makeCase, prepareSuite, mergeSuites} from 'ginny';

import PopularRecipesSuite from '@self/platform/spec/hermione/test-suites/blocks/n-popular-recipes';
/**
 * Тест на блок n-popular-recipes.
 * @param {PageObject.PopularRecipes} popularRecipes
 */
export default makeSuite('"Часто ищут".', {
    feature: 'SEO',
    story: mergeSuites(
        prepareSuite(PopularRecipesSuite, {
            params: {
                title: 'Часто ищут',
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
                                    links
                                        .filter(link =>
                                            !link.pathname.match(/promo\/(vendor|category)_promo(codes)?_for_\d+/)
                                        )
                                        .map(link => this.expect(link).to.be.link({
                                            pathname: '/catalog--.*/\\d+/list',
                                            query: {
                                                'glfilter': '.+',
                                            },
                                        }, {
                                            mode: 'match',
                                            skipProtocol: true,
                                            skipHostname: true,
                                        })))
                                    .then(() => Promise.all(
                                        links.map(({query}) => this.expect(query['show-reviews'])
                                            .to.be.equal(undefined, 'url не содержит параметр show-reviews=1')
                                        )
                                    )))
                            );
                    },
                }),
            },
        }
    ),
});
