import {
    mergeState,
    createOffer,
    createPrice,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {randomString} from '@self/root/src/helpers/string';

const offer = createOffer({
    cpc: randomString(),
    prices: createPrice(100, 'RUB', 100, false, {
        discount: null,
    }),
    shop: {
        id: 1,
        slug: 'shop',
        name: 'shop',
    },
    urls: {
        encrypted: '/redir/',
    },
    unitInfo: {
        mainUnit: 'уп',
        referenceUnits: [
            {
                unitName: 'м²',
                unitCount: 1.248,
                unitPrice: {
                    currency: 'RUR',
                    value: 1775,
                },
            },
        ],
    },
});

export const getReportState = view => {
    const reportState = mergeState([
        offer,
        {
            data: {
                search: {
                    view,
                },
            },
        },
    ]);

    return reportState;
};
