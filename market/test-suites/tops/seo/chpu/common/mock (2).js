import {
    createProduct,
    createEntityPicture,
    mergeState,
    createShopInfo,
    createOffer,
} from '@yandex-market/kadavr/mocks/Report/helpers';

const productId = 1;
const questionId = 1;
const questionSlug = 'question';
const defaultUserUid = '1';
const questionLikeCount = 12345;
const login = 'lol';
const shopId = 431782;
const shopSlug = 'beru';
const offerId = 1;

const product = {
    slug: 'product',
    categories: [{
        entity: 'category',
        id: 91491,
        name: 'Мобильные телефоны',
        fullName: 'Мобильные телефоны',
        slug: 'mobilnye-telefony',
        type: 'guru',
        isLeaf: true,
    }],
    opinions: 373,
    rating: 4.5,
    ratingCount: 82,
    reviews: 32,
    type: 'model',
    prices: {
        min: '23700',
        max: '47700',
        currency: 'RUR',
        avg: '34900',
    },
    titles: {
        raw: 'Классный продукт',
        highlighted: [{value: 'Классный продукт'}],
    },
};

const shop = createShopInfo({
    entity: 'shop',
    id: shopId,
    status: 'actual',
    oldStatus: 'actual',
    slug: shopSlug,
    ratingToShow: 3.166666667,
    overallGradesCount: 218,
}, shopId);

const picture = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
        thumbnails: [{
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/2hq',
        }],
    },
    'product', productId,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig/1'
);

const defaultQuestion = ({userUid = defaultUserUid, canDelete = false} = {}) => ({
    id: questionId,
    text: 'lol',
    user: {
        entity: 'user',
        uid: userUid,
    },
    product: {
        entity: 'product',
        id: productId,
    },
    votes: {
        likeCount: questionLikeCount,
        userVote: 0,
    },
    slug: questionSlug,
    answersCount: 0,
    canDelete,
});

const defaultUser = ({uid = defaultUserUid} = {}) => ({
    id: uid,
    uid: {
        value: uid,
    },
    login,
    dbfields: {
        'userinfo.firstname.uid': 'loli',
        'userinfo.lastname.uid': 'pop',
    },
    display_name: {
        name: 'someDisplayName',
        public_name: 'somePublicDisplayName',
        display_name_empty: false,
        avatar: {
            default: '36777/515095637-1546054309',
            empty: false,
        },
    },
});

const offer = {
    navnodes: [{
        entity: 'navnode',
        id: 54726,
        isLeaf: true,
        rootNavnode: {},
    }],
    categories: [{
        entity: 'category',
        id: 91491,
        isLeaf: true,
    }],
    pictures: [{
        entity: 'picture',
        original: {
            containerWidth: 340,
            containerHeight: 701,
            url: '//avatars.mds.yandex.net/get-mpic/466729/img_id639045282784638170/orig',
            width: 340,
            height: 701,
        },
        thumbnails: [{
            containerWidth: 50,
            containerHeight: 50,
            url: '//avatars.mds.yandex.net/get-mpic/466729/img_id639045282784638170/50x50',
            width: 24,
            height: 50,
        }],
    }],
    shop: {
        id: shopId,
        entity: 'shop',
        name: 'Test',
        status: 'actual',
        slug: 'slag-suag-swag',
        outletsCount: 1,
        storesCount: 1,
        phones: {raw: '+7 495 1111111', sanitized: '+74951111111'},
    },
};

const cataloger = {
    category: {
        entity: 'category',
        fullName: 'Электроника',
        id: 198119,
        isLeaf: false,
        modelsCount: 124572,
        name: 'Электроника',
        nid: 54440,
        offersCount: 1418191,
        type: 'gurulight',
        viewType: 'list',
    },
    childrenType: 'mixed',
    entity: 'navnode',
    fullName: 'Электроника',
    hasPromo: false,
    id: 54440,
    isLeaf: false,
    link: {
        params: {
            hid: [
                '198119',
            ],
            nid: [
                '54440',
            ],
        },
        target: 'department',
    },
    name: 'Электроника',
    navnodes: [{
        category: {
            entity: 'category',
            fullName: 'Телефоны и аксессуары к ним',
            id: 91461,
            isLeaf: false,
            modelsCount: 16660,
            name: 'Телефоны',
            nid: 54437,
            offersCount: 766969,
            type: 'gurulight',
            viewType: 'list',
        },
        childrenType: 'mixed',
        entity: 'navnode',
        fullName: 'Телефоны и аксессуары к ним',
        hasPromo: false,
        id: 54437,
        isLeaf: false,
        link: {
            params: {
                hid: [
                    '91461',
                ],
                nid: [
                    '54437',
                ],
            },
            target: 'catalog',
        },
        name: 'Телефоны',
        navnodes: [{
            category: {
                entity: 'category',
                fullName: 'Мобильные телефоны',
                id: 91491,
                isLeaf: true,
                modelsCount: 2763,
                name: 'Мобильные телефоны',
                nid: 54726,
                offersCount: 81728,
                type: 'guru',
                viewType: 'grid',
            },
            childrenType: 'mixed',
            entity: 'navnode',
            fullName: 'Мобильные телефоны',
            hasPromo: false,
            id: 54726,
            isLeaf: true,
            link: {
                params: {
                    hid: [
                        '91491',
                    ],
                    nid: [
                        '54726',
                    ],
                },
                target: 'catalogleaf',
            },
            name: 'Мобильные телефоны',
            rootNavnode: {
                entity: 'navnode',
                id: 54440,
            },
            slug: 'mobilnye-telefony',
            type: 'category',
        }],
        rootNavnode: {
            entity: 'navnode',
            id: 54440,
        },
        slug: 'telefony-i-aksessuary-k-nim',
        type: 'category',
    }],
    rootNavnode: {
        entity: 'navnode',
        id: 54440,
    },
    slug: 'elektronika',
    type: 'category',
};

const report = mergeState([
    createProduct(product, productId),
    picture,
]);

const schema = {
    users: [defaultUser()],
    modelQuestions: [defaultQuestion()],
};

const offerMock = createOffer(offer, offerId);

export {
    report,
    productId,
    shop,
    shopId,
    shopSlug,
    schema,
    offerMock as offer,
    offerId,
    cataloger,
};
