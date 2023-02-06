import { addHttps } from '../addHttps';

describe('utils/addHttps', () => {
    it('Adds https if url was without https', () => {
        expect(addHttps('yandex.ru')).toBe('https://yandex.ru');
    });

    it('Doesnt add https if url already has http', () => {
        expect(addHttps('http://yandex.ru')).toBe('http://yandex.ru');
    });

    it('Doesnt add another https if url already  has it', () => {
        expect(addHttps('https://yandex.ru')).toBe('https://yandex.ru');
    });
});
