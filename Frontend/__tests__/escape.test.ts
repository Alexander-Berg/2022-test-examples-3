import { escape, unescape, escapeURI } from '../escape';

describe('Util escape', () => {
    describe('#escape', () => {
        it('returns escaped string', () => {
            expect(escape('abc&<>"\'bca')).toBe('abc&amp;&lt;&gt;&quot;&#39;bca');
        });
    });

    describe('#unescape', () => {
        it('returns unescaped string', () => {
            expect(unescape('abc&amp;&lt;&gt;&quot;&#39;&nbsp;&yt;bca')).toBe('abc&<>"\'\xa0&yt;bca');
        });
    });

    describe('#escapeURI', () => {
        it('returns escaped uri', () => {
            expect(escapeURI('https://yandex.ru/search/?text="cats%20and%20dogs"'))
                .toBe('https://yandex.ru/search/?text=%22cats%20and%20dogs%22');
        });
    });
});
