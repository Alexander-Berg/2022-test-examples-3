// @flow

import {waitFor, fireEvent} from '@testing-library/dom';

import {makeMirrorTouch as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import * as commentaryVoteAction from '@self/root/src/actions/commentary/vote';

import VotesPageObject from '@self/platform/components/Votes/__pageObject';

import {
    VOTE_COUNT_INITIAL,
    mockResolveAuthUrlSync,
    mockResolveCurrentUserDenormalizedSync,
    mockResolveInitCommentariesBulk,
    mockResolveCommentaryVote,
} from './__mocks__';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

async function makeContext() {
    return mandrelLayer.initContext({});
}
// SKIPPED MARKETFRONT-96354
// eslint-disable-next-line jest/no-disabled-tests
describe.skip('Widget: Commentaries', () => {
    const WIDGET_PATH = '@self/platform/widgets/content/Commentaries';
    const WIDGET_OPTIONS = {
        entityId: 132294510,
        entity: 'productReviewComment',
        entityIdsMap: undefined,
    };

    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                skipLayer: true,
            },
        });

        mandrelLayer = mirror.getLayer('mandrel');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');

        await jestLayer.backend.runCode(
            (
                mockResolveAuthUrlSync,
                mockResolveCurrentUserDenormalizedSync,
                mockResolveInitCommentariesBulk,
                mockResolveCommentaryVote
            ) => {
                jest.spyOn(require('@self/platform/resolvers/user'), 'resolveAuthUrlSync')
                    .mockReturnValue(mockResolveAuthUrlSync);

                jest.spyOn(require('@self/root/src/resolvers/user'), 'resolveCurrentUserDenormalizedSync')
                    .mockReturnValue(mockResolveCurrentUserDenormalizedSync);

                jest.spyOn(require('@self/platform/resolvers/comments'), 'resolveInitCommentariesBulk')
                    .mockReturnValue(Promise.resolve(mockResolveInitCommentariesBulk));

                jest.spyOn(require('@self/platform/resolvers/comments'), 'addCommentaryLike')
                    .mockReturnValue(Promise.resolve(mockResolveCommentaryVote));

                jest.spyOn(require('@self/platform/resolvers/comments'), 'removeCommentaryVote')
                    .mockReturnValue(Promise.resolve(mockResolveCommentaryVote));

                jest.spyOn(require('@self/platform/resolvers/comments'), 'addCommentaryDislike')
                    .mockReturnValue(Promise.resolve(mockResolveCommentaryVote));
            }, [
                mockResolveAuthUrlSync,
                mockResolveCurrentUserDenormalizedSync,
                mockResolveInitCommentariesBulk,
                mockResolveCommentaryVote,
            ]);
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe('Комментарий 1-го уровня', () => {
        test('При двойном клике на кнопку лайка количество лайков сначала увеличивается, а потом возвращается', async () => {
            const commentaryVoteUpSuccessSpy = jest.spyOn(commentaryVoteAction, 'commentaryVoteUpSuccess');
            const removeCommentaryVoteSuccessSpy = jest.spyOn(commentaryVoteAction, 'removeCommentaryVoteSuccess');

            await makeContext();

            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
            const voteButtonElements = container.querySelectorAll(VotesPageObject.button);
            const likeButtonElement = voteButtonElements[1];
            const likeCountBeforeClick = Number(likeButtonElement.textContent);
            expect(likeCountBeforeClick).toEqual(VOTE_COUNT_INITIAL);

            fireEvent.click(likeButtonElement);

            await waitFor(() => {
                expect(commentaryVoteUpSuccessSpy).toHaveBeenCalled();
            });

            const likeCountAfterFirstClick = Number(likeButtonElement.textContent);
            expect(likeCountAfterFirstClick).toEqual(VOTE_COUNT_INITIAL + 1);

            fireEvent.click(likeButtonElement);

            await waitFor(() => {
                expect(removeCommentaryVoteSuccessSpy).toHaveBeenCalled();
            });

            const likeCountAfterSecondClick = Number(likeButtonElement.textContent);
            expect(likeCountAfterSecondClick).toEqual(VOTE_COUNT_INITIAL);

            commentaryVoteUpSuccessSpy.mockRestore();
            removeCommentaryVoteSuccessSpy.mockRestore();
        });

        test('При двойном клике на кнопку дизлайка количество дизлайков сначала увеличивается, а потом возвращается', async () => {
            const commentaryVoteDownSuccessSpy = jest.spyOn(commentaryVoteAction, 'commentaryVoteDownSuccess');
            const removeCommentaryVoteSuccessSpy = jest.spyOn(commentaryVoteAction, 'removeCommentaryVoteSuccess');

            await makeContext();

            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
            const voteButtonElements = container.querySelectorAll(VotesPageObject.button);
            const dislikeButtonElement = voteButtonElements[2];
            const dislikeCountBeforeClick = Number(dislikeButtonElement.textContent);
            expect(dislikeCountBeforeClick).toEqual(VOTE_COUNT_INITIAL);

            fireEvent.click(dislikeButtonElement);

            await waitFor(() => {
                expect(commentaryVoteDownSuccessSpy).toHaveBeenCalled();
            });

            const dislikeCountAfterFirstClick = Number(dislikeButtonElement.textContent);
            expect(dislikeCountAfterFirstClick).toEqual(VOTE_COUNT_INITIAL + 1);

            fireEvent.click(dislikeButtonElement);

            await waitFor(() => {
                expect(removeCommentaryVoteSuccessSpy).toHaveBeenCalled();
            });

            const dislikeCountAfterSecondClick = Number(dislikeButtonElement.textContent);
            expect(dislikeCountAfterSecondClick).toEqual(VOTE_COUNT_INITIAL);

            commentaryVoteDownSuccessSpy.mockRestore();
            removeCommentaryVoteSuccessSpy.mockRestore();
        });
    });
});
