// @flow

import {of} from 'rxjs';
// flowlint-next-line untyped-import: off
import {fireEvent, waitFor} from '@testing-library/dom';
// flowlint-next-line untyped-import: off
import {createProduct, createOfferForProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import * as locationActions from '@self/project/src/actions/location';

import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {makeMirror} from '@self/platform/helpers/testament';

import {
    // flowlint-next-line untyped-import: off
    createUserReviewWithoutPhotos,
} from '@self/platform/spec/hermione/fixtures/review';
// flowlint-next-line untyped-import: off
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// flowlint-next-line untyped-import: off
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';

const RATING_STARS_CONTAINER_SIZE = 100;

const reviewUserUid = profiles.testachi1.uid;
const reviewProductId = 14236972;
const reviewProductSlug = 'random-fake-slug';
const reviewId = 500;
const offerForReviewProductId = 'fFftggdaFshwfg3gFfregW';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

const simulateTap = (ratingInput, targetStarXCoordinate) => {
    const touchList = [
        {
            clientX: targetStarXCoordinate,
            clientY: 0,
            screenX: targetStarXCoordinate,
            screenY: 0,
            pageX: targetStarXCoordinate,
            pageY: 0,
            radiusX: 10,
            radiusY: 0,
        },
    ];

    fireEvent(
        ratingInput,
        new TouchEvent('touchstart', {
            touches: touchList,
            changedTouches: touchList,
            targetTouches: touchList,
            target: ratingInput,
            bubbles: true,
            cancelable: true,
        })
    );

    fireEvent(
        ratingInput,
        new TouchEvent('touchend', {
            touches: touchList,
            changedTouches: touchList,
            targetTouches: touchList,
            target: ratingInput,
            bubbles: true,
            cancelable: true,
        })
    );
};

async function makeContext() {
    return mandrelLayer.initContext({
        request: {
            cookie: {
                kadavr_session_id: await kadavrLayer.getSessionId(),
            },
        },
    });
}

beforeAll(async () => {
    mockIntersectionObserver();
    // $FlowFixMe<type of jest?>
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    kadavrLayer = mirror.getLayer('kadavr');
    apiaryLayer = mirror.getLayer('apiary');

    await jestLayer.runCode(() => {
        // eslint-disable-next-line global-require
        const {mockRouter} = require('@self/project/src/helpers/testament/mock');
        mockRouter();
    }, []);

    const reportProduct = createProduct({
        type: 'model',
        categories: [{
            id: 123,
        }],
        slug: reviewProductSlug,
        deletedId: null,
    }, reviewProductId);

    const offerForProduct = createOfferForProduct({
        cpa: 'real',
    }, reviewProductId, offerForReviewProductId);

    await kadavrLayer.setState('report', mergeState([
        reportProduct,
        offerForProduct,
    ]));
});

afterAll(() => {
    mirror.destroy();
    jest.unmock('@self/platform/helpers/ajax');
});

// TODO: Данный тестамент тест пришлось написать, так редирект на страницу
// оставления отзыва не получилось проверить автотестом из-за бага в методе
// page-object RatingInput.setRating(n). RatingInput.setRating выставляет
// неправильный рейтинг, и следовательно мы не можем проверить работу звезд.
// Данный testament-тест проверяет звезды хотя бы частично, гарантируя, что если мы
// нажали на звезду, соответствующую определенному рейтингу, то этот рейтинг
// попадет в query параметр averageGrade.
describe('Звезды рейтинга', () => {
    test(
        'При клике должен бросаться action @mandrel/LOCATION_CHANGE правильным query параметром averageGrade',
        async () => {
            process.env.PLATFORM = 'touch';
            await makeContext();

            // eslint-disable-next-line global-require
            const ajax = require('@self/platform/helpers/ajax');
            jest.spyOn(ajax, 'postJson').mockImplementation(() => of({
                reviewId,
            }));

            const userReviewWithoutPhotos = {
                ...createUserReviewWithoutPhotos(reviewProductId, reviewUserUid),
                reviewId,
            };
            await kadavrLayer.setState('schema', {
                gradesOpinions: [userReviewWithoutPhotos],
                modelOpinions: [userReviewWithoutPhotos],
            });

            const locationChangeSpy = jest.spyOn(locationActions, 'changeLocationByPageId');

            const {container} = await apiaryLayer.mountWidget(
                '../',
                {
                    productId: reviewProductId,
                    withAddMediaButton: true,
                }
            );

            container.querySelector(RatingInput.chubbyStarParent).getBoundingClientRect = jest.fn(
                () => ({
                    bottom: RATING_STARS_CONTAINER_SIZE,
                    height: RATING_STARS_CONTAINER_SIZE,
                    left: 0,
                    right: RATING_STARS_CONTAINER_SIZE,
                    top: 0,
                    width: RATING_STARS_CONTAINER_SIZE,
                })
            );

            const ratingInput = container.querySelector(RatingInput.chubbyStarParent);

            // Целимся в звезду, обозначающую рейтинг 4
            simulateTap(ratingInput, RATING_STARS_CONTAINER_SIZE * 0.8);

            await step(
                'бросается action @market/location/CHANGE_LOCATION_BY_PAGE_ID правильным query параметром averageGrade',
                async () => {
                    await waitFor(() => {
                        expect(locationChangeSpy).toHaveBeenCalledWith(
                            'market:product-reviews-add', {
                                averageGrade: '4',
                                productId: String(reviewProductId),
                                slug: reviewProductSlug,
                                retpath: 'http://localhost/',
                            }
                        );
                    });
                });

            // Целимся в звезду, обозначающую рейтинг 3
            simulateTap(ratingInput, RATING_STARS_CONTAINER_SIZE * 0.6);

            await step(
                'бросается action@market/location/CHANGE_LOCATION_BY_PAGE_ID правильным query параметром averageGrade',
                async () => {
                    await waitFor(() => {
                        expect(locationChangeSpy).toHaveBeenCalledWith(
                            'market:product-reviews-add', {
                                averageGrade: '3',
                                productId: String(reviewProductId),
                                slug: reviewProductSlug,
                                retpath: 'http://localhost/',
                            }
                        );
                    });
                });
        });
});
