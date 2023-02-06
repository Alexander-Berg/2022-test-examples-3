import debugFactory, { IDebugger } from 'debug';
import request from 'supertest';
import TestpalmApi from '@yandex-int/testpalm-api';

import createApp from '../src/app';
import { LaunchTypes } from '../src/models/Launch';

import LaunchApiService from '../src/lib/api/launch';
import TestpalmApiService from '../src/lib/api/testpalm';
import BookingApiService from '../src/lib/api/booking';

import createDBMock, { TestDB } from './helpers/test-db';
import createBookingApiServiceMock from './helpers/booking-api';

function createTestpalmApiMock() {
    return {
        getTestSuite: jest.fn().mockImplementation((_, id: string) => Promise.resolve({ id })),
        addTestRun: jest.fn().mockImplementation(() => Promise.resolve([{ id: '123' }, { id: '321' }])),
    } as unknown as TestpalmApi;
}

describe('POST /api/v1/launch/:id/start', () => {
    let debug: IDebugger;
    let db: TestDB;
    let launchApi: LaunchApiService;
    let testpalmApi: TestpalmApi;
    let testpalmApiService: TestpalmApiService;
    let bookingApi: BookingApiService;

    const ID = '746573747465737474657374';

    const payload = {
        runnerConfig: {
            title: 'United',
            runnerId: 'bulkcurl',
        },
    };

    beforeEach(async() => {
        debug = debugFactory('test');
        db = createDBMock();

        await db.initialize();

        launchApi = new LaunchApiService({ debug, db });

        testpalmApi = createTestpalmApiMock();
        testpalmApiService = new TestpalmApiService({ debug, testpalmApi });

        bookingApi = createBookingApiServiceMock(jest.fn);

        const originalCreateId = db.createId;
        db.createId = () => originalCreateId(ID);

        await launchApi.createLaunch({
            project: 'web4',
            title: 'test',
            author: 'robot',
            type: LaunchTypes.testsuite,
            content: {
                testsuiteId: 'fake-testsuite-id',
            },
        });

        db.createId = originalCreateId;
    });

    afterEach(async() => {
        await db.flushDB();
        await db.close();
    });

    it('should return 202 with successfully started runs', async() => {
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });
        const expected = {
            testRunIds: ['123', '321'],
        };

        const response = await request(app)
            .post(`/api/v1/launch/${ID}/start`)
            .send(payload)
            .expect('Content-Type', /json/)
            .expect(202);

        const actual = response.body;

        expect(typeof actual).not.toBeNull();
        expect(typeof actual).toBe('object');
        expect(actual).toEqual(expected);

        return response;
    });

    it('should return 404 when launch does not exists', () => {
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        return request(app)
            .post('/api/v1/launch/test12341234/start')
            .send(payload)
            .expect(404);
    });

    it('should return 406 when received invalid payload', () => {
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        return request(app)
            .post(`/api/v1/launch/${ID}/start`)
            .send({ runnerConfig: null })
            .expect(406);
    });

    it('should return 500 when failed to create launch', async() => {
        launchApi.getLaunchById = () => Promise.reject('unknown');
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        return request(app)
            .post(`/api/v1/launch/${ID}/start`)
            .send(payload)
            .expect(500);
    });

    it('should return 500 when failed to fetch test-suite', async() => {
        testpalmApi.getTestSuite = () => Promise.reject('unknown');
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        const expected = {
            comment: 'Internal Server Error',
            message: 'cannot fetch test-suite: unknown',
        };

        const response = await request(app)
            .post(`/api/v1/launch/${ID}/start`)
            .send(payload)
            .expect(500);

        const actual = response.body;

        expect(actual).toEqual(expected);
    });

    it('should return 500 when failed to create test-run', async() => {
        testpalmApi.addTestRun = () => Promise.reject('unknown');
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        const expected = {
            comment: 'Internal Server Error',
            message: 'cannot create run from launch: unknown',
        };

        const response = await request(app)
            .post(`/api/v1/launch/${ID}/start`)
            .send(payload)
            .expect(500);

        const actual = response.body;

        expect(actual).toEqual(expected);
    });

    it('should return 500 when failed to update launch', async() => {
        launchApi.updateLaunch = () => Promise.reject(new Error('unknown'));
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        const expected = {
            comment: 'Internal Server Error',
            message: 'unknown',
        };

        const response = await request(app)
            .post(`/api/v1/launch/${ID}/start`)
            .send(payload)
            .expect(500);

        const actual = response.body;

        expect(actual).toEqual(expected);
    });
});
