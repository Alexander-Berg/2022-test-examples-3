import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {createOfferForProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// imports
import getPrice from '@self/project/src/entities/delivery/helpers/getPrice';
import {NBSP} from '@self/project/src/constants/string';

// page objects
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup
    from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import ProductDefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import AmountSelect from '@self/project/src/components/AmountSelect/__pageObject';
import AmountInfo from '@self/project/src/widgets/content/upsale/CartUpsalePopup/containers/AmountSelect/__pageObject';

// fixtures
import oneOfferCpa
    from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product-offers/fixtures/oneOfferCPA';
import {spreadDiscountCountPromo} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';
// scenarios
import {prepareCartPopup} from '@self/platform/spec/hermione/scenarios/cartPopup';
// suites
import foodtechSuite from './foodtech';
import bypassToCheckout from './bypassToCheckout';

export default makeSuite('Апсейл попап.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    cartButton: () => this.createPageObject(CartButton, {
                        parent: ProductDefaultOffer.root,
                    }),
                    cartPopup: () => this.createPageObject(CartPopup),
                    counterCartButton: () => this.createPageObject(AmountSelect),
                    amountInfo: () => this.createPageObject(AmountInfo),
                });
            },
        },

        {
            'Цена за штуку.': {
                'Акция "Прогрессивная скидка за количество".': {
                    async beforeEach() {
                        const offer = createOfferForProduct(
                            {
                                ...oneOfferCpa.offerMock,
                                cpa: 'real',
                                benefit: {
                                    type: 'recommended',
                                    description: 'Хорошая цена от надёжного магазина',
                                    isPrimary: true,
                                },
                                promos: [spreadDiscountCountPromo],
                            },
                            oneOfferCpa.params.productId,
                            oneOfferCpa.offerId
                        );
                        const state = mergeState([oneOfferCpa.reportState, offer]);

                        return this.browser.yaScenario(this, prepareCartPopup, {
                            state,
                            pageParams: oneOfferCpa.params,
                        });
                    },
                    'При добавлении второго товара в корзину': {
                        'цена за единицу товара учитывает промку': makeCase({
                            async test() {
                                await this.counterCartButton.increase.click();
                                await this.counterCartButton.waitUntilCounterChanged(1, 2);

                                const {currency, value} =
                                    spreadDiscountCountPromo.itemsInfo.bounds[0].promoPriceWithTotalDiscount;
                                const expectedPrice = getPrice(value, currency).replace(NBSP, ' ');

                                await this.amountInfo.getPricePerItemText()
                                    .should.eventually
                                    .be.equal(`${expectedPrice}/шт.`);
                            },
                        }),
                    },
                },
            },
        },

        prepareSuite(foodtechSuite),
        prepareSuite(bypassToCheckout)
    ),
});
