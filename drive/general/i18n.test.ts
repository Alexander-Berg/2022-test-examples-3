import { i18n } from 'lib/i18n';
import { keyset } from 'lib/keyset';
import { plural as pluralEn } from 'plurals/langs/en';
import { plural as pluralBe } from 'plurals/langs/be';
import { MovaPlurals } from 'types';

const plurals: MovaPlurals = {
    en: pluralEn,
    be: pluralBe,
};

const translates = keyset({
    login: {
        en: 'Login',
        be: 'Увайсьці',
    },
    greating: {
        en: 'Hi, {username}',
        be: 'Прывітанне, {username}',
    },
    strangeCount: {
        en: 'Only: {count}',
        be: 'Толькі: {count}',
    },
    users: {
        en: {
            one: '{count} user',
            other: '{count} users',
        },
        be: {
            one: '{count} карыстальнік',
            few: '{count} карыстальніка',
            many: '{count} карыстальнікаў',
        },
    },
    users0: {
        en: {
            one: '{count} user',
            other: '{count} users',
            zero: 'no users here',
        },
        be: {
            one: '{count} карыстальнік',
            few: '{count} карыстальніка',
            many: '{count} карыстальнікаў',
            zero: 'нікога няма',
        },
    },
    brokenUsers: {
        en: {
            one: '{count} user',
        },
        be: {
            one: '{count} карыстальнік',
            many: '{count} карыстальнікаў',
        },
    } as any,
    superBrokenUsers: {
        en: {
            other: '{count} users',
        },
        be: {
            many: '{count} карыстальнікаў',
        },
    } as any,
});

