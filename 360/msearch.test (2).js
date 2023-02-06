'use strict';

const msearch = require('./msearch.js');
const getSearchFeatureParams = require('./_helpers/get-search-feature-params.js');

jest.mock('./_helpers/get-search-feature-params.js');

let suggestDefaultOptions;
let suggestDefaultOptionsGetter;
let core;

const emptyBase64String = Buffer.from('').toString('base64');

beforeEach(function() {
    core = {
        auth: {
            get: jest.fn(() => ({
                mdb: 'test-mdb',
                suid: 'test-suid',
                uid: 'test-uid',
                userTicket: 'test-user-ticket'
            }))
        },
        got: jest.fn(),
        config: {
            USER_IP: 'test_user_ip',
            services: {
                msearch: 'test-msearch'
            }
        },
        req: {},
        params: {}
    };

    suggestDefaultOptions = {
        queryParamNormal: 100,
        timeoutNormal: 200,
        timeoutFast: 300,
        queryParamFast: 400
    };

    suggestDefaultOptionsGetter = jest.fn(() => suggestDefaultOptions);
});

test('should calls core.got with correct url', function() {
    msearch(core, '/test-method', {}, {}, suggestDefaultOptionsGetter);

    expect(core.got).toHaveBeenCalledWith('test-msearch/api/async/mail/test-method', expect.any(Object));
});

describe('should calls core.got with correct options', function() {
    it('if IS_CORP=true', function() {
        core.config.IS_CORP = true;

        msearch(core, '/test-method', {}, {}, suggestDefaultOptionsGetter);
        expect(core.got).toHaveBeenCalledWith(
            expect.any(String),
            {
                json: true,
                query: {
                    mdb: 'test-mdb',
                    remote_ip: 'test_user_ip',
                    side: 'webYT',
                    suid: 'test-suid',
                    uid: 'test-uid'
                },
                headers: {
                    'x-feature-params': emptyBase64String
                }
            }
        );
    });

    it('if pddDomain exists', function() {
        core.config.pddDomain = 'test-pdd-domain';

        msearch(core, '/test-method', {}, {}, suggestDefaultOptionsGetter);
        expect(core.got).toHaveBeenCalledWith(
            expect.any(String),
            {
                json: true,
                query: {
                    mdb: 'test-mdb',
                    remote_ip: 'test_user_ip',
                    side: 'webpdd',
                    suid: 'test-suid',
                    uid: 'test-uid'
                },
                headers: {
                    'x-feature-params': emptyBase64String
                }
            }
        );
    });

    it('if service params are passed', function() {
        msearch(core, '/test-method', { testParam: true }, {}, suggestDefaultOptionsGetter);

        expect(core.got).toHaveBeenCalledWith(
            expect.any(String),
            {
                json: true,
                query: {
                    testParam: true,
                    mdb: 'test-mdb',
                    remote_ip: 'test_user_ip',
                    side: 'web',
                    suid: 'test-suid',
                    uid: 'test-uid'
                },
                headers: {
                    'x-feature-params': emptyBase64String
                }
            }
        );
    });

    describe('if params.twoSteps is passed', function() {
        it('params.status is passed', function() {
            msearch(core, '/test-method', { twoSteps: true, status: 'passed' }, {}, suggestDefaultOptionsGetter);

            expect(core.got).toHaveBeenCalledWith(
                expect.any(String),
                {
                    json: true,
                    query: {
                        mdb: 'test-mdb',
                        remote_ip: 'test_user_ip',
                        side: 'web',
                        suid: 'test-suid',
                        uid: 'test-uid',
                        timeout: 100,
                        twoSteps: true,
                        status: 'passed'
                    },
                    headers: {
                        'x-feature-params': emptyBase64String
                    },
                    timeout: 200
                }
            );
        });

        it('params.status is not passed', function() {
            msearch(core, '/test-method', { twoSteps: true }, {}, suggestDefaultOptionsGetter);
            expect(core.got).toHaveBeenCalledWith(
                expect.any(String),
                {
                    json: true,
                    query: {
                        mdb: 'test-mdb',
                        remote_ip: 'test_user_ip',
                        side: 'web',
                        suid: 'test-suid',
                        uid: 'test-uid',
                        timeout: 400,
                        twoSteps: true
                    },
                    headers: {
                        'x-feature-params': emptyBase64String
                    },
                    timeout: 300
                }
            );
        });
    });

    it('if getSearchFeatureParams returns feature flags', function() {
        const featureFlags = { expSearch1: '123', expSearch2: '456' };

        getSearchFeatureParams.mockImplementation(() => featureFlags);

        msearch(core, '/test-method', {}, {}, suggestDefaultOptionsGetter);
        expect(core.got).toHaveBeenCalledWith(
            expect.any(String),
            {
                json: true,
                query: {
                    mdb: 'test-mdb',
                    remote_ip: 'test_user_ip',
                    side: 'web',
                    suid: 'test-suid',
                    uid: 'test-uid'
                },
                headers: {
                    'x-feature-params': Buffer.from(JSON.stringify(featureFlags)).toString('base64')
                }
            }
        );
    });

    it('if additional options are passed', function() {
        msearch(core, '/test-method', {}, { testOption: true }, suggestDefaultOptionsGetter);

        expect(core.got).toHaveBeenCalledWith(
            expect.any(String),
            {
                testOption: true,
                json: true,
                query: {
                    mdb: 'test-mdb',
                    remote_ip: 'test_user_ip',
                    side: 'web',
                    suid: 'test-suid',
                    uid: 'test-uid'
                },
                headers: {
                    'x-feature-params': emptyBase64String
                }
            }
        );
    });

    describe('if there is tvm', () => {
        beforeEach(() => {
            core.req.tvm = {
                tickets: {
                    msearch: {
                        ticket: 'test-msearch-tvm-ticket'
                    }
                }
            };
        });

        it('should add tvm headers ', function() {
            msearch(core, '/test-method', {}, { testOption: true }, suggestDefaultOptionsGetter);

            expect(core.got).toHaveBeenCalledWith(
                expect.any(String),
                {
                    testOption: true,
                    json: true,
                    query: {
                        mdb: 'test-mdb',
                        remote_ip: 'test_user_ip',
                        side: 'web',
                        suid: 'test-suid',
                        uid: 'test-uid'
                    },
                    headers: {
                        'x-feature-params': emptyBase64String,
                        'x-ya-user-ticket': 'test-user-ticket',
                        'x-ya-service-ticket': 'test-msearch-tvm-ticket'
                    }
                }
            );
        });

        it('should append tvm headers', function() {
            const headers = {
                'x-test-header': 'test-header-value'
            };

            msearch(core, '/test-method', {}, { headers, testOption: true }, suggestDefaultOptionsGetter);

            expect(core.got.mock.calls[0][1].headers).toEqual({
                'x-test-header': 'test-header-value',
                'x-feature-params': emptyBase64String,
                'x-ya-user-ticket': 'test-user-ticket',
                'x-ya-service-ticket': 'test-msearch-tvm-ticket'
            });
        });
    });
});
