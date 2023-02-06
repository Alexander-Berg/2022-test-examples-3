import { applyJSDOMPostMessageFix, rollbackJSDOMPostMessageFix, scenarioFactory, pause } from '@yandex-int/messenger.utils';
import { IframePostMessageTransport } from '../IframePostMessageTransport';
import { WindowPostMessageTransport } from '../WindowPostMessageTransport';

describe('WindowPostMessateTransport', () => {
    const url = 'https://yandex.ru/chat';
    const origin = new URL(url).origin;
    let innerTransport: IframePostMessageTransport<any>;
    let outerTransport: WindowPostMessageTransport<any>;
    let iframe: HTMLIFrameElement;

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

        innerTransport = new IframePostMessageTransport(
            iframe.contentWindow,
            origin,
        );

        outerTransport = new WindowPostMessageTransport(
            iframe.contentWindow,
            origin,
            [origin],
        );

        innerTransport.connect();
        outerTransport.connect();
    });

    afterEach(() => {
        innerTransport.dispose();
        outerTransport.dispose();
    });

    it('Message should be sent only after ping', async() => {
        const scenario = scenarioFactory(
            (spy, i) => expect(spy).nthCalledWith(i, expect.objectContaining({
                data: '@@@@ping',
            })),
            (spy, i) => expect(spy).nthCalledWith(i, expect.objectContaining({
                data: '@@@@ping',
            })),
            (spy, i) => expect(spy).nthCalledWith(i, expect.objectContaining({
                data: '@@@@pong',
            })),
            (spy, i) => expect(spy).nthCalledWith(i, expect.objectContaining({
                data: '@@@@pong',
            })),
            (spy, i) => expect(spy).nthCalledWith(i, expect.objectContaining({
                data: 'hello world',
            })),
        );

        const scenario2 = scenarioFactory(
            (spy, i) => expect(spy).nthCalledWith(i, expect.objectContaining({
                data: 'hello world',
            })),
        );

        window.addEventListener('message', scenario.handle);
        iframe.contentWindow.addEventListener('message', scenario.handle);

        outerTransport.onMessage.addListener(scenario2.handle);

        innerTransport.send('hello world');

        await Promise.all([
            scenario.untilDone,
            scenario2.untilDone,
        ]);

        window.removeEventListener('message', scenario.handle);
        iframe.contentWindow.removeEventListener('message', scenario.handle);
    });

    it('Should receive messages', async() => {
        const scenario = scenarioFactory(
            (spy, i) => expect(spy).nthCalledWith(i, expect.objectContaining({
                data: 'hello world',
            })),
        );

        innerTransport.onMessage.addListener(scenario.handle);

        outerTransport.send('hello world');

        await scenario.untilDone;
    });

    it('Messages from not allowed origins should be skiped', () => {
        const spyOn = jest.spyOn(innerTransport.onMessage, 'dispatch');

        iframe.contentWindow.parent.dispatchEvent(new MessageEvent('message', {
            data: '@@@@ping',
            origin: 'https://evil.com',
            source: iframe.contentWindow,
        }));

        iframe.contentWindow.parent.dispatchEvent(new MessageEvent('message', {
            data: 'hello world',
            origin: 'https://eveil.com',
            source: iframe.contentWindow,
        }));

        expect(spyOn).toBeCalledTimes(0);
    });

    it('onClose should be called when close()', async function() {
        const spyOn = jest.spyOn(innerTransport.onMessage, 'dispatch');
        const openScenario = scenarioFactory(
            (spy, i) => expect(spy).nthCalledWith(i, { reconnect: false }),
        );

        innerTransport.onOpen.addListener(openScenario.handle);

        await openScenario.untilDone;

        expect(innerTransport.connectionStatus).toBe('opened');
        expect(outerTransport.connectionStatus).toBe('opened');

        const scenario = scenarioFactory(
            (spy, i) => expect(spy).nthCalledWith(i, undefined),
        );

        innerTransport.onClose.addListener(scenario.handle);

        innerTransport.close();

        await scenario.untilDone;

        expect(innerTransport.connectionStatus).toBe('closed');
        iframe.contentWindow.parent.postMessage('hello world', origin);

        await pause(10);

        expect(innerTransport.connectionStatus).toBe('closed');

        expect(spyOn).toBeCalledTimes(0);
    });

    it('transport should be died when dispose()', async function() {
        const spyOn = jest.spyOn(innerTransport.onMessage, 'dispatch');
        const scenario = scenarioFactory(
            (spy, i) => expect(spy).nthCalledWith(i, undefined),
        );

        innerTransport.onClose.addListener(scenario.handle);

        innerTransport.dispose();

        await scenario.untilDone;

        iframe.contentWindow.parent.postMessage('@@@@ping', origin);
        iframe.contentWindow.parent.postMessage('hello world', origin);

        await pause(10);

        expect(spyOn).toBeCalledTimes(0);
    });
});
