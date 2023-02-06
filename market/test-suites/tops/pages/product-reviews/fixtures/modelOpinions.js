import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {routes} from '@self/platform/spec/hermione/configs/routes';

const textStub = `Lorem ipsum dolor sit amet, consectetur adipiscing elit,
sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.`;

const PRODUCT_REVIEW_TYPE = 1;

const showDispayNameInPassportUser = createUser({
    id: '100500',
    uid: {
        value: '100500',
    },
    display_name: {
        name: 'someDisplayName',
        public_name: 'somePublicDisplayName',
        display_name_empty: false,
    },
    login: 'user',
});
const signedUpWithSocialNetworkUser = createUser({
    id: '100500',
    uid: {
        value: '100500',
    },
    display_name: {
        name: 'someDisplayName',
        public_name: 'somePublicDisplayName',
        display_name_empty: true,
    },
    login: 'user',
});

const productCfg = routes.product.nokia;

const productMock = createProduct({
    showUid: 'testProductShowUid',
    type: 'model',
    slug: productCfg.slug,
    categories: [
        {
            entity: 'category',
            id: 91491,
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            slug: 'mobilnye-telefony',
            type: 'guru',
            isLeaf: true,
        },
    ],
}, productCfg.productId);

const defaultReview = {
    id: 1,
    product: {
        id: productCfg.productId,
    },
    type: PRODUCT_REVIEW_TYPE,
    pro: textStub.repeat(2),
    contra: textStub,
    comment: textStub,
    anonymous: 0,
    user: {
        uid: '100500',
    },
    photos: null,
};

const basicProductGradesSchema = {
    modelOpinions: [],
    gradesOpinions: [{
        id: 1,
        product: {
            id: productCfg.productId,
        },
        type: PRODUCT_REVIEW_TYPE,
        pro: null,
        contra: null,
        comment: null,
        user: {
            uid: '100500',
        },
        photos: null,
        anonymous: 0,
    }, {
        id: 2,
        product: {
            id: productCfg.productId,
        },
        type: PRODUCT_REVIEW_TYPE,
        pro: null,
        contra: null,
        comment: null,
        user: {
            uid: '100500',
        },
        photos: null,
        provider: {
            type: 'vendor',
            name: 'Sony',
        },
        anonymous: 0,
    }],
};
const user = {
    id: '100500',
    uid: {
        value: '100500',
    },
    login: 'fake-user',
    regname: '100500',
    publicDisplayName: 'lol pop',
    public_id: '100500lolpop',
};

const user2 = {
    id: '100501',
    uid: {
        value: '100501',
    },
    login: 'fake-user2',
    regname: '100501',
    publicDisplayName: 'user',
};

const modelOpinionLong = {
    id: 1,
    anonymous: 0,
    product: {
        id: productCfg.productId,
    },
    type: PRODUCT_REVIEW_TYPE,
    pro: textStub.repeat(2),
    contra: textStub,
    comment: textStub,
    averageGrade: 3,
    user: {
        uid: '100500',
    },
    photos: null,
};
const modelOpinionShort = {
    id: 2,
    anonymous: 0,
    product: {
        id: productCfg.productId,
    },
    type: PRODUCT_REVIEW_TYPE,
    pro: textStub.substring(0, textStub.length / 4),
    contra: null,
    comment: null,
    averageGrade: 3,
    user: {
        uid: '100500',
    },
    photos: null,
    provider: {
        type: 'vendor',
        name: 'Sony',
    },
};
const modelOpinionWithPhotos = {
    id: 11,
    product: {
        id: productCfg.productId,
    },
    type: PRODUCT_REVIEW_TYPE,
    pro: textStub.substring(0, textStub.length / 4),
    contra: null,
    comment: null,
    averageGrade: 4,
    user: {
        uid: '100500',
    },
    photos: [{
        gradeId: 11,
        namespace: 'market-ugc',
        imageName: '2a0000016c47694e002f7e830176ce9e6f85',
        groupId: '3261',
    }, {
        gradeId: 11,
        namespace: 'market-ugc',
        imageName: '2a0000015d412e9b0acdf2a47b841734a52c',
        groupId: '3723',
    }],
    provider: {
        type: 'vendor',
        name: 'Sony',
    },
};
const modelOpinionWithPhotoAndComment = {
    id: 10,
    product: {
        id: productCfg.productId,
    },
    type: PRODUCT_REVIEW_TYPE,
    pro: textStub.substring(0, textStub.length / 4),
    contra: null,
    comment: 'Хороший телефон',
    averageGrade: 5,
    user: {
        uid: '100501',
    },
    photos: [{
        gradeId: 10,
        namespace: 'market-ugc',
        imageName: '2a0000015af5b204104d18e68e17867b677b',
        groupId: '3261',
    }],
    provider: {
        type: 'vendor',
        name: 'Sony',
    },
};

const userExpertise = {
    userId: user.id,
    expertiseId: 9,
    value: 33,
    levelValue: 13,
    level: 2,
};

export {
    productCfg,
    showDispayNameInPassportUser,
    signedUpWithSocialNetworkUser,
    productMock,
    defaultReview,
    basicProductGradesSchema,
    user,
    user2,
    modelOpinionLong,
    modelOpinionShort,
    modelOpinionWithPhotos,
    modelOpinionWithPhotoAndComment,
    userExpertise,
};
