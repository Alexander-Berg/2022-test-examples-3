import {
    setPhotoSelection,
    deselectAllStatesContext,
    setSelection,
    setFilteredSelection,
    setHighlighted,
    deselectResources
} from '../../../../components/redux/store/actions/selection';
import {
    DESELECT_RESOURCES,
    DESELECT_ALL,
    SET_SELECTED,
    SET_HIGHLIGHTED
} from '../../../../components/redux/store/actions/types';
import { PHOTO_SELECTION } from '../../../../components/redux/store/actions/photo-types';

jest.mock('../../../../components/redux/store/actions/photo', () => ({
    fetchAndApplyDiff: jest.fn()
}));
import { fetchAndApplyDiff } from '../../../../components/redux/store/actions/photo';

jest.mock('../../../../components/helpers/metrika', () => ({
    count: jest.fn()
}));
import { count } from '../../../../components/helpers/metrika';

describe('selection actions', () => {
    const getState = jest.fn();
    const dispatch = jest.fn((action) => typeof action === 'function' ? action(dispatch, getState) : action);

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('setPhotoSelection', () => {
        it('should dispatch `PHOTO_SELECTION`', () => {
            const payload = { total: 5, missing: 1, unfilteredTotal: 3 };
            setPhotoSelection(payload)(dispatch, () => ({
                photoslice: {
                    unfilteredTotalSelected: payload.unfilteredTotal
                }
            }));
            expect(dispatch).toBeCalledTimes(1);
            expect(dispatch).toBeCalledWith({ type: PHOTO_SELECTION, payload });
        });

        it('should call `fetchAndApplyDiff` if hasDiff and selection was reset', () => {
            const payload = { total: 5, missing: 1, unfilteredTotal: 0 };
            getState.mockReturnValueOnce({
                photoslice: {
                    hasDiff: true,
                    unfilteredTotalSelected: payload.unfilteredTotal
                }
            });
            setPhotoSelection(payload)(dispatch, getState);
            expect(dispatch).toBeCalledTimes(2);
            expect(fetchAndApplyDiff).toBeCalled();
        });

        it('should call metrika if unfilteredTotal was changed', () => {
            const payload = { total: 5, missing: 1, unfilteredTotal: 3 };
            getState.mockReturnValueOnce({
                photoslice: {
                    unfilteredTotalSelected: 2
                },
                page: {}
            });
            setPhotoSelection(payload)(dispatch, getState);
            expect(count).toBeCalledWith(
                'interface elements',
                'photo',
                'select',
                'many'
            );
        });
    });

    describe('deselectAllStatesContext', () => {
        it('should dispatch `DESELECT_ALL`', () => {
            getState.mockReturnValueOnce({
                page: { idContext: '' },
                photoslice: {}
            });
            deselectAllStatesContext()(dispatch, getState);
            expect(dispatch).toBeCalledTimes(1);
            expect(dispatch).toBeCalledWith({ type: DESELECT_ALL });
        });
    });

    describe('setSelection', () => {
        it('should return `SET_SELECTED` action', () => {
            const selected = ['res-1', 'res-2'];
            expect(setSelection(selected)).toEqual({
                type: SET_SELECTED,
                payload: {
                    selected
                }
            });
        });

        it('should convert id to array & return `SET_SELECTED` action', () => {
            expect(setSelection('res-1')).toEqual({
                type: SET_SELECTED,
                payload: {
                    selected: ['res-1']
                }
            });
        });
    });

    describe('setFilteredSelection', () => {
        it('should dispatch `SET_SELECTED` & `PHOTO_SELECTION`', () => {
            getState.mockReturnValue({
                page: { idContext: '/photo' },
                photoslice: {}
            });
            const resources = ['res-1', 'res-2'];
            setFilteredSelection({
                resources,
                missing: 0,
                total: 2,
                unfilteredTotal: 2
            })(dispatch);
            expect(dispatch).toBeCalledWith({
                type: SET_SELECTED,
                payload: {
                    selected: resources
                }
            });
            expect(dispatch).toBeCalledWith({
                type: PHOTO_SELECTION,
                payload: {
                    missing: 0,
                    total: 2,
                    unfilteredTotal: 2
                }
            });
        });
    });

    describe('setHighlighted', () => {
        it('should return `SET_HIGHLIGHTED` action', () => {
            const highlighted = ['res-1', 'res-2'];
            expect(setHighlighted(highlighted)).toEqual({
                type: SET_HIGHLIGHTED,
                payload: {
                    highlighted
                }
            });
        });
    });

    describe('deselectResources', () => {
        it('should return `DESELECT_RESOURCES` action', () => {
            const resourcesIds = ['res-1', 'res-2'];
            expect(deselectResources(resourcesIds)).toEqual({
                type: DESELECT_RESOURCES,
                payload: {
                    resourcesIds
                }
            });
        });

        it('should convert id to array & return `DESELECT_RESOURCES` action', () => {
            expect(deselectResources('res-1')).toEqual({
                type: DESELECT_RESOURCES,
                payload: {
                    resourcesIds: ['res-1']
                }
            });
        });
    });
});
