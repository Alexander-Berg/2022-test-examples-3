/* eslint-disable import/first */
jest.mock('../../services/CallController/DevicesProvider');
jest.mock('react-loadable', () => {
    return {
        default: {
            Map: () => () => null,
        },
    };
});

import { Event } from '@yandex-int/messenger.utils';
import { MockHelper } from '@yandex-int/messenger.utils/lib/mocks';
import store from '../../store';
import { setCallStatus } from '../../store/call';
import { CallStatus } from '../../constants/call';
import { setupCallClientListener, audioPlayer } from '../callClientListeners';

describe('callClientListeners', () => {
    let callClient;
    let call;
    let mockDispatch;
    let mockGetState;

    beforeEach(() => {
        callClient = {
            onIncomingCall: new Event(),
            onCallInit: new Event(),
            onCallAccept: new Event(),
            onCallStart: new Event(),
            onCallConnect: new Event(),
            onCallEnd: new Event(),
            onCallCancel: new Event(),
            onCallDecline: new Event(),
            onCallFail: new Event(),
            onCallConnecting: new Event(),
            onCallError: new Event(),
            onCallMediaUpdate: new Event(),
            onLocalMediaState: new Event(),
            onRemoteMediaState: new Event(),
            onLocalStream: new Event(),
            onRemoteStream: new Event(),
            onRinging: new Event(),
        };

        call = {
            guid: 'call-guid',
            chatId: 'chat-id',
            deviceId: 'device-id',
            videoAvailable: true,
            audioAvailable: true,
            startTime: 1566828334,
        };

        mockDispatch = jest.fn();
        mockGetState = jest.fn(() => ({ call: {} }));
        const dispatchFnMock: any = () => jest.fn((action) => typeof action === 'function' ? action(mockDispatch, mockGetState) : mockDispatch);
        MockHelper.mock(store, 'dispatch', dispatchFnMock);
        MockHelper.mock(audioPlayer, 'play', () => jest.fn().mockReturnValue(Promise.resolve()));
        MockHelper.mock(audioPlayer, 'stop', () => jest.fn());

        setupCallClientListener(callClient);

        jest.useFakeTimers();
    });

    afterAll(() => {
        MockHelper.unmock(store);
        MockHelper.unmock(audioPlayer);

        jest.clearAllMocks();
    });

    describe('onCallConnecting', () => {
        it('Dispatch setCallStatus action with CONNECTING status', (done) => {
            call.startTime = 1566828334;

            callClient.onCallConnecting.addListener(() => {
                expect(store.dispatch).toBeCalledWith(setCallStatus(CallStatus.CONNECTING));

                done();
            });

            callClient.onCallConnecting.dispatch(call);
        });

        it('Stop playing current audio', (done) => {
            callClient.onCallConnecting.addListener(() => {
                expect(audioPlayer.stop).toBeCalled();

                done();
            });

            callClient.onCallConnecting.dispatch(call);
        });

        it('Start playing connecting audio', (done) => {
            callClient.onCallConnecting.addListener(() => {
                expect(audioPlayer.play).toBeCalledWith('connecting.mp3', {});

                done();
            });

            callClient.onCallConnecting.dispatch(call);
        });

        it('Not dispatch action if call not start', (done) => {
            delete call.startTime;

            callClient.onCallConnecting.addListener(() => {
                expect(store.dispatch).not.toBeCalled();

                done();
            });

            callClient.onCallConnecting.dispatch(call);
        });
    });
});
