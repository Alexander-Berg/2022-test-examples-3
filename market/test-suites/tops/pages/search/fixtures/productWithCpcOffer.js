import {
    createProduct,
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

const WARE_ID = 'offerWareId';
const CPC = 'testCpc';

const CATEGORIES = [
    {
        entity: 'category',
        fullName: 'Чехлы для хранения одежды',
        id: 12807782,
        isLeaf: true,
        modelsCount: 45,
        name: 'Чехлы для одежды',
        nid: 62790,
        offersCount: 1680,
        type: 'gurulight',
        viewType: 'list',
    },
];
const NAVNODES = [
    {
        category: {
            entity: 'category',
            fullName: 'Чехлы для хранения одежды',
            id: 12807782,
            isLeaf: true,
            modelsCount: 45,
            name: 'Чехлы для одежды',
            nid: 62790,
            offersCount: 1680,
            type: 'gurulight',
            viewType: 'list',
        },
        childrenType: 'gurulight',
        entity: 'navnode',
        fullName: 'Чехлы для хранения одежды',
        hasPromo: false,
        id: 62790,
        isLeaf: true,
        link: {
            params: {
                hid: [
                    '12807782',
                ],
                nid: [
                    '62790',
                ],
            },
            target: 'catalog',
        },
        name: 'Чехлы для одежды',
        rootNavnode: {
            entity: 'navnode',
            id: 54422,
        },
        slug: 'chekhly-dlia-khraneniia-odezhdy',
        type: 'category',
    },
];
const ROUTE = {
    slug: 'sluggg',
    productId: 333,
};

const BENEFIT = {
    type: 'default',
    description: 'Хорошая цена от надёжного магазина',
    isPrimary: true,
    nestedTypes: ['default'],
};

const offerWithCpc = createOffer({
    navnodes: NAVNODES,
    categories: CATEGORIES,
    benefit: BENEFIT,
    delivery: {},
    isCutPrice: false,
    cpc: CPC,
    titles: {
        raw: 'Беспроводное колесо',
        highlighted: {
            value: 'Беспроводное колесо',
        },
    },
}, WARE_ID);

const product = createProduct({
    slug: ROUTE.slug,
    navnodes: NAVNODES,
    categories: CATEGORIES,
    offers: {
        items: [WARE_ID],
    },
}, ROUTE.productId);

const stateWithProductAndCpcOffer = mergeState([
    {
        collections: offerWithCpc.collections,
    },
    product,
]);

export default {
    state: stateWithProductAndCpcOffer,
    route: ROUTE,
    cpc: CPC,
};
