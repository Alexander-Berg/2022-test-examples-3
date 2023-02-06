import deepFreeze from 'deep-freeze';
import resources from '../../../../../src/store/reducers/resources';
import {
    UPDATE_RESOURCES, UPDATE_ONE_RESOURCE, FETCH_RESOURCES_LIST_START,
    FETCH_RESOURCES_LIST_SUCCESS, FETCH_RESOURCES_LIST_ERROR
} from '../../../../../src/store/action-types';

import getFixture from '../../../../fixtures';

const getResourcesFixture = (type, params) => getFixture({
    type,
    params,
    formatted: true
}).resources;

describe('resources reducer', () => {
    it('состояние по умолчанию', () => {
        expect(resources(undefined, {})).toEqual({});
    });

    describe('UPDATE_RESOURCES - обновление одиночного ресурса', () => {
        it('файл', () => {
            const stateAfter = resources(undefined, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources:
                        getResourcesFixture('public_info', { hash: 'Pc+MIothz8JeA0GSIWCiaTEkpSklOGg2AA4YZsoYCo=' })
                }
            });
            expect(Object.keys(stateAfter).length).toEqual(1);
            expect(stateAfter).toMatchSnapshot();
        });

        it('папка', () => {
            const stateAfter = resources(undefined, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources:
                        getResourcesFixture('public_info', { hash: '+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=' })
                }
            });
            expect(Object.keys(stateAfter).length).toEqual(1);
            expect(stateAfter).toMatchSnapshot();
        });
    });

    describe('UPDATE_RESOURCES - обновление списка ресурсов', () => {
        const rootHash = '+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=';
        const rootId = 'e574d5e59a3452adc6ed51bb5488f07876c5bbbe101940956ab628efea3974a7';
        const subFolderHash = '+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=000';
        const subFolderId = '91db7945c7664df88a65ab97767c56ff58327bc8c5354a648622c39f4f407e8f';
        const rootResource = getResourcesFixture('public_info', { hash: rootHash })[0];
        let state;

        beforeEach(() => {
            // записываем инфу о корневой папке
            state = resources(undefined, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: [rootResource]
                }
            });
            deepFreeze(state);
        });

        it('первая порция корневой папки', () => {
            const stateAfter = resources(state, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: getResourcesFixture('public_list', { hash: rootHash, offset: 0 })
                }
            });
            expect(Object.keys(stateAfter).length).toEqual(41);
            expect(stateAfter[rootId].completed).toEqual(false);
            expect(stateAfter).toMatchSnapshot();
        });

        it('вторая порция корневой папки', () => {
            state = resources(state, { type: UPDATE_RESOURCES,
                payload: {
                    resources: getResourcesFixture('public_list', { hash: rootHash, offset: 0 })
                }
            });
            deepFreeze(state);

            const stateAfter = resources(state, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: getResourcesFixture('public_list', { hash: rootHash, offset: 40 })
                }
            });
            expect(Object.keys(stateAfter).length).toEqual(81);
            expect(stateAfter[rootId].completed).toEqual(false);
            expect(stateAfter).toMatchSnapshot();
        });

        it('третья порция корневой папки', () => {
            state = resources(state, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: getResourcesFixture('public_list', { hash: rootHash, offset: 0 })
                }
            });
            deepFreeze(state);
            state = resources(state, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: getResourcesFixture('public_list', { hash: rootHash, offset: 40 })
                }
            });
            deepFreeze(state);

            const stateAfter = resources(state, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: getResourcesFixture('public_list', { hash: rootHash, offset: 80 })
                }
            });
            expect(Object.keys(stateAfter).length).toEqual(86);
            expect(stateAfter[rootId].completed).toEqual(true);
            expect(stateAfter).toMatchSnapshot();
        });

        it('первая порция подпапки', () => {
            state = resources(undefined, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: [rootResource, getResourcesFixture('public_info', { hash: subFolderHash })[0]]
                }
            });
            deepFreeze(state);

            const stateAfter = resources(state, { type: UPDATE_RESOURCES, payload: {
                resources: getResourcesFixture('public_list', { hash: subFolderHash, offset: 0 })
            } });
            expect(Object.keys(stateAfter).length).toEqual(42);
            expect(stateAfter[subFolderId].completed).toEqual(false);
            expect(stateAfter).toMatchSnapshot();
        });
    });

    describe('UPDATE_ONE_RESOURCE', () => {
        const hash = '+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=';
        const fileId = 'e574d5e59a3452adc6ed51bb5488f07876c5bbbe101940956ab628efea3974a7';
        let state;
        beforeEach(() => {
            state = resources(undefined, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: getResourcesFixture('public_info', { hash })
                }
            });
            deepFreeze(state);
        });

        it('дополнение ресурса информацией', () => {
            expect(state[fileId].meta.size).toBeUndefined();
            expect(state[fileId].meta.files_count).toBeUndefined();
            const stateAfter = resources(state, {
                type: UPDATE_ONE_RESOURCE,
                payload: {
                    resourceId: fileId,
                    resource: {
                        meta: {
                            size: 100500,
                            files_count: 85
                        }
                    }
                }
            });
            expect(stateAfter[fileId].meta.size).toEqual(100500);
            expect(stateAfter[fileId].meta.files_count).toEqual(85);
            expect(stateAfter).toMatchSnapshot();
        });

        it('обновление ресурса не должно затирать ранее записанные поля', () => {
            state = resources(state, {
                type: UPDATE_ONE_RESOURCE,
                payload: {
                    resourceId: fileId,
                    resource: {
                        meta: {
                            size: 100500,
                            files_count: 85
                        }
                    }
                }
            });
            deepFreeze(state);
            expect(state[fileId].meta.size).toEqual(100500);
            expect(state[fileId].meta.files_count).toEqual(85);

            const stateAfter = resources(state, {
                type: UPDATE_ONE_RESOURCE,
                payload: {
                    resourceId: fileId,
                    resource: {
                        meta: {}
                    }
                }
            });
            expect(stateAfter[fileId].meta.size).toEqual(100500);
            expect(stateAfter[fileId].meta.files_count).toEqual(85);
        });

        it('UPDATE_RESOURCES тоже не должен затирать ранее записанные поля', () => {
            state = resources(state, {
                type: UPDATE_ONE_RESOURCE,
                payload: {
                    resourceId: fileId,
                    resource: {
                        meta: {
                            size: 100500,
                            files_count: 85
                        }
                    }
                }
            });
            deepFreeze(state);
            expect(state[fileId].meta.size).toEqual(100500);
            expect(state[fileId].meta.files_count).toEqual(85);

            const stateAfter = resources(state, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: getResourcesFixture('public_info', { hash })
                }
            });
            expect(stateAfter[fileId].meta.size).toEqual(100500);
            expect(stateAfter[fileId].meta.files_count).toEqual(85);
        });
    });

    describe('FETCH_RESOURCES_*', () => {
        const fileId = 'e574d5e59a3452adc6ed51bb5488f07876c5bbbe101940956ab628efea3974a7';
        let state;

        beforeEach(() => {
            // записываем инфу о корневой папке
            state = resources(undefined, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources:
                        getResourcesFixture('public_info', { hash: '+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=' })
                }
            });
            deepFreeze(state);
        });

        it('FETCH_RESOURCES_LIST_START', () => {
            expect(state[fileId].loading).toEqual(false);
            const stateAfter = resources(state, {
                type: FETCH_RESOURCES_LIST_START,
                id: fileId
            });
            expect(stateAfter[fileId].loading).toEqual(true);
        });

        it('FETCH_RESOURCES_LIST_SUCCESS', () => {
            state = resources(state, {
                type: FETCH_RESOURCES_LIST_START,
                id: fileId
            });
            deepFreeze(state);
            expect(state[fileId].loading).toEqual(true);

            const stateAfter = resources(state, {
                type: FETCH_RESOURCES_LIST_SUCCESS,
                id: fileId
            });
            expect(stateAfter[fileId].loading).toEqual(false);
        });

        it('FETCH_RESOURCES_LIST_ERROR', () => {
            state = resources(state, {
                type: FETCH_RESOURCES_LIST_START,
                id: fileId
            });
            deepFreeze(state);
            expect(state[fileId].loading).toEqual(true);

            const stateAfter = resources(state, {
                type: FETCH_RESOURCES_LIST_ERROR,
                id: fileId
            });
            expect(stateAfter[fileId].loading).toEqual(false);
        });
    });
});
