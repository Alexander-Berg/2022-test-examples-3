import '../../../noscript';
import createStore from '../../../../../components/redux/store/create-store';
import * as rawFetchModel from '../../../../../components/redux/store/lib/raw-fetch-model';

import {
    TRASH_ID,
    submitCleanTrash,
    onSuccessCleanTrash
} from '../../../../../components/redux/store/actions/operations/clean-trash';

import { notify } from '../../../../../components/redux/store/actions/notifications';
jest.mock('../../../../../components/redux/store/actions/notifications', () => ({
    notify: jest.fn(() => jest.fn())
}));

import { fetchSpace, updateUserInfoForOverdraft } from '../../../../../components/redux/store/actions/space';
jest.mock('../../../../../components/redux/store/actions/space', () => ({
    fetchSpace: jest.fn(() => jest.fn()),
    updateUserInfoForOverdraft: jest.fn(() => jest.fn())
}));

describe('operation clean trash actions', () => {
    const trashFileId = TRASH_ID + '/file';

    const defaultState = {
        environment: {
            session: {
                experiment: {}
            }
        },
        resources: {
            [TRASH_ID]: { id: TRASH_ID, state: {}, children: { append_time_0: { ids: [trashFileId] } } },
            [trashFileId]: { id: trashFileId }
        }
    };

    /**
     * @param {Object} responses - моки ответов на запросы
     */
    const mockRawFetchModel = (responses) => {
        const responseIndex = {};
        rawFetchModel.default = jest.fn()
            .mockImplementation((modelName, params) => {
                responseIndex[modelName] = responseIndex[modelName] || 0;
                const response = responses[modelName][responseIndex[modelName]];

                if (response && (!response.condition || response.condition(params))) {
                    responseIndex[modelName]++;
                    return Promise[response.promise](response.data);
                }

                return Promise.reject({ id: 'HTTP_404' });
            });
    };

    const originalRawFetchModel = rawFetchModel.default;
    afterEach(() => {
        rawFetchModel.default = originalRawFetchModel;
    });

    describe('doCleanTrash', () => {
        it('успешное очищение корзины', (done) => {
            mockRawFetchModel({
                'do-clean-trash': [{
                    promise: 'resolve',
                    data: {
                        oid: 'o1'
                    }
                }],
                'do-status-operation': [{
                    promise: 'resolve',
                    condition: (params) => params.oid === 'o1',
                    data: {
                        state: 'COMPLETED',
                    }
                }]
            });

            const store = createStore(defaultState);
            store.dispatch(submitCleanTrash()).then(() => {
                expect(updateUserInfoForOverdraft).toHaveBeenCalled();
                expect(fetchSpace).toHaveBeenCalled();
                expect(notify).toHaveBeenCalledWith({
                    operation: 'cleanTrash',
                    status: 'done'
                });

                const { resources } = store.getState();
                expect(resources[TRASH_ID].children.append_time_0.ids).toEqual([]);
                expect(resources[trashFileId]).toBeUndefined();

                done();
            });
        });

        it('ошибка при очищении корзины', (done) => {
            mockRawFetchModel({
                'do-clean-trash': [{
                    promise: 'reject',
                    data: {}
                }]
            });

            const store = createStore(defaultState);
            store.dispatch(submitCleanTrash()).then(() => {
                expect(notify).toHaveBeenCalledWith({
                    operation: 'cleanTrash',
                    status: 'failed'
                });

                const { resources } = store.getState();
                expect(resources[TRASH_ID].children.append_time_0.ids).toEqual([trashFileId]);
                expect(resources[trashFileId]).toBeTruthy();

                done();
            });
        });
    });

    describe('onSuccessCleanTrash', () => {
        it('обработка пуша про очистку корзины', () => {
            const store = createStore(defaultState);
            store.dispatch(onSuccessCleanTrash());
            expect(fetchSpace).toHaveBeenCalled();
            expect(notify).toHaveBeenCalledWith({
                operation: 'cleanTrash',
                status: 'done'
            });

            const { resources } = store.getState();
            expect(resources[TRASH_ID].children.append_time_0.ids).toEqual([]);
            expect(resources[trashFileId]).toBeUndefined();
        });
    });
});
