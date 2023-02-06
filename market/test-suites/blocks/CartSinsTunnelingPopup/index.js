import {makeCase, makeSuite} from 'ginny';

// page-objects
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartSinsTunnelingPopup from '@self/platform/widgets/content/CartSinsTunnelingPopup/__pageObject';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
// fixtures
import {eatsState, reportState, routeParams} from '@self/root/src/spec/hermione/fixtures/sinsTunnelingPopup';
import {offerMock} from '@self/root/src/spec/hermione/kadavr-mock/report/foodtech';
// scenarios
import {setExpressAddress, setMoscowAddressToList} from '@self/root/src/spec/hermione/scenarios/express';

export default makeSuite('Попап после добавления в корзину.', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartButton: () => this.createPageObject(CartButton, {
                    parent: DefaultOffer.root,
                }),
                cartSinsTunnelingPopup: () => this.createPageObject(CartSinsTunnelingPopup),
            });

            await this.browser.yaScenario(this, setMoscowAddressToList);

            await this.browser.setState(
                'Eats',
                eatsState
            );

            await this.browser.setState(
                'report',
                reportState
            );

            await this.browser.yaOpenPage('touch:product', routeParams);

            await this.browser.yaScenario(this, setExpressAddress);

            return this.browser.allure.runStep(
                'Добавляем товар в корзину',
                () => this.cartButton.click()
            );
        },

        'Кнопка перехода в корзину.': {
            'По умолчанию': {
                'содержит ссылку на корзину': makeCase({
                    id: 'marketfront-5756',
                    async test() {
                        await this.cartSinsTunnelingPopup.isVisible()
                            .should.eventually
                            .to.be.equal(true, 'Попап должен отображаться');

                        const cartBtnHref = await this.cartSinsTunnelingPopup.getCartLinkHref();
                        const expectedUrl = await this.browser.yaBuildURL('touch:cart');

                        return this.expect(cartBtnHref)
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },

        'Фудтех.': {
            'По умолчанию': {
                'отображается и правильно рендерит содержимое': makeCase({
                    id: 'marketfront-5757',
                    async test() {
                        await this.cartSinsTunnelingPopup.isVisible()
                            .should.eventually
                            .to.be.equal(true, 'Попап должен отображаться');

                        const expectedUrl = await this.browser.yaBuildURL('market:business', {
                            businessId: offerMock.shop.business_id,
                            express: 'express',
                            slug: offerMock.shop.slug,
                        });

                        await this.browser.allure.runStep(
                            'Проверяем ссылку на кнопке "Перейти в магазин"',
                            () => this.cartSinsTunnelingPopup.getSinsLinkHref()
                                .should.eventually
                                .to.be.link(expectedUrl, {
                                    skipProtocol: true,
                                    skipHostname: true,
                                })
                        );

                        await this.browser.allure.runStep(
                            'Проверяем текст о доставке',
                            () => this.cartSinsTunnelingPopup.getDeliveryInfoText()
                                .should.eventually
                                .to.be.contain('До бесплатной доставки осталось добавить\nна 161 ₽')
                        );
                    },
                }),
            },
        },
    },
});
