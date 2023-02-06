import emailHelper from '../../../components/helpers/email';

/**
 * @param {string} email
 */
function isEmailValid(email) {
    it('Валидный `' + email + '`', () => {
        expect(emailHelper.isEmail(email)).toBe(true);
    });
}

/**
 * @param {string} email
 */
function isEmailInvalid(email) {
    it('Невалидный `' + email + '`', () => {
        expect(emailHelper.isEmail(email)).toBe(false);
    });
}

/**
 * @param {string} email
 * @param {string} expected
 */
function testNormalize(email, expected) {
    it('normalize(`' + email + '`) == `' + expected + '`', () => {
        expect(emailHelper.normalize(email)).toBe(expected);
    });
}

/**
 * @param {string} email
 * @param {boolean} expected
 */
function testIsYandexDomain(email, expected) {
    it('yandexDomainRegExp(`' + email + '`) == `' + expected + '`', () => {
        expect(emailHelper.yandexDomainRegExp.test(email)).toBe(expected);
    });
}

describe('emailHelper', () => {
    describe('Метод `isEmail`', () => {
        isEmailValid('mctep@yandex-team.ru');
        isEmailValid('мега-поц@яндекс.рф');
        isEmailValid('me@domain.yandex');
        isEmailValid('president@questions.gov.no');
        isEmailValid('"User with @ in LOGIn"@Example.Org');

        isEmailInvalid('president@questions');
        isEmailInvalid('president@questions.');
        isEmailInvalid('@me.ru');
        isEmailInvalid('me@.test.ru');
        isEmailInvalid('me@test.@ru');
    });

    describe('Метод `normalize`', () => {
        testNormalize('user@example.com', 'user@example.com');
        testNormalize('User@Example.Org', 'User@example.org');
        testNormalize('User+tag@Example.Org', 'User+tag@example.org');
        testNormalize('Юзер@Почта.РФ', 'Юзер@почта.рф');
        testNormalize('"User with @ in LOGIn"@Example.Org', '"User with @ in LOGIn"@example.org');

        testNormalize('user@YANdEX.com', 'user@yandex.ru');
        testNormalize('user@yandex.com.am', 'user@yandex.ru');
        testNormalize('user@yandex.co.il', 'user@yandex.ru');
        testNormalize('us.E-r@YANdEX.com.tr', 'us-e-r@yandex.ru');
        testNormalize('User+tag@Яндекс.рф', 'user@yandex.ru');
        testNormalize('Us.e-r+tag@Narod.ru', 'us-e-r@narod.ru');

        testNormalize('Us.er+tag@GoogleMail.com', 'user@gmail.com');
    });

    describe('Регулярка для проверки, что домен - яндексовый', () => {
        testIsYandexDomain('ya.ru', true);
        testIsYandexDomain('ya.ru2', false);
        testIsYandexDomain('ya.ru.com', false);
        testIsYandexDomain('qq.ya.ru', false);
        testIsYandexDomain('ya1ru', false);

        testIsYandexDomain('яндекс.рф', true);
        testIsYandexDomain('яндекс.рф2', false);
        testIsYandexDomain('1яндекс.рф', false);
        testIsYandexDomain('яндекс_рф', false);

        testIsYandexDomain('yandex.ru', true);
        testIsYandexDomain('yandex.com', true);
        testIsYandexDomain('yandex.ua', true);
        testIsYandexDomain('yandex.az', true);
        testIsYandexDomain('yandex.md', true);
        testIsYandexDomain('yandex.com.tr', true);
        testIsYandexDomain('yandex.co.il', true);
        testIsYandexDomain('yandex.com.ge', true);

        testIsYandexDomain('yandex.aaa.ru', false);
        testIsYandexDomain('yandex_ru', false);
        testIsYandexDomain('lol.yandex.com.tr', false);
        testIsYandexDomain('yandex_com.tr', false);
        testIsYandexDomain('yandex.com_tr', false);
        testIsYandexDomain('yandex_com_tr', false);
        testIsYandexDomain('@yandex.com.tr', false);
        testIsYandexDomain('yandex.ru.be', false);
        testIsYandexDomain('yandex.rube', false);
        testIsYandexDomain('yandex.com.tr.ru', false);
    });
});
