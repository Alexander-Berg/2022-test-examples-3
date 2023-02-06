jest.mock('../utils/device', () => ({
    isIOS: true,
}));

/* eslint-disable import/first */
import {
    enablePageScroll,
    disablePageScroll,
    POPUP_ANIMATION_TIMEOUT,
} from '../components/disable-page-scroll';

// @ts-ignore
window.scrollTo = (x: number, y: number) => {
    // @ts-ignore
    window.scrollX = x;

    // @ts-ignore
    window.scrollY = y;
};

beforeEach(() => {
    document.body.style.marginLeft = '0px';
    document.body.style.marginTop = '0px';

    jest.useFakeTimers();
});

afterEach(() => {
    jest.useRealTimers();
});

describe('disable-page-scroll', () => {
    describe('disablePageScroll()', () => {
        it('должен добавлять CSS-класс и стали на <body> при блокировке скролла', () => {
            disablePageScroll();

            window.scrollTo(100, 200);

            jest.runTimersToTime(POPUP_ANIMATION_TIMEOUT);

            expect(document.body.className).toEqual('ya-chat-disable-page-scroll_ios ya-chat-disable-page-scroll');
            expect(document.body.style.marginLeft).toEqual('-100px');
            expect(document.body.style.marginTop).toEqual('-200px');
        });
    });

    describe('enablePageScroll()', () => {
        it('должен удалять все CSS-классы блока и восстанавливать позицию скролла', () => {
            window.scrollTo(100, 200);

            disablePageScroll();

            jest.runTimersToTime(POPUP_ANIMATION_TIMEOUT);

            enablePageScroll();

            expect(document.body.className).toEqual('');

            expect(window.scrollX).toEqual(100);
            expect(window.scrollY).toEqual(200);

            expect(document.body.style.marginLeft).toEqual('0px');
            expect(document.body.style.marginTop).toEqual('0px');
        });
    });
});
