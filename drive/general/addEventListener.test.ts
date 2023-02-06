import { addEventListener } from 'shared/helpers/addEventListener/addEventListener';

describe('addEventListener', function () {
    it('addEventListener works correct', function () {
        const div = document.createElement('div');
        const func = jest.fn();

        addEventListener(div, 'touchstart', func);

        div.dispatchEvent(new Event('touchstart'));

        expect(func).toBeCalled();
    });
});
