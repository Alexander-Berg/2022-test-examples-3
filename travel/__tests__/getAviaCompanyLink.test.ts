import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import getAviaCompanyLink from '../getAviaCompanyLink';

const tld = Tld.ru;
const language = Lang.ru;

const expectedCompanyUrl = '/info/company/1';

const yandexAviaUrl = 'testAviaurl';

describe('getAviaCompanyLink', () => {
    it('Must return correct url', () => {
        expect(getAviaCompanyLink({id: 1, tld, language})).toBe(
            expectedCompanyUrl,
        );
    });

    it('Must return yandexAviaUrl if it is passed to function', () => {
        expect(getAviaCompanyLink({yandexAviaUrl})).toBe(yandexAviaUrl);
    });
});
