import { validateEmail } from '../validateEmail';

describe('Валидация email', () => {
    describe('Некорретный email', () => {
        it('Две точки после tld', () => {
            expect(validateEmail('kasimovrash@gmail..com')).toEqual(false);
        });

        it('С пробелом', () => {
            expect(validateEmail('niko 777.56@.mail.ru')).toEqual(false);
        });

        it('Две собачки', () => {
            expect(validateEmail('malgobek1965@@mail.ru')).toEqual(false);
        });

        it('Содержит кириллицу', () => {
            expect(validateEmail('ллд@mail.ru')).toEqual(false);
        });

        it('Содержит кириллицу (первая буква а - русская)', () => {
            expect(validateEmail('а6698649@mail.ru')).toEqual(false);
        });
    });

    describe('Корректный email', () => {
        it('Обычный email', () => {
            expect(validateEmail('email@email.ru')).toEqual(true);
        });

        it('Кириллический домен', () => {
            expect(validateEmail('email@яндекс.рф')).toEqual(true);
        });

        it('С подчеркиваниями', () => {
            expect(validateEmail('test@hel_lo.wor_ld.ru')).toEqual(true);
        });

        it('Несколько уровней доменов', () => {
            expect(validateEmail('email@email.com.рф')).toEqual(true);
        });
    });
});
