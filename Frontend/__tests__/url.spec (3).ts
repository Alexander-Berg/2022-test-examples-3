import {
    isAbsolute,
    isRelative,
    isRelativeRoot,
    isRelativePath,
    isRelativeAnchor,
    createUrl,
} from '../url';

describe('URL', () => {
    describe('isAbsolute()', () => {
        it('Проверяет абсолютный путь https', () => {
            expect(isAbsolute('https://yandex.ru/turbo?text=foo')).toBe(true);
        });

        it('Проверяет абсолютный путь HTTP', () => {
            expect(isAbsolute('HTTP://yandex.ru/turbo?text=foo')).toBe(true);
        });

        it('Проверяет абсолютный путь без протокола', () => {
            expect(isAbsolute('//yandex.ru/turbo?text=foo')).toBe(true);
        });

        it('Проверяет относительный путь', () => {
            expect(isAbsolute('/turbo?text=foo')).toBe(false);
        });
    });

    describe('isRelative()', () => {
        it('Проверяет абсолютный путь', () => {
            expect(isRelative('https://yandex.ru/turbo?text=foo')).toBe(false);
        });

        it('Проверяет абсолютный путь без протокола', () => {
            expect(isRelative('//yandex.ru/turbo?text=foo')).toBe(false);
        });

        it('Проверяет относительный путь от корня', () => {
            expect(isRelative('/turbo?text=foo')).toBe(true);
        });

        it('Проверяет относительный путь с точки от текущего пути', () => {
            expect(isRelative('./turbo?text=foo')).toBe(true);
        });

        it('Проверяет относительный путь без слеша от текущего пути', () => {
            expect(isRelative('turbo?text=foo')).toBe(true);
        });

        it('Проверяет относительный путь для якоря', () => {
            expect(isRelative('#turbo')).toBe(true);
        });
    });

    describe('isRelativeRoot()', () => {
        it('Проверяет относительный путь от корня', () => {
            expect(isRelativeRoot('/turbo?text=foo')).toBe(true);
        });

        it('Проверяет относительный путь с точки от текущего пути', () => {
            expect(isRelativeRoot('./turbo?text=foo')).toBe(false);
        });

        it('Проверяет относительный путь без слеша от текущего пути', () => {
            expect(isRelativeRoot('turbo?text=foo')).toBe(false);
        });

        it('Проверяет относительный путь для якоря', () => {
            expect(isRelativeRoot('#turbo')).toBe(false);
        });
    });

    describe('isRelativeAnchor()', () => {
        it('Проверяет относительный путь для якоря', () => {
            expect(isRelativeAnchor('#turbo')).toBe(true);
        });
    });

    describe('isRelativePath()', () => {
        it('Проверяет относительный путь от корня', () => {
            expect(isRelativePath('/turbo?text=foo')).toBe(false);
        });

        it('Проверяет относительный путь с точки от текущего пути', () => {
            expect(isRelativePath('./turbo?text=foo')).toBe(true);
        });

        it('Проверяет относительный путь с двух точек от текущего пути', () => {
            expect(isRelativePath('../turbo?text=foo')).toBe(true);
        });

        it('Проверяет относительный путь без слеша от текущего пути', () => {
            expect(isRelativePath('turbo?text=foo')).toBe(true);
        });

        it('Проверяет относительный путь для якоря', () => {
            expect(isRelativePath('#turbo')).toBe(false);
        });
    });

    describe('toString()', () => {
        it('Обрабатывает абсолютную ссылку', () => {
            const url = createUrl('https://yandex.ru/turbo?text=foo');
            expect(url.toString()).toBe('https://yandex.ru/turbo?text=foo');
        });

        it('Обрабатывает абсолютную ссылку без протокола', () => {
            const url = createUrl('//yandex.ru/turbo?text=foo');
            expect(url.toString()).toBe('//yandex.ru/turbo?text=foo');
        });

        it('Обрабатывает относительную ссылку от корня', () => {
            const url = createUrl('/turbo?text=foo');
            expect(url.toString()).toBe('/turbo?text=foo');
        });

        it('Обрабатывает относительную ссылку из одной точки', () => {
            const url = createUrl('.');
            expect(url.toString()).toBe('');
        });

        it('Обрабатывает относительную ссылку с точки от текущего пути', () => {
            const url = createUrl('./turbo?text=foo');
            expect(url.toString()).toBe('turbo?text=foo');
        });

        it('Обрабатывает относительную ссылку без слеша от текущего пути', () => {
            expect(createUrl('yandex.ru').toString()).toBe('yandex.ru');
        });

        it('Обрабатывает относительную ссылку из двух точек', () => {
            const url = createUrl('..');
            expect(url.toString()).toBe('..');
        });

        it('Обрабатывает относительную ссылку с двух точек от текущего пути', () => {
            const url = createUrl('../turbo?text=foo');
            expect(url.toString()).toBe('../turbo?text=foo');
        });

        it('Обрабатывает относительную ссылку с двумя точками в середине', () => {
            const url = createUrl('../turbo/foo/../bar');
            expect(url.toString()).toBe('../turbo/bar');
        });

        it('Обрабатывает якорную ссылку', () => {
            const url = createUrl('#anchor');
            expect(url.toString()).toBe('#anchor');
        });

        it('Корректно формирует адрес относительной ссылки после добавления хоста', () => {
            const url = createUrl('/turbo?text=foo');
            url.host = 'yandex.ru';
            expect(url.toString()).toBe('https://yandex.ru/turbo?text=foo');
        });

        it('Корректно формирует адрес якорной ссылки после добавления хоста и пути', () => {
            const url = createUrl('#anchor');
            url.host = 'yandex.ru';
            url.pathname = 'path/to';
            expect(url.toString()).toBe('https://yandex.ru/path/to#anchor');
        });
    });

    describe('getClearPath()', () => {
        it('Метод getClearPath() должен возвращать путь без слэша на конце', () => {
            expect(createUrl('//ya.ru/path/to/?foo=bar').getClearPath()).toBe('/path/to');
            expect(createUrl('//ya.ru/path/to').getClearPath()).toBe('/path/to');
        });
    });
});
