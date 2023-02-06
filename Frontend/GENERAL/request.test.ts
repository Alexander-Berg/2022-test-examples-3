import { parse as urlParse } from 'url';
import httpServer, { Server, Params } from '../__mocks__/http-server';
import { get } from './request';

jest.mock('./rum');

type MockedFetch = jest.MockedFunction<typeof fetch>;

const originalFetch = fetch;

describe('lib', () => {
    describe('request', () => {
        let server: Server;
        let counters: { [key: string]: number } = {};

        beforeAll(async function() {
            const serverParams: Params = {
                middleware: (req, res) => {
                    res.setHeader('Access-Control-Allow-Origin', '*');
                    res.setHeader('Access-Control-Allow-Credentials', 'true');
                    res.setHeader(
                        'Access-Control-Allow-Headers',
                        [
                            'accept-language',
                            'content-type',
                            'x-request-id',
                            'ta-features',
                            'csrf-token',
                            'ya-weather-specific',
                        ].join(',')
                    );
                },
            };

            // автоматически выбранный порт порой может оказаться недоступным
            server = await Promise.resolve(httpServer(serverParams))
                .catch(() => httpServer(serverParams))
                .catch(() => httpServer(serverParams));

            server.on('/test/ok', (req, res) => {
                res.end('{"ok":true}');
            });

            server.on('/test/bug', (req, res) => {
                if (req.method === 'OPTIONS') {
                    /**
                     * повтор при фейле OPTIONS-запросе есть
                     * однако в тестах валятся красные сообщения от preflight
                     * и тесты проходят
                     */
                    return res.end();
                }

                const retryCounter = req.parsedUrl.qs.retryCounter;
                const retryAmount = parseInt(req.parsedUrl.qs.retry, 10);

                let needRetry = false;

                if (retryCounter) {
                    counters[retryCounter] = counters[retryCounter] || 0;

                    if (counters[retryCounter] < retryAmount) {
                        needRetry = true;
                        counters[retryCounter]++;
                    }
                }

                let response = '{"bug":true}';

                if (needRetry) {
                    res.statusCode = 503;
                    res.statusMessage = 'Expected need retry';
                }

                res.end(response);
            });

            // @ts-ignore
            await server.start();

            Object.defineProperty(window, 'fetch', {
                writable: true,
                value: jest.fn().mockImplementation((url: string, options) => {
                    url = (url.match(/(\/test\/.*)/) && RegExp.$1) || '';

                    return originalFetch(`${server.url}${url}`, options);
                }),
            });
        });

        beforeEach(() => {
            (fetch as MockedFetch).mockClear();
        });

        afterAll(async function() {
            Object.defineProperty(window, 'fetch', {
                writable: true,
                value: originalFetch,
            });

            // @ts-ignore
            await server.finish();
        });

        it('should be ok in common cases', async function() {
            expect(await get('/test/ok')).toEqual({ ok: true });
            expect((fetch as MockedFetch).mock.calls.length).toBe(1);
            expect(await get('/test/ok', { retryCount: 0 })).toEqual({ ok: true });
            expect((fetch as MockedFetch).mock.calls.length).toBe(2);
        });

        it('should be signed', async function() {
            const mockCalls = (fetch as MockedFetch).mock.calls;
            const query = {
                param: '1',
                'get-param': 'abcde',
                foo: 'bar'
            };
            const expectedQuery = {
                ...query,
                turboapp: '1',
                ta_features: 'favorites;sgn'
            };
            const expectedQueryMD5 = 'ee43b7fe3696a08d6edf3cb415af4c71'; // encodeURI + toLowercase

            expect(await get('/test/ok', { query })).toEqual({ ok: true });

            const [lastUrl, lastOpts] = mockCalls[mockCalls.length - 1];
            const parsedLastUrl = urlParse(lastUrl as string, true);
            const yaWeatherSpecific = (lastOpts?.headers as { [key: string]: string })['ya-weather-specific'];

            expect(parsedLastUrl.query).toMatchObject(expectedQuery);
            expect(yaWeatherSpecific).toMatch(new RegExp(`^\\w{32}${expectedQueryMD5}\\w{32}$`));
        });

        it('should be ok with some retries', async function() {
            expect(await get('/test/bug?retryCounter=retries1&retry=2', { retryCount: 3, retryTimeout: 10 })).toEqual({
                bug: true,
            });
            expect((fetch as MockedFetch).mock.calls.length).toBe(3);
        });

        it('should be able to abort', async function() {
            const aborter = new AbortController();
            const request = get('/test/bug?retryCounter=retries2&retry=2', {
                retryCount: 3,
                retryTimeout: 20,
                signal: aborter.signal,
            });

            await new Promise(resolve =>
                setTimeout(() => {
                    aborter.abort();
                    resolve();
                }, 10)
            );

            await expect(request).rejects.toThrow('Aborted');
            expect((fetch as MockedFetch).mock.calls.length).toBeLessThan(3);
        });
    });
});
