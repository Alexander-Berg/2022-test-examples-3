import debugFactory, { IDebugger } from 'debug';
import request from 'supertest';

import createApp from '../src/app';
import LaunchApiService from '../src/lib/api/launch';
import TestpalmApiService from '../src/lib/api/testpalm';
import BookingApiService from '../src/lib/api/booking';

import createTestDb, { TestDB } from './helpers/test-db';
import createTestpalmApiMock from './helpers/testpalm-api';
import createBookingApiServiceMock from './helpers/booking-api';

describe('GET /', () => {
    let debug: IDebugger;
    let db: TestDB;
    let launchApi: LaunchApiService;
    let testpalmApi: TestpalmApiService;
    let bookingApi: BookingApiService;

    beforeEach(async() => {
        debug = debugFactory('test');
        db = createTestDb();
        launchApi = new LaunchApiService({ debug, db });

        testpalmApi = new TestpalmApiService({
            debug,
            testpalmApi: createTestpalmApiMock(jest.fn),
        });

        bookingApi = createBookingApiServiceMock(jest.fn);
    });

    it('should return 404', async() => {
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        return request(app)
            .get('/')
            .send()
            .expect(404);
    });

    it('should return X-Request-ID header', async() => {
        const app = createApp({ launchApi, testpalmApi, bookingApi });

        return request(app)
            .get('/')
            .send()
            .expect('X-Request-ID', /^[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    });
});
