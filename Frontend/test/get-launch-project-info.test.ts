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
        getProject: jest.fn().mockResolvedValue({
            settings: {
                environments: [
                    { title: 'ios_safari_7' },
                    { title: 'ios_safari_10' },
                    { title: 'yabro' },
                ],
                runnerConfigItems: [
                    { runnerId: 'bulkcurl', title: 'United' },
                    { runnerId: 'serpFormRunner', title: 'CrowdTest Runner' },
                ],
            },
        }),
    } as unknown as TestpalmApi;
}

function createBookingApiServiceMock() {
    const debug = debugFactory('test1');

    const requests = jest.fn().mockResolvedValue({ body: [] }) as unknown as GotInstance<GotJSONFn>;

    const sandboxMock = jest.fn().mockResolvedValue({ items: [{ id: 1, http: { proxy: 'test-url' } }] });
    const sandbox = new SandboxerMock({ token: 'fake-token' }, sandboxMock);
    const devicesDownloader = new DevicesDownloader({ debug, sandbox, requests });

    return new BookingApiService({ debug, devicesDownloader, requests });
}

describe('GET /api/v1/launch/:id/project-info', () => {
    let debug: IDebugger;
    let db: TestDB;
    let launchApi: LaunchApiService;
    let testpalmApi: TestpalmApi;
    let testpalmApiService: TestpalmApiService;
    let bookingApi: BookingApiService;

    const ID = '746573747465737474657374';

    beforeEach(async() => {
        debug = debugFactory('test1');
        db = createDBMock();

        await db.initialize();

        launchApi = new LaunchApiService({ debug, db });

        testpalmApi = createTestpalmApiMock();
        testpalmApiService = new TestpalmApiService({ debug, testpalmApi });

        bookingApi = createBookingApiServiceMock();

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

    it('should return 200 with launch info', async() => {
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });
        const expected = {
            platforms: [],
            runners: [
                { runnerId: 'bulkcurl', title: 'United' },
            ],
        };

        const response = await request(app)
            .get(`/api/v1/launch/${ID}/project-info`)
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
            .get('/api/v1/launch/test12341234/project-info')
            .send()
            .expect(404);
    });

    it('should return 500 when failed to fetch launch', async() => {
        launchApi.getLaunchById = () => Promise.reject('unknown');
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        return request(app)
            .get(`/api/v1/launch/${ID}/project-info`)
            .send()
            .expect(500);
    });

    it('should return 500 when failed to fetch project', async() => {
        testpalmApi.getProject = () => Promise.reject('unknown');
        const app = createApp({ launchApi, testpalmApi: testpalmApiService, bookingApi });

        const expected = {
            comment: 'Internal Server Error',
            message: 'cannot fetch project "web4" from TestPalm: unknown',
        };

        const response = await request(app)
            .get(`/api/v1/launch/${ID}/project-info`)
            .send()
            .expect(500);

        const actual = response.body;

        expect(actual).toEqual(expected);
    });
});
