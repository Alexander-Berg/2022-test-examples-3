import { classname } from '../utils/bem';

import { ChatWidget } from '../widget_light';
import { Gonec } from './gonec';
import { assertDefinedAndReturn } from './util';

let widget: ChatWidget;
let widgetNode;

const DEFAULT_GUID = 'default guid';
const DEFAULT_THEME = 'dark';

const mountNode = document.createElement('div');

const gonec = new Gonec();

document.body.appendChild(mountNode);

const DEFAULT_OPTIONS: Partial<ChatWidget.Options> = {
    guid: DEFAULT_GUID,
    theme: DEFAULT_THEME,
    mountNode,
};

afterEach(() => {
    widget.destroy();
    widgetNode = undefined;
    jest.restoreAllMocks();
});

describe('widget', () => {
    describe('dom', () => {
        beforeEach(() => {
            // @ts-ignore
            widget = new ChatWidget(DEFAULT_OPTIONS);
            widgetNode = document.body.querySelector(`.${classname('widget')}`);

            gonec.setWidget(widgetNode);
        });

        it('вставляет виджет на страницу', () => {
            expect(widgetNode).not.toBeNull();
        });

        it('монтирует iframe', () => {
            const mount = widgetNode.querySelector(`.${classname('widget', 'mount')}`);
            const actual = mount.querySelector('iframe');

            expect(actual).not.toBeNull();
        });

        it('удаляет ноду виджета из документа', () => {
            widget.destroy();

            expect(document.head.querySelector('style')).toBeNull();
            expect(document.body.querySelector(`.${classname('widget')}`)).toBeNull();
        });
    });

    describe('#handleMessengerMessage', () => {
        beforeEach(() => {
            // @ts-ignore
            widget = new ChatWidget(DEFAULT_OPTIONS);
            widgetNode = document.body.querySelector(`.${classname('widget')}`);

            gonec.setWidget(widgetNode);
        });

        it('вызывает widget.handleReadyMessage() при получении сообщения "ready" только один раз', async () => {
            await gonec.sendFromIframe({
                type: 'ready',
                namespace: 'messenger',
            });

            await gonec.sendFromIframe({
                type: 'ready',
                namespace: 'messenger',
            });

            expect(gonec.spy).toHaveBeenCalledTimes(1);
        });
    });

    describe('#getIframeUrl', () => {
        it('возвращает урл на iframe сборки серпа', () => {
            const dateMock = jest.spyOn(Date, 'now');

            const chatId = '8222f7a0-a8c4-4b8e-9869-4d37e3adb4e1_d9b982fb-6c24-490c-b7f4-5fdb80313bd4';

            widget = new ChatWidget({
                ...DEFAULT_OPTIONS,
                chatId,
            });

            const date = dateMock.mock.results[0].value;

            expect(assertDefinedAndReturn(document.body.querySelector('iframe')).src).toBe(
                `https://yandex.ru/chat?build=chamb&lang=ru&parentOrigin=http%3A%2F%2Flocalhost&parentUrl=http%3A%2F%2Flocalhost%2F&utm_source=widget&utm_medium=iframe&widgetId=${widget.widgetId}&widgetInitTimestamp=${date}&flags=newHeader%3D1%3Bembed_button%3D1%3BhideClose%3D1%3Btheme%3Ddark#/chats/8222f7a0-a8c4-4b8e-9869-4d37e3adb4e1_d9b982fb-6c24-490c-b7f4-5fdb80313bd4`,
            );

            dateMock.mockRestore();
        });

        it('возвращает урл на iframe сборки эфира', () => {
            const dateMock = jest.spyOn(Date, 'now');

            const chatId = '0/7/41982ecac610fe988f266cfde5ad5a08';

            widget = new ChatWidget({
                ...DEFAULT_OPTIONS,
                chatId,
            });

            const date = dateMock.mock.results[0].value;

            expect(assertDefinedAndReturn(document.body.querySelector('iframe')).src).toBe(
                `https://yandex.ru/chat?build=ether&lang=ru&parentOrigin=http%3A%2F%2Flocalhost&parentUrl=http%3A%2F%2Flocalhost%2F&utm_source=widget&utm_medium=iframe&widgetId=${widget.widgetId}&widgetInitTimestamp=${date}&flags=newHeader%3D1%3Bembed_button%3D1%3BhideClose%3D1%3Btheme%3Ddark#/chats/0%2F7%2F41982ecac610fe988f266cfde5ad5a08`,
            );
        });
    });
});
