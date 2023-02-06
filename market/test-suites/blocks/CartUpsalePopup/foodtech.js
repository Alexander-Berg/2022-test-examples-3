import {makeCase, makeSuite} from 'ginny';

// fixtures
import {offerMock} from '@self/root/src/spec/hermione/kadavr-mock/report/foodtech';
import {eatsState, reportState, routeParams} from '@self/root/src/spec/hermione/fixtures/sinsTunnelingPopup';
// scenarios
import {prepareCartPopup} from '@self/platform/spec/hermione/scenarios/cartPopup';
// page objects
import CartPopupSinsTunnelingContent
    from '@self/project/src/widgets/content/upsale/CartUpsalePopupSinsTunnelingContent/components/View/__pageObject';

export default makeSuite('Фудтех.', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                sinsTunnelingContent: () => this.createPageObject(CartPopupSinsTunnelingContent),
            });

            await this.browser.setState(
                'Eats',
                eatsState
            );

            return this.browser.yaScenario(
                this,
                prepareCartPopup,
                {
                    state: reportState,
                    pageParams: routeParams,
                    isExpress: true,
                }
            );
        },

        'Кнопка "Перейти в магазин".': {
            'По умолчанию': {
                'должна иметь ссылку на ШиШ магазина': makeCase({
                    id: 'marketfront-5755',
                    async test() {
                        const shopLinkHref = await this.sinsTunnelingContent.getShopLinkHref();
                        const expectedUrl = await this.browser.yaBuildURL('market:business', {
                            businessId: offerMock.shop.business_id,
                            express: 'express',
                            slug: offerMock.shop.slug,
                        });

                        return this.expect(shopLinkHref)
                            .to.be.link(
                                expectedUrl,
                                {
                                    skipProtocol: true,
                                    skipHostname: true,
                                }
                            );
                    },
                }),
            },
        },

        'Надпись про бесплатную доставку.': {
            'При изменении количества товаров в корзине': {
                'должна меняться': makeCase({
                    id: 'marketfront-5968',
                    async test() {
                        await this.sinsTunnelingContent.getDeliveryInfoText()
                            .should.eventually
                            .to.be.contain('До бесплатной доставки осталось добавить на 81 ₽');

                        await this.counterCartButton.increase.click();
                        await this.counterCartButton.waitUntilCounterChanged(1, 2);

                        await this.sinsTunnelingContent.getDeliveryInfoText()
                            .should.eventually
                            .to.be.contain('До бесплатной доставки осталось добавить на 1 ₽');

                        await this.counterCartButton.increase.click();
                        await this.counterCartButton.waitUntilCounterChanged(2, 3);

                        await this.sinsTunnelingContent.getDeliveryInfoText()
                            .should.eventually
                            .to.not.be.contain('До бесплатной доставки осталось');
                    },
                }),
            },
        },
    },
});
