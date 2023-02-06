import {
    mergeState,
    createProduct,
    createOffer,
    createFilter,
    createFilterValue,
    createEntityFilterValue,
} from '@yandex-market/kadavr/mocks/Report/helpers';

const slug = 'planshet-samsung-galaxy-tab-s2-9-7-sm-t819-lte-32gb';

const goldId = '14896254';
const goldFixture = {
    'id': goldId,
    'value': 'золотистый',
    'code': '#FFD700',
    'initialFound': 5,
    'found': 5,
};

const blackId = '14896255';
const blackFixture = {
    'id': blackId,
    'value': 'черный',
    'code': '#000000',
    'initialFound': 5,
    'found': 5,
};

const filterId = '14871214';
const filterFixture = {
    'id': filterId,
    'type': 'enum',
    'name': 'Цвет товара',
    'xslname': 'color_vendor',
    'subType': 'image_picker',
    'kind': 2,
    'position': 4,
    'noffers': 1,
    'valuesGroups': [{
        type: 'all',
        valuesIds: [goldId, blackId],
    }],
};

const offer1Id = '456';
const offer2Id = '457';

const encryptedUrl = '/redir/encrypted';

const categories = [
    {
        entity: 'category',
        id: 91491,
        name: 'Мобильные телефоны',
        fullName: 'Мобильные телефоны',
        type: 'guru',
        isLeaf: true,
        slug: 'mobilnye-telefony',
    },
];

const navnodes = [
    {
        entity: 'navnode',
        id: 54726,
        name: 'Мобильные телефоны',
        slug: 'mobilnye-telefony',
        fullName: 'Мобильные телефоны',
        isLeaf: true,
        rootNavnode: {},
    },
];

const payments = {
    deliveryCard: true,
    deliveryCash: true,
    prepaymentCard: true,
    prepaymentOther: false,
};

const productId = 13905590;
const product = createProduct({
    offers: {
        count: 2,
    },
    type: 'model',
    categories,
    navnodes,
    slug,
}, productId);

// Создаем общий фильтр с двумя значениями
const colorFilter = createFilter(filterFixture, filterId);
const goldValue = createFilterValue(goldFixture, filterId, goldId);
const blackValue = createFilterValue(blackFixture, filterId, blackId);
const filterValue = createEntityFilterValue(goldFixture, offer2Id, filterId, goldFixture.id);

const offer = {
    isCutPrice: false,
    categories,
    navnodes,
    payments,
    filters: [filterId],
    wareId: productId,
    urls: {
        encrypted: encryptedUrl,
        decrypted: '/redir/decrypted',
        geo: '/redir/geo',
        offercard: '/redir/offercard',
    },
    shop: {
        id: 1,
        name: 'shop',
        slug: 'shop',
        feed: {
            id: 123123,
        },
    },
    cpc: 'DqqPjIrWS5xIT',
    vendor: {
        id: 2222,
        entity: 'vendor',
        name: 'some_vendor_name',
        slug: 'some-vendor-name',
    },
    delivery: {
        shopPriorityRegion: {
            entity: 'region',
            id: 62007514,
            name: 'ea mol',
            lingua: {
                name: {
                    accusative: 'ut aliqua',
                    genitive: 'veniam Excepteur consequat',
                    preposition: 'sit',
                    prepositional: 'nulla amet',
                },
            },
        },
        shopPriorityCountry: {
            entity: 'region',
            id: 59868827,
            name: 'in officia exercitation',
            lingua: {
                name: {
                    accusative: 'anim aute',
                    genitive: 'reprehenderit',
                    preposition: 'dolor ad Duis aliqua sunt',
                    prepositional: 'voluptate cillum',
                },
            },
        },
        region: {
            lingua: {
                name: {
                    accusative: 'anim aute',
                    genitive: 'reprehenderit',
                    preposition: 'dolor ad Duis aliqua sunt',
                    prepositional: 'voluptate cillum',
                },
            },
            title: 'Регион, в который будет осуществляться доставка курьером',
        },
        price: {
            currency: 'reprehenderit ',
            value: 87304050,
        },
        options: [],
    },
};

const defaultOffer = {
    ...offer,
    cpc: `DqqPjIrWS5xIT${offer1Id}`,
    benefit: {
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
        type: 'cheapest',
    },
};

const wareMd5Offer = {
    ...offer,
    cpc: `DqqPjIrWS5xIT${offer2Id}`,
    benefit: {
        type: 'waremd5',
        isPrimary: true,
    },
};

const route = {
    productId,
    slug,
    'do-waremd5': offer2Id,
};

const state = mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 2,
            },
        },

    },
    product,
    createOffer(defaultOffer, offer1Id),
    createOffer(wareMd5Offer, offer2Id),

    createOffer(offer),
    createOffer(offer),
    createOffer(offer),
    createOffer(offer),
    createOffer(offer),

    colorFilter,
    goldValue,
    blackValue,
    filterValue,
]);

export default {
    state,
    route,
    encryptedUrl,
    defaultOfferId: offer2Id,
};
