import { EMPTY_DATA } from 'constants/constants';

import { dataOrEmpty } from 'shared/helpers/dataOrEmpty/dataOrEmpty';

describe('dataOrEmpty', function () {
    it('works with empty params', function () {
        expect(dataOrEmpty(null, 'lol kek cheburek')).toMatch(EMPTY_DATA);
        expect(dataOrEmpty(undefined, 'lol kek cheburek')).toMatch(EMPTY_DATA);
        expect(dataOrEmpty('', 'lol kek cheburek')).toMatch(EMPTY_DATA);
    });

    it('works with filled params', function () {
        expect(dataOrEmpty('lol', 'lol kek cheburek')).toMatch('lol kek cheburek');
    });
});
