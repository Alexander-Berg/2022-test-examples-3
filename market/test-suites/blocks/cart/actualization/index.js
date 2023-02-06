import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import ExpiredCartOffer
    from '@self/root/src/widgets/content/cart/CartList/components/ExpiredCartOffer/__pageObject';
import CartOfferAvailabilityInfo
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferAvailabilityInfo/__pageObject';
import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

import oneOffer from './nullStockOneOffer';
import manyOffers from './nullStockManyOffers';
import changeCountOneOffer from './changeCountOneOffer';

export default makeSuite('Актуализация.', {
    environment: 'kadavr',
    feature: 'Актуализация',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    cartHeader: () => this.createPageObject(CartHeader),
                    cartItem: () => this.createPageObject(
                        CartItem,
                        {root: `${CartItem.root}:nth-child(1)`}
                    ),
                    orderTotal: () => this.createPageObject(OrderTotal),
                    unavailableCartOffer: () => this.createPageObject(
                        ExpiredCartOffer
                    ),
                    unavailableCartOfferAvailabilityInfo: () => this.createPageObject(
                        CartOfferAvailabilityInfo,
                        {parent: this.unavailableCartOffer}
                    ),
                    availableCartOfferAvailabilityInfo: () => this.createPageObject(
                        CartOfferAvailabilityInfo,
                        {parent: this.cartItem}
                    ),
                });
            },
        },
        {

            'Обнуление стока.': mergeSuites(
                prepareSuite(manyOffers, {}),
                prepareSuite(oneOffer, {})
            ),
        },

        prepareSuite(changeCountOneOffer, {})
    ),
});
