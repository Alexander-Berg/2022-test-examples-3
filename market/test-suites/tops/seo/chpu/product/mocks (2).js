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
const questionId = 1;
const questionSlug = 'question';
const defaultUserUid = '1';
const questionLikeCount = 12345;
const login = 'lol';
const publicId = 'z1x2c3v4qwe5asdf';

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
    public_id: publicId,
});

const defaultAnswer = ({text = 'lol', id, userUid = defaultUserUid}) => ({
    ...(id ? {id} : {}),
    text,
    user: {
        uid: userUid,
    },
    question: {
        id: questionId,
    },
    votes: {
        likeCount: 0,
        dislikeCount: 0,
        userVote: 0,
    },
});

const defaultGrade = ({id, pId} = {id: 1, pId: productId}) => ({
    id,
    product: {
        id: pId,
    },
    type: 1,
    pro: null,
    contra: null,
    comment: null,
    user: {
        uid: defaultUserUid,
    },
    photos: null,
    anonymous: 0,
});

const defaultAnswers = (count = 1) => new Array(count)
    .fill(null)
    .map((next, key) =>
        defaultAnswer({text: `lol - ${key.toString(10)}`}));

const defaultReview = () => ({
    id: 1,
    product: {
        id: productId,
    },
    type: 0,
    cpa: true,
    pro: 'Lorem ipsum.',
    contra: 'Lorem ipsum.',
    comment: 'Lorem ipsum.',
    anonymous: 0,
    user: {
        uid: defaultUserUid,
    },
    photos: null,
});

const report = ((options = {}) => mergeState([
    createProduct(Object.assign({}, product, options), productId),
    picture])
);

const schema = {
    users: [defaultUser()],
    modelQuestions: [defaultQuestion()],
    modelAnswers: [defaultAnswers()],
    modelOpinions: [defaultReview()],
    gradesOpinions: [defaultGrade()],
};

const wishlishItem = {
    id: '234288990',
    type: 'MODEL',
    hid: 1,
    displayName: 'Продукт',
    itemId: productId.toString(),
};

export {
    report,
    schema,
    login,
    publicId,
    wishlishItem,
};
