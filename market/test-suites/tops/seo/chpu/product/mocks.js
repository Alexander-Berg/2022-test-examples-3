import {createProduct, createEntityPicture, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

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
};

const productId = 1;
const defaultUserUid = '1';
const login = 'lol';
const userPublicId = 'x1y2z3publicid7z8x9y';

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

const defaultUser = ({uid = defaultUserUid} = {}) => ({
    id: uid,
    uid: {
        value: uid,
    },
    login,
    dbfields: {
        'userinfo.firstname.uid': 'firstName',
        'userinfo.lastname.uid': 'lastName',
    },
    region: {
        id: 213,
    },
    public_id: userPublicId,
});

const defaultGrade = ({id, pId} = {id: 1, pId: productId}) => ({
    id,
    product: {
        id: pId,
    },
    type: 1,
    region: {
        id: 213,
    },
});

const report = ((options = {}) => mergeState([
    createProduct(Object.assign({}, product, options), productId),
    picture,
]));

const schema = {
    users: [defaultUser()],
    gradesOpinions: [defaultGrade()],
};

const wishlishItem = {
    type: 'MODEL',
    displayName: 'Продукт',
    modelId: productId,
    id: productId,
    shopId: 0,
};

export {
    report,
    productId,
    schema,
    login,
    wishlishItem,
    userPublicId,
};
