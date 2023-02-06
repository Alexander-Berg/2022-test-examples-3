import {
    makeSuite,
    makeCase,
} from 'ginny';

/**
 * Тесты на страницу успешного офорления заказа, общие для пред- и постоплатных,
 * @param {PageObject.OrderConfirmation} orderConfirmation
 */
// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Заказ успешно оформлен.', {
    environment: 'testing',
    feature: 'Спасибо за заказ',
    story: {
        'Шапка и заголовок': {
            'по умолчанию': {
                'содержит правильный состав': makeCase({
                    id: 'bluemarket-506',
                    issue: 'BLUEMARKET-459',
                    async test() {
                        await this.expect(await this.orderConfirmationSubpageHeader.closeLink.isVisible())
                            .to.equal(true, 'крестик для закрытия должен быть отображен');
                    },
                }),
            },
        },

        'Cсылка "Отследить заказ"': {
            'по умолчанию': {
                'отображена и при клике открывает страницу с информацией о заказе': makeCase({
                    id: 'bluemarket-515',
                    issue: 'BLUEMARKET-460',
                    test() {
                        const {browser} = this;

                        return this.orderConfirmation
                            .isVisible(this.orderConfirmation.trackLink)
                            .should.eventually.to.be.equal(true, 'Cсылка "Отследить заказ" должна быть отображена')
                            .then(() => (
                                browser
                                    .yaWaitForChangeUrl(() => this.orderConfirmation.trackClick())
                                    .should.eventually.to.be.link({
                                        pathname: /^\/my\/order\/\d+\/track$/,
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                        skipQuery: true,
                                    })
                            ));
                    },
                }),
            },
        },
    },
});
