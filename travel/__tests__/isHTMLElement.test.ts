import isHTMLElement from '../isHTMLElement';

describe('isHTMLElement', () => {
    it('HTMLElement', () => {
        expect(
            isHTMLElement({
                offsetHeight: 1,
            } as HTMLElement),
        ).toBe(true);
    });

    it('Element', () => {
        expect(isHTMLElement({} as Element)).toBe(false);
    });
});
