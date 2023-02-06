import { calculatePosition, getViewportBounds } from '../components/popup';

const WINDOW_WIDTH = 1024;
const WINDOW_HEIGHT = 768;
const ICON_SIZE = 40;

beforeEach(() => {
    // @ts-ignore
    window.pageXOffset = 0;
    // @ts-ignore
    window.pageYOffset = 0;
    // @ts-ignore
    window.innerWidth = WINDOW_WIDTH;
    // @ts-ignore
    window.innerHeight = WINDOW_HEIGHT;
});

describe('popup', () => {
    describe('getViewportBounds()', () => {
        it('должен возвращать корректные позиции для вьюпорта', () => {
            expect(getViewportBounds()).toEqual({
                left: 10,
                top: 10,
                right: 1014,
                bottom: 758,
            });
        });
    });

    describe('calcPosition()', () => {
        it('должен вычислять позицию попапа', () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: 400,
                offsetHeight: 600,
            };

            expect(calculatePosition(target, {
                left: 900,
                right: 900 + ICON_SIZE,
                top: 50,
                bottom: 50 + ICON_SIZE,
            })).toEqual({
                left: 540,
                top: 90,
            });
        });

        it('должен вычислять позицию попапа, если иконка у правого края окна', () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: 400,
                offsetHeight: 600,
            };

            expect(calculatePosition(target, {
                left: WINDOW_WIDTH - 50,
                right: WINDOW_WIDTH - 50 + ICON_SIZE,
                top: 50,
                bottom: 50 + ICON_SIZE,
            })).toEqual({
                left: 614,
                top: 90,
            });
        });

        it('должен вычислять позицию попапа, если иконка в правой части маленького окна', () => {
            // @ts-ignore
            window.innerWidth = 700;

            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: 400,
                offsetHeight: 600,
            };

            expect(calculatePosition(target, {
                left: 320,
                right: 320 + ICON_SIZE,
                top: 50,
                bottom: 50 + ICON_SIZE,
            })).toEqual({
                left: 10,
                top: 90,
            });
        });
    });
});
