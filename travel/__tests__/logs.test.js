jest.mock('fs');
// синхронный "резолв" чтобы не переписывать тесты на асинхронные
jest.mock('../../helpers/geobaseHelper', () => ({
    getGeoIdByRequest: () => ({then: cb => cb(213)}),
}));
jest.unmock('../logs');

jest.mock('../../../reexports', () => ({
    ...require.requireActual('../../../reexports'),
    momentTimezone: {
        ...require.requireActual('../../../reexports').momentTimezone,
        utc: jest.fn(),
    },
}));
import fs from 'fs';
import {momentTimezone as moment} from '../../../reexports';
import {logSearchResult, logSearchTransfersResult} from '../logs';

const logPathsConfig = {
    search: 'search.log',
};

moment.utc.mockImplementation(() => ({
    unix: () => 123456789,
    format: () => '20000101000000',
}));

function makeDummyRequest(extra) {
    return {cookies: {}, blackbox: {}, get: () => null, ...extra};
}

describe('logSearchResult', () => {
    it('creates directory when it not exists', () => {
        fs.statSync.mockImplementation(() => {
            throw new Error();
        });

        try {
            logSearchResult({
                req: makeDummyRequest(),
                logPathsConfig,
                query: {},
                result: {segments: []},
            });

            expect(fs.mkdirSync).toBeCalled();
        } finally {
            fs.statSync.mockImplementation();
        }
    });

    it("doesn't create directory when it already exists", () => {
        logSearchResult({
            req: makeDummyRequest(),
            logPathsConfig,
            query: {},
            result: {segments: []},
        });

        expect(fs.mkdirSync).not.toBeCalled();
    });

    it('logs into configured file', () => {
        logSearchResult({
            req: makeDummyRequest(),
            logPathsConfig,
            query: {},
            result: {segments: []},
        });

        expect(fs.appendFileSync.mock.calls[0][0]).toBe('search.log');
    });

    it('should log request fields', () => {
        const req = makeDummyRequest({
            cookies: {
                yandexuid: 'yandexuid',
            },
            blackbox: {
                uid: 'passportuid',
            },
            ip: '12.34.56.78',
            requestId: 'request_id',
            get: () => 'referer',
        });

        logSearchResult({
            req,
            logPathsConfig,
            query: {},
            result: {segments: []},
        });
        const entry = fs.appendFileSync.mock.calls[0][1];

        expect(entry).toBe(
            'tskv\ttskv_format=rasp-users-search-log\t' +
                'adults=null\tchildren=null\teventtime=20000101000000\t' +
                'from_id=undefined\tgeoid=213\tinfants=null\tklass=null\t' +
                'national_version=undefined\tpassportuid=passportuid\treferer=referer\t' +
                'request_id=request_id\treturn_date=null\tservice=rasp\t' +
                't_type_counts={}\tto_id=undefined\ttransport_type=all\t' +
                'unixtime=123456789\tuserip=12.34.56.78\t' +
                'when=undefined\tyandexuid=yandexuid\n',
        );
    });

    it('should log query fields', () => {
        logSearchResult({
            req: makeDummyRequest(),
            logPathsConfig,
            query: {
                nationalVersion: 'us',
                pointFrom: 'c213',
                pointTo: 'c54',
                transportType: 'hyperloop',
                when: '2000-01-01',
            },
            result: {segments: []},
        });
        const entry = fs.appendFileSync.mock.calls[0][1];

        expect(entry).toBe(
            'tskv\ttskv_format=rasp-users-search-log\tadults=null\t' +
                'children=null\teventtime=20000101000000\tfrom_id=c213\t' +
                'geoid=213\tinfants=null\tklass=null\tnational_version=us\t' +
                'passportuid=undefined\treferer=null\trequest_id=undefined\t' +
                'return_date=null\tservice=rasp\tt_type_counts={}\tto_id=c54\t' +
                'transport_type=hyperloop\tunixtime=123456789\tuserip=undefined\t' +
                'when=2000-01-01\tyandexuid=undefined\n',
        );
    });

    it('should log segments transport codes', () => {
        logSearchResult({
            req: makeDummyRequest(),
            logPathsConfig,
            query: {},
            result: {
                segments: [
                    {transport: {code: 'bycicle'}},
                    {transport: {code: 'hyperloop'}},
                    {transport: {code: 'bycicle'}},
                    {transport: {code: 'bycicle'}},
                ],
            },
        });
        const entry = fs.appendFileSync.mock.calls[0][1];

        expect(entry).toBe(
            'tskv\ttskv_format=rasp-users-search-log\tadults=null\tchildren=null\t' +
                'eventtime=20000101000000\tfrom_id=undefined\tgeoid=213\t' +
                'infants=null\tklass=null\tnational_version=undefined\t' +
                'passportuid=undefined\treferer=null\trequest_id=undefined\t' +
                'return_date=null\tservice=rasp\tt_type_counts={"bycicle":3,"hyperloop":1}\t' +
                'to_id=undefined\ttransport_type=all\tunixtime=123456789\t' +
                'userip=undefined\twhen=undefined\tyandexuid=undefined\n',
        );
    });
});

