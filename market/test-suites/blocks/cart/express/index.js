import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';

import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CartParcel from '@self/root/src/widgets/content/cart/CartList/components/CartParcel/__pageObject';
import CartCheckoutButton
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import LoginAgitation
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/LoginAgitation/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import expressOfferMock from '@self/root/src/spec/hermione/kadavr-mock/report/offer/express';
import expressSkuMock from '@self/root/src/spec/hermione/kadavr-mock/report/sku/express';
import {prepareCartPageBySkuId} from '@self/root/src/spec/hermione/scenarios/cart';

import cartParcelSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/cartParcelInfo';

import params from './params';

export default makeSuite('Экспресс-оффер.', {
    environment: 'kadavr',
    id: 'marketfront-5044',
    issue: 'MARKETFRONT-54428',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    cart: () => this.createPageObject(CartParcel),
                    cartItem: () => this.createPageObject(CartItem, {
                        parent: this.cart,
                    }),
                    cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
                    loginAgitationWrapper: () => this.createPageObject(LoginAgitation),
                    agitationModal: () => this.createPageObject(PopupBase, {
                        parent: this.loginAgitationWrapper,
                    }),
                    notNowButton: () => this.createPageObject(Clickable, {
                        parent: this.agitationModal,
                        root: '[data-autotest-id="notNow"]',
                    }),
                });

                const carts = [
                    buildCheckouterBucket({
                        items: [{
                            skuMock: expressSkuMock,
                            offerMock: expressOfferMock,
                            count: 1,
                        }],
                    }),
                ];


                const {
                    reportSkus,
                    checkoutItems,
                } = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts
                );

                return this.browser.yaScenario(this, prepareCartPageBySkuId, {
                    region: this.params.region,
                    items: checkoutItems,
                    reportSkus,
                });
            },
            'Экспресс-оффер в списке посылок': prepareSuite(cartParcelSuite, {
                params: {
                    isExpress: true,
                    count: 1,
                },
            }),
            'Возможен переход к чекауту': makeCase({
                async test() {
                    await this.cartCheckoutButton.goToCheckout();

                    if (params.hasAgitation && !this.params.isAuthWithPlugin) {
                        await this.agitationModal.waitForVisible();
                        await this.browser.yaWaitForChangeUrl(() => this.notNowButton.click());
                    }

                    const [openedUrl, expectedPath] = await Promise.all([
                        this.browser.getUrl(),
                        this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT),
                    ]);

                    await this.expect(openedUrl).to.be.link({pathname: expectedPath}, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        }
    ),
});
