import { getDomain } from './online-event-card.utils';

describe('online-event-card', function () {
    describe('utils', function () {
        describe('getDomain', function () {
            it('Возвращает пустую строку если не передать домен', () => {
                expect((getDomain as () => string)()).toBe('');
            });

            it('Возвращает домен если передан домен', () => {
                expect(getDomain('yandex.ru')).toBe('yandex.ru');
            });

            it('Возвращает домен если передан домен без протокола', () => {
                expect(getDomain('//yandex.ru')).toBe('yandex.ru');
            });

            it('Возвращает домен если передан домен с http', () => {
                expect(getDomain('http://yandex.ru')).toBe('yandex.ru');
            });

            it('Возвращает домен если передан домен с https', () => {
                expect(getDomain('https://yandex.ru')).toBe('yandex.ru');
            });

            it('Возвращает домен если передан домен и путь', () => {
                expect(getDomain('yandex.ru/path')).toBe('yandex.ru');
            });
        });
    });
});
