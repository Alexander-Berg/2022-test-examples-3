import { getViewportBounds, getViewportCenter, calculatePopupBounds } from '../bounds';

const WINDOW_WIDTH = 1024;
const WINDOW_HEIGHT = 768;

const defaultOffset = 10;
const shiftedYOffset = 200;

describe('popup', () => {
    describe('getViewportBounds', () => {
        beforeEach(() => {
            // @ts-ignore
            window.pageXOffset = 0;
            // @ts-ignore
            window.pageYOffset = shiftedYOffset;
            // @ts-ignore
            window.innerWidth = WINDOW_WIDTH;
            // @ts-ignore
            window.innerHeight = WINDOW_HEIGHT;
        });

        it('должен возвращать корректные позиции по горизонтали', () => {
            expect(getViewportBounds({
                top: 0,
                bottom: 0,
                left: defaultOffset,
                right: defaultOffset,
            })).toEqual({
                left: defaultOffset,
                right: WINDOW_WIDTH - defaultOffset,
                width: WINDOW_WIDTH - 2 * defaultOffset,
                top: shiftedYOffset,
                bottom: WINDOW_HEIGHT + shiftedYOffset,
                height: WINDOW_HEIGHT,
            });
        });

        it('должен возвращать корректные позиции по вертикали', () => {
            expect(getViewportBounds({
                top: defaultOffset,
                bottom: defaultOffset,
                left: 0,
                right: 0,
            })).toEqual({
                left: 0,
                right: WINDOW_WIDTH,
                width: WINDOW_WIDTH,
                top: shiftedYOffset + defaultOffset,
                bottom: WINDOW_HEIGHT - defaultOffset + shiftedYOffset,
                height: WINDOW_HEIGHT - 2 * defaultOffset,
            });
        });
    });

    describe('getViewportCenter', () => {
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

        it('должен возвращать корректные позиции центра вьюпорта', () => {
            expect(getViewportCenter({
                left: 200,
                right: 400,
                width: 200,
                top: 500,
                bottom: 700,
                height: 200,
            })).toEqual({
                left: 300,
                top: 600,
            });
        });
    });

    describe('calculatePopupBounds', () => {
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

        it(`если прижатый левой гранью к левой границе якоря элемент вылезает за пределы вьюпорта,
            правая граница якоря справа от центра вьюпорта,
            и прижатый правой гранью к правой границе якоря элемент дотягивается до положения левого отступа вьюпорта:
            элемент прижимается левой гранью к левому отступу вьюпорта`, () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: Math.ceil(WINDOW_WIDTH * 2 / 3),
                offsetHeight: 0,
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: Math.ceil(WINDOW_WIDTH * 2 / 3),
                right: Math.ceil(WINDOW_WIDTH * 2 / 3),
                top: 0,
                bottom: 0,
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: defaultOffset,
                right: Math.ceil(WINDOW_WIDTH * 2 / 3) + defaultOffset,
                width: Math.ceil(WINDOW_WIDTH * 2 / 3),
                top: 10,
                bottom: 10,
                height: 0,
            });
        });

        it(`если прижатый левой гранью к левой границе якоря элемент вылезает за пределы вьюпорта,
            правая граница якоря справа от центра вьюпорта,
            и прижатый правой гранью к правой границе якоря элемент не дотягивается до положения левого отступа вьюпорта:
            элемент прижимается правой гранью к правой границе якоря`, () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: Math.ceil(WINDOW_WIDTH / 3),
                offsetHeight: 0,
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: Math.ceil(WINDOW_WIDTH * 2 / 3),
                right: Math.ceil(WINDOW_WIDTH * 2 / 3),
                top: 0,
                bottom: 0,
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: Math.floor(WINDOW_WIDTH / 3),
                right: Math.ceil(WINDOW_WIDTH * 2 / 3),
                width: Math.ceil(WINDOW_WIDTH / 3),
                top: 10,
                bottom: 10,
                height: 0,
            });
        });

        it(`если прижатый левой гранью к левой границе якоря элемент вылезает за пределы вьюпорта,
            правая граница якоря слева от центра вьюпорта:
            элемент прижимается правой гранью к правому отступу вьюпорта`, () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: Math.ceil(WINDOW_WIDTH * 2 / 3),
                offsetHeight: 0,
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: Math.ceil(WINDOW_WIDTH / 3),
                right: Math.ceil(WINDOW_WIDTH / 3),
                top: 0,
                bottom: 0,
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: Math.floor(WINDOW_WIDTH / 3) - defaultOffset,
                right: WINDOW_WIDTH - defaultOffset,
                width: Math.ceil(WINDOW_WIDTH * 2 / 3),
                top: 10,
                bottom: 10,
                height: 0,
            });
        });

        it(`если прижатый верхней гранью к нижней границе якоря элемент вылезает за пределы вьюпорта,
            верхняя граница якоря ниже центра вьюпорта,
            и прижатый нижней гранью к верхней границе якоря элемент не влезает во вьюпорт:
            элемент прижимается верхней гранью к верхней границе вьюпорта, сохраняя исходную высоту`, () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: 0,
                offsetHeight: Math.ceil(WINDOW_HEIGHT * 2 / 3),
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: defaultOffset,
                right: defaultOffset,
                top: Math.ceil(WINDOW_HEIGHT * 2 / 3),
                bottom: Math.ceil(WINDOW_HEIGHT * 2 / 3),
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: defaultOffset,
                right: defaultOffset,
                width: 0,
                top: defaultOffset,
                bottom: defaultOffset + Math.ceil(WINDOW_HEIGHT * 2 / 3),
                height: Math.ceil(WINDOW_HEIGHT * 2 / 3),
            });
        });

        it(`если прижатый верхней гранью к нижней границе якоря элемент вылезает за пределы вьюпорта,
            верхняя граница якоря ниже центра вьюпорта,
            прижатый нижней гранью к верхней границе якоря элемент не влезает во вьюпорт,
            и высота элемента больше высоты вьюпорта:
            элемент прижимается верхней гранью к верхней границе вьюпорта и обрезается до высоты вьюпорта `, () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: 0,
                offsetHeight: WINDOW_HEIGHT + 10,
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: defaultOffset,
                right: defaultOffset,
                top: Math.ceil(WINDOW_HEIGHT * 2 / 3),
                bottom: Math.ceil(WINDOW_HEIGHT * 2 / 3),
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: defaultOffset,
                right: defaultOffset,
                width: 0,
                top: defaultOffset,
                bottom: WINDOW_HEIGHT - defaultOffset,
                height: WINDOW_HEIGHT - 2 * defaultOffset,
            });
        });

        it(`если прижатый верхней гранью к нижней границе якоря элемент вылезает за пределы вьюпорта,
            верхняя граница якоря ниже центра вьюпорта,
            и прижатый нижней гранью к верхней границе якоря элемент влезает во вьюпорт:
            элемент прижимается нижней гранью к верхней границе якоря`, () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: 0,
                offsetHeight: Math.ceil(WINDOW_HEIGHT / 3),
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: 0,
                right: 0,
                top: Math.ceil(WINDOW_HEIGHT * 2 / 3),
                bottom: Math.ceil(WINDOW_HEIGHT * 2 / 3),
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: defaultOffset,
                right: defaultOffset,
                width: 0,
                top: Math.floor(WINDOW_HEIGHT / 3),
                bottom: Math.ceil(WINDOW_HEIGHT * 2 / 3),
                height: Math.ceil(WINDOW_HEIGHT / 3),
            });
        });

        it(`если прижатый верхней гранью к нижней границе якоря элемент вылезает за пределы вьюпорта,
            верхняя граница якоря выше центра вьюпорта:
            элемент прижимается нижней гранью к нижней границе якоря, сохраняя исходную высоту`, () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: 0,
                offsetHeight: Math.ceil(WINDOW_HEIGHT * 2 / 3),
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: 0,
                right: 0,
                top: Math.ceil(WINDOW_HEIGHT / 3),
                bottom: Math.ceil(WINDOW_HEIGHT / 3),
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: defaultOffset,
                right: defaultOffset,
                width: 0,
                top: WINDOW_HEIGHT - defaultOffset - Math.ceil(WINDOW_HEIGHT * 2 / 3),
                bottom: WINDOW_HEIGHT - defaultOffset,
                height: Math.ceil(WINDOW_HEIGHT * 2 / 3),
            });
        });

        it(`если прижатый верхней гранью к нижней границе якоря элемент вылезает за пределы вьюпорта,
            верхняя граница якоря выше центра вьюпорта, и высота элемента больше высоты вьюпорта:
            элемент прижимается нижней гранью к нижней границе вьюпорта, обрезаясь до высоты вьюпорта`, () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: 0,
                offsetHeight: WINDOW_HEIGHT * 2,
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: 0,
                right: 0,
                top: Math.ceil(WINDOW_HEIGHT / 3),
                bottom: Math.ceil(WINDOW_HEIGHT / 3),
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: defaultOffset,
                right: defaultOffset,
                width: 0,
                top: defaultOffset,
                bottom: WINDOW_HEIGHT - defaultOffset,
                height: WINDOW_HEIGHT - 2 * defaultOffset,
            });
        });

        it(`должен вычислять позицию попапа без горизонтального отступа,
            если ширина окна меньше или равна ширине элемента + отступ`, () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: WINDOW_WIDTH,
                offsetHeight: 0,
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: Math.ceil(WINDOW_WIDTH / 2),
                right: Math.ceil(WINDOW_WIDTH / 2),
                top: 0,
                bottom: 0,
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: 0,
                right: WINDOW_WIDTH,
                width: WINDOW_WIDTH,
                top: 10,
                bottom: 10,
                height: 0,
            });
        });

        it('должен правильно вычислять позицию попапа без прочих условий', () => {
            // @ts-ignore
            const target: HTMLElement = {
                offsetWidth: 300,
                offsetHeight: 100,
            };

            // @ts-ignore
            const anchorBounds: ClientRect = {
                left: Math.ceil(WINDOW_WIDTH / 2),
                right: Math.ceil(WINDOW_WIDTH / 2),
                top: Math.ceil(WINDOW_HEIGHT / 3),
                bottom: Math.ceil(WINDOW_HEIGHT / 3),
            };

            expect(calculatePopupBounds(target, anchorBounds, defaultOffset)).toEqual({
                left: Math.ceil(WINDOW_WIDTH / 2),
                right: Math.ceil(WINDOW_WIDTH / 2) + target.offsetWidth,
                width: target.offsetWidth,
                top: Math.ceil(WINDOW_HEIGHT / 3),
                bottom: Math.ceil(WINDOW_HEIGHT / 3) + target.offsetHeight,
                height: target.offsetHeight,
            });
        });
    });
});
