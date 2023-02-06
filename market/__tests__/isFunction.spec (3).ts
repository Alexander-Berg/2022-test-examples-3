import {isFunction} from '../typeGuards';

describe('isfunction', () => {
    it('аргумент является фнукицей', () => {
        expect(isFunction(() => {})).toEqual(true);
    });

    it('аргумент не является функцией', () => {
        expect(isFunction('')).toEqual(false);
    });
});
