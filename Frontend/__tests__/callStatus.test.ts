jest.mock('../../services/History', () => {});

import { CallStatus } from '../../constants/call';
import { isNeedFeedback } from '../callStatus';
import { AppState } from '../../store';

describe('CallStatusSelectors', () => {
    let state: AppState;

    beforeEach(() => {
        state = {
            // @ts-ignore
            call: {
                status: CallStatus.INITED,
            },
        };
    });

    describe('isNeedFeedback', () => {
        beforeEach(() => {
            state.call.status = CallStatus.ENDED;
            state.call.startTime = 1562596254;
        });

        it('Returns true if active call ended', () => {
            expect(isNeedFeedback(state)).toBeTruthy();
        });

        it('Returns false if call not ended', () => {
            state.call.status = CallStatus.STARTED;

            expect(isNeedFeedback(state)).toBeFalsy();
        });

        it('Returns false if not active call ended', () => {
            state.call.startTime = undefined;

            expect(isNeedFeedback(state)).toBeFalsy();
        });
    });
});
