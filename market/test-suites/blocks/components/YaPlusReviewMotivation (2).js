import {makeSuite, makeCase} from 'ginny';

/**
 * @property {PageObjects.YaPlusReviewMotivation} yaPlusReviewMotivation
 */
export default makeSuite('Бейдж с баллами Яндекс.Плюса за отзыв.', {
    params: {
        paymentOfferAmount: 'Сколько баллов готовы дать за отзыв',
    },
    story: {
        'По умолчанию': {
            'должен отображаться с количеством баллов за отзыв.': makeCase({
                async test() {
                    await this.expect(this.yaPlusReviewMotivation.isVisible())
                        .to.be.equal(true, 'Бейдж должен быть виден');
                    return this.expect(this.yaPlusReviewMotivation.getText())
                        .to.be.equal(
                            `${this.params.paymentOfferAmount} баллов за отзыв`,
                            'Должно быть правильное количество баллов за отзыв'
                        );
                },
            }),
            'содержит ссылку на правила Плюса.': makeCase({
                async test() {
                    const actualUrl = this.yaPlusReviewMotivation.getLinkHref();

                    return this.expect(actualUrl, 'Ссылка ведет на страницу с правилами')
                        .to.be.link({
                            hostname: 'yandex.ru',
                            pathname: '/legal/plus_loyalty_conditions',
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
        },
    },
});
