import {
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {offer} from '@self/platform/spec/hermione/fixtures/offer';

// подготавливает item вишлиста к offerId
export const createWishlistItem = offerId => ({
    id: 101684014,
    owner_id: 3884,
    rgb: 'white',
    reference_type: 'offer',
    reference_id: offerId,
    title: 'Смартфон 4Good Clab Test Phone teso2 Голубой (Зоопарк)',
    image_base_url: '//avatars.mds.yandex.net/get-mpic/1928572/f8ff5708-0d53-4b38-b813-112c8ec64902/orig',
    price: {
        currency: 'RUR',
        amount: 9081,
    },
    added_at: '2020-11-17T10:59:56.433421Z',
    secondary_references: [
        {
            type: 'hid',
            id: '91491',
        },
        {
            type: 'modelId',
            id: '142798000',
        },
    ],
    regionId: 213,
});

// подготавливает стейт вишлиста под оффера
export const createWishlistItems = offerIds => ({
    items: offerIds.map(createWishlistItem),
    token: {
        lastCrTime: '2020-11-17T10:59:56.433421Z',
        lastId: null,
    },
    hasMore: false,
});

// подготавливает оффера вишлиста
export const createWishlistOfferState = (offerIds, itemsCount) => {
    const offerLists = offerIds.map(offerId => ({
        ...createOffer(offer, offerId),
    }));

    return mergeState([offerLists, {
        data: {
            search: {
                total: itemsCount,
                totalOffers: itemsCount,
            },
        },
    }]);
};

// подготавливает стейт итемов для страницы вишлиста
export const createWishlistState = itemsCount => {
    const offerIds = [];
    for (let offerId = 0; offerId < itemsCount; offerId++) {
        offerIds.push([offerId]);
    }

    const offerState = createWishlistOfferState(offerIds, itemsCount);
    const wishlistState = createWishlistItems(offerIds);

    return {
        offerState,
        wishlistState,
    };
};
