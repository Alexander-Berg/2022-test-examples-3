import {createUser, createReportProductStateWithPicture, createQuestion} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const slug = 'any';
const productId = '14236972';
const uid = profiles.testachi.uid;

export const getPath = () => `/product--${slug}/${productId}/questions`;

export const preparePageState = (actions, questionCount = 4) => {
    const question = createQuestion({
        author: {
            entity: 'user',
            id: uid,
        },
        user: {
            entity: 'user',
            uid,
        },
        product: {
            id: productId,
            entity: 'product',
            description: 'Ящик пандоры',
            titles: {
                raw: 'Ящик пандоры',
            },
        },
        slug: 'why-did-you-open-pandora-s-box',
    });


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
    const questionAuthor = createUser({
        id: uid,
        uid: {
            value: uid,
        },
        display_name: {
            public_name: 'Сurious',
        },
    });

    return {
        reportProduct,
        question: [...Array(questionCount)].map((_, id) => ({...question, id: (id + 1000)})),
        questionAuthor,
    };
};

