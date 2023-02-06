import { isEmailValid } from '.';

describe('Validation', () => {
    describe('isEmailValid', () => {
        it('должен возвращать `false` для некорректоного адреса почты', () => {
            expect(isEmailValid('incorrect')).toBe(false);
        });

        it('должен возвращать `true` для корректоного адреса почты', () => {
            expect(isEmailValid('email@example.com')).toBe(true);
        });

        it('должен корректно обрабатывать разные адреса почты', () => {
            expect(isEmailValid('email@пример.рф')).toBe(true);
            expect(isEmailValid('firstname.lastname@example.com')).toBe(true);
            expect(isEmailValid('email@subdomain.example.com')).toBe(true);
            expect(isEmailValid('firstname+lastname@example.com')).toBe(true);
            expect(isEmailValid('email@123.123.123.123')).toBe(true);
            expect(isEmailValid('1234567890@example.com')).toBe(true);
            expect(isEmailValid('email@example-one.com')).toBe(true);
            expect(isEmailValid('_______@example.com')).toBe(true);
            expect(isEmailValid('email@example.name')).toBe(true);
            expect(isEmailValid('email@example.museum')).toBe(true);
            expect(isEmailValid('email@example.co.jp')).toBe(true);
            expect(isEmailValid('firstname-lastname@example.com')).toBe(true);

            expect(isEmailValid('#@%^%#$@#$@#.com')).toBe(false);
            expect(isEmailValid('@example.com')).toBe(false);
            expect(isEmailValid('Joe Smith <email@example.com>')).toBe(false);
            expect(isEmailValid('email.example.com')).toBe(false);
            expect(isEmailValid('email@example@example.com')).toBe(false);
            expect(isEmailValid('あいうえお@example.com')).toBe(false);
            expect(isEmailValid('email@example.com (Joe Smith)')).toBe(false);
            expect(isEmailValid('email@example')).toBe(false);
            expect(isEmailValid('email@-example.com')).toBe(false);
            expect(isEmailValid('email@example..com')).toBe(false);
        });
    });
});
