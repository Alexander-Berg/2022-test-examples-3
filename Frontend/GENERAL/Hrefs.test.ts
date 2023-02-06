import { Hrefs } from './Hrefs';

function createHref(url: string): HTMLAnchorElement {
    const href = document.createElement('a');

    href.setAttribute('href', url);

    return href;
}

describe('Hrefs', () => {
    it('Should be an instance of Hrefs', () => {
        expect(new Hrefs()).toBeInstanceOf(Hrefs);
    });

    it('Should manage url hrefs in collection', () => {
        const url1 = 'https://href.com/1';
        const href11 = createHref(url1);
        const href12 = createHref(url1);
        const ctxElem = document.createElement('div');

        const hrefs = new Hrefs();

        hrefs.addHref(href11, url1, ctxElem);

        expect(hrefs.getHref(href11)).toEqual([href11, url1, ctxElem]);
        expect(hrefs.hasHrefsOf(url1)).toBe(true);

        hrefs.addHref(href12, url1, ctxElem);

        expect(hrefs.getHref(href12)).toEqual([href12, url1, ctxElem]);
        expect(hrefs.hasHrefsOf(url1)).toBe(true);

        const callback = jest.fn();

        hrefs.forHrefsOf(url1, callback);

        expect(callback).toHaveBeenCalledTimes(1);
        expect(callback).toHaveBeenCalledWith(ctxElem, [href11, href12]);

        callback.mockReset();

        hrefs.delHref(href11, url1);

        expect(hrefs.getHref(href11)).toBeUndefined();
        expect(hrefs.hasHrefsOf(url1)).toBe(true);

        hrefs.forHrefsOf(url1, callback);

        expect(callback).toHaveBeenCalledTimes(1);
        expect(callback).toHaveBeenCalledWith(ctxElem, [href12]);

        callback.mockReset();

        hrefs.delHref(href12, url1);

        expect(hrefs.getHref(href12)).toBeUndefined();
        expect(hrefs.hasHrefsOf(url1)).toBe(false);

        hrefs.forHrefsOf(url1, callback);

        expect(callback).not.toHaveBeenCalled();
        expect(() => { hrefs.delHref(href12, url1) }).not.toThrow();
    });
});
