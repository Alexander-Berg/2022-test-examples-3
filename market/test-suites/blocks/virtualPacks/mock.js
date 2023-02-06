import {mergeState, createOffer, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import offerMock from '@self/root/src/spec/hermione/kadavr-mock/report/offer/sock';
import productMock from '@self/root/src/spec/hermione/kadavr-mock/report/product/sock';

const minimum = 4;
export const minCount = minimum - 1;

const virtualPackOffer = {
    ...offerMock,
    bundleSettings: {
        quantityLimit: {
            minimum: minimum,
            step: 1,
            maximum: 999,
        },
    },
    stockStoreCount: minCount,
};

const offerId = virtualPackOffer.wareId;
const productId = productMock.id;
const slug = productMock.slug;

const offer = createOffer(virtualPackOffer, offerId);
const product = createProduct(productMock, productId);

export const state = mergeState([
    product,
    offer,
]);

export const path = {productId, slug};
