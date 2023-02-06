import {makeSuite, makeCase} from '@yandex-market/ginny';

// page-objects
import YandexGoCartButtonPopupPO from '@self/root/src/widgets/content/YandexGoCartButtonPopup/__pageObject';

/**
 * @param {PageObject.YandexGoCartButtonPopup} cartButtonPopup
 */
export default makeSuite('Попап корзины в Yandex.Go', {
    environment: 'kadavr',
    tags: ['Контур#Интеграции'],
    id: 'm-touch-3856',
    story: {
        async beforeEach() {
            hermione.setPageObjects.call(this, {
                cartButtonPopup: () => this.browser.createPageObject(YandexGoCartButtonPopupPO),
            });
        },
        'По-умолчанию': {
            'отображается': makeCase({
                async test() {
                    await this.cartButtonPopup.waitForVisible();

                    return this.cartButtonPopup.isVisible()
                        .should.eventually.be.equal(true, 'Попап корзины отображается');
                },
            }),
            'содержит ссылку на страницу корзины': makeCase({
                async test() {
                    await this.cartButtonPopup.waitForVisible();

                    const cartButtonLink = await this.cartButtonPopup.getCartButtonLink();

                    return this.browser.expect(cartButtonLink).to.be.link({
                        pathname: '/yandex-go/cart',
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
