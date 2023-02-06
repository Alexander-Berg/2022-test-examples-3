import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на компонент ReviewSnippet
 * @param {PageObject.ReviewSnippet} reviewSnippet
 */
export default makeSuite('Карточка с отзывом.', {
    feature: 'Новая морда',
    story: {
        'По умолчанию': {
            'отображается на странице': makeCase({
                id: 'm-touch-2381',
                issue: 'MOBMARKET-9755',
                test() {
                    return this.browser.allure.runStep(
                        'Смотрим, что блок c отзывом присутствует на странице',
                        () => this.reviewSnippet
                            .isExisting()
                            .should.eventually.to.be.equal(true, 'Блок с отзывом отображается')
                    );
                },
            }),
            'ведет на нужную страницу.': makeCase({
                id: 'm-touch-2189',
                issue: 'MOBMARKET-8726',
                async test() {
                    const actualUrl = await this.reviewSnippet.fetchLinkHref();
                    const expectedPath = await this.browser.yaBuildURL('touch:product-reviews', {
                        productId: '\\d+',
                        slug: '[\\w-]+',
                    });
                    const expected = {
                        pathname: `^${expectedPath}$`,
                        query: {
                            firstReviewId: '^\\d+$',
                        },
                    };
                    return this.browser.allure.runStep(
                        'Проверяем, что URL соответствует шаблону',
                        () => this.expect(actualUrl).to.be.link(expected, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),
        },
    },
});
