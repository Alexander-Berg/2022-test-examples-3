import { CallClient } from '..';
import mediatorTransport from '../../lib/__mocks__/transport';
import switchMock from '../../lib/__mocks__/switch';
import logger from '../../lib/__mocks__/logger';

const mockEvent = jest.fn().mockImplementation(() => ({
    addListener: jest.fn(),
    dispatch: jest.fn(),
}));

describe('CallClient', () => {
    let callClient;

    beforeAll(() => {
        // @ts-ignore
        global.MediaStream = jest.fn().mockImplementation();
    });

    beforeEach(() => {
        callClient = new CallClient({
            appId: 'app-id',
            appName: 'app-name',
            appVersion: '0.0.1',
            userGuid: 'test-user-guid',
            // @ts-ignore
            switch: switchMock,
            // @ts-ignore
            mediatorTransport,
            logger,
        });

        callClient.onIncomingCall = mockEvent();
    });

    afterEach(() => {
        logger.log.mockClear();
    });

    describe('handleRinging', () => {
        let call;

        beforeEach(() => {
            callClient.currentCall = {
                guid: 'call-guid',
            };

            call = {
                guid: 'call-guid',
            };

            callClient.onRinging = {
                dispatch: jest.fn(),
            };
        });

        it('Dispatch onRinging event', () => {
            callClient.handleRinging(call);

            expect(callClient.onRinging.dispatch).toBeCalledWith({ guid: 'call-guid' });
        });

        it('Not dispatch onRinging if current call not exists', () => {
            delete callClient.currentCall;

            callClient.handleRinging(call);

            expect(callClient.onRinging.dispatch).not.toBeCalled();
        });

        it('Not dispatch onRinging if handle ringing with another call', () => {
            call.guid = 'another-call';

            callClient.handleRinging(call);

            expect(callClient.onRinging.dispatch).not.toBeCalled();
        });
    });

    describe('handleIncommingCall', () => {
        let incomingCall;

        beforeEach(() => {
            incomingCall = {
                guid: 'call-guid',
                chatId: 'chat-id',
            };
        });

        it('Log incoming call', () => {
            switchMock.onIncomingCall.dispatch(incomingCall);

            expect(logger.log).toBeCalledWith({
                action: 'incomingCall',
                component: 'CallClient',
                guid: expect.any(String),
                userGuid: 'test-user-guid',
                data: {
                    guid: 'call-guid',
                    chatId: 'chat-id',
                },
            });
        });

        it('Dispatch onIncomingCall event', () => {
            switchMock.onIncomingCall.dispatch(incomingCall);

            expect(callClient.onIncomingCall.dispatch).toBeCalledWith({
                guid: 'call-guid',
                chatId: 'chat-id',
                videoAvailable: true,
                audioAvailable: true,
                displayAvailable: false,
            });
        });

        it('Dispatch onIncomingCall one time for one call', () => {
            switchMock.onIncomingCall.dispatch(incomingCall);
            switchMock.onIncomingCall.dispatch(incomingCall);

            expect(callClient.onIncomingCall.dispatch).toBeCalledTimes(1);
        });

        it('Dispatch onIncomingCall if incoming is not current call', () => {
            callClient.currentCall = { guid: 'another-call' };

            switchMock.onIncomingCall.dispatch(incomingCall);

            expect(callClient.onIncomingCall.dispatch).toBeCalled();
        });

        it('Not dispatch onIncomingCall if incoming is current call', () => {
            callClient.currentCall = incomingCall;

            switchMock.onIncomingCall.dispatch(incomingCall);

            expect(callClient.onIncomingCall.dispatch).not.toBeCalled();
        });
    });
});
