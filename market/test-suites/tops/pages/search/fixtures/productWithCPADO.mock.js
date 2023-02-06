import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

const productId = 123;
const slug = 'onetwothree';
const vendorId = 123455;

const createVendor = () => ([{
    entity: 'vendor',
    id: vendorId,
    name: 'Бренд',
    slug: 'brandSlug',
}]);
const createNavnodes = nid => [
    {
        entity: 'navnode',
        id: nid,
        name: 'Мобильные телефоны',
        slug: 'mobilnye-telefony',
        fullName: 'Мобильные телефоны',
        isLeaf: true,
        rootNavnode: {},
    },
];

const route = {
    productId,
    slug,
};

function generateStateAndDataFromMock(offerMock) {
    const offerId = offerMock.wareId;

    const product = createProduct({
        deletedId: null,
        categories: offerMock.categories,
        slug: 'product',
        offers: {
            items: [offerId],
        },
        isNew: false,
        description: 'test',
        prices: {
            min: '100',
            max: '10000',
            currency: 'RUR',
        },
        rating: 4,
        titles: {
            raw: 'Наушники Apple EarPods (3.5 мм) белый',
            highlighted: [{value: 'Наушники Apple EarPods (3.5 мм)', highlight: false}],
        },
        type: 'guru',
        navnodes: createNavnodes(offerMock.categories[0].nid),
        offersCount: 1,
        reviewsCount: 0,
        vendor: {
            entity: 'vendor',
            id: vendorId,
            name: 'Apple',
            slug: 'apple',
            website: 'http://www.apple.com/ru',
            logo: {
                entity: 'picture',
                url: '//avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                thumbnails: [],
            },
            filter: '7893318:153043',
        },
        id: productId,
    }, productId);

    const offer = createOffer({
        ...offerMock,
        navnodes: createNavnodes(offerMock.categories[0].nid),
        model: {id: parseInt(productId, 10)},
        benefit: {
            type: 'default',
            description: 'Хорошая цена от надёжного магазина',
            isPrimary: true,
        },
    }, offerId);

    const state = mergeState([
        product,
        offer,
    ]);
    return {
        state,
        product,
        offer,
        vendor: createVendor(),
    };
}

export default {
    route,
    generateStateAndDataFromMock,
};
