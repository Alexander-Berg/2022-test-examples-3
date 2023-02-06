import { assert } from 'chai';
import {
    testdataSlowReplacer,
    testdataQueryObjectReplacer,
    testdataReplacer,
    testdataStringReplacer,
} from '../../../src/plugins/tide-testdata-manager/options';
import { TestdataManagerOptions, UrlPlain } from '../../../src/plugins/tide-testdata-manager/types';

describe('tide-testdata-manager / utils', () => {
    const options = {
        oldUrl: {
            query: [['exp_flags', 'organicable=1;organicable_auto=1']],
        },
        newUrl: {
            query: [['exp_flags', 'another-flag']],
        },
    } as TestdataManagerOptions;

    describe('testdataStringReplacer', () => {
        it('should replace full urls in text', () => {
            const input = {
                url: `x-forwarded-host: %%HOST%%~X-Yandex-LaaS-UaaS-Disabled: 1~X-Test-Header: /some/path?param=value
            ~user-agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36~X-Version: v=2
            ~X-Yandex-RandomUID: 2401830841622807447~x-original-url: https://yp-c.yandex.net/search/?text=foreverdata&foreverdata=2780918842&exp_flags=organicable%3D1%3Borganicable_auto%3D1&exp_flags=test_tool%3Dhermione~X-Metabalancer-Fqdn:
            ~https://bolver-5.vla.yp-c.yandex.net/api?exp_flags=organicable%3D1%3Borganicable_auto%3D1~X-Yandex-HTTPS: yes`,
            };
            const expected = `x-forwarded-host: %%HOST%%~X-Yandex-LaaS-UaaS-Disabled: 1~X-Test-Header: /some/path?param=value
            ~user-agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36~X-Version: v=2
            ~X-Yandex-RandomUID: 2401830841622807447~x-original-url: https://yp-c.yandex.net/search/?text=foreverdata&foreverdata=2780918842&exp_flags=another-flag&exp_flags=test_tool%3Dhermione~X-Metabalancer-Fqdn:
            ~https://bolver-5.vla.yp-c.yandex.net/api?exp_flags=another-flag~X-Yandex-HTTPS: yes`;

            const actual = testdataStringReplacer(input, ['url'], options);

            assert.equal(actual, expected);
        });

        it('should replace urls in query strings', () => {
            const input = {
                url: `some text dump=reqans&exp_flags=test_tool%3Dhermione&exp_flags=organicable%3D1%3Borganicable_auto%3D1&exp_flags=cspid%3DY2h1Y2sgbm9ycmlz&tpid=889ff9b%2Fchrome-desktop~header`,
            };
            const expected = `some text dump=reqans&exp_flags=test_tool%3Dhermione&exp_flags=another-flag&exp_flags=cspid%3DY2h1Y2sgbm9ycmlz&tpid=889ff9b%2Fchrome-desktop~header`;

            const actual = testdataStringReplacer(input, ['url'], options);

            assert.equal(actual, expected);
        });
    });

    describe('testdataQueryObjectReplacer', () => {
        it('should replace params in query object', () => {
            const input = {
                data: {
                    args: {
                        exp_flags: [
                            'test_tool=hermione',
                            'organicable=1;organicable_auto=1',
                            'cspid=Y2h1Y2sgbm9ycmlz',
                        ],
                    },
                },
            };
            const path = ['data', 'args', 'exp_flags'];
            const expected = ['test_tool=hermione', 'another-flag', 'cspid=Y2h1Y2sgbm9ycmlz'];

            const actual = testdataQueryObjectReplacer(input, path, options);

            assert.deepEqual(actual, expected);
        });
    });

    describe('testdataSlowReplacer', () => {
        it('should not change object if no url is found', () => {
            const input = {
                prop: {
                    anotherProp: [1, 2, 3],
                },
            };

            const actual = testdataSlowReplacer(input, ['prop', 'anotherProp'], options);

            assert.deepEqual(actual, input.prop.anotherProp);
        });

        it('should call string replacer for strings', () => {
            const input = {
                url: `some text dump=reqans&exp_flags=test_tool%3Dhermione&exp_flags=organicable%3D1%3Borganicable_auto%3D1&exp_flags=cspid%3DY2h1Y2sgbm9ycmlz&tpid=889ff9b%2Fchrome-desktop~header`,
            };
            const options = {
                oldUrl: {
                    query: [['exp_flags', 'organicable=1;organicable_auto=1']],
                },
                newUrl: {
                    query: [['exp_flags', 'another-flag']],
                },
            } as TestdataManagerOptions;
            const expected = `some text dump=reqans&exp_flags=test_tool%3Dhermione&exp_flags=another-flag&exp_flags=cspid%3DY2h1Y2sgbm9ycmlz&tpid=889ff9b%2Fchrome-desktop~header`;

            const actual = testdataSlowReplacer(input, ['url'], options);

            assert.equal(actual, expected);
        });

        it('should call object replacer for query objects', () => {
            const input = {
                data: {
                    args: {
                        exp_flags: [
                            'test_tool=hermione',
                            'organicable=1;organicable_auto=1',
                            'cspid=Y2h1Y2sgbm9ycmlz',
                        ],
                    },
                },
            };
            const path = ['data', 'args', 'exp_flags'];
            const expected = ['test_tool=hermione', 'another-flag', 'cspid=Y2h1Y2sgbm9ycmlz'];

            const actual = testdataSlowReplacer(input, path, options);

            assert.deepEqual(actual, expected);
        });
    });

    describe('testdataReplacer', () => {
        it('should replace whole url without query', () => {
            const contents =
                '1 2 3 http://example.com/api/v2/?method=1 23 x-forwarded-from:http://example.com/api/v2/?method=2~tilda 1 2 3';
            const search: Partial<UrlPlain> = {
                protocol: 'http',
                hostname: 'example.com',
                pathname: 'api/v2',
            };
            const replacement: Partial<UrlPlain> = {
                protocol: 'https',
                hostname: 'example-2.com',
                pathname: 'api/v3',
            };
            const expected =
                '1 2 3 https://example-2.com/api/v3/?method=1 23 x-forwarded-from:https://example-2.com/api/v3/?method=2~tilda 1 2 3';

            assert.equal(testdataReplacer(search, replacement, contents), expected);
        });

        it('should replace pathname in url without query', () => {
            const contents =
                '~user-agent: Mozilla/5.0; HEADER: /search/category?test=1 X-HTTP-TYPE: /search/category?text=0&q=1 ; category';
            const search: Partial<UrlPlain> = {
                protocol: 'http',
                hostname: 'example.com',
                pathname: 'search/category',
            };
            const replacement: Partial<UrlPlain> = {
                protocol: 'https',
                hostname: 'example-2.com',
                pathname: 'search2/new-category',
            };
            const expected =
                '~user-agent: Mozilla/5.0; HEADER: /search2/new-category?test=1 X-HTTP-TYPE: /search2/new-category?text=0&q=1 ; category';

            assert.equal(testdataReplacer(search, replacement, contents), expected);
        });

        it('should update query in full or partial urls', () => {
            const contents =
                '~user-agent: Mozilla/5.0; HEADER: https://example.com/search/category?param1=test\n' +
                'X-HTTP-TYPE: /search/category?param1=test ; category /search/category?param1=test2\n';
            // TODO: check url prefix 'F: https://example-3.com/search?param1=test';
            const search: Partial<UrlPlain> = {
                hostname: 'example.com',
                query: [['param1', 'test']],
            };
            const replacement: Partial<UrlPlain> = {
                hostname: 'example-2.com',
                query: [['new_param', 'new_value']],
            };
            const expected =
                '~user-agent: Mozilla/5.0; HEADER: https://example-2.com/search/category?new_param=new_value\n' +
                'X-HTTP-TYPE: /search/category?new_param=new_value ; category /search/category?param1=test2\n';
            // TODO: check url prefix 'F: https://example-3.com/search?param1=test';

            assert.equal(testdataReplacer(search, replacement, contents), expected);
        });

        it('should remove url parameter from the url', () => {
            // TODO
            const contents =
                '~user-agent: Mozilla/5.0; HEADER: https://example.com/search/category?param1=value\n' +
                'X-HTTP-TYPE: /search/category?param1=value ; category /search/category?text=test&param1=value2\n' +
                'debug; https://example.com/search/category?param1=value&text=test ~ \n' +
                'Accept-type: https://example.com/search/category?text=test&param1=value&flag=2\n' +
                'encoding# /search/category?test=1&param1=value\n' +
                'If:"~VIDEOWIZ.feat.Pos && UPPER.VideoSerpdata.wasDocWithFragment == 1';
            // TODO: check url prefix

            const search: Partial<UrlPlain> = {
                query: [['param1', 'value']],
            };
            const replacement: Partial<UrlPlain> = {
                query: [['', '']],
            };
            const expected =
                '~user-agent: Mozilla/5.0; HEADER: https://example.com/search/category\n' +
                'X-HTTP-TYPE: /search/category ; category /search/category?text=test&param1=value2\n' +
                'debug; https://example.com/search/category?text=test ~ \n' +
                'Accept-type: https://example.com/search/category?text=test&flag=2\n' +
                'encoding# /search/category?test=1\n' +
                'If:"~VIDEOWIZ.feat.Pos && UPPER.VideoSerpdata.wasDocWithFragment == 1';
            // TODO: check url prefix

            assert.equal(testdataReplacer(search, replacement, contents), expected);
        });

        it('should remove flag by name', () => {
            // TODO
            const contents =
                '~user-agent: Mozilla/5.0; HEADER: https://example.com/search/category?exp_flags=flag=3\n' +
                'X-HTTP-TYPE: /search/category?exp_flags=flag=3 ; category /search/category?text=test&exp_flags=flag2=3\n' +
                'debug; https://example.com/search/category?exp_flags=flag=3&text=test ~ \n' +
                'Accept-type: https://example.com/search/category?text=test&exp_flags=flag=3&flag=2\n' +
                'encoding# /search/category?test=1&exp_flags=flag=3\n' +
                'If:"~VIDEOWIZ.feat.Pos && UPPER.VideoSerpdata.wasDocWithFragment == 1';
            // TODO: check url prefix

            const search: Partial<UrlPlain> = {
                query: [['exp_flags', 'flag=[\\w%-]*']],
            };
            const replacement: Partial<UrlPlain> = {
                query: [['', '']],
            };
            const expected =
                '~user-agent: Mozilla/5.0; HEADER: https://example.com/search/category\n' +
                'X-HTTP-TYPE: /search/category ; category /search/category?text=test&exp_flags=flag2=3\n' +
                'debug; https://example.com/search/category?text=test ~ \n' +
                'Accept-type: https://example.com/search/category?text=test&flag=2\n' +
                'encoding# /search/category?test=1\n' +
                'If:"~VIDEOWIZ.feat.Pos && UPPER.VideoSerpdata.wasDocWithFragment == 1';
            // TODO: check url prefix

            assert.equal(testdataReplacer(search, replacement, contents), expected);
        });

        it('should add url parameter to the url', () => {
            // TODO additions to url without query
            const contents =
                '~user-agent: Mozilla/5.0; HEADER: https://example.com/search/category\n' +
                'X-HTTP-TYPE: /search/category ; category /search/category?text=test&param1=value2\n' +
                'debug; https://example.com/search/category?text=test ~ \n' +
                'Accept-type: https://example.com/search/category?text=test&tpid=797940d/chrome-desktop&flag=2 ' +
                'encoding# /search/category?test=1';
            // TODO: check url prefix

            const search: Partial<UrlPlain> = {
                query: [['', '']],
            };
            const replacement: Partial<UrlPlain> = {
                query: [['param1', 'value']],
            };
            const expected =
                '~user-agent: Mozilla/5.0; HEADER: https://example.com/search/category\n' +
                'X-HTTP-TYPE: /search/category ; category /search/category?text=test&param1=value2&param1=value\n' +
                'debug; https://example.com/search/category?text=test&param1=value ~ \n' +
                'Accept-type: https://example.com/search/category?text=test&tpid=797940d/chrome-desktop&flag=2&param1=value ' +
                'encoding# /search/category?test=1&param1=value';

            // TODO: check url prefix

            assert.equal(testdataReplacer(search, replacement, contents), expected);
        });
    });
});
