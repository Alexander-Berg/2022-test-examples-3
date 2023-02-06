import { StaticSender } from './static';
import { HttpReq } from './types';

describe('StaticSender', function() {
    let staticSender: StaticSender;
    const mocksPath = require('path').resolve(__dirname, './mocks/');

    beforeEach(() => {
        staticSender = new StaticSender();
    });

    function fakeReq(path: string) {
        return {
            path: path,
        } as HttpReq;
    }

    it('возвращает 404 для неизвестных урлов', function() {
        const res = staticSender.handler(fakeReq('/unknown'));

        expect(typeof res).toBe('object');
        expect(res.StatusCode).toEqual(404);
    });

    it('добавляет файлы по одному', function() {
        staticSender.addFile('/qwe/rty', mocksPath + '/dummy.txt');
        staticSender.addFile('/qwe/rty/baz', mocksPath + '/dummy.txt', {
            headers: [
                {
                    Name: 'X-test',
                    Value: 'xxx',
                },
            ],
        });

        const one = staticSender.handler(fakeReq('/qwe/rty'));
        const two = staticSender.handler(fakeReq('/qwe/rty/baz'));

        [one, two].forEach((res, i) => {
            expect(typeof res).toBe('object');
            expect(res.StatusCode).toEqual(200);
            expect(res.Content && res.Content.toString()).toEqual('test string');
            expect(res.Headers && res.Headers.filter(({ Name }) => Name === 'X-test')).toHaveLength(i);
        });
    });

    it('добавляет файлы в директории', function() {
        staticSender.addDir('/test/mocks', mocksPath);
        [
            '/test/mocks/dummy.xml',
            '/test/mocks/dummy.js',
            '/test/mocks/dummy.txt',
        ].forEach((url, i) => {
            const res = staticSender.handler(fakeReq(url));

            expect(res.StatusCode).toEqual(200);
            expect(res.Content && res.Content.toString()).toEqual([
                '<xml></xml>',
                'alert();\n',
                'test string',
            ][i]);
            expect(res.Headers).toBeTruthy();

            if (res.Headers) {
                expect(res.Headers.filter(({ Name }) => Name === 'Content-Type')[0].Value)
                    .toBe([
                        'application/xml',
                        'application/javascript; charset=UTF-8',
                        'text/plain; charset=UTF-8',
                    ][i]);
            }
        });
    });

    describe('заголовки', function() {
        beforeEach(() => {
            staticSender.addFile('/test/file', mocksPath + '/dummy.txt');
        });
        it('возвращает зголовки', function() {
            const res = staticSender.handler(fakeReq('/test/file'));

            expect(res.Headers).toBeTruthy();

            if (res.Headers) {
                const header = res.Headers.find(({ Name }) => Name === 'Last-Modified');

                expect(typeof header).toBe('object');

                if (header && header.Value) {
                    expect(typeof header.Value).toBe('string');
                    const date = new Date(header.Value);

                    expect(date.toString()).not.toBe('Invalid Date');
                }

                const date = res.Headers.find(({ Name }) => Name === 'Date');

                expect(date && date.Value).toBeTruthy();
                if (date && date.Value) {
                    expect(new Date(date.Value).toString()).not.toBe('Invalid Date');
                }

                const contentType = res.Headers.find(({ Name }) => Name === 'Content-Type');

                expect(contentType && contentType.Value).toBe('text/plain; charset=UTF-8');
            }
        });

        it('отдаёт 304, если файл не изменился', function() {
            const res = staticSender.handler({
                path: '/test/file',
                headers: {
                    'if-modified-since': new Date().toUTCString(),
                },
            } as unknown as HttpReq);

            expect(res.StatusCode).toBe(304);
            if (res.Content) {
                expect(res.Content.toString()).toBe('');
            }
        });

        it('отдаёт 200, если файл изменился', function() {
            const res = staticSender.handler({
                path: '/test/file',
                headers: {
                    'if-modified-since': new Date(1999).toUTCString(),
                },
            } as unknown as HttpReq);

            expect(res.StatusCode).toBe(200);
            if (res.Content) {
                expect(res.Content.toString()).toBe('test string');
            }
        });

        it('умеет отдавать HEAD', function() {
            const res = staticSender.handler({
                path: '/test/file',
                method: 'Head',
            } as unknown as HttpReq);

            expect(res.StatusCode).toBe(200);
            expect(res.Headers).toBeTruthy();
            if (res.Headers) {
                expect(res.Headers.find(({ Name }) => Name === 'Content-Type')).toBeTruthy();
                expect(res.Headers.find(({ Name }) => Name === 'Last-Modified')).toBeTruthy();
            }
            if (res.Content) {
                expect(res.Content.toString()).toBe('');
            }
        });
    });
});
