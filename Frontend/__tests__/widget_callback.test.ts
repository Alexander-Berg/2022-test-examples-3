import {
    hasCallback,
    onDOMContentLoaded,
    setGlobalVar,
} from '../components/widget/global';

function setCallback() {
    window.yandexChatWidgetCallback = () => { /* empty */ };
}

afterEach(() => {
    delete window.yandexChatWidgetCallback;
});

describe('yandexChatWidgetCallback', () => {
    it('hasCallback()', () => {
        expect(hasCallback()).toBe(false);

        setCallback();

        expect(hasCallback()).toBe(true);
    });

    it('onDOMContentLoaded()', () => {
        setCallback();

        const spy = jest.spyOn(window as any, 'yandexChatWidgetCallback');

        onDOMContentLoaded();

        expect(spy).toHaveBeenCalledTimes(1);
        spy.mockClear();
    });

    it('setGlobalVar()', () => {
        setCallback();

        const widgetMock = { someProp: true };
        const spy = jest.spyOn(window as any, 'yandexChatWidgetCallback');

        setGlobalVar(widgetMock);

        expect(spy).toHaveBeenCalledTimes(1);
        spy.mockClear();

        expect(window.Ya.ChatWidget).toEqual(widgetMock);
    });
});
