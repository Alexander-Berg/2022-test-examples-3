import { UslugiFormsLoader, CLASSNAME, NAMESPACE } from './index';

let loader: UslugiFormsLoader;
let iframe: HTMLIFrameElement | null;

const mockOnMessage = jest.fn();
const mockOnError = jest.fn();
const mockOnReady = jest.fn();

const container = document.createElement('div');
document.body.appendChild(container);

beforeEach(() => {
    loader = new UslugiFormsLoader({
        className: 'my-component',
        container,
        url: 'about:blank',
        onError: mockOnError,
        onMessage: mockOnMessage,
        onReady: mockOnReady,
    });

    loader.createIframe();
    iframe = document.body.querySelector(`.${CLASSNAME}`);
});

afterEach(() => {
    loader.destroy();
});

describe('loader', () => {
    describe('createIframe()', () => {
        it('добавляет параметр parentOrigin в src Iframe', () => {
            expect(iframe!.src).toBe('about:blank?parentOrigin=http%3A%2F%2Flocalhost');
        });

        it('выставляет правильный className', () => {
            expect(iframe!.className).toBe(`${CLASSNAME} my-component`);
        });
    });

    describe('hideIframe()', () => {
        it('скрывает iframe', () => {
            loader.showIframe();
            expect(iframe!.style.display).toBe('block');

            loader.hideIframe();
            expect(iframe!.style.display).toBe('none');
        });
    });

    describe('sendMessage()', () => {
        it('сразу отправляет событие в Iframe, если он готов (было событие ready)', () => {
            const spy = jest.spyOn(loader, 'sendMessageToIframe');

            window.postMessage({
                type: 'ready',
                namespace: NAMESPACE,
            }, '*');

            setTimeout(() => {
                loader.sendMessage({
                    form: 'evacuator',
                    type: 'setInitialData',
                    payload: {},
                });

                expect(spy).toHaveBeenCalledTimes(1);
            }, 0);
        });

        it('сохраняет события в буфер, если ещё Iframe не готов (не было события ready)', () => {
            loader.sendMessage({
                form: 'evacuator',
                type: 'setInitialData',
                payload: {},
            });
            expect(loader.delayedMessages.length).toBe(1);
        });
    });

    describe('onMessage()', () => {
        it('вызывает onMessage() при получении события ready из Iframe', () => {
            window.postMessage({
                type: 'ready',
                namespace: NAMESPACE,
            }, '*');

            setTimeout(() => {
                expect(mockOnReady).toHaveBeenCalled();
            }, 0);
        });

        it('не вызывает onMessage() при получении события не для загрузчика', () => {
            window.postMessage({
                type: 'ready',
            }, '*');

            setTimeout(() => {
                expect(mockOnReady).not.toHaveBeenCalled();
            }, 0);
        });
    });

    describe('destroy()', () => {
        it('удаляет iframe из document', () => {
            loader.destroy();

            expect(document.body.querySelector(`.${CLASSNAME}`)).toBeNull();
        });
    });
});
