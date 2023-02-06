import {getEmailValidationRegExp} from '../validationRules';

describe('Валидация email по базовым требованиям из RFC 5322', () => {
    const emailRegExEng = new RegExp(getEmailValidationRegExp());

    test('Можно пробелы вначале и в конце', () => {
        expect(emailRegExEng.test('  user@yandex.ru  ')).toBeTruthy();
    });

    test('Валидная простая почта', () => {
        expect(emailRegExEng.test('user@yandex.ru')).toBeTruthy();
    });

    test('Проверяет обязательное наличие @', () => {
        expect(emailRegExEng.test('useryandex.ru')).toBeFalsy();
    });

    test('Проверяет отсутствие дублирования @', () => {
        expect(emailRegExEng.test('user@@yandex.ru')).toBeFalsy();
        expect(emailRegExEng.test('use@r@yandex.ru')).toBeFalsy();
    });

    test('Нельзя точку вначале', () => {
        expect(emailRegExEng.test('.user@yandex.ru')).toBeFalsy();
    });

    test('Можно точку внутри левой части', () => {
        expect(emailRegExEng.test('us.er@yandex.ru')).toBeTruthy();
    });

    test("Валидные символы в левой части: _#$%&'*+/=!?^`{}|~-", () => {
        expect(
            emailRegExEng.test("_#$%&'*+/=!?^`{}|~-@yandex.ru"),
        ).toBeTruthy();
    });

    test.each('—,;:'.split(''))(
        'Не пропускает невалидный символ %s в левой части',
        invalidSymbol => {
            expect(
                emailRegExEng.test(`user${invalidSymbol}@yandex.ru`),
            ).toBeFalsy();
        },
    );

    test('Валидные символы в правой части до точки: -', () => {
        expect(emailRegExEng.test('user@yande-x.ru')).toBeTruthy();
    });

    test.each("_#$%&'*+/=!?^`{}|~;,".split(''))(
        'Не пропускает невалидный символ %s в правой части до точки',
        invalidSymbol => {
            expect(
                emailRegExEng.test(`user@yan${invalidSymbol}dex.ru`),
            ).toBeFalsy();
        },
    );

    test('Можно точку внутри правой части', () => {
        expect(emailRegExEng.test('user@yan.dex.ru')).toBeTruthy();
    });

    test('Цифры могут быть везде, кроме TLD', () => {
        expect(emailRegExEng.test('1us1er1@1yan1dex1.ru')).toBeTruthy();
    });

    test('Очень короткая почта', () => {
        expect(emailRegExEng.test('u@y.ru')).toBeTruthy();
    });

    test.each("1234567890-_#$%&'*+/=!?^`{}|~;,".split(''))(
        'TLD может содержать только буквы',
        invalidSymbol => {
            expect(
                emailRegExEng.test(`user@yandex.r${invalidSymbol}u`),
            ).toBeFalsy();
        },
    );

    test('Длина TLD должна быть больше или равна двум символам', () => {
        expect(emailRegExEng.test('user@yandex.r')).toBeFalsy();
    });

    test('Опционально можно почту с русскими символами', () => {
        const emailToRegExRu = new RegExp(
            getEmailValidationRegExp({allowRussianCharacters: true}),
        );

        expect(emailToRegExRu.test('почтаеё@россеёия.рф')).toBeTruthy();
    });
});
