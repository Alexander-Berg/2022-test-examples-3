/* eslint-disable import/first */
jest.mock('../../History', () => {});
jest.mock('../../MessengerApi');

import MessengerApi from '../../MessengerApi';
import { callApi } from '../index';

describe('CallApi', () => {
    let call;

    beforeEach(() => {
        call = {
            guid: 'call-guid',
            chatId: 'call-chatId',
            deviceId: 'call-deviceId',
            deviceInfo: {},
            videoEnabled: true,
            audioEnabled: true,
        };
    });

    describe('makeCall', () => {
        beforeEach(() => {
            (MessengerApi.sendCallingMessage as jest.Mock).mockReset();
            (MessengerApi.sendCallingMessage as jest.Mock).mockReturnValue(Promise.resolve());
        });

        it('Send calling message with correct params', () => {
            callApi.makeCall(call);

            expect(MessengerApi.sendCallingMessage).toBeCalledWith({
                CallGuid: 'call-guid',
                ChatId: 'call-chatId',
                DeviceId: 'call-deviceId',
                MakeCall: {
                    CallType: 0,
                    DeviceInfo: 'eyJkZXZpY2VfaW5mbyI6eyJwbGF0Zm9ybSI6IldFQiIsIndlYiI6e319LCJkZWJ1Z19vcHRpb25zIjp7fX0=',
                },
            });
        });

        it('Send calling message with correct params for audio call', () => {
            call.videoEnabled = false;

            callApi.makeCall(call);

            expect(MessengerApi.sendCallingMessage).toBeCalledWith({
                CallGuid: 'call-guid',
                ChatId: 'call-chatId',
                DeviceId: 'call-deviceId',
                MakeCall: {
                    CallType: 1,
                    DeviceInfo: 'eyJkZXZpY2VfaW5mbyI6eyJwbGF0Zm9ybSI6IldFQiIsIndlYiI6e319LCJkZWJ1Z19vcHRpb25zIjp7fX0=',
                },
            });
        });
    });

    describe('getDebugOptions', () => {
        beforeEach(() => {
            // @ts-ignore
            global.window.flags = {
                sticker_packs: '18,154',
                calls_enabled: '1',
                calls_debug_record: 'true',
                calls_debug_falsy: 'false',
                calls_debug_number: '12345',
                calls_debug_string: 'value',
            };
        });

        it('Return correct object', () => {
            // @ts-ignore
            expect(callApi.getDebugOptions()).toStrictEqual({
                record: true,
                falsy: false,
                number: 12345,
                string: 'value',
            });
        });
    });
});
