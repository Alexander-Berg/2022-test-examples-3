import debugFactory, { IDebugger } from 'debug';
import request from 'supertest';

import createApp from '../src/app';
import LaunchApiService from '../src/lib/api/launch/launch';
import { LaunchTypes } from '../src/models/Launch';
import TestpalmApiService from '../src/lib/api/testpalm/testpalm';
import BookingApiService from '../src/lib/api/booking';

import createDBMock, { TestDB } from './helpers/test-db';
import createTestpalmApiMock from './helpers/testpalm-api';
import createBookingApiServiceMock from './helpers/booking-api';

describe('PATCH /api/v1/launch/:id', () => {
    let debug: IDebugger;
    let db: TestDB;
    let launchApi: LaunchApiService;
    let testpalmApi: TestpalmApiService;
    let bookingApi: BookingApiService;

    const ID = '746573747465737474657374';

    beforeEach(async() => {
        debug = debugFactory('test-update');
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

    it('should return 202 when launch successfully updated', () => {
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        const payload = {
            tags: ['123'],
            environments: ['test env'],
            properties: [{
                key: 'test',
                value: 'test',
            }],
        };

        return request(app)
            .patch(`/api/v1/launch/${ID}`)
            .send(payload)
            .expect(202);
    });

    it('should return 404 when launch does not exists', () => {
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        return request(app)
            .patch('/api/v1/launch/testtest1234')
            .send({})
            .expect(404);
    });

    it('should return 406 when received invalid payload', async() => {
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        const payload = {
            tags: [123],
        };

        const expected = {
            comment: 'Invalid Payload',
            message: 'failed to validate schema: data.tags[0] should be string',
        };

        const result = await request(app)
            .patch(`/api/v1/launch/${ID}`)
            .send(payload)
            .expect(406);

        const actual = result.body;

        expect(typeof actual).not.toBeNull();
        expect(typeof actual).toBe('object');
        expect(actual).toEqual(expected);
    });

    it('should return 500 when unknown error ocurred', async() => {
        launchApi.updateLaunch = () => Promise.reject('unknown');
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        return request(app)
            .patch('/api/v1/launch/testtest1234')
            .send()
            .expect(500);
    });
});
