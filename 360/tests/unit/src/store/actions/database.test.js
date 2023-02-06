import { initNotesDatabase } from '../../../../../src/store/actions/database';
import { UPDATE_NOTES } from '../../../../../src/store/types';
import { ERRORS, STATES } from '../../../../../src/consts';

jest.mock('../../../../../src/store/actions/common', () => ({
    getCloudApiBaseUrl: jest.fn(),
    handleError: jest.fn()
}));
jest.mock('@ps-int/ufo-helpers/lib/datasync', () => ({
    openDatabase: jest.fn()
}));
jest.mock('../../../../../src/store/actions/notes-db-updated', () => jest.fn());

import { openDatabase } from '@ps-int/ufo-helpers/lib/datasync';
import { getCloudApiBaseUrl, handleError } from '../../../../../src/store/actions/common';
import notesDBUpdated from '../../../../../src/store/actions/notes-db-updated';

describe('src/store/actions/database', () => {
    const mockedGetState = () => ({
        user: { id: 'userId' },
        services: { yastatic: 'yastatic.net' },
        environment: {
            scripts: { datasync: 'datasync.js' }
        }
    });
    const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch, mockedGetState) : arg);

    describe('initNotesDatabase', () => {
        beforeEach(() => {
            jest.clearAllMocks();
        });

        it('should try to connect to database and update notes in store on successful connection', (done) => {
            openDatabase.mockImplementation(() => Promise.resolve());
            initNotesDatabase()(dispatch, mockedGetState)
                .then(() => {
                    expect(dispatch.mock.calls).toContainEqual([{
                        type: UPDATE_NOTES,
                        payload: { state: STATES.LOADING }
                    }]);

                    expect(getCloudApiBaseUrl).toBeCalled();
                    expect(openDatabase).toBeCalled();
                    expect(notesDBUpdated).toBeCalledTimes(1);
                    done();
                });
        });

        it('should try to connect to database and handle errors on error connection', (done) => {
            openDatabase.mockImplementation(() => Promise.reject());
            initNotesDatabase()(dispatch, mockedGetState)
                .then(() => {
                    expect(dispatch.mock.calls).toContainEqual([{
                        type: UPDATE_NOTES,
                        payload: { state: STATES.ERROR }
                    }]);

                    expect(popFnCalls(handleError)[0]).toEqual([ERRORS.BAD_READ_RESPONSE]);
                    done();
                });
        });
    });
});
