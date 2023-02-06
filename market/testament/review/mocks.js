import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createProduct, createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';
import {reviewStates} from '@self/root/src/entities/review/constants';

export const productCfg = {
    productId: 1019919,
    slug: 'telefon-nokia-2630',
};

export const shopCfg = {
    shopId: 1925,
    slug: 'tekhnopark',
};

export const DEFAULT_REVIEW_ID = 12221;
export const DEFAULT_VOTES_COUNT = 10;
export const DEFAULT_AVERAGE_GRADE = 5;
export const DEFAULT_MODERATION_REASON = 'moderation-reason';

const PRODUCT_REVIEW_TYPE = 1;
const SHOP_REVIEW_TYPE = 0;

export const PRO = 'Pro';
export const CONTRA = 'Contra';
export const COMMENT = 'Comment';

export const USER_UID = '100500';
export const PUBLIC_USER_ID = '2v4wotdenptgwxukqhriedjbaxtc90m7vf';

export const displayName = 'someDisplayName';
export const publicDisplayName = 'somePublicDisplayName';

export const showDisplayNameInPassportUser = createUser({
    id: USER_UID,
    uid: {
        value: USER_UID,
    },
    public_id: PUBLIC_USER_ID,
    display_name: {
        name: displayName,
        public_name: publicDisplayName,
        display_name_empty: false,
    },
    login: 'user',
});

export const signedUpWithSocialNetworkUser = createUser({
    id: USER_UID,
    uid: {
        value: USER_UID,
    },
    public_id: PUBLIC_USER_ID,
    display_name: {
        name: displayName,
        public_name: publicDisplayName,
        display_name_empty: true,
    },
    login: 'user',
});

export const productMock = createProduct({
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

const createReview = (params = {}) => {
    const {
        id,
        productId,
        shopId,
        anonymous,
        gradesOnly,
        moderationState,
        spam,
    } = params;

    const moderationReasonRecommendation =
        moderationState === reviewStates.AUTOMATICALLY_REJECTED
            ? null
            : DEFAULT_MODERATION_REASON;

    return ({
        id,
        type: productId ? PRODUCT_REVIEW_TYPE : SHOP_REVIEW_TYPE,
        product: productId ? {id: productId} : null,
        shop: shopId ? {id: shopId} : null,
        user: {
            uid: USER_UID,
        },
        anonymous,
        averageGrade: DEFAULT_AVERAGE_GRADE,
        cpa: true,
        pro: gradesOnly ? null : PRO,
        contra: gradesOnly ? null : CONTRA,
        comment: gradesOnly ? null : COMMENT,
        photos: null,
        votes: {
            agree: DEFAULT_VOTES_COUNT,
            reject: DEFAULT_VOTES_COUNT,
        },
        moderationState,
        moderationReasonRecommendation,
        spam,
        retpath: '',
    });
};

export const product = {
    product: {
        [productCfg.productId]: {
            entity: 'product',
            categoryIds: [],
            navnodeIds: [],
            offersCount: 7913318,
            offersWithCutPriceCount: 0,
            showReview: false,
            reviewsCount: 19327341,
            ratingCount: 0,
            overviewsCount: 0,
            reviewIds: [],
            specs: {},
            vendorId: 6722888,
            colorVendorCount: undefined,
            links: undefined,
            sale: undefined,
            isExclusive: false,
            hypeGoods: false,
            isRare: false,
            id: String(productCfg.productId),
            description: 'commodo',
            isNew: false,
            pictures: [],
            prices: {},
            rating: 5,
            titles: {},
            type: 'model',
            slug: productCfg.slug,
            titlesWithoutVendor: {},
        },
    },
};

export const createUserReviews = (params = {}) => {
    const {
        reviewsCount = 1,
        productId = productCfg.productId,
        shopId = shopCfg.shopId,
        anonymous = 0,
        gradesOnly = false,
        moderationState = reviewStates.APPROVED,
        spam = false,
    } = params;

    let reviews = [];
    if (reviewsCount > 0) {
        reviews = [...Array(reviewsCount)].map((_, index) => createReview({
            id: DEFAULT_REVIEW_ID + index,
            productId,
            shopId,
            anonymous,
            gradesOnly,
            moderationState,
            spam,
        }));
    }

    return reviews;
};

const basicProductGradesSchema = {
    modelOpinions: [],
    gradesOpinions: createUserReviews({shopId: null}),
};

export const reviewRequestParams = {
    ...productCfg,
    reviewId: DEFAULT_REVIEW_ID,
};

export const socialUserSchema = {
    users: [signedUpWithSocialNetworkUser],
    modelOpinions: createUserReviews({shopId: null}),
};

export const passportUserSchema = {
    users: [showDisplayNameInPassportUser],
    modelOpinions: createUserReviews({shopId: null}),
};

export const productGradesSchema = {
    ...basicProductGradesSchema,
    users: [showDisplayNameInPassportUser],
};

const defaultShopReview = ({recommended, id = 0}) => ({
    id,
    shop: {
        id: shopCfg.shopId,
    },
    type: SHOP_REVIEW_TYPE,
    cpa: recommended,
    pro: 'Lorem ipsum.',
    contra: 'Lorem ipsum.',
    comment: 'Lorem ipsum.',
    anonymous: 0,
    user: {
        uid: USER_UID,
    },
    photos: null,
});

export const shopReview = defaultShopReview({recommended: true});
export const socialUserShopSchema = {
    users: [signedUpWithSocialNetworkUser],
    modelOpinions: [shopReview],
};
export const passportUserShopSchema = {
    users: [showDisplayNameInPassportUser],
    modelOpinions: [shopReview],
};

export const shopInfo = createShopInfo({
    entity: 'shop',
    id: shopCfg.shopId,
    status: 'actual',
    oldStatus: 'actual',
    slug: shopCfg.slug,
    ratingToShow: 3.166666667,
    overallGradesCount: 218,
}, shopCfg.shopId);
