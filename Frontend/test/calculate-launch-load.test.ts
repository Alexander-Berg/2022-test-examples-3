import debugFactory, { IDebugger } from 'debug';
import request from 'supertest';
import TestpalmApi from '@yandex-int/testpalm-api';
import { GotInstance, GotJSONFn } from '@yandex-int/si.ci.requests';

import createApp from '../src/app';
import { LaunchTypes } from '../src/models/Launch';

import LaunchApiService from '../src/lib/api/launch';
import TestpalmApiService from '../src/lib/api/testpalm';
import BookingApiService, { DevicesDownloader } from '../src/lib/api/booking';

import createDBMock, { TestDB } from './helpers/test-db';
import SandboxerMock from './helpers/sandboxer';

function createTestpalmApiMock() {
    return {
        getTestSuite: jest.fn().mockImplementation((_, id: string) => Promise.resolve({
            id,
            filter: { expression: {} },
        })),
        getTestCasesWithPost: jest.fn().mockResolvedValue(Promise.resolve(Array(5).fill({ estimatedTime: 6596000 }))),
    } as unknown as TestpalmApi;
}

function createBookingApiServiceMock() {
    const debug = debugFactory('test');

    const requests = jest.fn().mockResolvedValue({ body: [] }) as unknown as GotInstance<GotJSONFn>;

    const sandboxMock = jest.fn().mockResolvedValue({ items: [{ id: 1, http: { proxy: 'test-url' } }] });
    const sandbox = new SandboxerMock({ token: 'fake-token' }, sandboxMock);
    const devicesDownloader = new DevicesDownloader({ debug, sandbox, requests });

    return new BookingApiService({ debug, devicesDownloader, requests });
}

describe('POST /api/v1/launch/:id/calculate-launch', () => {
    let debug: IDebugger;
    let db: TestDB;
    let launchApi: LaunchApiService;
    let testpalmApi: TestpalmApi;
    let testpalmApiService: TestpalmApiService;
    let bookingApi: BookingApiService;

    const ID = '746573747465737474657374';

    beforeEach(async() => {
        debug = debugFactory('test');
        db = createDBMock();

        await db.initialize();

        launchApi = new LaunchApiService({ debug, db });

        testpalmApi = createTestpalmApiMock();
        testpalmApiService = new TestpalmApiService({ debug, testpalmApi });

        bookingApi = createBookingApiServiceMock();

        const originalCreateId = db.createId;
        db.createId = () => originalCreateId(ID);

        const launch = await launchApi.createLaunch({
            project: 'web4',
            title: 'test',
            author: 'robot',
            type: LaunchTypes.testsuite,
            content: {
                testsuiteId: 'fake-testsuite-id',
            },
        });

        await launchApi.updateLaunch(launch._id, { environments: ['Chrome'] });

        db.createId = originalCreateId;
    });

    afterEach(async() => {
        await db.flushDB();
        await db.close();
    });

    it('should return 200 with calculated load', async() => {
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });
        const expected = {
            load: 14,
            environments: {
                Chrome: {
                    ratio: 1,
                    launchEnvironment: 'Chrome',
                    bookingEnvironmentName: null,
                    bookingEnvironmentCode: null,
                },
            },
        };

        const response = await request(app)
            .post(`/api/v1/launch/${ID}/calculate-load`)
            .send()
            .expect('Content-Type', /json/)
            .expect(200);

        const actual = response.body;

        expect(typeof actual).not.toBeNull();
        expect(typeof actual).toBe('object');
        expect(actual).toEqual(expected);

        return response;
    });

    it('should return 404 when launch does not exists', () => {
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        return request(app)
            .post('/api/v1/launch/test12341234/calculate-load')
            .send()
            .expect(404);
    });

    it('should return 500 when failed to fetch launch', async() => {
        launchApi.getLaunchById = () => Promise.reject('unknown');
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        return request(app)
            .post(`/api/v1/launch/${ID}/calculate-load`)
            .send()
            .expect(500);
    });

    it('should return 500 when failed to fetch test-suite', async() => {
        testpalmApi.getTestCasesWithPost = () => Promise.reject('unknown');
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        const expected = {
            comment: 'Internal Server Error',
            message: 'cannot fetch test-cases: unknown',
        };

        const response = await request(app)
            .post(`/api/v1/launch/${ID}/calculate-load`)
            .send()
            .expect(500);

        const actual = response.body;

        expect(actual).toEqual(expected);
    });
});
