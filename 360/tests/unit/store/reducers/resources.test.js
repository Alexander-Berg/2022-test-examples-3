import resources from '../../../../components/redux/store/reducers/resources';
import {
    UPDATE_RESOURCES,
    UPDATE_CLUSTERS_AND_RESOURCES,
    MOVE_CLONED_RESOURCES,
    DESTROY_RESOURCE,
    DESTROY_RESOURCE_CHILDREN,
    CREATE_RESOURCES
} from '../../../../components/redux/store/actions/types';
import deepFreeze from 'deep-freeze';

const defaultState = {
    '/disk': {
        children: {
            name_1: {
                isComplete: true,
                countLost: 0,
                ids: ['/disk/1', '/disk/2', '/disk/1.jpg', '/disk/2.mp4', '/disk/3.jpg']
            },
            name_0: {
                isComplete: true,
                countLost: 0,
                ids: ['/disk/2', '/disk/1', '/disk/3.jpg', '/disk/2.mp4', '/disk/1.jpg']
            },
            size_1: {
                isComplete: true,
                countLost: 0,
                ids: ['/disk/2', '/disk/1', '/disk/3.jpg', '/disk/1.jpg', '/disk/2.mp4']
            },
            size_0: {
                isComplete: true,
                countLost: 0,
                ids: ['/disk/2', '/disk/1', '/disk/2.mp4', '/disk/1.jpg', '/disk/3.jpg']
            },
            type_1: {
                isComplete: true,
                countLost: 0,
                ids: ['/disk/1', '/disk/2', '/disk/1.jpg', '/disk/3.jpg', '/disk/2.mp4']
            },
            type_0: {
                isComplete: true,
                countLost: 0,
                ids: ['/disk/1', '/disk/2', '/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
            },
            mtime_1: {
                isComplete: true,
                countLost: 0,
                ids: ['/disk/2', '/disk/1', '/disk/1.jpg', '/disk/3.jpg', '/disk/2.mp4']
            },
            mtime_0: {
                isComplete: true,
                countLost: 0,
                ids: ['/disk/1', '/disk/2', '/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
            }
        }
    },
    '/recent': {
        id: '/recent',
        children: {
            mtime_0: {
                isComplete: true,
                countLost: 0,
                ids: ['/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
            }
        }
    },
    '/shared': {
        id: '/shared',
        children: {
            name_1: {
                isComplete: true,
                countLost: 0,
                ids: []
            }
        }
    },
    '/published': {
        id: '/published',
        children: {
            name_1: {
                isComplete: true,
                countLost: 0,
                ids: []
            }
        }
    },
    '/disk/1/shared-folder': {
        id: '/disk/1/shared-folder',
        name: 'shared-folder',
        type: 'dir',
        mtime: 1583400016,
        meta: {
            group: {
                is_root: 1
            }
        },
        parents: [{ id: '/disk' }, { id: '/disk/1' }]
    },
    '/disk/1/public-folder': {
        id: '/disk/1/public-folder',
        name: 'public-folder',
        type: 'dir',
        mtime: 1583400016,
        meta: {
            public: 1
        },
        parents: [{ id: '/disk' }, { id: '/disk/1' }]
    },
    '/disk/1': {
        id: '/disk/1',
        name: '1',
        type: 'dir',
        mtime: 1583400016,
        parents: [{ id: '/disk' }]
    },
    '/disk/2': {
        id: '/disk/2',
        name: '2',
        type: 'dir',
        mtime: 1583340914,
        parents: [{ id: '/disk' }]
    },
    '/disk/0': {
        id: '/disk/0',
        name: '0',
        type: 'dir',
        mtime: 1583401128,
        parents: [{ id: '/disk' }]
    },
    '/disk/1.jpg': {
        id: '/disk/1.jpg',
        name: '1.jpg',
        type: 'file',
        mtime: 1583340973,
        meta: {
            mediatype: 'image',
            mimetype: 'image/jpeg',
            size: 25961
        },
        parents: [{ id: '/disk' }]
    },
    '/disk/2.mp4': {
        id: '/disk/2.mp4',
        name: '2.mp4',
        type: 'file',
        mtime: 1583341246,
        meta: {
            mediatype: 'video',
            mimetype: 'video/quicktime',
            size: 1449490
        },
        parents: [{ id: '/disk' }]
    },
    '/disk/3.jpg': {
        id: '/disk/3.jpg',
        name: '3.jpg',
        type: 'file',
        mtime: 1583341241,
        meta: {
            mediatype: 'image',
            mimetype: 'image/jpeg',
            size: 370
        },
        parents: [{ id: '/disk' }]
    },
    '/disk/0.mp4': {
        id: '/disk/0.mp4',
        name: '0.mp4',
        type: 'file',
        mtime: 1583400907,
        meta: {
            mediatype: 'video',
            mimetype: 'video/quicktime',
            size: 4001877
        },
        parents: [{ id: '/disk' }]
    },
    '/disk/new.jpg': { // part-????????????
        id: '/disk/new.jpg',
        type: 'file',
        name: 'new.jpg',
        parents: [{ id: '/disk' }]
    }
};

describe('resources reducer', () => {
    describe('UPDATE_RESOURCES, UPDATE_CLUSTERS_AND_RESOURCES', () => {
        const RESOURCES = [{
            id: '/disk/1',
            name: '1',
            meta: {
                size: 1
            },
            state: {}
        }, {
            id: '/disk/2',
            name: '2',
            meta: {
                size: 2
            },
            state: {}
        }];

        let defaultState;
        beforeEach(() => {
            defaultState = {};
            deepFreeze(defaultState);
        });

        [UPDATE_RESOURCES, UPDATE_CLUSTERS_AND_RESOURCES].forEach((type) => {
            it(`???????????? ?????????????????? ?????????????????? ???????????????? (${type})`, () => {
                const state = resources(defaultState, {
                    type,
                    payload: { resources: [RESOURCES[0], RESOURCES[1]] }
                });
                expect(state).toEqual({
                    [RESOURCES[0].id]: RESOURCES[0],
                    [RESOURCES[1].id]: RESOURCES[1]
                });
            });

            it(`???????????? ?????????????????? ???? ???????????? ?????????????? (${type})`, () => {
                let state = resources(defaultState, {
                    type,
                    payload: { resources: [RESOURCES[0]] }
                });
                deepFreeze(state);
                state = resources(state, {
                    type,
                    payload: { resources: [RESOURCES[1]] }
                });
                expect(state).toEqual({
                    [RESOURCES[0].id]: RESOURCES[0],
                    [RESOURCES[1].id]: RESOURCES[1]
                });
            });
        });
    });

    describe('MOVE_CLONED_RESOURCES', () => {
        let defaultState;
        beforeEach(() => {
            defaultState = {
                '/photo': {
                    id: '/photo'
                },
                '/remember/id': {
                    id: '/remember/id',
                    children: ['/disk/1', '/disk/2', '/disk/3']
                },
                '/disk/1': {
                    id: '/disk/1',
                    name: '1'
                },
                '/disk/2': {
                    id: '/disk/2',
                    name: '3'
                },
                '/disk/3': {
                    id: '/disk/3',
                    name: '3'
                }
            };
            deepFreeze(defaultState);
        });

        it('?? ?????????? ???????????????????????? ???????????????????????? ???????????? ???????????? ???????????????? ???? ?????? ???? ??????????', () => {
            const state = resources(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    idContext: '/remember/id',
                    resources: [{
                        src: { id: '/disk/2', name: '2' },
                        dst: { id: '/disk/2__', name: '2__' }
                    }]
                }
            });

            expect(state['/remember/id'].children).toEqual(['/disk/1', '/disk/2__', '/disk/3']);
            expect(state['/disk/2__']).toEqual({ id: '/disk/2__', name: '2__' });
        });

        it('?? ?????????? ???????????????????????? ???????????????????????? ?????????????? ???????????? ???????????????? ?? ????????????????', () => {
            const state = resources(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    idContext: '/remember/id',
                    resources: [
                        {
                            src: { id: '/disk/1', name: '1' },
                            dst: { id: '/disk/1__', name: '1__' }
                        },
                        {
                            src: { id: '/disk/2', name: '2' },
                            dst: { id: '/disk/2__', name: '2__' }
                        },
                        {
                            src: { id: '/disk/3', name: '3' },
                            dst: { id: '/disk/3__', name: '3__' }
                        }
                    ]
                }
            });

            expect(state['/remember/id'].children).toEqual(['/disk/1__', '/disk/2__', '/disk/3__']);
            expect(state['/disk/1__']).toEqual({ id: '/disk/1__', name: '1__' });
            expect(state['/disk/2__']).toEqual({ id: '/disk/2__', name: '2__' });
            expect(state['/disk/3__']).toEqual({ id: '/disk/3__', name: '3__' });
        });

        it('?? ?????????????????? ???????????? ?????????????? ?????????????? ???? ???????? ????????????????????', () => {
            const state = resources(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    idContext: '/photo',
                    resources: [{
                        src: { id: '/disk/2', name: '2' },
                        dst: { id: '/disk/2__', name: '2__' }
                    }, {
                        src: { id: '/disk/3', name: '3' },
                        dst: { id: '/disk/3__', name: '3__' }
                    }]
                }
            });

            expect(state['/disk/2__']).toEqual({ id: '/disk/2__', name: '2__' });
            expect(state['/disk/3__']).toEqual({ id: '/disk/3__', name: '3__' });
        });

        it('?? ?????????????????? ???? ???????????? ???????????? ???????????? ???????? ?????????????? ??????????????????', () => {
            const state = resources(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    idContext: '/photo',
                    resources: [{
                        src: { id: '/disk/2', name: '2' },
                        dst: { id: '/trash/2', name: '2', isInTrash: true }
                    }, {
                        src: { id: '/disk/3', name: '3' },
                        dst: { id: '/trash/3', name: '3', isInTrash: true }
                    }]
                }
            });

            expect(state).toBe(defaultState);
        });

        it('?????????????????? ?????????? ?? ?????????????????? (1 ???????????? ??????????????, 1 ??????????????????????)', () => {
            const state = resources(defaultState, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    idContext: '/photo',
                    resources: [{
                        src: { id: '/disk/2', name: '2' },
                        dst: { id: '/disk/2__', name: '2__' }
                    }, {
                        src: { id: '/disk/3', name: '3' },
                        dst: { id: '/trash/3', name: '3', isInTrash: true }
                    }]
                }
            });

            expect(state['/disk/2__']).toEqual({ id: '/disk/2__', name: '2__' });
            expect(state['/trash/3']).toBeUndefined();
        });
    });

    describe('UPDATE_RESOURCES', () => {
        it('?????? ?????????????????? mtime ???????????? ???????????? children ?? ?????????????? "??????????????????"', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: [{ id: '/disk/1.jpg', mtime: 1583409415 }]
                }
            });

            expect(state['/recent'].children).toEqual({
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1.jpg', '/disk/2.mp4', '/disk/3.jpg']
                }
            });
        });

        it('???????? ?? ?????????????? ???????????? mtime, ???? ???? ???????????? ???????????????? ???? ?????????????????? ????????????', () => {
            deepFreeze(defaultState);
            expect(defaultState['/recent'].children.mtime_0.ids.includes('/disk/1.jpg')).toBe(true);
            const state = resources(defaultState, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: [{ id: '/disk/1.jpg', mtime: undefined }]
                }
            });

            expect(state['/recent'].children).toEqual({
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2.mp4', '/disk/3.jpg']
                }
            });
        });

        it('???????? ?? ?????????????? ???????????????? mtime, ???? ???? ???????????? ?????????????????? ?? ???????????????????? ????????????', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: [{ id: '/disk/new.jpg', mtime: 1583409415 }]
                }
            });

            expect(state['/recent'].children).toEqual({
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/new.jpg', '/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                }
            });
        });

        it('???????????? ???????????????? ?????????? ?? /shared, ???????? ?????? ?????????? ??????????', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: [{ id: '/disk/1', meta: { group: { is_root: 1 } } }]
                }
            });

            expect(state['/shared'].children).toEqual({
                name_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1']
                }
            });
        });

        it('???????????? ?????????????? ?????????? ???? /shared, ???????? ?????? ?????????????????? ???????? ??????????', () => {
            const stateWithShared = Object.assign({}, defaultState, {
                '/shared': Object.assign({}, defaultState['/shared'], {
                    children: {
                        name_1: {
                            isComplete: true,
                            countLost: 0,
                            ids: ['/disk/1/shared-folder']
                        }
                    }
                })
            });
            deepFreeze(stateWithShared);
            const state = resources(stateWithShared, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: [{ id: '/disk/1/shared-folder', meta: { } }]
                }
            });

            expect(state['/shared'].children).toEqual({
                name_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: []
                }
            });
        });

        it('???????????? ???????????????? ?????????? ?? /published, ???????? ?????? ?????????? ??????????????????', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: [{ id: '/disk/1', meta: { public: 1 } }]
                }
            });

            expect(state['/published'].children).toEqual({
                name_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1']
                }
            });
        });

        it('???????????? ?????????????? ?????????? ???? /published, ???????? ?????? ?????????????????? ???????? ??????????????????', () => {
            const stateWithPublished = Object.assign({}, defaultState, {
                '/published': Object.assign({}, defaultState['/published'], {
                    children: {
                        name_1: {
                            isComplete: true,
                            countLost: 0,
                            ids: ['/disk/1/public-folder']
                        }
                    }
                })
            });
            deepFreeze(stateWithPublished);
            const state = resources(stateWithPublished, {
                type: UPDATE_RESOURCES,
                payload: {
                    resources: [{ id: '/disk/1/public-folder', meta: { } }]
                }
            });

            expect(state['/published'].children).toEqual({
                name_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: []
                }
            });
        });
    });

    describe('DESTROY_RESOURCE', () => {
        it('???????????? ?????????????? ???????????? ???? ?????????? ?? children ?????????????????????????? ??????????????', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: DESTROY_RESOURCE,
                payload: { id: '/disk/1', data: {} }
            });

            expect(state['/disk/1']).toBeUndefined();
            expect(state['/disk'].children).toEqual({
                name_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1.jpg', '/disk/2.mp4', '/disk/3.jpg']
                },
                name_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/3.jpg', '/disk/2.mp4', '/disk/1.jpg']
                },
                size_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/3.jpg', '/disk/1.jpg', '/disk/2.mp4']
                },
                size_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/2.mp4', '/disk/1.jpg', '/disk/3.jpg']
                },
                type_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1.jpg', '/disk/3.jpg', '/disk/2.mp4']
                },
                type_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                },
                mtime_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1.jpg', '/disk/3.jpg', '/disk/2.mp4']
                },
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                },
            });
        });
    });

    describe('CREATE_RESOURCES', () => {
        it('???????????? ?????????????????? children ?? ???????????????????????? ?????????? ?????? ???????????????????? ?????????? ?? ?????????????????????? ???????????? c ???????????? ????????????????????', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: CREATE_RESOURCES,
                payload: [{ id: '/disk/0.mp4', options: { insertOnlyIfPortionLoaded: true } }]
            });

            expect(state['/disk'].children).toEqual({
                name_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1', '/disk/2', '/disk/0.mp4', '/disk/1.jpg', '/disk/2.mp4', '/disk/3.jpg']
                },
                name_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1', '/disk/3.jpg', '/disk/2.mp4', '/disk/1.jpg', '/disk/0.mp4']
                },
                size_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1', '/disk/3.jpg', '/disk/1.jpg', '/disk/2.mp4', '/disk/0.mp4']
                },
                size_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1', '/disk/0.mp4', '/disk/2.mp4', '/disk/1.jpg', '/disk/3.jpg']
                },
                type_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1', '/disk/2', '/disk/1.jpg', '/disk/3.jpg', '/disk/0.mp4', '/disk/2.mp4']
                },
                type_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1', '/disk/2', '/disk/2.mp4', '/disk/0.mp4', '/disk/3.jpg', '/disk/1.jpg']
                },
                mtime_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1', '/disk/1.jpg', '/disk/3.jpg', '/disk/2.mp4', '/disk/0.mp4']
                },
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1', '/disk/2', '/disk/0.mp4', '/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                }
            });
        });

        it('???????????? ?????????????????? children ?? ???????????????????????? ?????????? ?????? ???????????????????? ?????????? ?? ?????????????????????? ???????????? c ???????????? ????????????????????', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: CREATE_RESOURCES,
                payload: [{ id: '/disk/0', options: { insertOnlyIfPortionLoaded: true } }]
            });

            expect(state['/disk'].children).toEqual({
                name_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/0', '/disk/1', '/disk/2', '/disk/1.jpg', '/disk/2.mp4', '/disk/3.jpg']
                },
                name_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1', '/disk/0', '/disk/3.jpg', '/disk/2.mp4', '/disk/1.jpg']
                },
                size_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1', '/disk/0', '/disk/3.jpg', '/disk/1.jpg', '/disk/2.mp4']
                },
                size_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/0', '/disk/2', '/disk/1', '/disk/2.mp4', '/disk/1.jpg', '/disk/3.jpg']
                },
                type_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/0', '/disk/1', '/disk/2', '/disk/1.jpg', '/disk/3.jpg', '/disk/2.mp4']
                },
                type_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1', '/disk/2', '/disk/0', '/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                },
                mtime_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2', '/disk/1', '/disk/0', '/disk/1.jpg', '/disk/3.jpg', '/disk/2.mp4']
                },
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/0', '/disk/1', '/disk/2', '/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                }
            });
        });

        it('???????????? ?????????????????? children ?????????????? "?????????????????? ??????????" ?????? ???????????????????? ??????????', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: CREATE_RESOURCES,
                payload: [{ id: '/disk/0.mp4', options: { insertOnlyIfPortionLoaded: true } }]
            });

            expect(state['/recent'].children).toEqual({
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/0.mp4', '/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                }
            });
        });

        it('???? ???????????? ?????????????????? children ?????????????? "?????????????????? ??????????" ?????? ???????????????????? ??????????', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: CREATE_RESOURCES,
                payload: [{ id: '/disk/0', options: { insertOnlyIfPortionLoaded: true } }]
            });

            expect(state['/recent'].children).toEqual({
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                }
            });
        });

        it('???? ???????????? ?????????????????? children ?????????????? "?????????????????? ??????????" ?????? ???????????????????? ?????????????? ?????? mtime', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: CREATE_RESOURCES,
                payload: [{ id: '/disk/new.jpg', options: { insertOnlyIfPortionLoaded: true } }]
            });

            expect(state['/recent'].children).toEqual({
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                }
            });
        });

        it('???? ???????????? ?????????????????? children ???????? ?????????????????????? ???????????? ?????????????? ?????? ???????? ?? children', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: CREATE_RESOURCES,
                payload: [{ id: '/disk/1.jpg', options: { insertOnlyIfPortionLoaded: true } }]
            });

            expect(state['/recent'].children).toEqual({
                mtime_0: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/2.mp4', '/disk/3.jpg', '/disk/1.jpg']
                }
            });
        });

        it('?????? ???????????????????? ?????????? ?????????? ???????????? ?????????????????? children ?????????????? "?????????? ??????????"', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: CREATE_RESOURCES,
                payload: [{ id: '/disk/1/shared-folder', options: { insertOnlyIfPortionLoaded: true } }]
            });

            expect(state['/shared'].children).toEqual({
                name_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1/shared-folder']
                }
            });
        });

        it('?????? ???????????????????? ?????????????????? ?????????? ???????????? ?????????????????? children ?????????????? "????????????"', () => {
            deepFreeze(defaultState);
            const state = resources(defaultState, {
                type: CREATE_RESOURCES,
                payload: [{ id: '/disk/1/public-folder', options: { insertOnlyIfPortionLoaded: true } }]
            });

            expect(state['/published'].children).toEqual({
                name_1: {
                    isComplete: true,
                    countLost: 0,
                    ids: ['/disk/1/public-folder']
                }
            });
        });
    });

    describe('DESTROY_RESOURCE_CHILDREN', () => {
        it('???????????? ?????????????? children ???????????????????? ?????????????? ?? ???? ???????????????????? isComplete, ???????? ?????? ???? ??????????', () => {
            const defaultState = {
                '/disk/1': {
                    id: '/disk/1',
                    children: {
                        name_1: { ids: ['/disk/1/1'], isComplete: true },
                        name_0: { ids: ['/disk/1/1'], isComplete: true }
                    }
                },
                '/disk/1/1': {
                    id: '/disk/1/1'
                },
                '/disk/2': {
                    id: '/disk/2',
                    name: '2',
                    children: { name_1: { ids: ['/disk/2/1'], isComplete: true } }
                },
                '/disk/2/1': {
                    id: '/disk/2/1'
                },
            };
            deepFreeze(defaultState);

            const state = resources(defaultState, {
                type: DESTROY_RESOURCE_CHILDREN,
                payload: { parentId: '/disk/1', resetComplete: false }
            });

            expect(state['/disk/1'].children.name_1.ids).toEqual([]);
            expect(state['/disk/1'].children.name_1.isComplete).toEqual(true);
            expect(state['/disk/1'].children.name_0.ids).toEqual([]);
            expect(state['/disk/1'].children.name_0.isComplete).toEqual(true);
            expect(state['/disk/1/1']).toBeUndefined();
        });

        it('???????????? ???????????????????? isCompleted ???????????????????? ?????????????? ?????? ??????????????????????????', () => {
            const defaultState = {
                '/disk/1': {
                    id: '/disk/1',
                    children: {
                        name_1: { ids: ['/disk/1/1'], isComplete: true },
                        name_0: { ids: ['/disk/1/1'], isComplete: true }
                    }
                },
                '/disk/1/1': {
                    id: '/disk/1/1'
                }
            };
            deepFreeze(defaultState);

            const state = resources(defaultState, {
                type: DESTROY_RESOURCE_CHILDREN,
                payload: { parentId: '/disk/1', resetComplete: true }
            });

            expect(state['/disk/1'].children.name_1.ids).toEqual([]);
            expect(state['/disk/1'].children.name_1.isComplete).toEqual(false);
            expect(state['/disk/1'].children.name_0.ids).toEqual([]);
            expect(state['/disk/1'].children.name_0.isComplete).toEqual(false);
            expect(state['/disk/1/1']).toBeUndefined();
        });
    });
});
