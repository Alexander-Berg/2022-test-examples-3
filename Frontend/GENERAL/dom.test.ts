import {
    keepTopElems,
    isVisible,
} from './dom';

describe('dom', () => {
    describe('keepTopElems()', () => {
        it('Should keep top elems', () => {
            const elem11 = document.createElement('div');
            const elem12 = document.createElement('div');
            const elem21 = document.createElement('div');
            const elem22 = document.createElement('div');

            elem11.appendChild(elem12);
            elem22.appendChild(elem21);

            expect(elem11.contains(elem12)).toBe(true);
            expect(elem22.contains(elem21)).toBe(true);

            document.body.appendChild(elem11);
            document.body.appendChild(elem22);

            expect(keepTopElems([elem11, elem12, elem21, elem22]))
                .toEqual([elem11, elem22]);
        });
    });

    describe('isVisible()', () => {
        it('Should compute elem visibility', () => {
            const domElem = document.createElement('div');
            const ctxElem = document.createElement('div');

            Object.defineProperties(screen, {
                availWidth: { value: 350 },
                availHeight: { value: 350 },
            });

            ctxElem.getBoundingClientRect = () => ({
                top: 50,
                right: 300,
                bottom: 300,
                left: 50,
            } as DOMRect);

            domElem.getBoundingClientRect = () => ({
                height: 0,
            } as DOMRect);

            ctxElem.appendChild(domElem);
            document.body.appendChild(ctxElem);

            expect(isVisible(domElem)).toBe(false);

            domElem.getBoundingClientRect = () => ({
                height: 500,
                top: 500,
                right: 1000,
                bottom: 1000,
                left: 500,
            } as DOMRect);

            expect(isVisible(domElem)).toBe(false);

            domElem.getBoundingClientRect = () => ({
                height: 10,
                top: 0,
                right: 10,
                bottom: 10,
                left: 0,
            } as DOMRect);

            expect(isVisible(domElem)).toBe(false);

            domElem.getBoundingClientRect = () => ({
                height: 100,
                top: 100,
                right: 200,
                bottom: 200,
                left: 100,
            } as DOMRect);

            expect(isVisible(domElem)).toBe(true);

            document.body.appendChild(domElem);

            expect(isVisible(domElem)).toBe(true);
        });
    });
});
