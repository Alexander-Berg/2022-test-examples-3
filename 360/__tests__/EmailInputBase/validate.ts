import {
    validateLogin,
    validateDomainBase,
} from '../../src/components/EmailInputBase/validate';

const fakeGetI18n = (key: string, params?: Record<string, string|number|boolean>) =>
    key + (params ? ' / ' + params.prefix : '');

describe('EmailInputBase/validate – validateLogin', () => {
    it('empty login is valid', () => {
        expect(validateLogin('', fakeGetI18n)).toEqual([]);
    });
    it('letter, digits, - & . allowed', () => {
        expect(validateLogin('abc', fakeGetI18n)).toEqual([]);
        expect(validateLogin('abc555', fakeGetI18n)).toEqual([]);
        expect(validateLogin('abc-5.z', fakeGetI18n)).toEqual([]);
    });
    it('cyrillic letters is not allowed', () => {
        expect(validateLogin('абв', fakeGetI18n)).toEqual(['validate-allowed-symbols']);
        expect(validateLogin('яzz', fakeGetI18n)).toEqual(['validate-allowed-symbols']);
        expect(validateLogin('zяz', fakeGetI18n)).toEqual(['validate-allowed-symbols']);
    });
    it('cant start with digit', () => {
        expect(validateLogin('1abc', fakeGetI18n)).toEqual(['validate-login-start']);
        expect(validateLogin('123', fakeGetI18n)).toEqual(['validate-login-start']);
    });
    it('cant start with - or .', () => {
        expect(validateLogin('-abc', fakeGetI18n)).toEqual(['validate-login-start']);
        expect(validateLogin('.abc', fakeGetI18n)).toEqual(['validate-login-start']);
    });
    it('cant end with - or .', () => {
        expect(validateLogin('abc-', fakeGetI18n)).toEqual(['validate-login-end']);
        expect(validateLogin('abc.', fakeGetI18n)).toEqual(['validate-login-end']);
    });
    it('cant contain --, .., -., .-', () => {
        expect(validateLogin('abc--z', fakeGetI18n)).toEqual(['validate-login-2-hyphens']);
        expect(validateLogin('abc..z', fakeGetI18n)).toEqual(['validate-login-2-dots']);
        expect(validateLogin('abc-.z', fakeGetI18n)).toEqual(['validate-login-hyphen-and-dot']);
        expect(validateLogin('abc.-z', fakeGetI18n)).toEqual(['validate-login-hyphen-and-dot']);
    });
    it('prohibited prefixes', () => {
        expect(validateLogin('yndxaa', fakeGetI18n)).toEqual(['validate-login-prefix / yndx']);
        expect(validateLogin('yndx-aa', fakeGetI18n)).toEqual(['validate-login-prefix / yndx']);
        expect(validateLogin('yndx.aa', fakeGetI18n)).toEqual(['validate-login-prefix / yndx']);
        expect(validateLogin('yandex-teama', fakeGetI18n)).toEqual(['validate-login-prefix / yandex-team']);
        expect(validateLogin('yandex-team-aa', fakeGetI18n)).toEqual(['validate-login-prefix / yandex-team']);
        expect(validateLogin('yandex.team.aa', fakeGetI18n)).toEqual(['validate-login-prefix / yandex.team']);
        expect(validateLogin('yandex-team.b', fakeGetI18n)).toEqual(['validate-login-prefix / yandex-team']);
        expect(validateLogin('yandex.team-cde', fakeGetI18n)).toEqual(['validate-login-prefix / yandex.team']);
        expect(validateLogin('kid-paul', fakeGetI18n)).toEqual(['validate-login-prefix / kid-']);
        expect(validateLogin('kid.nap', fakeGetI18n)).toEqual(['validate-login-prefix / kid.']);
        expect(validateLogin('frodo-spam', fakeGetI18n)).toEqual(['validate-login-prefix / frodo-spam']);
        expect(validateLogin('frodo-spamm', fakeGetI18n)).toEqual(['validate-login-prefix / frodo-spam']);
        expect(validateLogin('frodo-spam-m', fakeGetI18n)).toEqual(['validate-login-prefix / frodo-spam']);

        // не в начале можно
        expect(validateLogin('ayndx', fakeGetI18n)).toEqual([]);
        expect(validateLogin('a-yndx-b', fakeGetI18n)).toEqual([]);
        expect(validateLogin('a.yndx.b', fakeGetI18n)).toEqual([]);
        expect(validateLogin('xfrodo-spam', fakeGetI18n)).toEqual([]);

        expect(validateLogin('uid-1', fakeGetI18n)).toEqual(['validate-login-prefix / uid-']);
        expect(validateLogin('uid.1', fakeGetI18n)).toEqual(['validate-login-prefix / uid.']);
        expect(validateLogin('uid1', fakeGetI18n)).toEqual([]);
        expect(validateLogin('uid', fakeGetI18n)).toEqual([]);
    });
    it('multiple errors', () => {
        expect(validateLogin('11я1', fakeGetI18n)).toEqual(['validate-allowed-symbols', 'validate-login-start']);
        expect(validateLogin('-я-', fakeGetI18n)).toEqual([
            'validate-allowed-symbols',
            'validate-login-start',
            'validate-login-end'
        ]);

        expect(validateLogin('yndx.', fakeGetI18n)).toEqual([
            'validate-login-end',
            'validate-login-prefix / yndx',
        ]);

        expect(validateLogin('yndx.-я-', fakeGetI18n)).toEqual([
            'validate-allowed-symbols',
            'validate-login-hyphen-and-dot',
            'validate-login-end',
            'validate-login-prefix / yndx'
        ]);
    });
});

