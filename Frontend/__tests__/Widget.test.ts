import '@yandex-int/messenger.utils/lib_cjs/test/MessageChannel';

import { ChannelIncoming, CLOSE_CODE, WindowPostMessageTransport } from '@yandex-int/messenger.channels';
import { applyJSDOMPostMessageFix, rollbackJSDOMPostMessageFix } from '@yandex-int/messenger.utils';
import { IframeOpenParams } from '@yandex-int/messenger.sdk/private/ClientApi';

import { Widget } from '../Widget';
import { BeforeShowEvent } from '../types';
import { WidgetEvent } from '../libs/WidgetEvent/WidgetEvent';
import { MockedHooksFunctions, MockedPlugin, MockedUIPlugin } from './mocks';
import { Config } from '../config';

type Observables = {};

type Requests = {
    setVisibility(data: { visible: boolean }): any;
    iframeOpen(data: { visitId: string; eventTimestamp: number; clickId: string; chatId: string; }): any;
    serviceMeta(data: object): any;
};

type Events = {};

type Events2 = {};

type ScopeParams = {};

describe('#Widget', () => {
    let widget: Widget;
    let options: Config;
    let incomingChannel: ChannelIncoming<Requests, Requests, Events, Events2, Observables, Observables, ScopeParams>;

    let windowTransport: WindowPostMessageTransport<any>;
    let iframe: HTMLIFrameElement;

    const url = 'https://yandex.ru/chat';
    const origin = new URL(url).origin;

    const setVisibilitySpy = jest.fn();
    const iframeOpenSpy = jest.fn();
    const serviceMetaSpy = jest.fn();

    beforeAll(() => {
        applyJSDOMPostMessageFix();
    });

    afterAll(() => {
        rollbackJSDOMPostMessageFix();
    });

    beforeEach(() => {
        iframe = document.createElement('iframe');

        iframe.src = url;

        document.body.appendChild(iframe);

        jest.spyOn(navigator, 'onLine', 'get').mockReturnValueOnce(true);

        windowTransport = new WindowPostMessageTransport(
            iframe.contentWindow,
            origin,
            [origin],
        );

        options = new Config({
            origin: url,
            lang: 'ru',
            serviceId: 20,
        });

        widget = new Widget(options);

        incomingChannel = new ChannelIncoming<
            Requests,
            Requests,
            Events,
            Events2,
            Observables,
            Observables,
            ScopeParams>(
                widget.widgetId,
                windowTransport,
                () => {
                    return Promise.resolve({});
                },
            );

        windowTransport.connect();

        const mockedUIPlugin = new MockedUIPlugin(
            iframe.contentWindow,
            origin,
        );

        widget.setUI(mockedUIPlugin);

        incomingChannel.onRequest.addListener((event) => {
            if (event.type === 'iframeOpen') {
                iframeOpenSpy(event);
            }

            if (event.type === 'setVisibility') {
                setVisibilitySpy(event);
            }

            if (event.type === 'serviceMeta') {
                serviceMetaSpy(event);
            }
        });
    });

    afterEach(() => {
        incomingChannel.dispose();

        jest.spyOn(navigator, 'onLine', 'get').mockRestore();

        iframeOpenSpy.mockRestore();
        setVisibilitySpy.mockRestore();
        serviceMetaSpy.mockRestore();
    });

    it('Instance succesfully created', () => {
        expect(widget).toBeDefined();
    });

    it('Preload method must send registration event', (done) => {
        windowTransport.onMessage.addListener((event) => {
            expect(event.data.type).toBe('registration');

            done();
        });

        widget.init();
        widget.preload();
    });

    describe('#Show method', () => {
        it('Must pass params to iframeOpen', (done) => {
            const IframeOpenParams: IframeOpenParams = {
                chatId: '1',
                guid: '2',
                inviteHash: '3',
                username: 'smith',
            };

            iframeOpenSpy.mockImplementationOnce((event) => {
                expect(event.payload).toMatchObject({
                    ...IframeOpenParams,
                    visitId: widget.widgetId,
                });

                event.response(Promise.resolve());
            });

            setVisibilitySpy.mockImplementationOnce((event) => {
                expect(event.payload.visible).toBeTruthy();

                event.response(Promise.resolve());
            });

            widget.init();

            widget.show(IframeOpenParams, () => done());
        });

        it('if don`t pass params iframeOpen must call 1 time', () => {
            let visibilityCounter = 0;
            let openCounter = 0;

            setVisibilitySpy.mockImplementation((event) => {
                visibilityCounter++;

                event.response(Promise.resolve());
            });
            iframeOpenSpy.mockImplementation((event) => {
                openCounter++;

                event.response(Promise.resolve());
            });

            widget.init();

            widget.show(undefined, () => {
                widget.show(undefined, () => {
                    expect(visibilityCounter).toBe(2);
                    expect(openCounter).toBe(1);
                });
            });
        });

        it('if setVisibility rejected, then nothing happend', (done) => {
            setVisibilitySpy.mockImplementation((event) => {
                event.reject(new Error());
            });

            iframeOpenSpy.mockImplementation((event) => {
                event.response(Promise.resolve());
            });

            widget.init();

            widget.show(undefined, () => {
                done();
            });
        });

        it('if iframeOpen rejected, then callback accept as argument Error', (done) => {
            const testErr = new Error('Test error');

            setVisibilitySpy.mockImplementation((event) => {
                event.response(Promise.resolve());
            });

            iframeOpenSpy.mockImplementation((event) => {
                event.reject(testErr);
            });

            widget.init();

            widget.show(undefined, (err) => {
                expect(err).toEqual(testErr);

                done();
            });
        });
    });

    describe('#Hide method', () => {
        it('should not call setVisibility(false) when transport is closed', () => {
            widget.init();

            widget.hide();

            expect(setVisibilitySpy).toBeCalledTimes(0);
        });

        it('must call setVisivility with visible: false', (done) => {
            widget.init();

            iframeOpenSpy.mockImplementationOnce((event) => {
                event.response(Promise.resolve());
            });

            widget.show(undefined, () => {
                widget.hide();
                expect(setVisibilitySpy).toBeCalledTimes(1);

                done();
            });
        });

        it('if setVisibility rejected, then nothing happend', async() => {
            widget.init();

            setVisibilitySpy.mockImplementation((event) => {
                event.reject(new Error('Test error'));
            });

            serviceMetaSpy.mockImplementation((event) => {
                event.response(Promise.resolve());
            });

            widget.hide();

            // Для того, чтобы успела выпасть ошибка
            await widget.api.serviceMeta({});
        });
    });

    describe('#Plugins', () => {
        let plugin: MockedPlugin;
        let mockedHooksCallbacks: MockedHooksFunctions;

        beforeEach(() => {
            mockedHooksCallbacks = {
                initCallback: jest.fn(),
                LCBeforeShowCallback: jest.fn(),
                LCReadyCallback: jest.fn(),
                LCHiddenCallback: jest.fn(),
                LCBeforeHideCallback: jest.fn(),
                LCCloseCallback: jest.fn(),
                LCShownCallback: jest.fn(),
                LCBeforeRequestCallback: jest.fn(),
                LCErrorCriticalCallback: jest.fn(),
            };

            plugin = new MockedPlugin(mockedHooksCallbacks);
        });

        it('Must initialize correctly', () => {
            widget.addPlugin(plugin);
            widget.init();

            expect(mockedHooksCallbacks.initCallback).toBeCalledTimes(1);
            expect(mockedHooksCallbacks.initCallback).toBeCalledWith(options.values, widget);
        });

        it('Must initialize multiple plugins', () => {
            widget.addPlugin(plugin);

            const anotherMockedHooks: MockedHooksFunctions = {
                initCallback: jest.fn(),
            };

            const anotherPlugin = new MockedPlugin(anotherMockedHooks);

            widget.addPlugin(anotherPlugin);

            widget.init();

            expect(mockedHooksCallbacks.initCallback).toBeCalledTimes(1);
            expect(anotherMockedHooks.initCallback).toBeCalledTimes(1);
        });

        it('LCReady dispatched on dispatch ready event', () => {
            widget.addPlugin(plugin);

            widget.init();

            widget.events.ready.dispatch(new WidgetEvent());

            expect(mockedHooksCallbacks.LCReadyCallback).toBeCalledTimes(1);
        });

        it('LCHidden dispatched on dispatch close event', () => {
            widget.addPlugin(plugin);

            widget.init();

            widget.events.close.dispatch(new WidgetEvent());

            expect(mockedHooksCallbacks.LCHiddenCallback).toBeCalledTimes(1);
        });

        it('LCHidden dispatched on call widget hide method', () => {
            widget.addPlugin(plugin);

            widget.hide();

            expect(mockedHooksCallbacks.LCHiddenCallback).toBeCalledTimes(1);
        });

        it('LCBeforeHide dispatched on call hide widget method', () => {
            widget.addPlugin(plugin);

            widget.hide();

            expect(mockedHooksCallbacks.LCBeforeHideCallback).toBeCalledTimes(1);
        });

        it('LCClose dispatched on call widget destroy method', () => {
            widget.addPlugin(plugin);

            widget.init();

            widget.destroy();

            expect(mockedHooksCallbacks.LCCloseCallback).toBeCalledTimes(1);
        });

        it('LCBeforeShow dispatched on call widget show method', () => {
            widget.addPlugin(plugin);

            widget.init();

            widget.show();

            expect(mockedHooksCallbacks.LCBeforeShowCallback).toBeCalledTimes(1);
        });

        it('LCShown dispatched on call widget show method', () => {
            widget.addPlugin(plugin);

            widget.init();

            widget.show();

            expect(mockedHooksCallbacks.LCShownCallback).toBeCalledTimes(1);
        });

        it('If LCBeforeShow has been prevented, then LCShown shouldn`t been called', () => {
            const anotherMockedHooks: MockedHooksFunctions = {
                initCallback: jest.fn(),
                LCBeforeShowCallback: jest.fn((event: BeforeShowEvent) => { event.preventDefault() }),
                LCShownCallback: jest.fn(),
            };
            const mockPlugin = new MockedPlugin(anotherMockedHooks);

            widget.addPlugin(mockPlugin);

            widget.init();

            widget.show();

            expect(anotherMockedHooks.LCBeforeShowCallback).toBeCalledTimes(1);
            expect(anotherMockedHooks.LCShownCallback).toBeCalledTimes(0);
        });

        it('LCBeforeRequest dispatced on api call', () => {
            widget.addPlugin(plugin);

            widget.init();

            widget.api.serviceMeta({});
            widget.api.pasteMessage({ chatId: '', guid: '', pasteText: '' });
            widget.api.setVisibility({ visible: true });
            widget.api.setThemeVariables({ themeVariables: { value: {} } });
            widget.api.chatMetadata({ guid: '', chatId: '', inviteHash: '', username: '' });

            expect(mockedHooksCallbacks.LCBeforeRequestCallback).toBeCalledTimes(5);
        });

        it('LCErrorCritical dispatched on criticalTransport error', async() => {
            widget.addPlugin(plugin);

            widget.init();

            serviceMetaSpy.mockImplementation((event) => {
                event.response(Promise.resolve());
            });

            await widget.api.serviceMeta({});

            incomingChannel.close({ closeCode: CLOSE_CODE.ERROR, error: new Error('Test error') });

            try {
                // Для того, чтобы успел закрыться транспорт
                await widget.api.serviceMeta({});
            } catch (error) {}

            expect(mockedHooksCallbacks.LCErrorCriticalCallback).toBeCalledTimes(1);
        });
    });
});
