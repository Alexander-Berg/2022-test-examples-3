import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const userMock = profiles.testachi;

const randomProductReview = {
    gradeId: 59734513,
    productId: 14236972,
    slug: 'random-fake-slug',
};

const userReviewWithPhotos = {
    id: 500,
    type: 1,
    product: {
        id: randomProductReview.productId,
    },
    region: {
        id: 213,
    },
    user: {
        uid: userMock.uid,
    },
    averageGrade: 3,
    grade: {
        main: 0,
        0: 0,
        1: null,
        2: null,
        3: null,
    },
    anonymous: 0,
    provider: null,
    photos: [
        {
            gradeId: 500,
            orderNumber: 0,
            namespace: 'market-ugc',
            groupId: '3261',
            imageName: '2a0000015b47267976edee4aa6632825296a',
        },
        {
            gradeId: 500,
            orderNumber: 1,
            namespace: 'market-ugc',
            groupId: '3723',
            imageName: '2a0000015b472778c8a103954e354d8f8ab2',
        },
        {
            gradeId: 500,
            orderNumber: 2,
            namespace: 'market-ugc',
            groupId: '3723',
            imageName: '2a0000015b4727900659a468704bd0ea8db0',
        },
    ],
};

export const reportProduct = createProduct({
    type: 'model',
    categories: [{
        id: 123,
    }],
    slug: 'random-fake-slug',
    deletedId: null,
}, randomProductReview.productId);

export const reviewWithPhotosSchema = {
    users: [
        createUser({ // Автор отзыва
            id: userMock.uid,
            uid: {
                value: userMock.uid,
            },
            display_name: {
                public_name: 'review-creator',
            },
        }),
    ],
    gradesOpinions: [userReviewWithPhotos],
    modelOpinions: [userReviewWithPhotos],
};

const userReviewWithoutPhotos = {
    id: 500,
    type: 1,
    product: {
        id: randomProductReview.productId,
    },
    region: {
        id: 213,
    },
    user: {
        uid: userMock.uid,
    },
    anonymous: 0,
    provider: null,
    photos: [],
};

export const reviewWithoutPhotosSchema = {
    users: [
        createUser({ // Автор отзыва
            id: userMock.uid,
            uid: {
                value: userMock.uid,
            },
            display_name: {
                public_name: 'review-creator',
            },
        }),
    ],
    gradesOpinions: [userReviewWithoutPhotos],
    modelOpinions: [userReviewWithoutPhotos],
};
