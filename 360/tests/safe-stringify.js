const safeStringify = require('../lib/safe-stringify');
const expect = require('expect');

describe('safe-stringify', () => {
    it('пустой объект', () => {
        expect(safeStringify({}))
            .toEqual('{}');
    });

    it('"безопасный" объект', () => {
        const object = {
            name: 'document.pdf',
            preview: {
                s: 'https://downloader.disk.yandex.net/s',
                xxl: 'https://downloader.disk.yandex.net/xxl'
            },
            sizes: ['s', 'xxl']
        };
        expect(safeStringify(object))
            .toEqual(JSON.stringify(object));
    });

    it('"опасный" объект', () => {
        const object = {
            name: '"<script>alert(1)</script>',
            preview: {
                s: 'https://downloader.disk.yandex.net/preview?size=s&param=value',
                xxl: 'https://downloader.disk.yandex.net/preview?size=xxl&param=value'
            },
            sizes: ['s', 'xxl']
        };
        expect(safeStringify(object))
            .toEqual(
                // eslint-disable-next-line max-len
                '{\"name\":\"\\\"\\u003cscript\\u003ealert(1)\\u003c/script\\u003e\",\"preview\":{\"s\":\"https://downloader.disk.yandex.net/preview?size=s\\u0026param=value\",\"xxl\":\"https://downloader.disk.yandex.net/preview?size=xxl\\u0026param=value\"},\"sizes\":[\"s\",\"xxl\"]}'
            );
    });
});
