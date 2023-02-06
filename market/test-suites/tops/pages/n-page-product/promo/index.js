import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct, createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// suites
import DealsTermsPopupSutie from '@self/platform/spec/hermione/test-suites/blocks/n-deals-terms/popup';
// page-objects
import ProductDefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import DealsTerms from '@self/platform/spec/page-objects/components/DealsTerms';
import DealsDescriptionPopup from '@self/project/src/components/DealDescription/__pageObject';

// mocks
import {guruMock} from '@self/platform/spec/hermione/fixtures/promo/product.mock';
import {offerMock} from '@self/platform/spec/hermione/fixtures/promo/offer.mock';
import {
    nPlusMPromo,
    promocodePromo,
    giftPromo,
} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';

const popupDOStories = [{
    promos: [nPlusMPromo],
    meta: {
        issue: 'MARKETVERSTKA-35175',
        id: 'marketfront-3632',
    },
    description: 'Попап на акции N+M в ДО',
}, {
    promos: [promocodePromo],
    meta: {
        issue: 'MARKETVERSTKA-35173',
        id: 'marketfront-3630',
    },
    description: 'Попап на акции промокод в ДО',
}, {
    promos: [giftPromo],
    meta: {
        issue: 'MARKETVERSTKA-35174',
        id: 'marketfront-3631',
    },
    description: 'Попап на акции подарок в ДО',
}];

const prepareState = ({promos = null, isDefaultOffer = false}) => {
    const {mock} = guruMock;
    const product = createProduct(mock, mock.id);
    const offer = createOffer({
        ...offerMock,
        prices: !promos ? offerMock.prices : {
            currency: 'RUR',
            value: '500',
            isDeliveryIncluded: false,
        },
        promos,
        benefit: !isDefaultOffer ? undefined : {
            type: 'recommended',
            description: 'Хорошая цена от надёжного магазина',
            isPrimary: true,
        },
    }, offerMock.wareId);
    return mergeState([product, offer, {
        data: {
            search: {
                totalOffersBeforeFilters: 2,
            },
        },
    }]);
};

export default makeSuite('Акции.', {
    environment: 'kadavr',
    feature: 'Скидки и акции',
    story: mergeSuites(
        createStories(popupDOStories, ({promos, meta, description}) =>
            prepareSuite(DealsTermsPopupSutie, {
                meta,
                description,
                hooks: {
                    async beforeEach() {
                        const {id: productId, slug} = guruMock.mock;
                        await this.browser.setState('report', prepareState({promos, isDefaultOffer: true}));
                        await this.browser.yaOpenPage('market:product', {productId, slug});
                        return this.dealsBadge.scrollToBadge();
                    },
                },
                pageObjects: {
                    dealsBadge() {
                        return this.createPageObject(DealsTerms, {
                            parent: ProductDefaultOffer.root,
                        });
                    },
                    dealsDescriptionPopup() {
                        return this.createPageObject(DealsDescriptionPopup);
                    },
                },
            })
        )
    ),
});
