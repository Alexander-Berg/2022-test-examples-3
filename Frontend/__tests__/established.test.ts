import { ConnectionEstablishedState } from '../established';
import MediaSessionContextMock from '../../../__mocks__/context';
import { ConnectionDisconnectedState } from '../disconnected';
import { ConnectionReconnectingState } from '../reconnecting';

describe('ConnectionEstablishedState', () => {
    let context;
    let state;

    beforeEach(() => {
        context = new MediaSessionContextMock();
        state = new ConnectionEstablishedState(context);
    });

    describe('handleStateChange', () => {
        it('Change state to disconnected if connection disconnected', () => {
            state.handleStateChange('disconnected');

            expect(context.state).toBeInstanceOf(ConnectionDisconnectedState);
        });

        it('Change state to reconnecting if connection failed', () => {
            state.handleStateChange('failed');

            expect(context.state).toBeInstanceOf(ConnectionReconnectingState);
        });
    });
});
