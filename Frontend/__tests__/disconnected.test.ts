import { ConnectionDisconnectedState } from '../disconnected';
import MediaSessionContextMock from '../../../__mocks__/context';
import { MediaSessionStatus } from '../../../status';
import { ConnectionEstablishedState } from '../established';
import { ConnectionReconnectingState } from '../reconnecting';

describe('ConnectionDisconnectedState', () => {
    let context;
    let state;

    beforeEach(() => {
        context = new MediaSessionContextMock();
        state = new ConnectionDisconnectedState(context);
    });

    describe('enter', () => {
        it('Subscribe to onConfig event', () => {
            context.connection.onStateChange.addListener = jest.fn();

            state.enter();

            expect(context.connection.onStateChange.addListener).toBeCalledWith(state.handleStateChange);
        });

        it('Set status to CONNECTING', () => {
            state.enter();

            expect(context.status).toStrictEqual(MediaSessionStatus.CONNECTING);
        });
    });

    describe('exit', () => {
        it('Unsubscribe to onConfig event', () => {
            context.connection.onStateChange.removeListener = jest.fn();

            state.exit();

            expect(context.connection.onStateChange.removeListener).toBeCalledWith(state.handleStateChange);
        });
    });

    describe('handleStateChange', () => {
        it('Change state to established if connection connected', () => {
            state.handleStateChange('connected');

            expect(context.state).toBeInstanceOf(ConnectionEstablishedState);
        });

        it('Change state to established if connection completed', () => {
            state.handleStateChange('completed');

            expect(context.state).toBeInstanceOf(ConnectionEstablishedState);
        });
        it('Change state to reconnecting if connection failed', () => {
            state.handleStateChange('failed');

            expect(context.state).toBeInstanceOf(ConnectionReconnectingState);
        });
    });
});
