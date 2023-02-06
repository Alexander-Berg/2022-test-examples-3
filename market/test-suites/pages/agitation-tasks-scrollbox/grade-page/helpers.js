import {createUser, createReportProductStateWithPicture} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {createUserGrade} from '@self/platform/spec/hermione/fixtures/review';

const slug = 'any';
const productId = '14236972';

export const getPath = () => `/product--${slug}/${productId}/reviews`;

export const preparePageState = (actions, reviewCount = 4) => {
    const reviewUserUid = profiles.testachi.uid;

    const reportProduct = createReportProductStateWithPicture({
        slug,
        id: productId,
        description: 'Ящик пандоры',
        titles: {
            raw: 'Ящик пандоры',
        },
        preciseRating: 4.25,
        opinions: 16,
        rating: 4.5,
        ratingCount: 60,
        review: 12,
    });
    const userReviewWithoutPhotos = createUserGrade(Number(productId), profiles.testachi.uid);
    const reviewAuthor = createUser({
        id: reviewUserUid,
        uid: {
            value: reviewUserUid,
        },
        display_name: {
            public_name: 'review-creator',
        },
    });

    return {
        reportProduct,
        review: [...Array(reviewCount)].map((_, id) => ({...userReviewWithoutPhotos, id: (id + 1000)})),
        reviewAuthor,
    };
};
