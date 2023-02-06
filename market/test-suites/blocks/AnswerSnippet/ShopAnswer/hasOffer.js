import {makeCase, makeSuite} from 'ginny';

/**
 * Проверка блока автора ответа, если ответ от магазина с оффером
 *
 * @param {PageObject.AnswerOffer} answerOffer
 * @param {string} params.expectedPrice
 * @param {string} params.expectedOfferUrl
 */
export default makeSuite('Блок снипета ответа c ответом от магазина. Блок CPC оффера.', {
    story: {
        'Цена': {
            'должна отображаться': makeCase({
                id: 'm-touch-2834',
                issue: 'MOBMARKET-12444',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.answerOffer.getPriceText()).to.be.equal(this.params.expectedPrice);
                },
            }),
        },
        'По умолчанию': {
            'оффер отображается': makeCase({
                id: 'm-touch-2832',
                issue: 'MOBMARKET-12445',
                feature: 'Оффер',
                test() {
                    return this.answerOffer.isVisible()
                        .should.eventually.be.equal(true, 'Оффер отображается');
                },
            }),

            'ссылка в магазин корректная': makeCase({
                id: 'm-touch-2835',
                issue: 'MOBMARKET-12445',
                feature: 'Оффер',
                async test() {
                    const expectedUrl = `https:${await this.browser.yaBuildURL('external:clickdaemon', {
                        url: this.params.expectedOfferUrl,
                        tld: 'ru',
                    })}`;
                    await this.answerOffer.isShopLinkVisible();
                    const offerUrl = await this.answerOffer.getShopLink();

                    await this.expect(offerUrl, 'Ссылка из кнопки "В магазин" корректная')
                        .to.be.link(expectedUrl, {
                            skipHostname: true,
                            skipProtocol: true,
                        });
                },
            }),
        },
    },
});
