/* eslint-disable import/first */
jest.mock('../../History', () => {});
jest.mock('../DevicesProvider');
jest.mock('react-loadable', () => {
    return {
        default: {
            Map: () => () => null,
        },
    };
});

import device from '../../../components/Root/device';
import emitter from '../../../emitter';
import CallController from '../../CallController';

jest.mock('../../../components/Root/device');
jest.mock('../../../emitter');
jest.mock('../../../listeners/assistantListeners');

describe('CallController', () => {
    describe('#isSupported', () => {
        beforeEach(() => {
            // @ts-ignore
            window.flags = {
                calls: '1',
            };

            device.isDesktop = true;

            // @ts-ignore
            global.window.navigator.mediaDevices = {
                getUserMedia: jest.fn(),
            };

            // @ts-ignore
            Object.defineProperty(global.window.navigator, 'userAgent', {
                value: 'Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36',
                configurable: true,
            });
        });

        it('Returns true for Chrome', () => {
            // @ts-ignore
            expect(CallController.isSupported()).toBeTruthy();
        });

        it('Returns true for YaBro', () => {
            // @ts-ignore
            Object.defineProperty(global.window.navigator, 'userAgent', {
                value: 'Mozilla/5.0 AppleWebKit/537.36 Chrome/74.0.3729.169 YaBrowser/19.6.0.1583 Yowser/2.5 Safari/537.36',
            });

            // @ts-ignore
            expect(CallController.isSupported()).toBeTruthy();
        });

        it('Returns true for Firefox', () => {
            // @ts-ignore
            Object.defineProperty(global.window.navigator, 'userAgent', {
                value: 'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:64.0) Gecko/20100101 Firefox/64.0',
            });

            // @ts-ignore
            expect(CallController.isSupported()).toBeTruthy();
        });

        it('Returns false for Edge', () => {
            // @ts-ignore
            Object.defineProperty(global.window.navigator, 'userAgent', {
                value: 'Mozilla/5.0 AppleWebKit/537.36 (KHTML like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14931',
            });

            // @ts-ignore
            expect(CallController.isSupported()).toBeFalsy();
        });

        it('Returns false if device is not desktop', () => {
            device.isDesktop = false;

            // @ts-ignore
            expect(CallController.isSupported()).toBeFalsy();
        });

        it('Returns false if WebRTC not supported', () => {
            // @ts-ignore
            global.window.navigator.mediaDevices.getUserMedia = undefined;

            // @ts-ignore
            expect(CallController.isSupported()).toBeFalsy();
        });

        it('Returns false if calls disabled', () => {
            // @ts-ignore
            window.flags.calls = '0';

            // @ts-ignore
            expect(CallController.isSupported()).toBeFalsy();
        });
    });

    describe('#checkMediaAccess', () => {
        let originalEmit;

        beforeEach(() => {
            // @ts-ignore
            global.process.env.DESKTOP = 1;

            originalEmit = emitter.emit;
        });

        afterEach(() => {
            emitter.emit = originalEmit;
        });
    });
});
