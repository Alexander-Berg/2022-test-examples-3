import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideRegionPopup, hideParanja, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import UgcMediaGallerySuite from '@self/platform/spec/gemini/test-suites/blocks/UgcMediaGallery';
import {createUserReviewWithPhotos} from '@self/platform/spec/hermione/fixtures/review';

import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const reviewUserUid = profiles.testachi.uid;
const reviewProductId = 14236972;

export default {
    suiteName: 'Model-card-review-UGC-media-gallery[KADAVR]',
    url: '/product--random-fake-slug/14236972/reviews',
    before(actions) {
        createSession.call(actions);
        const reportProduct = createProduct({
            type: 'model',
            categories: [{
                id: 123,
            }],
            slug: 'random-fake-slug',
            deletedId: null,
        }, reviewProductId);
        const userReviewWithPhotos = createUserReviewWithPhotos(reviewProductId, reviewUserUid);
        setState.call(actions, 'report', reportProduct);
        const reviewAuthor = createUser({ // Автор отзыва
            id: reviewUserUid,
            uid: {
                value: reviewUserUid,
            },
            display_name: {
                public_name: 'review-creator',
            },
        });
        setState.call(actions, 'schema', {
            users: [reviewAuthor],
            gradesOpinions: [userReviewWithPhotos],
            modelOpinions: [userReviewWithPhotos],
        });
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
    },
    after(actions) {
        deleteSession.call(actions);
    },
    childSuites: [
        UgcMediaGallerySuite,
    ],
};
