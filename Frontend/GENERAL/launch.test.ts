import debugFactory, { IDebugger } from 'debug';
import { Collection } from 'mongodb';

import { LaunchPayload, LaunchTypes, LaunchStatuses, Launch, LaunchId } from '../../../models/Launch';
import LaunchApiService, {
    LaunchCreationError,
    LaunchFindingError,
    LaunchExistingError,
    LaunchUpdateError,
    InvalidIdError,
} from './launch';

import { getMockedDBClass, createCollectionStub } from '../../../../test/helpers/mocked-db';

function createDBMock(collection: Partial<Collection> = {}) {
    const collectionStub = createCollectionStub(jest.fn);
    const MockedDB = getMockedDBClass({ ...collectionStub, ...collection });

    return new MockedDB();
}

function createLaunchPayload(): LaunchPayload {
    return {
        title: 'test-title',
        project: 'test-project',
        author: 'robot',
        type: LaunchTypes.testsuite,
        content: {
            testsuiteId: 'fake-test-suite',
        },
    };
}

function createLaunch(): Launch {
    return {
        ...createLaunchPayload(),

        _id: 'fake-id' as unknown as LaunchId,
        status: LaunchStatuses.draft,
        platform: null,
        tags: [],
        properties: [],
        environments: [],
        bookingId: null,
        testRunIds: [],
        createdAt: 1,
        updatedAt: 1,
        quota: null,
    };
}

