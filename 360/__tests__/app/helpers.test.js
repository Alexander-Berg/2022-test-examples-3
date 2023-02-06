import { getXFrameOptions } from '../../app/helpers';

const testAllowedIframeDomains = [
    'allow.iframe.domain.ru',
    '*.allow2-domain.ru'
];

describe('helpers', () => {
    describe('getXFrameOptions', () => {
        it('без Referer должен вернуть X-Frame-Options со значением SAMEORIGIN', () => {
            expect(getXFrameOptions('test.ru', undefined, testAllowedIframeDomains)).toEqual('SAMEORIGIN');
        });
        it('с Referer из разрешенного списка должен вернуть X-Frame-Options с пустым значением ', () => {
            expect(getXFrameOptions('test.ru', 'https://allow.iframe.domain.ru/test', testAllowedIframeDomains)).toEqual('');
        });
        it('с Referer из разрешенного wildcard-а должен вернуть X-Frame-Options с пустым значением ', () => {
            expect(getXFrameOptions('test.ru', 'https://my.allow2-domain.ru/test', testAllowedIframeDomains)).toEqual('');
        });
        it('если хост Referer и HOST совпадают, должен вернуть X-Frame-Options с пустым значением ', () => {
            expect(getXFrameOptions('my.test.ru', 'https://my.test.ru/test', [])).toEqual('');
        });
        it('с Referer не из разрешенного списка должен вернуть X-Frame-Options со значением SAMEORIGIN', () => {
            expect(getXFrameOptions('test.ru', 'https://deny.iframe.domain.ru/test', testAllowedIframeDomains)).toEqual('SAMEORIGIN');
        });
    });
});
