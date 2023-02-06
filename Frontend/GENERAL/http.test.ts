import { ProtobufJSContext } from '@yandex-int/apphost-lib';
import { NAppHostHttp } from '../generated/protos';
import { HttpReq } from './types';
import { HttpRequestParser } from './http';
const { THttpRequest } = NAppHostHttp;

describe('HttpRequestParser', () => {
    let parser: HttpRequestParser;
    const req = {
        url: '/qwe/rty',
        path: '/qwe/rty',
    } as unknown as HttpReq;
    const ctx = {} as unknown as ProtobufJSContext;
    beforeEach(() => {
        parser = new HttpRequestParser(() => {
            return Promise.resolve(THttpRequest.fromObject({}));
        });
    });
    describe('match', () => {
        it('использует строгое сравнение, если path строка', () => {
            const log: number[] = [];
            parser.match('/qwe/rty', () => { log.push(1) })(req, ctx);
            parser.match('/qwe/rtybar', () => { log.push(2) })(req, ctx);
            parser.match('/qwe/rty/bar', () => { log.push(3) })(req, ctx);
            parser.match('/qwe/rt', () => { log.push(4) })(req, ctx);
            parser.match('/qwe', () => { log.push(5) })(req, ctx);
            parser.match('/qwe/rty/', () => { log.push(6) })(req, ctx);
            parser.match('/qwe/rty/', () => { log.push(7) })({
                url: '/qwe/rty/',
                path: '/qwe/rty/',
            } as unknown as HttpReq, ctx);
            expect(log).toEqual([1, 7]);
        });

        it('допускает разный регистр, если path строка', () => {
            const log: number[] = [];
            parser.match('/QWE/rty', () => { log.push(1) })(req, ctx);
            expect(log).toEqual([1]);
        });

        it('допускает опциональный завершающий слэш, если path строка', () => {
            const log: number[] = [];
            parser.match('/qwe/rty', () => { log.push(1) })({
                url: '/qwe/rty/',
                path: '/qwe/rty/',
            } as unknown as HttpReq, ctx);
            expect(log).toEqual([1]);
        });

        it('допускает разный регистр, если path - regexp', () => {
            const log: number[] = [];
            parser.match(/\/qwe/, () => { log.push(1) })(req, ctx);
            parser.match(/\/qWe/, () => { log.push(2) })(req, ctx);
            parser.match(/\/qWe\/baz/, () => { log.push(3) })(req, ctx);
            expect(log).toEqual([1, 2]);
        });
    });
});
