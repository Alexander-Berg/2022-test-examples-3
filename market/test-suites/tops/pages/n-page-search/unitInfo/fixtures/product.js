import {mergeState, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import productMock from '@self/root/src/spec/hermione/kadavr-mock/report/product/unitInfo';

export const productId = 1488678631;

export const slug = 'laminat-kronopol-2594-platinium-linea-plus-4v-dub-celtic';

export const product = createProduct(productMock, productId);

export const createReportProductState = productMock => {
    const product = createProduct(productMock);
    const dataMixin = {
        data: {
            search: {
                total: 1,
                totalOffers: 1,
            },
        },
    };

    return mergeState([
        product,
        dataMixin,
    ]);
};

export const reportProductState = createReportProductState(productMock);
