const { appendQueryParameter } = require('./url');

describe('url utils', () => {
    describe('appendQueryParameter', () => {
        it('should append query parameter if none were specified', () => {
            const res = appendQueryParameter('/some/test/path', 'some', 'value');

            expect(res).toBe('/some/test/path?some=value');
        });

        it('should append query parameter if other were specified', () => {
            const res = appendQueryParameter('/some/test/path?existing=param', 'some', 'value');

            expect(res).toBe('/some/test/path?existing=param&some=value');
        });

        it('should append query parameter if same was specified', () => {
            const res = appendQueryParameter('/some/test/path?some=oldValue', 'some', 'value');

            expect(res).toBe('/some/test/path?some=oldValue&some=value');
        });

        it('should not break anchor', () => {
            const res = appendQueryParameter('/some/test/path#anchor', 'some', 'value');

            expect(res).toBe('/some/test/path?some=value#anchor');
        });
    });
});
