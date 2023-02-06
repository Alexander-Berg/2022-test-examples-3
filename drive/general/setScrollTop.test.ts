import { setScrollTop } from 'shared/helpers/setScrollTop/setScrollTop';

describe('setScrollTop', function () {
    window.scrollTo = jest.fn();

    it('works with value', function () {
        let spy = jest.spyOn(window, 'scrollTo');
        setScrollTop(100);

        expect(spy).toBeCalledWith(0, 100);
    });
});
