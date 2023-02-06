import * as sinon from 'sinon';

import * as boardFullness from 'services/board/fetchBoardFullness/fixtures/boardFullness.json';
import { testService } from 'services/spec/testService/testService';
import { updateUISideblock } from 'services/ui/updateUISideblock/updateUISideblock';

import { jestBuilder } from 'spec/jest';

const builder = jestBuilder(__dirname);

describe('fetchBoardFullness', function() {
    let boardId = '5f2ebd574b60726f12463d36';
    let boardFullnessUrl = `/collections/api/boards/${boardId}/fullness`;

    testService('should update ui sideblock with an empty data before setting board fullness data', async function(base) {
        // arrange
        let service = base.getService('services/board/fetchBoardFullness/fetchBoardFullness');
        let state = await builder.board('aeynbpkxakakxebj0kvqr7jhtg', 'zvezdy-sverkhnovye').state();

        base.mockState(state);
        base.mockRequest(boardFullnessUrl, 'get', boardFullness);

        // act
        await base.callServiceAsync(service.fetchBoardFullness, { boardId });

        // assert
        let updateUISideblockSpy = base.getServiceSpy(updateUISideblock);

        // @ts-ignore
        sinon.assert.calledWith(updateUISideblockSpy.firstCall, {
            data: [],
        });

        // @ts-ignore
        sinon.assert.calledWith(updateUISideblockSpy.secondCall, {
            data: boardFullness,
        });
    });
});