describe('LaunchApiService', () => {
    const now = Date.now;

    let debug: IDebugger;
    let fakeTime: number;

    beforeEach(() => {
        fakeTime = 1;
        Date.now = () => fakeTime;
        debug = debugFactory('test');
    });

    afterEach(() => {
        Date.now = now;
    });

    describe('.createLaunch', () => {
        let insertOneMock: jest.Mock;

        beforeEach(() => {
            insertOneMock = jest.fn().mockImplementation(() => ({ insertedId: 'fake-id' }));
        });

        it('should use id returned from db', async() => {
            const db = createDBMock({ insertOne: insertOneMock });
            const api = new LaunchApiService({ debug, db });
            const payload = createLaunchPayload();

            const expected = 'fake-id';

            const actual = await api.createLaunch(payload);

            expect(actual._id).toEqual(expected);
        });

        it('should create in draft status', async() => {
            const db = createDBMock();
            const api = new LaunchApiService({ debug, db });
            const payload = createLaunchPayload();

            const expected = 'draft';

            const actual = await api.createLaunch(payload);

            expect(actual.status).toEqual(expected);
        });

        it('should return populated payload', async() => {
            const db = createDBMock({ insertOne: insertOneMock });
            const api = new LaunchApiService({ debug, db });
            const payload = createLaunchPayload();

            const expected = {
                _id: 'fake-id',
                title: 'test-title',
                project: 'test-project',
                author: 'robot',
                type: 'testsuite',
                content: {
                    testsuiteId: 'fake-test-suite',
                },
                platform: null,
                status: 'draft',
                tags: [],
                properties: [],
                environments: [],
                bookingId: null,
                testRunIds: [],
                createdAt: 1,
                updatedAt: 1,
                quota: null,
            };

            const actual = await api.createLaunch(payload);

            expect(actual).toEqual(expected);
        });

        it('should throw LaunchCreationError when failed to create run', async() => {
            const fakeError = new Error('fake error');
            insertOneMock = jest.fn().mockImplementation(() => Promise.reject(fakeError));

            const db = createDBMock({ insertOne: insertOneMock });
            const api = new LaunchApiService({ debug, db });
            const payload = createLaunchPayload();

            const expected = new LaunchCreationError(fakeError);

            const actual = api.createLaunch(payload);

            expect(actual).rejects.toThrowError(expected);
        });
    });

    describe('.getLaunchById', () => {
        let findOneMock: jest.Mock;

        beforeEach(() => {
            findOneMock = jest.fn();
        });

        it('should return launch', async() => {
            findOneMock.mockImplementation(({ _id }) => Promise.resolve({ ...createLaunch(), _id }));
            const db = createDBMock({ findOne: findOneMock });
            const api = new LaunchApiService({ debug, db });

            const expected = {
                ...createLaunch(),

                _id: 'test',
            };

            const actual = await api.getLaunchById('test' as unknown as LaunchId);

            expect(actual).toEqual(expected);
        });

        it('should return LaunchFindingError when failed to find launch', async() => {
            findOneMock.mockImplementation(() => Promise.reject(new Error('fake error')));
            const db = createDBMock({ findOne: findOneMock });
            const api = new LaunchApiService({ debug, db });

            const expected = new LaunchFindingError('test' as unknown as LaunchId);

            const actual = api.getLaunchById('test' as unknown as LaunchId);

            expect(actual).rejects.toThrowError(expected);
        });

        it('should return LaunchExistingError when launch does not exists', async() => {
            findOneMock.mockImplementation(() => Promise.resolve(null));
            const db = createDBMock({ findOne: findOneMock });
            const api = new LaunchApiService({ debug, db });

            const expected = new LaunchExistingError('test' as unknown as LaunchId);

            const actual = api.getLaunchById('test' as unknown as LaunchId);

            expect(actual).rejects.toThrowError(expected);
        });
    });

    describe('.updateLaunch', () => {
        let updateOneMock: jest.Mock;

        beforeEach(() => {
            updateOneMock = jest.fn();
        });

        it('should return nothing when launch was successfully updated', async() => {
            updateOneMock.mockImplementation(() => Promise.resolve({ result: { n: 1 } }));
            const db = createDBMock({ updateOne: updateOneMock });
            const api = new LaunchApiService({ debug, db });

            const expected = undefined;

            const actual = await api.updateLaunch('test' as unknown as LaunchId, {});

            expect(actual).toEqual(expected);
        });

        it('should set new updatedAt time', async() => {
            updateOneMock.mockImplementation(() => Promise.resolve({ result: { n: 1 } }));
            const db = createDBMock({ updateOne: updateOneMock });
            const api = new LaunchApiService({ debug, db });

            const payload = {
                title: 'test',
            };

            const expectedFilter = { _id: 'test' };
            const expectedUpdate = { $set: { title: 'test', updatedAt: 1 } };

            await api.updateLaunch('test' as unknown as LaunchId, payload);

            expect(updateOneMock).toHaveBeenCalledWith(expectedFilter, expectedUpdate);
        });

        it('should throw LaunchUpdateError when failed to update launch', async() => {
            updateOneMock.mockImplementation(() => Promise.reject(new Error('fake error')));
            const db = createDBMock({ updateOne: updateOneMock });
            const api = new LaunchApiService({ debug, db });

            const expected = new LaunchUpdateError('test' as unknown as LaunchId, new Error('fake error'));

            const actual = api.updateLaunch('test' as unknown as LaunchId, {});

            expect(actual).rejects.toThrowError(expected);
        });

        it('should return LaunchExistingError when launch does not exists', async() => {
            updateOneMock.mockImplementation(() => Promise.resolve({ result: { n: 0 } }));
            const db = createDBMock({ updateOne: updateOneMock });
            const api = new LaunchApiService({ debug, db });

            const expected = new LaunchExistingError('test' as unknown as LaunchId);

            const actual = api.updateLaunch('test' as unknown as LaunchId, {});

            expect(actual).rejects.toThrowError(expected);
        });
    });

    describe('.createId', () => {
        it('should call database dependency', () => {
            const db = createDBMock();
            const api = new LaunchApiService({ debug, db });

            const expected = 'fake-id';

            const actual = api.createId('whatever');

            expect(actual).toEqual(expected);
        });

        it('should throw an InvalidIdError when invalid id passed', () => {
            const db = createDBMock();
            db.createId = jest.fn().mockImplementation(() => {
                throw new Error();
            });

            const api = new LaunchApiService({ debug, db });

            const actual = () => api.createId('whatever');

            expect(actual).toThrowError(InvalidIdError);
        });
    });

    describe('.toPublic', () => {
        it('should correctly map launch to public', () => {
            const db = createDBMock();
            const api = new LaunchApiService({ debug, db });
            const launch = createLaunch();

            const expected = {
                id: 'fake-id',
                title: 'test-title',
                project: 'test-project',
                author: 'robot',
                type: 'testsuite',
                content: {
                    testsuiteId: 'fake-test-suite',
                },
                status: 'draft',
                tags: [],
                platform: null,
                properties: [],
                environments: [],
                bookingId: null,
                testRunIds: [],
                createdAt: 1,
                updatedAt: 1,
                quota: null,
            };

            const actual = api.toPublic(launch);

            expect(actual).toEqual(expected);
        });
    });
});
