import debugFactory, { IDebugger } from 'debug';
import request from 'supertest';

import createApp from '../src/app';
import LaunchApiService from '../src/lib/api/launch/launch';
import { LaunchTypes } from '../src/models/Launch';
import TestpalmApiService from '../src/lib/api/testpalm';
import BookingApiService from '../src/lib/api/booking';

import createDBMock, { TestDB } from './helpers/test-db';
import createTestpalmApiMock from './helpers/testpalm-api';
import createBookingApiServiceMock from './helpers/booking-api';

describe('GET /api/v1/launch/:id', () => {
    let debug: IDebugger;
    let db: TestDB;
    let launchApi: LaunchApiService;
    let testpalmApi: TestpalmApiService;
    let bookingApi: BookingApiService;

    const ID = '746573747465737474657374';

    beforeEach(async() => {
        debug = debugFactory('test');
        db = createDBMock();

        await db.initialize();

        launchApi = new LaunchApiService({ debug, db });

        testpalmApi = new TestpalmApiService({
            debug,
            testpalmApi: createTestpalmApiMock(jest.fn),
        });

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

    it('should return 200 with existed launch', async() => {
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        const response = await request(app)
            .get(`/api/v1/launch/${ID}`)
            .send()
            .expect('Content-Type', /json/)
            .expect(200);

        const actual = response.body;

        expect(typeof actual).not.toBeNull();
        expect(typeof actual).toBe('object');
        expect(actual.id).toBe('746573747465737474657374');

        return response;
    });

    it('should return 404 when launch does not exists', () => {
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        return request(app)
            .get('/api/v1/launch/testtest1234')
            .send()
            .expect(404);
    });

    it('should return 404 when passed invalid id', () => {
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        return request(app)
            .get('/api/v1/launch/1234')
            .send()
            .expect(404);
    });

    it('should return 500 when unknown error ocurred', async() => {
        launchApi.getLaunchById = () => Promise.reject('unknown');
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        return request(app)
            .get('/api/v1/launch/testtest1234')
            .send()
            .expect(500);
    });
});
