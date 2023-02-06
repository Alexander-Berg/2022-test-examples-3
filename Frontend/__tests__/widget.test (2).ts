import { classname } from '../utils/bem';

jest.mock('../utils/device', () => ({
    isMobile: false,
}));

// eslint-disable-next-line import/first
import { ChatWidget } from '../widget';
// eslint-disable-next-line import/first
import { Gonec } from './gonec';
// eslint-disable-next-line import/first
import { Helper } from './helper';
// eslint-disable-next-line import/first
import { assertDefinedAndReturn } from './util';

const DEFAULT_GUID = 'default guid';
const DEFAULT_THEME = 'dark';
const DEFAULT_BUTTON_TEXT = 'default greeting';
const DEFAULT_TITLE = 'default header text';
const DEFAULT_COLLAPSED_TOUCH = 'always';
const DEFAULT_COLLAPSED_DESKTOP = 'always';
const DEFAULT_BODY_OVERFLOW_X = 'hidden';
const DEFAULT_BODY_OVERFLOW_Y = 'scroll';

const DEFAULT_OPTIONS = {
    autocloseable: true,
    badgeType: 'num',
    badgeMaxCount: 99,
    guid: DEFAULT_GUID,
    theme: DEFAULT_THEME,
    buttonText: DEFAULT_BUTTON_TEXT,
    title: DEFAULT_TITLE,
    collapsedTouch: DEFAULT_COLLAPSED_TOUCH,
    collapsedDesktop: DEFAULT_COLLAPSED_DESKTOP,
};

let widget: ChatWidget;
let widgetNode: HTMLElement;
let buttonNode: HTMLElement;
let popupNode: HTMLElement;
let headerNode: HTMLElement;

const gonec = new Gonec();
const helper = new Helper(gonec);

beforeEach(() => {
    // @ts-ignore
    widget = new ChatWidget(DEFAULT_OPTIONS);
    widgetNode = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('widget')}`));
    buttonNode = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('button')}`));
    popupNode = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('popup')}`));
    headerNode = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('header')}`));

    gonec.setWidget(widgetNode);
    helper.setWidget(widget);
});

afterEach(() => {
    widget.destroy();
});

