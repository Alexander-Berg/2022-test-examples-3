import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на появление попапа подписки на снижение цены
 * @property {PageObject.ProductSubscriptionPopup} this.productSubscriptionPopup попап подписки на снижение цены
 * @property {PageObject.Button2}  this.openerButton кнопка "Следить за снижением цены"
 */

export default makeSuite('Попап подписки на снижение цены.', {
    feature: 'Подписка на снижение цены',
    environment: 'kadavr',
    story: {
        'По клику на кнопку "Следить за снижением цены"': {
            'попап появляется.': makeCase({
                issue: 'MARKETVERSTKA-33967',
                id: 'marketfront-2650',
                async test() {
                    await this.openerButton.subscribeButtonClick();
                    return this.productSubscriptionPopup.waitForPopupVisible()
                        .should.eventually.to.be.equal(true, 'Появился попап подписки на снижение цены');
                },
            }),
        },
    },
});
