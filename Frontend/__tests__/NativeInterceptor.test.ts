import { isDefined, nextTick } from '@yandex-int/messenger.utils';
import { WidgetEvent } from '../../../libs/WidgetEvent/WidgetEvent';
import { NativeInterceptor } from '../NativeInterceptor';
import * as IWidget from '../../../types';
import { clearGetFeatureInfoMemo } from '../../../helpers';
import { Config } from '../../../config';

function assertWidgetOptions(config: Partial<IWidget.Options>): asserts config is IWidget.Options {
    if (!isDefined(config.serviceId)) {
        throw new Error('serviceId was not defined');
    }
}

describe('#NativeInterceptor', () => {
    let windowSpy;
    let interceptor: NativeInterceptor;
    let beforeShowPreventDefault: jest.Mock;
    let beforeRequestPreventDefault: jest.Mock;
    let locationReplace: jest.Mock;
    let beforeRequestPreventDefaultCaller: jest.Mock;
    let beforeShowEvent: IWidget.BeforeShowEvent;
    let beforeRequestEvent: IWidget.BeforeRequestEvent;
    const options = new Config({
        serviceId: 1,
    }).values as IWidget.Options;

    assertWidgetOptions(options);

    beforeEach(() => {
        windowSpy = jest.spyOn(window, 'window', 'get');
        interceptor = new NativeInterceptor();
        locationReplace = jest.fn();
        beforeShowPreventDefault = jest.fn();
        beforeRequestPreventDefaultCaller = jest.fn();
        beforeRequestPreventDefault = jest.fn((cb) => {
            if (cb && 'catch' in cb) {
                cb.catch(beforeRequestPreventDefaultCaller);
            }
        });
        beforeShowEvent = new WidgetEvent();
        beforeRequestEvent = new WidgetEvent();
        beforeShowEvent.preventDefault = beforeShowPreventDefault;
        beforeRequestEvent.preventDefault = beforeRequestPreventDefault;
    });

    afterEach(() => {
        windowSpy.mockRestore();
        clearGetFeatureInfoMemo();
    });

    it('should do nothing', () => {
        interceptor.init(options);

        interceptor.LCBeforeShow(beforeShowEvent);
        interceptor.LCBeforeRequest(beforeRequestEvent);

        expect(beforeShowPreventDefault).toBeCalledTimes(0);
        expect(beforeRequestPreventDefault).toBeCalledTimes(0);
    });

    it('should intercept before show method when YandexApplicationsFeatureAvailability present on window', async() => {
        windowSpy.mockImplementation(() => ({
            YandexApplicationsFeatureAvailability: {
                getFeatureInfo() {
                    return {
                        isAvailable: true,
                        params: {},
                    };
                },
            },
            location: {
                replace: locationReplace,
            },
        }));

        interceptor.init(options);

        interceptor.LCBeforeRequest(beforeRequestEvent);

        await nextTick();

        expect(beforeRequestPreventDefaultCaller).toBeCalledTimes(1);

        interceptor.LCBeforeShow(beforeShowEvent);

        expect(locationReplace).toBeCalledTimes(1);
        expect(locationReplace).toBeCalledWith('messenger://chat/list?serviceId=1');
        expect(beforeShowPreventDefault).toBeCalledTimes(1);
        expect(beforeRequestPreventDefault).toBeCalledTimes(1);
    });

    [
        {
            title: 'should construct proper deeplink with chatId',
            availablePath: 'messenger://chat/open',
            data: { chatId: '123' },
            expectedDeeplink: 'messenger://chat/open?chat_id=123&serviceId=1',
        },
        {
            title: 'should construct proper deeplink with invite hash',
            availablePath: 'messenger://chat/invite_byhash',
            data: { inviteHash: '123' },
            expectedDeeplink: 'messenger://chat/invite_byhash?invite_hash=123&serviceId=1',
        },
        {
            title: 'should construct proper deeplink with user',
            availablePath: 'messenger://user/',
            data: { guid: '123' },
            expectedDeeplink: 'messenger://user/?user_id=123&serviceId=1',
        },
        {
            title: 'should construct proper deeplink with chat list',
            availablePath: '',
            data: {},
            expectedDeeplink: 'messenger://chat/list?serviceId=1',
        },
        {
            title: 'should construct proper deeplink with context',
            availablePath: '',
            data: { context: '%7B%22context%22%3A%7B%22A%22%3A%20%22s%22%7D%7D' },
            expectedDeeplink: 'messenger://chat/list?context=%257B%2522context%2522%253A%257B%2522A%2522%253A%2520%2522s%2522%257D%257D&serviceId=1',
        },
    ].forEach(({ title, availablePath, data, expectedDeeplink }) => {
        it(title, async() => {
            windowSpy.mockImplementation(() => ({
                YandexApplicationsFeatureAvailability: {
                    getFeatureInfo() {
                        return {
                            isAvailable: true,
                            params: {
                                'available-paths': [
                                    availablePath,
                                ],
                            },
                        };
                    },
                },
                location: {
                    replace: locationReplace,
                } as any,
            }));

            interceptor.init(options);

            interceptor.LCBeforeRequest(beforeRequestEvent);

            await nextTick();

            expect(beforeRequestPreventDefaultCaller).toBeCalledTimes(1);

            beforeShowEvent.data = data as any;

            interceptor.LCBeforeShow(beforeShowEvent);

            expect(locationReplace).toBeCalledTimes(1);
            expect(locationReplace).toBeCalledWith(expectedDeeplink);
            expect(beforeShowPreventDefault).toBeCalledTimes(1);
            expect(beforeRequestPreventDefault).toBeCalledTimes(1);
        });
    });

    it('should intercept before show method when YandexApplicationsFeatureAvailability present on window.top', async() => {
        windowSpy.mockImplementation(() => ({
            top: {
                YandexApplicationsFeatureAvailability: {
                    getFeatureInfo() {
                        return {
                            isAvailable: true,
                            params: {},
                        };
                    },
                },
            },
            location: {
                replace: locationReplace,
            } as any,
        }));

        interceptor.init(options);

        interceptor.LCBeforeRequest(beforeRequestEvent);

        await nextTick();

        expect(beforeRequestPreventDefaultCaller).toBeCalledTimes(1);

        interceptor.LCBeforeShow(beforeShowEvent);

        expect(locationReplace).toBeCalledTimes(1);
        expect(locationReplace).toBeCalledWith('messenger://chat/list?serviceId=1');
        expect(beforeShowPreventDefault).toBeCalledTimes(1);
        expect(beforeRequestPreventDefault).toBeCalledTimes(1);
    });
});
