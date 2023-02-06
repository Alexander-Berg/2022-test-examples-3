import { isCenterInViewport } from '../dom-helpers';

describe('dom-helpers', () => {
    describe('isCenterInViewport', () => {
        beforeAll(() => {
            // @ts-ignore
            window.innerHeight = 1000;
        });

        test('should return true when element center contain in viewport', () => {
            const el = {
                getBoundingClientRect: () =>
                    ({
                        top: 0,
                        height: 100,
                    } as DOMRect),
            } as HTMLElement;

            expect(isCenterInViewport(el)).toEqual(true);
        });

        test('should return true when element center partially abroad', () => {
            const el = {
                getBoundingClientRect: () =>
                    ({
                        top: -100,
                        height: 300,
                    } as DOMRect),
            } as HTMLElement;

            expect(isCenterInViewport(el)).toEqual(true);
        });

        test('should return false when element center fully abroad', () => {
            const el = {
                getBoundingClientRect: () =>
                    ({
                        top: -200,
                        height: 100,
                    } as DOMRect),
            } as HTMLElement;

            expect(isCenterInViewport(el)).toEqual(false);
        });

        test('should return false when element is empty', () => {
            expect(isCenterInViewport(null)).toEqual(false);
        });
    });
});
