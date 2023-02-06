import debugFactory, { IDebugger } from 'debug';

import { NotFoundError } from '../../errors';
import LaunchApiService, {
    Dependencies as LaunchApiServiceDependencies,
    InvalidIdError,
} from '../../../lib/api/launch';
import BaseLaunchController, { LaunchControllerRequest } from './base-launch';

import { getMockedDBClass, createCollectionStub } from '../../../../test/helpers/mocked-db';

function createDbMock() {
    const collectionStub = createCollectionStub(jest.fn);
    const MockedDB = getMockedDBClass(collectionStub);

    return new MockedDB();
}

function createLaunchApiService(deps: Partial<LaunchApiServiceDependencies> = {}) {
    const debug = debugFactory('test');

    return new LaunchApiService({ debug, db: createDbMock(), ...deps });
}

class TestBaseLaunchController extends BaseLaunchController {
    extractIdFromRequest(req: LaunchControllerRequest) {
        return this._extractIdFromRequest(req);
    }
}

describe('BaseLaunchController', () => {
    let debug: IDebugger;
    let launchApi: LaunchApiService;

    beforeEach(() => {
        debug = debugFactory('test');
        launchApi = createLaunchApiService();
    });

    describe('._extractIdFromRequest', () => {
        it('should return id created by launch service', () => {
            const controller = new TestBaseLaunchController({ debug, launchApi });

            const fakeRequest = { params: { id: '746573747465737474657374' } } as LaunchControllerRequest;

            const expected = 'fake-id';

            const actual = controller.extractIdFromRequest(fakeRequest);

            expect(actual).toStrictEqual(expected);
        });

        it('should throw a NotFound error when passed invalid id', () => {
            const db = createDbMock();
            db.createId = jest.fn().mockImplementation(() => {
                throw new InvalidIdError('1111');
            });
            launchApi = createLaunchApiService({ db });

            const controller = new TestBaseLaunchController({ debug, launchApi });

            const fakeRequest = { params: { id: '7465737474657374746' } } as LaunchControllerRequest;

            const actual = () => controller.extractIdFromRequest(fakeRequest);

            expect(actual).toThrowError(NotFoundError);
        });
    });
});
