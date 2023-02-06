import { getQueryParam } from '../request';

describe('getQueryParam', () => {
    const query = {
        one: ['1', '2'],
    };

    it('должен возвращать cgi параметр', () => {
        expect(getQueryParam('one', query)).toEqual('1');
    });

    it('должен возвращать undefined если cgi параметр отсутствует', () => {
        expect(getQueryParam('notExist', query)).toEqual(undefined);
    });
});
