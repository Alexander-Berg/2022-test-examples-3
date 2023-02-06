import { getParameterFromBody } from './getParameterFromBody';

describe('getParameterFromBody', () => {
    it('should return correct value', () => {
        expect(getParameterFromBody('', 'foo')).toBeUndefined();
        expect(getParameterFromBody('foo=1&bar=2', 'baz')).toBeUndefined();
        expect(getParameterFromBody('foo=1&bar=2', 'foo')).toEqual('1');
    });
});
