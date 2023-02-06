import { fetchUserInfo } from '../../../../components/redux/store/actions/user';

jest.mock('../../../../components/redux/store/actions', () => ({
    updateUser: jest.fn()
}));
jest.mock('../../../../components/redux/store/lib/raw-fetch-model', () => jest.fn());
jest.mock('../../../../components/redux/store/lib/api', () => jest.fn());
jest.mock('../../../../components/extract-preloaded-data');
jest.mock('config', () => ({}));

import { updateUser } from '../../../../components/redux/store/actions';
import rawFetchModel from '../../../../components/redux/store/lib/raw-fetch-model';

const mockedDispatch = (arg) => typeof arg === 'function' ? arg(mockedDispatch) : arg;

describe('user actions', () => {
    describe('fetchUserInfo', () => {
        it('should fetch user info and update store', (done) => {
            rawFetchModel.mockImplementation(() => Promise.resolve());

            fetchUserInfo()(mockedDispatch).then(() => {
                expect(rawFetchModel.mock.calls[0]).toEqual(['do-get-user-info']);
                expect(updateUser).toBeCalled();
                done();
            });
        });
    });
});
