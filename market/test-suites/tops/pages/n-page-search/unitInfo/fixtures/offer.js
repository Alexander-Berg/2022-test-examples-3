import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import offerMock from '@self/root/src/spec/hermione/kadavr-mock/report/offer/unitInfo';

export const offerId = 'pqqSie9wuNOmYgPW_1Mwbg';

export const offer = createOffer(offerMock, offerId);

export const createReportOfferState = offerMock => {
    const offer = createOffer(offerMock);
    const dataMixin = {
        data: {
            search: {
                total: 1,
                totalOffers: 1,
            },
        },
    };

    return mergeState([
        offer,
        dataMixin,
    ]);
};

export const reportOfferState = createReportOfferState(offerMock);
