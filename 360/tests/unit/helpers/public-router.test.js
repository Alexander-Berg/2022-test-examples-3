import publicRouter from '../../../components/routers/public';

describe('publicRouter', () => {
    describe('Метод `parseUrl`', () => {
        it('Короткий hash', () => {
            expect(publicRouter.parseUrl('/d/d5FOTH8yRdDc')).toEqual({
                prefix: 'd',
                hashShort: 'd5FOTH8yRdDc',
                hashLong: undefined,
                pathResource: undefined,
                sizeImage: undefined
            });
        });

        it('Короткий hash для альбомов', () => {
            expect(publicRouter.parseUrl('/a/d5FOTH8yRdDc')).toEqual({
                prefix: 'a',
                hashShort: 'd5FOTH8yRdDc',
                hashLong: undefined,
                pathResource: undefined,
                sizeImage: undefined
            });
        });

        it('Короткий hash, суффикс картинки', () => {
            expect(publicRouter.parseUrl('/d/d5FOTH8yRdDc_xxl.jpg')).toEqual({
                prefix: 'd',
                hashShort: 'd5FOTH8yRdDc',
                hashLong: undefined,
                pathResource: undefined,
                sizeImage: 'XXL'
            });
        });

        it('Короткий hash, путь к вложенному ресурсу', () => {
            expect(publicRouter.parseUrl('/d/d5FOTH8yRdDc/path/to/resource')).toEqual({
                prefix: 'd',
                hashShort: 'd5FOTH8yRdDc',
                hashLong: undefined,
                pathResource: '/path/to/resource',
                sizeImage: undefined
            });
        });

        it('Короткий hash, `/i/hash/%3C:%3E%23!%20`', () => {
            expect(publicRouter.parseUrl('/i/hash/%3C:%3E%23!%20')).toEqual({
                prefix: 'i',
                hashShort: 'hash',
                hashLong: undefined,
                pathResource: '/<:>#! ',
                sizeImage: undefined
            });
        });

        it('Невалидный короткий hash, `/i/hash_s.png/path`', () => {
            expect(publicRouter.parseUrl('/i/hash_s.png/path')).toBeUndefined();
        });

        it('Длинный hash', () => {
            expect(publicRouter.parseUrl('/public/?hash=LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI=')).toEqual({
                prefix: 'public',
                hashShort: undefined,
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI=',
                pathResource: undefined,
                sizeImage: undefined
            });
        });

        it('Длинный hash, путь к вложенному ресурсу', () => {
            expect(publicRouter.parseUrl('/public/?hash=LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI=:/path/to/resource')).toEqual({
                prefix: 'public',
                hashShort: undefined,
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI=',
                pathResource: '/path/to/resource',
                sizeImage: undefined
            });
        });

        it('Длинный hash, `/mail/?hash=a%2Bb%3D%3A%2F%3C%3A%3E!%20`', () => {
            expect(publicRouter.parseUrl('/mail/?hash=a%2Bb%3D%3A%2F%3C%3A%3E!%20')).toEqual({
                prefix: 'mail',
                hashShort: undefined,
                hashLong: 'a+b=',
                pathResource: '/<:>! ',
                sizeImage: undefined
            });
        });

        it('Путь альбома', () => {
            expect(publicRouter.parseUrl('/a/d5FOTH8yRdDc')).toEqual({
                prefix: 'a',
                hashShort: 'd5FOTH8yRdDc',
                hashLong: undefined,
                pathResource: undefined,
                sizeImage: undefined
            });
        });

        it('Путь ресурса в альбоме', () => {
            expect(publicRouter.parseUrl('/a/d5FOTH8yRdDc/54886de09292653a7da90c84')).toEqual({
                prefix: 'a',
                hashShort: 'd5FOTH8yRdDc',
                hashLong: undefined,
                pathResource: '54886de09292653a7da90c84',
                sizeImage: undefined
            });
        });
    });

    describe('Методы `formatIdRoot`/`formatIdContext`', () => {
        it('Без вложенного ресурса', () => {
            const parsedUrl = {
                prefix: 'i',
                hashShort: 'hash',
                hashLong: 'a+b/c=',
                pathResource: null
            };
            expect(publicRouter.formatIdRoot(parsedUrl)).toEqual('/public/a-b_c=');
            expect(publicRouter.formatIdContext(parsedUrl)).toEqual('/public/a-b_c=');
        });
        it('Вложенный ресурс', () => {
            const parsedUrl = {
                prefix: 'i',
                hashShort: 'hash',
                hashLong: 'a+b/c=',
                pathResource: '/<:>! '
            };
            expect(publicRouter.formatIdRoot(parsedUrl)).toEqual('/public/a-b_c=');
            expect(publicRouter.formatIdContext(parsedUrl)).toEqual('/public/a-b_c=:/<:>! ');
        });
        it('Альбом', () => {
            const parsedUrl = {
                prefix: 'a',
                hashShort: 'd5FOTH8yRdDc',
                hashLong: '045dcde9e6a4457e80fe6edf03c99d4c',
                pathResource: undefined
            };
            expect(publicRouter.formatIdRoot(parsedUrl)).toEqual('/album/045dcde9e6a4457e80fe6edf03c99d4c');
            expect(publicRouter.formatIdContext(parsedUrl)).toEqual('/album/045dcde9e6a4457e80fe6edf03c99d4c');
        });
        it('Ресурс в альбоме', () => {
            const parsedUrl = {
                prefix: 'a',
                hashShort: 'd5FOTH8yRdDc',
                hashLong: '045dcde9e6a4457e80fe6edf03c99d4c',
                pathResource: '54886de09292653a7da90c84'
            };
            expect(publicRouter.formatIdRoot(parsedUrl)).toEqual('/album/045dcde9e6a4457e80fe6edf03c99d4c');
            expect(publicRouter.formatIdContext(parsedUrl)).toEqual('/album/045dcde9e6a4457e80fe6edf03c99d4c:54886de09292653a7da90c84');
        });
    });

    describe('Метод `formatUrl`', () => {
        it('Короткий hash', () => {
            expect(publicRouter.formatUrl({
                prefix: 'd',
                hashShort: 'd5FOTH8yRdDc'
            })).toEqual('/d/d5FOTH8yRdDc');
        });
        it('Короткий hash, путь к вложенному ресурсу', () => {
            expect(publicRouter.formatUrl({
                prefix: 'd',
                hashShort: 'd5FOTH8yRdDc',
                pathResource: '/path/to/<:>#! '
            })).toEqual('/d/d5FOTH8yRdDc/path/to/%3C%3A%3E%23!%20');
        });

        it('Длинный hash', () => {
            expect(publicRouter.formatUrl({
                prefix: 'public',
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI='
            })).toEqual('/public/?hash=' + encodeURIComponent('LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI='));
        });
        it('Длинный hash, путь к вложенному ресурсу', () => {
            expect(publicRouter.formatUrl({
                prefix: 'public',
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDt+=',
                pathResource: '/path/to/<:>! '
            })).toEqual('/public/?hash=' + encodeURIComponent('LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDt+=:/path/to/<:>! '));
        });
        it('Альбом', () => {
            expect(publicRouter.formatUrl({
                prefix: 'a',
                hashShort: 'd5FOTH8yRdDc'
            })).toEqual('/a/d5FOTH8yRdDc');
        });
        it('Ресурс в альбоме', () => {
            expect(publicRouter.formatUrl({
                prefix: 'a',
                hashShort: 'd5FOTH8yRdDc',
                hashLong: '045dcde9e6a4457e80fe6edf03c99d4c',
                pathResource: '54886de09292653a7da90c84'
            })).toEqual('/a/d5FOTH8yRdDc/54886de09292653a7da90c84');
        });
    });
    describe('Метод `parseId`', () => {
        it('Без пути к вложенному ресурсу', () => {
            expect(publicRouter.parseId('/public/LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI=')).toEqual({
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI='
            });
        });
        it('С путём к вложенному ресурсу', () => {
            expect(publicRouter.parseId('/public/LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI=:/path/to/resource')).toEqual({
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI=',
                pathResource: '/path/to/resource'
            });
        });
        it('С путём к вложенному ресурсу, содержащим символ ":"', () => {
            expect(publicRouter.parseId('/public/LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI=:/path:to:resource')).toEqual({
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI=',
                pathResource: '/path:to:resource'
            });
        });
        it('Альбом', () => {
            expect(publicRouter.parseId('/album/LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI')).toEqual({
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI'
            });
        });
        it('Ресурс в альбоме', () => {
            expect(publicRouter.parseId('/album/LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI:54886de09292653a7da90c84')).toEqual({
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDtI',
                pathResource: '54886de09292653a7da90c84'
            });
        });
    });

    describe('Совместимость методов parseUrl и formatUrl', () => {
        it('formatUrl(parseUrl(short)) == short', () => {
            const url = '/i/hash/path/%3C%3A%3E%23!%20';
            expect(publicRouter.formatUrl(publicRouter.parseUrl(url))).toEqual(url);
        });
        it('formatUrl(parseUrl(long)) == long', () => {
            const url = '/public/?hash=' + encodeURIComponent('a+b=:/path/<:>! ');
            expect(publicRouter.formatUrl(publicRouter.parseUrl(url))).toEqual(url);
        });
        it('parseUrl(formatUrl(longParsed)) == longParsed', () => {
            const urlParsed = {
                prefix: 'public',
                hashShort: undefined,
                hashLong: 'LIs5yfuz46AjvHYHA1fZjN4LFJ0H9fSbeZOHudKrDt+=',
                pathResource: '/path/to/<:>! ',
                sizeImage: undefined
            };
            expect(publicRouter.parseUrl(publicRouter.formatUrl(urlParsed))).toEqual(urlParsed);
        });
        it('formatUrl(parseUrl(album)) == album', () => {
            const url = '/a/d5FOTH8yRdDc';
            expect(publicRouter.formatUrl(publicRouter.parseUrl(url))).toEqual(url);
        });
        it('formatUrl(parseUrl(albumResource)) == albumResource', () => {
            const url = '/a/d5FOTH8yRdDc/54886de09292653a7da90c84';
            expect(publicRouter.formatUrl(publicRouter.parseUrl(url))).toEqual(url);
        });
    });
});
