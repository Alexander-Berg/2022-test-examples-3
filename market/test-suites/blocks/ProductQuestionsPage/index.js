import {makeCase, makeSuite} from 'ginny';
import NoQuestions from '@self/platform/spec/page-objects/components/Questions/NoQuestions';

/**
 * @param {PageObject.ProductQuestionsPage} productQuestionsPage
 */
export default makeSuite('Вопросы на товар', {
    params: {
        productId: 'Id продукта, с которым работаем',
        slug: 'Slug продукта',
    },
    story: {
        'Хлебная крошка': {
            'содержит ссылку на товар': makeCase({
                id: 'm-touch-2179',
                issue: 'MOBMARKET-8790',
                test() {
                    return Promise
                        .all([
                            this.browser.yaBuildURL('touch:product', {
                                productId: this.params.productId,
                                slug: this.params.slug,
                            }),
                            this.productQuestionPage.backLinkHref,
                        ])
                        .then(([expectedUrl, currentUrl]) =>
                            this.expect(currentUrl).to.be.link(
                                {pathname: expectedUrl},
                                {
                                    skipProtocol: true,
                                    skipHostname: true,
                                }
                            )
                        );
                },
            }),
        },
        'Блок "Пока никто не задавал вопросов"': {
            'если вопросы существуют': {
                beforeEach() {
                    return this.setPageObjects({
                        noQuestions: () => this.createPageObject(NoQuestions),
                    });
                },
                'не отображается': makeCase({
                    id: 'm-touch-2978',
                    issue: 'MARKETFRONT-3451',
                    test() {
                        return this.noQuestions.isVisible()
                            .should
                            .eventually
                            .to
                            .be
                            .equal(false, 'Блок "Пока никто не задавал вопросов" не отображается');
                    },
                }),
            },
        },
        'Блок "Как правильно задать вопрос?"': {
            'по умолчанию': {
                'содержит правильную ссылку': makeCase({
                    id: 'm-touch-2652',
                    issue: 'MARKETFRONT-3451',
                    test() {
                        return this.productQuestionPage.getHowToAskUrl()
                            .should
                            .eventually
                            .to
                            .be
                            .equal(
                                'https://yandex.ru/support/market/opinions/discussion.html#discussion__rules',
                                'Блок "Как правильно задать вопрос?" содержит правильную ссылку'
                            );
                    },
                }),
            },
        },
    },
});