describe('logSearchTransfersResult', () => {
    it('creates directory when it not exists', () => {
        fs.statSync.mockImplementation(() => {
            throw new Error();
        });

        try {
            logSearchTransfersResult({
                req: makeDummyRequest(),
                logPathsConfig,
                query: {},
                result: [],
            });

            expect(fs.mkdirSync).toBeCalled();
        } finally {
            fs.statSync.mockImplementation();
        }
    });

    it("doesn't create directory when it already exists", () => {
        logSearchTransfersResult({
            req: makeDummyRequest(),
            logPathsConfig,
            query: {},
            result: [],
        });

        expect(fs.mkdirSync).not.toBeCalled();
    });

    it('logs into configured file', () => {
        logSearchTransfersResult({
            req: makeDummyRequest(),
            logPathsConfig,
            query: {},
            result: [],
        });

        expect(fs.appendFileSync.mock.calls[0][0]).toBe('search.log');
    });

    it('should log request fields', () => {
        const req = makeDummyRequest({
            cookies: {
                yandexuid: 'yandexuid',
            },
            blackbox: {
                uid: 'passportuid',
            },
            ip: '12.34.56.78',
            requestId: 'request_id',
            get: () => 'referer',
        });

        logSearchTransfersResult({req, logPathsConfig, query: {}, result: []});
        const entry = fs.appendFileSync.mock.calls[0][1];

        expect(entry).toBe(
            'tskv\ttskv_format=rasp-users-search-log\tadults=null\tchildren=null\t' +
                'eventtime=20000101000000\tfrom_id=undefined\tgeoid=213\t' +
                'infants=null\tklass=null\tnational_version=undefined\t' +
                'passportuid=passportuid\treferer=referer\trequest_id=request_id\t' +
                'return_date=null\tservice=rasp\tto_id=undefined\t' +
                'transfer_count=[]\ttransport_type=all\tunixtime=123456789\t' +
                'userip=12.34.56.78\twhen=undefined\tyandexuid=yandexuid\n',
        );
    });

    it('should log query fields', () => {
        logSearchTransfersResult({
            req: makeDummyRequest(),
            logPathsConfig,
            query: {
                pointFrom: 'c213',
                pointTo: 'c54',
                transportType: 'hyperloop',
                when: '2000-01-01',
            },
            result: [],
        });
        const entry = fs.appendFileSync.mock.calls[0][1];

        expect(entry).toBe(
            'tskv\ttskv_format=rasp-users-search-log\tadults=null\t' +
                'children=null\teventtime=20000101000000\tfrom_id=c213\t' +
                'geoid=213\tinfants=null\tklass=null\tnational_version=undefined\t' +
                'passportuid=undefined\treferer=null\trequest_id=undefined\t' +
                'return_date=null\tservice=rasp\tto_id=c54\ttransfer_count=[]\t' +
                'transport_type=hyperloop\tunixtime=123456789\tuserip=undefined\t' +
                'when=2000-01-01\tyandexuid=undefined\n',
        );
    });

    it('should log transfers segments transport codes', () => {
        logSearchTransfersResult({
            req: makeDummyRequest(),
            query: {},
            logPathsConfig,
            result: [
                {
                    segments: [
                        {transport: {code: 'bycicle'}},
                        {transport: {code: 'hyperloop'}},
                        {transport: {code: 'bycicle'}},
                        {transport: {code: 'bycicle'}},
                    ],
                },
                {
                    segments: [
                        {transport: {code: 'bycicle'}},
                        {transport: {code: 'bycicle'}},
                        {transport: {code: 'train'}},
                    ],
                },
                {
                    segments: [
                        {transport: {code: 'bycicle'}},
                        {transport: {code: 'hyperloop'}},
                        {transport: {code: 'bycicle'}},
                        {transport: {code: 'train'}},
                    ],
                },
            ],
        });
        const entry = fs.appendFileSync.mock.calls[0][1];

        expect(entry).toBe(
            'tskv\ttskv_format=rasp-users-search-log\tadults=null\tchildren=null\t' +
                'eventtime=20000101000000\tfrom_id=undefined\tgeoid=213\tinfants=null\t' +
                'klass=null\tnational_version=undefined\tpassportuid=undefined\t' +
                'referer=null\trequest_id=undefined\treturn_date=null\tservice=rasp\t' +
                'to_id=undefined\ttransfer_count=[{"bycicle":3,"hyperloop":1},{"bycicle":2,"train":1},{"bycicle":2,"hyperloop":1,"train":1}]\t' +
                'transport_type=all\tunixtime=123456789\tuserip=undefined\t' +
                'when=undefined\tyandexuid=undefined\n',
        );
    });
});