describe('i18n', function () {
    it('plain text', function () {
        const i18nEn = i18n('en', plurals)(translates);
        const i18nBe = i18n('be', plurals)(translates);

        expect(i18nEn('login')).toMatchInlineSnapshot(`"Login"`);
        expect(i18nBe('login')).toMatchInlineSnapshot(`"Увайсьці"`);
    });

    it('parametrized text', function () {
        const i18nEn = i18n('en', plurals)(translates);
        const i18nBe = i18n('be', plurals)(translates);

        expect(i18nEn('greating', { username: 'Grzegorz' })).toMatchInlineSnapshot(`"Hi, Grzegorz"`);
        expect(i18nBe('greating', { username: 'Grzegorz' })).toMatchInlineSnapshot(`"Прывітанне, Grzegorz"`);
    });

    it('parametrized text', function () {
        const i18nEn = i18n('en', plurals)(translates);
        const i18nBe = i18n('be', plurals)(translates);

        expect(i18nEn('strangeCount', { count: 2 })).toMatchInlineSnapshot(`"Only: 2"`);
        expect(i18nBe('strangeCount', { count: 2 })).toMatchInlineSnapshot(`"Толькі: 2"`);
    });

    it('plural', function () {
        const i18nEn = i18n('en', plurals)(translates);
        const i18nBe = i18n('be', plurals)(translates);

        expect(i18nEn('users', { count: 0 })).toMatchInlineSnapshot(`"0 users"`);
        expect(i18nEn('users', { count: 1 })).toMatchInlineSnapshot(`"1 user"`);
        expect(i18nEn('users', { count: 2 })).toMatchInlineSnapshot(`"2 users"`);
        expect(i18nEn('users', { count: 5 })).toMatchInlineSnapshot(`"5 users"`);

        expect(i18nBe('users', { count: 0 })).toMatchInlineSnapshot(`"0 карыстальнікаў"`);
        expect(i18nBe('users', { count: 1 })).toMatchInlineSnapshot(`"1 карыстальнік"`);
        expect(i18nBe('users', { count: 2 })).toMatchInlineSnapshot(`"2 карыстальніка"`);
        expect(i18nBe('users', { count: 5 })).toMatchInlineSnapshot(`"5 карыстальнікаў"`);
    });

    it('plural with formatted number', function () {
        const i18nEn = i18n('en', plurals)(translates);
        const i18nBe = i18n('be', plurals)(translates);

        expect(i18nEn('users', { _count: 1000, count: '1 000' })).toMatchInlineSnapshot(`"1 000 users"`);
        expect(i18nBe('users', { _count: 1000, count: '1 000' })).toMatchInlineSnapshot(`"1 000 карыстальнікаў"`);
    });

    it('native plural with formatted number', function () {
        const i18nEn = i18n('en')(translates);
        const i18nBe = i18n('be')(translates);

        expect(i18nEn('users', { _count: 1000, count: '1 000' })).toMatchInlineSnapshot(`"1 000 users"`);
        expect(i18nBe('users', { _count: 1000, count: '1 000' })).toMatchInlineSnapshot(`"1 000 карыстальнікаў"`);
    });

    it('plural with zero', function () {
        const i18nEn = i18n('en', plurals)(translates);
        const i18nBe = i18n('be', plurals)(translates);

        expect(i18nEn('users0', { count: 0 })).toMatchInlineSnapshot(`"no users here"`);
        expect(i18nEn('users0', { count: 1 })).toMatchInlineSnapshot(`"1 user"`);
        expect(i18nEn('users0', { count: 2 })).toMatchInlineSnapshot(`"2 users"`);
        expect(i18nEn('users0', { count: 5 })).toMatchInlineSnapshot(`"5 users"`);

        expect(i18nBe('users0', { count: 0 })).toMatchInlineSnapshot(`"нікога няма"`);
        expect(i18nBe('users0', { count: 1 })).toMatchInlineSnapshot(`"1 карыстальнік"`);
        expect(i18nBe('users0', { count: 2 })).toMatchInlineSnapshot(`"2 карыстальніка"`);
        expect(i18nBe('users0', { count: 5 })).toMatchInlineSnapshot(`"5 карыстальнікаў"`);
    });

    it('non existing key', function () {
        const i18nEn = i18n('en', plurals)(translates);
        const i18nBe = i18n('be', plurals)(translates);

        expect(i18nEn('nonExistingKey' as any)).toMatchInlineSnapshot(`""`);
        expect(i18nBe('nonExistingKey' as any)).toMatchInlineSnapshot(`""`);
    });

    it('broken plural forms with fallback', function () {
        const i18nEn = i18n('en', plurals)(translates);
        const i18nBe = i18n('be', plurals)(translates);

        expect(i18nEn('brokenUsers', { count: 1 })).toMatchInlineSnapshot(`"1 user"`);
        expect(i18nEn('brokenUsers', { count: 2 })).toMatchInlineSnapshot(`"2 user"`);
        expect(i18nEn('brokenUsers', { count: 5 })).toMatchInlineSnapshot(`"5 user"`);

        expect(i18nBe('brokenUsers', { count: 1 })).toMatchInlineSnapshot(`"1 карыстальнік"`);
        expect(i18nBe('brokenUsers', { count: 2 })).toMatchInlineSnapshot(`"2 карыстальнік"`);
        expect(i18nBe('brokenUsers', { count: 5 })).toMatchInlineSnapshot(`"5 карыстальнікаў"`);
    });

    it('super broken plural forms without fallback form', function () {
        const i18nEn = i18n('en', plurals)(translates);
        const i18nBe = i18n('be', plurals)(translates);

        expect(i18nEn('superBrokenUsers', { count: 1 })).toMatchInlineSnapshot(`""`);
        expect(i18nEn('superBrokenUsers', { count: 2 })).toMatchInlineSnapshot(`"2 users"`);
        expect(i18nEn('superBrokenUsers', { count: 5 })).toMatchInlineSnapshot(`"5 users"`);

        expect(i18nBe('superBrokenUsers', { count: 1 })).toMatchInlineSnapshot(`""`);
        expect(i18nBe('superBrokenUsers', { count: 2 })).toMatchInlineSnapshot(`""`);
        expect(i18nBe('superBrokenUsers', { count: 5 })).toMatchInlineSnapshot(`"5 карыстальнікаў"`);
    });
});
