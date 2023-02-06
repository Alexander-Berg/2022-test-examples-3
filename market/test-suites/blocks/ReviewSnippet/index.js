import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок ReviewSnippet
 *
 * @param {PageObject.ReviewSnippet} reviewSnippet
 */
export default makeSuite('Сниппет продукта с отзывом.', {
    feature: 'редирект',
    params: {
        id: 'ID продукта, который отображен в сниппете',
        slug: 'Slug продукта',
    },
    defaultParams: {
        slug: '.*',
    },
    story: {
        'Ссылка "Читать все отзывы".': {
            'При клике': {
                'должна открыться вкладка "Отзывы" страницы товара': makeCase({
                    id: 'marketfront-1590',
                    issue: 'MARKETVERSTKA-26226',
                    test() {
                        return this.reviewSnippet.getShowAllUrl()
                            .should.eventually.be
                            .link({
                                pathname: `product--${this.params.slug}/${this.params.id}/reviews`,
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
    },
});
