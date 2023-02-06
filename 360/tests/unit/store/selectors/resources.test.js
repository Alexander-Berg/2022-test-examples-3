import '../../noscript';
import {
    isLoadingResource,
    getResources,
    shouldScrollToResource
} from '../../../../components/redux/store/selectors/resources';

describe('resources selectors', () => {
    describe('isLoadingResource', () => {
        it('should return false if resource is not in store', () => {
            const state = {
                environment: { agent: {}, session: { experiment: {} } },
                page: {
                    idContext: '/disk'
                },
                uploader: { uploadingStatesByIds: { } },
                defaultFolders: { folders: { } },
                statesContext: {
                    sort: { '/disk': { sort: 'name', order: '0' } }
                },
                settings: {},
                resources: {}
            };

            expect(isLoadingResource(state)).toBe(false);
        });

        it('should return false if resource is in not loading', () => {
            const state = {
                environment: { agent: {} },
                page: {
                    idContext: '/disk'
                },
                uploader: { uploadingStatesByIds: { } },
                defaultFolders: { folders: { } },
                statesContext: {
                    sort: { '/disk': { sort: 'name', order: '0' } }
                },
                settings: {},
                resources: {
                    '/disk': {
                        isLoading: false
                    }
                }
            };

            expect(isLoadingResource(state)).toBe(false);
        });

        it('should return false if resource is loading second portion', () => {
            const state = {
                environment: { agent: {} },
                page: {
                    idContext: '/disk'
                },
                uploader: { uploadingStatesByIds: { } },
                defaultFolders: { folders: { } },
                statesContext: {
                    sort: { '/disk': { sort: 'name', order: '0' } }
                },
                settings: { },
                resources: {
                    '/disk': {
                        id: '/disk',
                        isLoading: true,
                        children: {
                            name_0: {
                                isComplete: false,
                                ids: ['/disk/foo']
                            }
                        }
                    },
                    '/disk/foo': {}
                }
            };

            expect(isLoadingResource(state)).toBe(false);
        });

        it('should return true if resource is loading its first portion', () => {
            const state = {
                environment: { agent: {} },
                page: {
                    idContext: '/disk'
                },
                uploader: { uploadingStatesByIds: { } },
                defaultFolders: { folders: { } },
                statesContext: {
                    sort: { '/disk': { sort: 'name', order: '0' } }
                },
                settings: { },
                resources: {
                    '/disk': {
                        isLoading: true,
                        children: []
                    }
                }
            };

            expect(isLoadingResource(state)).toBe(true);
        });
    });

    describe('getResources', () => {
        it('должен фильтровать скрытые ресурсы', () => {
            const state = {
                environment: { agent: {} },
                page: { idContext: '/disk' },
                uploader: { uploadingStatesByIds: { } },
                defaultFolders: { folders: { } },
                statesContext: {
                    sort: { '/disk': { sort: 'name', order: '0' } }
                },
                settings: { },
                resources: {
                    '/disk': {
                        id: '/disk',
                        children: {
                            name_0: {
                                isComplete: false,
                                ids: ['/disk/one', '/disk/two', '/disk/three']
                            }
                        }
                    },
                    '/disk/one': {
                        id: '/disk/one'
                    },
                    '/disk/two': {
                        id: '/disk/two',
                        state: {
                            hidden: true
                        }
                    },
                    '/disk/three': {
                        id: '/disk/three'
                    }
                }
            };

            expect(getResources(state)).toEqual([
                { id: '/disk/one' },
                { id: '/disk/three' }
            ]);
        });
    });

    describe('shouldScrollToResource', () => {
        it('должен возвращать { shouldScroll: true, shouldSelect: true } для листинга на десктопе', () => {
            const state = {
                environment: { agent: { isMobile: false }, session: { experiment: {} } },
                user: { },
                page: { idContext: '/disk/some-folder', search: {} },
                suggest: {},
                config: {}
            };
            expect(shouldScrollToResource(state)).toEqual({ shouldScroll: true, shouldSelect: true });
        });

        it('должен возвращать { shouldScroll: true, shouldSelect: false } для листинга на тачах', () => {
            const state = {
                environment: { agent: { isMobile: true }, session: { experiment: {} } },
                user: { },
                page: { idContext: '/disk/some-folder', search: {} },
                suggest: {},
                config: {}
            };
            expect(shouldScrollToResource(state)).toEqual({ shouldScroll: true, shouldSelect: false });
        });

        it('должен возвращать { shouldScroll: true, shouldSelect: false } для блока воспоминапний', () => {
            const state = {
                environment: { agent: { isMobile: true }, session: { experiment: {} } },
                user: { },
                page: { idContext: '/remember/block-id', search: {} },
                suggest: {},
                config: {}
            };
            expect(shouldScrollToResource(state)).toEqual({ shouldScroll: true, shouldSelect: false });
        });

        it('должен возвращать { shouldScroll: true, shouldSelect: false } для фотосреза', () => {
            const state = {
                environment: { agent: { isMobile: false }, session: { experiment: {} } },
                user: { },
                page: { idContext: '/photo', search: {} },
                suggest: {},
                config: {}
            };
            expect(shouldScrollToResource(state)).toEqual({ shouldScroll: true, shouldSelect: false });
        });

        it('должен возвращать { shouldScroll: false, shouldSelect: false } если открыт саджест', () => {
            const state = {
                environment: { agent: { }, session: { experiment: {} } },
                user: { },
                page: { idContext: '/disk/some-folder', search: { } },
                suggest: { active: true },
                config: {}
            };
            expect(shouldScrollToResource(state)).toEqual({ shouldScroll: false, shouldSelect: false });
        });
    });
});
