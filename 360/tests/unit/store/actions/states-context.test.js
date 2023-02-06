import {
    updateSort,
    setViewStatesContext
} from '../../../../components/redux/store/actions/states-context';
import {
    SET_VIEW,
    UPDATE_SORT
} from '../../../../components/redux/store/actions/types';

jest.mock('../../../../components/redux/store/actions/settings', () => ({
    saveSettings: jest.fn()
}));
import { saveSettings } from '../../../../components/redux/store/actions/settings';

describe('states-context actions', () => {
    let dispatch;

    beforeEach(() => {
        dispatch = jest.fn();
    });

    describe('updateSort', () => {
        it('should dispatch `UPDATE_SORT` action with current `idContext`', () => {
            updateSort('name', '0')(dispatch, () => ({
                page: {
                    idContext: '/disk'
                }
            }));
            expect(dispatch).toBeCalledWith({
                type: UPDATE_SORT,
                payload: {
                    idContext: '/disk',
                    sort: 'name',
                    order: '0'
                }
            });
        });
    });

    describe('setViewStatesContext', () => {
        it('should dispatch `SET_VIEW` and saveSettings', () => {
            const saveSettingsActionMock = {
                type: 'SAVE_SETTINGS'
            };
            saveSettings.mockReturnValueOnce(saveSettingsActionMock);
            setViewStatesContext('tile')(dispatch);

            expect(dispatch).toBeCalledTimes(2);
            expect(dispatch).toBeCalledWith({
                type: SET_VIEW,
                payload: {
                    view: 'tile'
                }
            });
            expect(dispatch).toBeCalledWith(saveSettingsActionMock);
            expect(saveSettings).toBeCalledWith({ key: 'view', value: 'tile' });
        });
    });
});
