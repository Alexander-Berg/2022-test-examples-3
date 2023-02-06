import { Deeplinker } from '../Deeplinker';

const triggerClickEvent = (window: Window) => {
    const linkTag = document.createElement('a');
    const clickEvent = new MouseEvent('click', { relatedTarget: linkTag });

    linkTag.href = 'https://yandex.ru/chat/';

    Object.defineProperty(clickEvent, 'target', { value: linkTag });

    window.dispatchEvent(clickEvent);
};

describe('#Deeplinker plugin', () => {
    let origWindow;
    let deeplinkerPlugin;
    let widgetIntanceMock;

    beforeAll(() => {
        origWindow = { ...global.window };
    });

    beforeEach(() => {
        deeplinkerPlugin = new Deeplinker();

        widgetIntanceMock = {
            show: jest.fn(),
        };
    });

    afterEach(() => {
        deeplinkerPlugin = undefined;

        global.window = { ...origWindow };
    });

    it('Instance create', () => {
        expect(deeplinkerPlugin).toBeInstanceOf(Deeplinker);
    });

    it('Plugin succesfully init', () => {
        deeplinkerPlugin.init({}, widgetIntanceMock);

        triggerClickEvent(window);

        expect(widgetIntanceMock.show).toHaveBeenCalledTimes(1);
    });

    it('Plugin succesfully destroyed', () => {
        deeplinkerPlugin.init({}, widgetIntanceMock);

        deeplinkerPlugin.LCClose();

        triggerClickEvent(window);

        expect(widgetIntanceMock.show).toHaveBeenCalledTimes(0);
    });
});