describe('widget', () => {
    describe('render()', () => {
        it('вставляет виджет на страницу', () => {
            expect(widgetNode).not.toBeNull();
        });

        it('текст приветствия соответствует передаваемому', () => {
            const textContainer = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('button', 'text-container')}`));

            const actual = textContainer.innerHTML;
            const expected = DEFAULT_BUTTON_TEXT;

            expect(actual).toBe(expected);
        });

        it('кнопка имеет модификатор "dark", когда в конструктор виджета передана тема оформления "dark"', () => {
            const actual = buttonNode.classList.contains(classname('button', { theme: 'dark' }));

            expect(actual).toBe(true);
        });

        it('кнопка виджета имеет модификатор "button_collapsed_never" при передачи свойства collapsedDesktop = "never"', () => {
            widget.destroy();

            // @ts-ignore
            widget = new ChatWidget({
                ...DEFAULT_OPTIONS,
                collapsedDesktop: 'never',
            });

            buttonNode = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('button')}`));

            const actual = buttonNode.classList.contains(classname('button', { collapsed: 'never' }));
            expect(actual).toBe(true);
        });

        it('виджет имеет модификатор "button_collapsed" при передачи свойства collapsedDesktop = "always"', () => {
            widget.destroy();

            // @ts-ignore
            widget = new ChatWidget({
                ...DEFAULT_OPTIONS,
                collapsedDesktop: 'always',
            });

            buttonNode = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('button')}`));

            const actual = buttonNode.classList.contains(classname('button', { collapsed: 'always' }));
            expect(actual).toBe(true);
        });
    });

    describe('handleButtonClick()', () => {
        it('по клику по кнопке разворачивает чат', () => {
            buttonNode.click();

            const buttonHidden = buttonNode.classList.contains(classname('button', { hidden: true }));
            expect(buttonHidden).toBe(true);
        });

        it('по второму клику по кнопке сворачивает чат', () => {
            buttonNode.click();

            const visible = popupNode.classList.contains(classname('popup', { visible: true }));
            expect(visible).toBe(true);

            buttonNode.click();

            const notVisible = popupNode.classList.contains(classname('popup', { visible: true }));
            expect(notVisible).toBe(false);
        });
    });

    describe('автозакрытие чата', () => {
        it('при клике не в виджет чат закрывается', () => {
            buttonNode.click();
            document.body.click();

            const visible = popupNode.classList.contains(classname('popup', { visible: true }));
            expect(visible).toBe(false);
        });

        it('при клике не в виджет чат закрывается, hideChat вызывается один раз', () => {
            const spy = jest.spyOn(widget, 'hideChat');

            buttonNode.click();

            document.body.click();
            document.body.click();
            document.body.click();

            expect(spy).toHaveBeenCalledTimes(1);
        });

        it('при клике не в виджет чат не закрывается, если установлено свойство autocloseable: false', () => {
            widget.destroy();

            // @ts-ignore
            const widget2 = new ChatWidget({
                ...DEFAULT_OPTIONS,
                autocloseable: false,
            });

            widget2.showChat();

            document.body.click();

            const popupNode2 = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('popup')}`));
            const visible = popupNode2.classList.contains(classname('popup', { visible: true }));
            expect(visible).toBe(true);

            widget2.destroy();
        });
    });

    describe('createMessengerIframe()', () => {
        it('монтирует iframe', () => {
            widget.showChat();

            const mountNode = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('widget', 'mount')}`));
            const actual = mountNode.querySelector('iframe');

            expect(actual).not.toBeNull();
        });
    });

    describe('destroy()', () => {
        it('удаляет ноду виджета из документа', () => {
            widget.destroy();

            expect(document.head.querySelector('style')).toBeNull();
            expect(document.body.querySelector(`.${classname('widget')}`)).toBeNull();
        });
    });

    describe('showChat()', () => {
        it('при раскрытии виджета добавляет модификатор "popup_visible"', () => {
            // сначала проверяем, что чат свернут
            expect(popupNode.classList.contains(classname('popup', { visible: true }))).toBeFalsy();

            widget.showChat();

            expect(popupNode.classList.contains(classname('popup', { visible: true }))).toBeTruthy();
        });
    });

    describe('hideChat()', () => {
        it('при сворачивании виджета удаляет модификатор "popup_visible"', () => {
            buttonNode.click();

            expect(popupNode.classList.contains(classname('popup', { visible: true }))).toBeTruthy();

            widget.hideChat();

            expect(popupNode.classList.contains(classname('popup', { visible: true }))).toBeFalsy();
        });
    });

    describe('handleMessengerMessage()', () => {
        let dateMock;

        beforeEach(() => {
            dateMock = jest.spyOn(Date, 'now');
        });

        afterEach(() => {
            dateMock.mockRestore();
        });

        it('вызывает widget.hideChat() при событии "close"', async () => {
            const spy = jest.fn();

            widget.addListener('chatHide', spy);
            widget.showChat();

            await gonec.sendFromIframe({
                namespace: 'messenger',
                type: 'close',
            });

            expect(spy).toHaveBeenCalledTimes(1);
        });

        it('отправляет сообщение "iframe-open" с guid из showChat()', async () => {
            await helper.showChat({ guid: '123' });

            const eventTimestamp = dateMock.mock.results[1].value;

            expect(gonec.spy).toHaveBeenCalledTimes(1);
            expect(gonec.spy).toHaveBeenCalledWith(expect.objectContaining({
                namespace: 'messenger',
                payload: {
                    guid: '123',
                    parentUrl: 'http://localhost/',
                    visitId: widget.widgetId,
                    clickId: expect.any(String),
                    initial: true,
                    eventTimestamp,
                },
                type: 'iframe-open',
            }), 'https://yandex.ru');
        });

        it('отправляет сообщение "iframe-open" с guid, установленным в конструкторе, при вызове showChat() без параметров', async () => {
            await helper.showChat();

            const eventTimestamp = dateMock.mock.results[1].value;

            expect(gonec.spy).toHaveBeenCalledTimes(1);
            expect(gonec.spy).toHaveBeenCalledWith(expect.objectContaining({
                namespace: 'messenger',
                payload: {
                    guid: 'default guid',
                    parentUrl: 'http://localhost/',
                    visitId: widget.widgetId,
                    clickId: expect.any(String),
                    initial: true,
                    eventTimestamp,
                },
                type: 'iframe-open',
            }), 'https://yandex.ru');
        });
    });

    describe('handleReadyMessage()', () => {
        it('не вызывает widget.handleReadyMessage(), если у сообщения нет свойства namespace="messenger"', async () => {
            widget.showChat();

            await gonec.sendFromIframe({
                type: 'ready',
            });

            expect(gonec.spy).not.toHaveBeenCalled();
        });

        it('вызывает widget.handleReadyMessage() при получении сообщения "ready" только один раз', async () => {
            await widget.showChat();

            await gonec.sendFromIframe({
                type: 'ready',
            });

            expect(gonec.spy).toHaveBeenCalledTimes(0);

            await gonec.sendFromIframe({
                namespace: 'messenger',
                type: 'ready',
            });

            expect(gonec.spy).toHaveBeenCalledTimes(1);
        });
    });

    describe('handleUnreadCounter()', () => {
        it('устанавливает правильное значение в бадж непрочитанных', async () => {
            await helper.showChat();

            widget.hideChat();

            const payload = { value: 10 };

            await gonec.sendFromIframe({
                namespace: 'messenger',
                type: 'counter',
                payload,
            });

            const badgeNode = assertDefinedAndReturn(document.body.querySelector<HTMLElement>(`.${classname('badge', 'count')}`));

            expect(badgeNode.innerText).toBe(`${payload.value}`);
        });

        it('не вызывает дважды событие unreadCounterChange при одинаковом значении', async () => {
            const spy = jest.fn();

            widget.addListener('unreadCounterChange', spy);

            await helper.showChat();

            widget.hideChat();

            const payload = { value: 10, valueForChat: 0, lastTimestamp: 0, seqnoForChat: 0 };

            await gonec.sendFromIframe({
                namespace: 'messenger',
                type: 'counter',
                payload,
            });

            await gonec.sendFromIframe({
                namespace: 'messenger',
                type: 'counter',
                payload,
            });

            expect(spy).toHaveBeenCalledTimes(1);
            expect(spy).toHaveBeenCalledWith(payload);

            await gonec.sendFromIframe({
                namespace: 'messenger',
                type: 'counter',
                payload: {
                    value: 10,
                },
            });

            expect(spy).toHaveBeenCalledTimes(1);
        });
    });

    describe('Заголовок', () => {
        it('при пустом title должен выставляться модификатор _empty', () => {
            expect(headerNode.classList.contains(classname('header', { empty: true }))).toBeFalsy();

            widget.destroy();

            // @ts-ignore
            const widget2 = new Ya.ChatWidget({
                ...DEFAULT_OPTIONS,
                title: '',
            });

            widget2.showChat();

            const headerNode2 = document.body.querySelector(`.${classname('header')}`) as HTMLElement;

            expect(headerNode2.classList.contains(classname('header', { empty: true }))).toBeTruthy();

            widget2.destroy();
        });
    });

    describe('setFullscreen()', () => {
        it('при раскрытии виджета на весь экран скрывается шапка в виджете', async () => {
            // сначала проверяем, что шапка не скрыта
            expect(headerNode.classList.contains(classname('header', { hidden: true }))).toBeFalsy();

            await helper.showChat();

            await gonec.sendFromIframe({
                namespace: 'messenger',
                type: 'fullscreenOn',
            });

            expect(headerNode.classList.contains(classname('header', { hidden: true }))).toBeTruthy();
            expect(popupNode.classList.contains(classname('popup', { fullscreen: true }))).toBeTruthy();
        });

        it('вызывается метод setFullscreen у попапа', async () => {
            await helper.setFullscreen(true);

            expect(popupNode.classList.contains(classname('popup', { fullscreen: true }))).toBeTruthy();
        });

        it('body имеет overflow-x = overflow-y = hidden в fullscreen режиме', async () => {
            // Высталяем исходное значение для overflow
            document.body.style.overflowX = DEFAULT_BODY_OVERFLOW_X;
            document.body.style.overflowY = DEFAULT_BODY_OVERFLOW_Y;

            await helper.setFullscreen(true);

            expect(document.body.style.overflowX).toEqual('hidden');
            expect(document.body.style.overflowY).toEqual('hidden');
        });
    });

    describe('cancelFullscreen()', () => {
        it('при возвращении из полноэкранного режима, возвращается шапка виджета', async () => {
            await helper.setFullscreen(true);

            // сначала проверяем, что шапка не скрыта
            expect(headerNode.classList.contains(classname('header', { hidden: true }))).toBeTruthy();

            await helper.cancelFullscreen();

            expect(popupNode.classList.contains(classname('header', { hidden: true }))).toBeFalsy();
            expect(popupNode.classList.contains(classname('popup', { fullscreen: true }))).toBeFalsy();
        });

        it('body получает исходные значения overflow-x и overflow-y после выхода из fullscreen режима', async () => {
            // Высталяем исходное значение для overflow
            document.body.style.overflowX = DEFAULT_BODY_OVERFLOW_X;
            document.body.style.overflowY = DEFAULT_BODY_OVERFLOW_Y;

            await helper.setFullscreen(true);
            await helper.cancelFullscreen();

            expect(document.body.style.overflowX).toEqual(DEFAULT_BODY_OVERFLOW_X);
            expect(document.body.style.overflowY).toEqual(DEFAULT_BODY_OVERFLOW_Y);
        });
    });

    describe('sendServiceMeta()', () => {
        it('добавляет отложенное сообщение в список, когда чат ещё не создан', async () => {
            const message1 = { payload: 'bar' };
            const message2 = { payload: 'test' };

            await helper.showChat(undefined, false);

            widget.sendChatMetadata(message1);
            widget.sendServiceMeta(message2);

            expect(gonec.spy).toHaveBeenCalledTimes(0);

            await helper.ready();

            expect(gonec.spy).toHaveBeenCalledTimes(3);
            expect(gonec.spy).toHaveBeenNthCalledWith(1, expect.objectContaining({ type: 'iframe-open' }), expect.any(String));
            expect(gonec.spy).toHaveBeenNthCalledWith(
                2,
                expect.objectContaining({ type: 'chatMetadata', payload: expect.objectContaining(message1) }),
                expect.any(String),
            );

            expect(gonec.spy).toHaveBeenNthCalledWith(
                3,
                expect.objectContaining({ type: 'serviceMeta', payload: expect.objectContaining(message2) }),
                expect.any(String),
            );
        });

        it('заменяет отложенные сообщения serviceMeta на последнее', async () => {
            const message1 = { payload: 'bar' };
            const message2 = { payload: 'test' };

            await helper.showChat(undefined, false);

            widget.sendServiceMeta(message1);
            widget.sendServiceMeta(message2);

            expect(gonec.spy).toHaveBeenCalledTimes(0);

            await helper.ready();

            expect(gonec.spy).toHaveBeenCalledTimes(2);
            expect(gonec.spy).toHaveBeenNthCalledWith(1, expect.objectContaining({ type: 'iframe-open' }), expect.any(String));
            expect(gonec.spy).toHaveBeenNthCalledWith(
                2,
                expect.objectContaining({ type: 'serviceMeta', payload: expect.objectContaining(message2) }),
                expect.any(String),
            );
        });
    });
});