describe('EmailInputBase/validate – validateDomainBase', () => {
    it('empty domainBase is valid', () => {
        expect(validateDomainBase('', fakeGetI18n)).toEqual([]);
    });
    it('letter, digits & - allowed', () => {
        expect(validateDomainBase('abc', fakeGetI18n)).toEqual([]);
        expect(validateDomainBase('abc555', fakeGetI18n)).toEqual([]);
        expect(validateDomainBase('abc-5z', fakeGetI18n)).toEqual([]);
    });
    it('cyrillic letters is not allowed', () => {
        expect(validateDomainBase('абв', fakeGetI18n)).toEqual(['validate-allowed-symbols']);
        expect(validateDomainBase('яzz', fakeGetI18n)).toEqual(['validate-allowed-symbols']);
        expect(validateDomainBase('zяz', fakeGetI18n)).toEqual(['validate-allowed-symbols']);
        expect(validateDomainBase('11я1', fakeGetI18n)).toEqual(['validate-allowed-symbols']);
    });
    it('dot is not allowed', () => {
        expect(validateDomainBase('ab.c', fakeGetI18n)).toEqual(['validate-domain-dot']);
        expect(validateDomainBase('.abc', fakeGetI18n)).toEqual(['validate-domain-dot']);
        expect(validateDomainBase('abc.', fakeGetI18n)).toEqual(['validate-domain-dot']);
    });
    it('can start with digit', () => {
        expect(validateDomainBase('1abc', fakeGetI18n)).toEqual([]);
        expect(validateDomainBase('123', fakeGetI18n)).toEqual([]);
    });
    it('cant start with -', () => {
        expect(validateDomainBase('-abc', fakeGetI18n)).toEqual(['validate-domain-start']);
    });
    it('cant end with -', () => {
        expect(validateDomainBase('abc-', fakeGetI18n)).toEqual(['validate-domain-end']);
    });
    it('cant contain --', () => {
        expect(validateDomainBase('abc--z', fakeGetI18n)).toEqual(['validate-domain-hyphen']);
    });
    it('multiple errors', () => {
        expect(validateDomainBase('я.такой', fakeGetI18n)).toEqual([
            'validate-allowed-symbols',
            'validate-domain-dot'
        ]);

        expect(validateDomainBase('--я.-', fakeGetI18n)).toEqual([
            'validate-allowed-symbols',
            'validate-domain-dot',
            'validate-domain-hyphen',
            'validate-domain-start',
            'validate-domain-end'
        ]);
    });
});
