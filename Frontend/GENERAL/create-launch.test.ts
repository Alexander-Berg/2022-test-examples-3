import debugFactory, { IDebugger } from 'debug';
import { Request, Response } from 'express';

import LaunchApiService, { Dependencies as LaunchApiServiceDependencies } from '../../../lib/api/launch';
import TestpalmApiService from '../../../lib/api/testpalm';
import CreateLaunchController from './create-launch';
import * as errors from '../../errors';

import { getMockedDBClass, createCollectionStub } from '../../../../test/helpers/mocked-db';
import createTestpalmApiMock from '../../../../test/helpers/testpalm-api';

function createDbMock() {
    const collectionStub = createCollectionStub(jest.fn);
    const MockedDB = getMockedDBClass(collectionStub);

    return new MockedDB();
}

function createLaunchApiServiceMock(deps: Partial<LaunchApiServiceDependencies> = {}) {
    const debug = debugFactory('test');

    return new LaunchApiService({ debug, db: createDbMock(), ...deps });
}

function createTestpalmApiServiceMock() {
    const debug = debugFactory('test');
    const testpalmApi = createTestpalmApiMock(jest.fn);

    return new TestpalmApiService({ debug, testpalmApi });
}

function getPayload(type = 'testsuite') {
    const contentByType: Record<string, Record<string, string>> = {
        testsuite: {
            testSuiteId: 'fake-testsuite-id',
        },
        testplan: {
            testplanId: 'fake-testplan-id',
        },
        expression: {
            expression: 'fake expression',
        },
    };

    return {
        title: 'test-launch',
        project: 'test-project',
        author: 'robot',
        type,
        content: contentByType[type],
    };
}

function getMockedResponse() {
    return {
        status: jest.fn().mockReturnThis(),
        json: jest.fn().mockReturnThis(),
        end: jest.fn(),
    } as unknown as Response;
}

describe('CreateLaunchController', () => {
    let debug: IDebugger;

    beforeEach(() => {
        debug = debugFactory('test');
    });

    describe('.getValidateMiddleware', () => {
        it('should return function', () => {
            const launchApi = createLaunchApiServiceMock();
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });

            const expected = 'function';

            const actual = controller.getValidateMiddleware();

            expect(typeof actual).toBe(expected);
        });

        it('should call next function when payload are valid', () => {
            const launchApi = createLaunchApiServiceMock();
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });

            const body = getPayload();

            const next = jest.fn();

            const middleware = controller.getValidateMiddleware();

            middleware({ body } as Request, {} as Response, next);

            expect(next).toHaveBeenCalled();
            expect(next).toHaveBeenCalledWith();
        });

        it('should return InvalidPayloadError when payload are invalid', () => {
            const launchApi = createLaunchApiServiceMock();
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });

            const body = getPayload();
            delete body.project;
            const response = getMockedResponse();
            const next = jest.fn();

            const expected = new errors.InvalidPayloadError("failed to validate schema: data should have required property 'project'");

            const middleware = controller.getValidateMiddleware();

            middleware({ body } as Request, response, next);

            expect(next).toBeCalledWith(expected);
        });
    });

    describe('.getHandler', () => {
        it('should return function', () => {
            const launchApi = createLaunchApiServiceMock();
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });

            const expected = 'function';

            const actual = controller.getHandler();

            expect(typeof actual).toBe(expected);
        });

        it('should return 201 when launch successfully created with type === "testsuite"', async() => {
            const launchApi = createLaunchApiServiceMock();
            jest.spyOn(launchApi, 'createLaunch');
            const testpalmApi = createTestpalmApiServiceMock();
            testpalmApi.fetchTestSuiteFromLaunch = jest.fn().mockResolvedValue({ id: 1, properties: [] });
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });
            const handler = controller.getHandler();

            const request = { body: getPayload('testsuite') } as Request;
            const response = getMockedResponse();

            await handler(request, response);

            expect(launchApi.createLaunch).toHaveBeenCalledTimes(1);
            expect(response.status).toHaveBeenCalledWith(201);
        });

        it('should return 201 when launch successfully created with type === "testplan"', async() => {
            const launchApi = createLaunchApiServiceMock();
            jest.spyOn(launchApi, 'createLaunch');
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });
            const handler = controller.getHandler();

            const payload = { ...getPayload('testplan') };
            const request = { body: payload } as Request;
            const response = getMockedResponse();

            await handler(request, response);

            expect(launchApi.createLaunch).toHaveBeenCalledTimes(1);
            expect(response.status).toHaveBeenCalledWith(201);
        });

        it('should return 201 when launch successfully created with type === "expression"', async() => {
            const launchApi = createLaunchApiServiceMock();
            jest.spyOn(launchApi, 'createLaunch');
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });
            const handler = controller.getHandler();

            const payload = { ...getPayload('expression') };
            const request = { body: payload } as Request;
            const response = getMockedResponse();

            await handler(request, response);

            expect(launchApi.createLaunch).toHaveBeenCalledTimes(1);
            expect(response.status).toHaveBeenCalledWith(201);
        });

        it('should throw InvalidPayloadError when unknown type presented', () => {
            const launchApi = createLaunchApiServiceMock();
            jest.spyOn(launchApi, 'createLaunch');
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });
            const handler = controller.getHandler();

            const payload = { ...getPayload('unknown') };
            const request = { body: payload } as Request;
            const response = getMockedResponse();
            const expected = new errors.InvalidPayloadError('unknown type: unknown');

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });

        it('should call api with extended payload', async() => {
            const launchApi = createLaunchApiServiceMock();
            jest.spyOn(launchApi, 'createLaunch');
            const testpalmApi = createTestpalmApiServiceMock();
            testpalmApi.fetchTestSuiteFromLaunch = jest.fn().mockResolvedValue({
                id: 1,
                properties: [{ key: 'platform', value: 'desktop' }, { key: '2' }],
            });
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });
            const handler = controller.getHandler();

            const payload = getPayload();
            const request = { body: { ...payload, test: 1, additional: true } } as Request;
            const response = getMockedResponse();

            const expected = {
                ...getPayload(),
                properties: [{ key: 'platform', value: 'desktop' }, { key: '2', value: '' }],
                platform: 'desktop',
            };

            expected.content = {
                testsuiteId: expected.content.testSuiteId,
            };

            await handler(request, response);

            expect(launchApi.createLaunch).toHaveBeenCalledTimes(1);
            expect(launchApi.createLaunch).toHaveBeenCalledWith(expected);
            expect(response.status).toHaveBeenCalledWith(201);
        });

        it('should throw InternalServerError when failed to create launch', () => {
            const launchApi = createLaunchApiServiceMock();
            launchApi.createLaunch = jest.fn().mockImplementation(() => Promise.reject('fake error'));
            const testpalmApi = createTestpalmApiServiceMock();
            testpalmApi.fetchTestSuiteFromLaunch = jest.fn().mockResolvedValue({ id: 1, properties: [] });
            const controller = new CreateLaunchController({ debug, launchApi, testpalmApi });
            const handler = controller.getHandler();

            const request = { body: getPayload() } as Request;
            const response = getMockedResponse();
            const expected = new errors.InternalServerError();

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });
    });
});
