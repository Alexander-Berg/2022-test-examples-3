import '../noscript';
import merge from 'lodash/merge';
import {
    getAvailableActions,
    getDisableReasons,
    can,
    PUBLISH,
    UNPUBLISH,
    FOTKI_INFO,
    DOWNLOAD,
    CLEAN_TRASH,
    RESTORE,
    EDIT,
    VIEW,
    RENAME,
    MOVE,
    MOVE_INTO,
    DELETE,
    DELETE_FROM_TRASH,
    EXCLUDE_FROM_ALBUM,
    EXCLUDE_FROM_GEO_ALBUM,
    EXCLUDE_FROM_PERSONAL_ALBUM,
    COPY,
    ADD_TO_ALBUM,
    SHARE_ACCESS,
    SHOW_FULLSIZE,
    VERSIONS,
    GO_TO_FILE,
    GO_TO_FOLDER,
    DISABLE_REASONS,
    CREATE_INSIDE,
    TOGGLE_FAVORITES
} from '../../../components/helpers/resource-actions';
import { normalize } from '../../../components/helpers/path';
import TEST_RESOURCES from '../../fixtures/resources';
import { getPreloadedData } from '../../../components/preloaded-data';
import createStore from '../../../components/redux/store/create-store';
import { getResourceData } from '../../../components/redux/store/selectors/resource';
import { setResourceData, batchSetResourceData } from '../../../components/redux/store/actions/resources';

describe('resourceActionsHelper', () => {
    const defaultState = {
        page: { idContext: '/disk' },
        settings: { },
        statesContext: { sort: { } },
        defaultFolders: getPreloadedData('defaultFolders'),
        environment: {
            session: { experiment: {} },
            agent: {
                hasAdblock: false,
                BrowserBase: 'Chromium',
                BrowserBaseVersion: '65.0.3319.0',
                BrowserEngine: 'WebKit',
                BrowserEngineVersion: '537.36',
                BrowserName: 'Chrome',
                BrowserVersion: '65.0.3319',
                OSFamily: 'MacOS',
                OSVersion: '10.12.6',
                isBrowser: true,
                isMobile: false,
                isTablet: false,
                isSmartphone: false,
                botSocial: false,
                isSupported: true,
                osId: 'mac'
            }
        },
        user: { states: { narod_migrated: 1 } },
        personalAlbums: { albumsByIds: {} }
    };

    let store;

    describe('папки', () => {
        beforeEach(() => {
            store = createStore(defaultState);
        });

        it('действия над обычной папкой', () => {
            const id = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.privateFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                MOVE_INTO,
                COPY,
                SHARE_ACCESS,
                CREATE_INSIDE
            ]);
        });

        it('действия над своей общей папкой', () => {
            const id = normalize(TEST_RESOURCES.sharedOwnedFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.sharedOwnedFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                MOVE_INTO,
                COPY,
                SHARE_ACCESS,
                CREATE_INSIDE
            ]);
        });

        it('действия над гостевой общей папкой', () => {
            const id = normalize(TEST_RESOURCES.sharedGuestFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.sharedGuestFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                MOVE_INTO,
                COPY,
                CREATE_INSIDE
            ]);
        });

        it('действия над гостевой общей папкой доступной только для чтения', () => {
            const id = normalize(TEST_RESOURCES.sharedGuestReadonlyFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.sharedGuestReadonlyFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY
            ]);
        });

        it('действия над папкой внутри гостевой общей папки', () => {
            const id = normalize(TEST_RESOURCES.folderInsideSharedGuestFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.folderInsideSharedGuestFolder));

            const state = store.getState();
            const resource = getResourceData(state, id);
            expect(getAvailableActions([resource], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                MOVE_INTO,
                COPY,
                CREATE_INSIDE
            ]);
            expect(getDisableReasons([resource], state)).toEqual([{
                action: SHARE_ACCESS,
                reason: DISABLE_REASONS.NESTED_SHARED_FOLDERS
            }]);
        });

        it('действия над папкой внутри /attach/YaFotki', () => {
            const id = normalize(TEST_RESOURCES.folderInsideYaFotkiFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.folderInsideYaFotkiFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                MOVE,
                COPY
            ]);
        });

        it('действия над папкой Yandex Team (NDA)', () => {
            const id = normalize(TEST_RESOURCES.yaTeamNDAFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.yaTeamNDAFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                DOWNLOAD,
                MOVE_INTO,
                CREATE_INSIDE
            ]);
        });

        it('действия над папкой "Отправленные как ссылки на диск"', () => {
            const id = normalize(TEST_RESOURCES.attachFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.attachFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([]);
        });

        it('действия над корнем', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/disk/folder' } }));

            const id = normalize(TEST_RESOURCES.root.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.root));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                MOVE_INTO,
                CREATE_INSIDE
            ]);
        });

        it('действия над корнем в /disk', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/disk' } }));

            const id = normalize(TEST_RESOURCES.root.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.root));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                CREATE_INSIDE
            ]);
        });

        it('действия над корнем диска в папке в /attach', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/attach' } }));

            const id = normalize(TEST_RESOURCES.root.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.root));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                CREATE_INSIDE
            ]);
        });

        it('действия над корнем диска в /attach/YaFotki', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/attach/YaFotki' } }));

            const id = normalize(TEST_RESOURCES.root.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.root));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                MOVE_INTO,
                CREATE_INSIDE
            ]);
        });

        it('действия над корзиной', () => {
            const id = normalize(TEST_RESOURCES.trash.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.trash));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                CLEAN_TRASH,
                MOVE_INTO
            ]);
        });

        it('действия над корзиной в корзине', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/trash' } }));

            const id = normalize(TEST_RESOURCES.trash.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.trash));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                CLEAN_TRASH
            ]);
        });

        it('действия над корзиной во время очистки', () => {
            const id = normalize(TEST_RESOURCES.trash.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.trashDuringCleaning));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([]);
        });

        it('действия над папкой в корзине', () => {
            const id = normalize(TEST_RESOURCES.folderInTrash.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.folderInTrash));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                DELETE_FROM_TRASH,
                RESTORE
            ]);
        });
    });

    describe('файлы', () => {
        beforeEach(() => {
            store = createStore(defaultState);
        });

        it('действия над обычным файлом', () => {
            const id = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.file));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                VIEW,
                RENAME,
                MOVE,
                COPY,
                VERSIONS,
                GO_TO_FILE
            ]);
        });

        it('действия над файлом зараженным вирусом', () => {
            const id = normalize(TEST_RESOURCES.virus.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.virus));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY
            ]);
        });

        it('действия над видео', () => {
            const id = normalize(TEST_RESOURCES.video.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.video));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                VERSIONS,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над видео без превью', () => {
            const id = normalize(TEST_RESOURCES.videoWithoutPreview.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.videoWithoutPreview));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                VERSIONS
            ]);
        });

        it('действия над фото', () => {
            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photo));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото в Альбоме-срезе(Красивые)', () => {
            store = createStore(
                Object.assign(
                    {},
                    defaultState,
                    { page: { idContext: '/photo', filter: 'beautiful' } }
                )
            );

            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photo));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                EXCLUDE_FROM_ALBUM,
                GO_TO_FILE,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото из чужой общей папки в Альбоме-срезе(Красивые)', () => {
            store = createStore(
                Object.assign(
                    {},
                    defaultState,
                    { page: { idContext: '/photo', filter: 'beautiful' } }
                )
            );

            const id = normalize(TEST_RESOURCES.photoInsideGuestSharedFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photoInsideGuestSharedFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                GO_TO_FILE,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото в Альбоме-срезе(отличном от Красивое и Разобрать)', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/photo', filter: 'camera' } }));

            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photo));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                GO_TO_FILE,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над опубликованным фото', () => {
            const id = normalize(TEST_RESOURCES.publicPhoto.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.publicPhoto));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                UNPUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                VERSIONS,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над безлимитным фото', () => {
            const id = normalize(TEST_RESOURCES.unlimPhoto.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.unlimPhoto));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                GO_TO_FILE,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над документом', () => {
            const id = normalize(TEST_RESOURCES.document.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.document));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                EDIT,
                VIEW,
                RENAME,
                MOVE,
                COPY,
                VERSIONS
            ]);
        });

        it('действия над фото из гостевой общей папки', () => {
            const id = normalize(TEST_RESOURCES.photoInsideGuestSharedFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photoInsideGuestSharedFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                GO_TO_FILE,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото из гостевой общей папки доступной только на чтение', () => {
            const id = normalize(TEST_RESOURCES.photoInsideGuestReadonlySharedFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photoInsideGuestReadonlySharedFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                SHOW_FULLSIZE,
                DOWNLOAD,
                COPY,
                ADD_TO_ALBUM,
                GO_TO_FILE,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над публичным фото из гостевой общей папки доступной только на чтение', () => {
            const id = normalize(TEST_RESOURCES.publicPhotoInsideGuestReadonlySharedFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.publicPhotoInsideGuestReadonlySharedFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                COPY,
                ADD_TO_ALBUM,
                GO_TO_FILE,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над документом из гостевой общей папки доступной только на чтение', () => {
            const id = normalize(TEST_RESOURCES.documentInsideGuestReadonlySharedFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.documentInsideGuestReadonlySharedFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                DOWNLOAD,
                VIEW,
                COPY,
                GO_TO_FILE
            ]);
        });

        it('действия над фото в корзине', () => {
            const id = normalize(TEST_RESOURCES.photoInTrash.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photoInTrash));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                DELETE_FROM_TRASH,
                RESTORE
            ]);
        });

        it('действия над файлом с народа', () => {
            const id = normalize(TEST_RESOURCES.narodFile.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.narodFile));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                COPY
            ]);
        });

        it('действия над фото из почтовых вложений отправленных как ссылки на диск', () => {
            const id = normalize(TEST_RESOURCES.mailAttachAsLinkPhoto.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.mailAttachAsLinkPhoto));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                COPY
            ]);
        });

        it('действия над фото из /attach/YaFotki (из архива Яндекс.Фоток)', () => {
            const id = normalize(TEST_RESOURCES.photoInsideYaFotki.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photoInsideYaFotki));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                FOTKI_INFO,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото из /attach/YaFotki (из архива Яндекс.Фоток) с публичной ссылкой', () => {
            const id = normalize(TEST_RESOURCES.photoInsideYaFotkiPublic.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photoInsideYaFotkiPublic));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                UNPUBLISH,
                FOTKI_INFO,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото без ссылки на данные из /attach/YaFotki (из архива Яндекс.Фоток)', () => {
            const id = normalize(TEST_RESOURCES.photoInsideYaFotkiNoDataUrl.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photoInsideYaFotkiNoDataUrl));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                TOGGLE_FAVORITES
            ]);
        });
    });

    describe('действия над несколькими ресурсами', () => {
        beforeEach(() => {
            store = createStore(defaultState);
        });

        it('действия над файлом и папкой', () => {
            const id1 = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(id1, TEST_RESOURCES.file));
            const id2 = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(id2, TEST_RESOURCES.privateFolder));

            const state = store.getState();
            const resource1 = getResourceData(state, id1);
            const resource2 = getResourceData(state, id2);
            expect(getAvailableActions([resource1, resource2], state)).toEqual([
                DELETE,
                MOVE,
                COPY
            ]);
        });

        it('действия над несколькими фото', () => {
            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photo));

            const state = store.getState();
            const resource = getResourceData(state, id);
            expect(getAvailableActions([resource, resource], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                MOVE,
                COPY,
                ADD_TO_ALBUM
            ]);
        });

        it('действия над фото и безлимитным фото', () => {
            const id1 = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id1, TEST_RESOURCES.photo));
            const id2 = normalize(TEST_RESOURCES.unlimPhoto.id);
            store.dispatch(setResourceData(id2, TEST_RESOURCES.unlimPhoto));

            const state = store.getState();
            const resource1 = getResourceData(state, id1);
            const resource2 = getResourceData(state, id2);
            expect(getAvailableActions([resource1, resource2], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                MOVE,
                COPY,
                ADD_TO_ALBUM
            ]);
        });

        it('действия над несколькими фото из /attach/YaFotki (из архива Яндекс.Фоток)', () => {
            const id = normalize(TEST_RESOURCES.photoInsideYaFotki.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photoInsideYaFotki));

            const state = store.getState();
            const resource = getResourceData(state, id);
            expect(getAvailableActions([resource, resource], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                MOVE,
                COPY,
                ADD_TO_ALBUM
            ]);
        });

        it('действия над несколькими документами', () => {
            const id = normalize(TEST_RESOURCES.document.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.document));

            const state = store.getState();
            const resource = getResourceData(state, id);
            expect(getAvailableActions([resource, resource], state)).toEqual([
                DOWNLOAD,
                DELETE,
                MOVE,
                COPY
            ]);
        });

        it('действия над несколькими почтовыми вложениями отправленными как ссылки на диск', () => {
            const id = normalize(TEST_RESOURCES.mailAttachAsLinkPhoto.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.mailAttachAsLinkPhoto));

            const state = store.getState();
            const resource = getResourceData(state, id);
            expect(getAvailableActions([resource, resource], state)).toEqual([
                DELETE,
                COPY
            ]);
        });
    });

    describe('недоступные действия над несколькими ресурсами', () => {
        beforeEach(() => {
            store = createStore(defaultState);
        });

        it('над файлом и папкой', () => {
            const id1 = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(id1, TEST_RESOURCES.file));
            const id2 = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(id2, TEST_RESOURCES.privateFolder));

            const state = store.getState();
            const resource1 = getResourceData(state, id1);
            const resource2 = getResourceData(state, id2);
            expect(getDisableReasons([resource1, resource2], state)).toEqual([]);
        });

        it('над файлом и фото', () => {
            const id1 = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(id1, TEST_RESOURCES.file));
            const id2 = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id2, TEST_RESOURCES.photo));

            const state = store.getState();
            const resource1 = getResourceData(state, id1);
            const resource2 = getResourceData(state, id2);
            expect(getDisableReasons([resource1, resource2], state)).toEqual([]);
        });

        it('над 2 фото', () => {
            const id1 = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id1, TEST_RESOURCES.photo));
            const id2 = normalize(TEST_RESOURCES.photo.id + 1);
            store.dispatch(setResourceData(id2, Object.assign({}, TEST_RESOURCES.photo, {
                id: TEST_RESOURCES.photo.id + 1
            })));

            const state = store.getState();
            const resource1 = getResourceData(state, id1);
            const resource2 = getResourceData(state, id2);
            expect(getDisableReasons([resource1, resource2], state)).toEqual([]);
        });
    });

    describe('действия в особых контекстах', () => {
        beforeEach(() => {
            store = createStore(defaultState);
        });

        it('действия над файлом в последних файлах', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/recent' } }));

            const id = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.file));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                VIEW,
                RENAME,
                MOVE,
                COPY,
                VERSIONS,
                GO_TO_FILE
            ]);
        });

        it('действия над файлом в блоке воспоминаний', () => {
            store = createStore(Object.assign({}, defaultState, {
                page: { idContext: '/remember/dc09d202-794e-4bbd-a151-4e80d85a248e' }
            }));

            const id = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.file));

            const state = store.getState();

            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                VIEW,
                RENAME,
                MOVE,
                COPY,
                VERSIONS,
                GO_TO_FILE
            ]);
        });

        it('действия над файлом в последних файлах после удаления', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/recent' } }));

            const id = normalize(TEST_RESOURCES.fileDuringDelete.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.fileDuringDelete));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([]);
        });

        it('действия над файлом в разделе "Поиск по диску"', () => {
            store = createStore(Object.assign({}, defaultState, {
                page: { idContext: '/search/fileName/disk' }
            }));

            const id = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.file));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                VIEW,
                RENAME,
                MOVE,
                COPY,
                VERSIONS,
                GO_TO_FILE
            ]);
        });

        it('действия над папкой в разделе "Поиск по диску"', () => {
            store = createStore(Object.assign({}, defaultState, {
                page: { idContext: '/search/fileName/disk' }
            }));

            const id = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.privateFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                MOVE_INTO,
                COPY,
                SHARE_ACCESS,
                GO_TO_FOLDER,
                CREATE_INSIDE
            ]);
        });

        it('действия над файлом в разделе "ссылки"', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/published' } }));

            const id = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.file));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                VIEW,
                RENAME,
                MOVE,
                COPY,
                VERSIONS,
                GO_TO_FILE
            ]);
        });

        it('действия над папкой в разделе "общие папки"', () => {
            store = createStore(Object.assign({}, defaultState, { page: { idContext: '/shared' } }));

            const id = normalize(TEST_RESOURCES.sharedOwnedFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.sharedOwnedFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                MOVE_INTO,
                COPY,
                SHARE_ACCESS,
                GO_TO_FOLDER,
                CREATE_INSIDE
            ]);
        });

        it('действия над фото в текущем альбоме', () => {
            const ALBUM_ID = '5da60da4e4d70eb354781da6';
            store = createStore(Object.assign({}, defaultState, {
                page: { idContext: '/album/' + ALBUM_ID, albumId: ALBUM_ID },
                environment: Object.assign({}, defaultState.environment, {
                    session: { experiment: { } }
                }),
                personalAlbums: {
                    albumsByIds: {
                        [ALBUM_ID]: {
                            id: ALBUM_ID
                        }
                    }
                }
            }));

            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, Object.assign({}, TEST_RESOURCES.photo, {
                albumIds: [ALBUM_ID]
            })));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                EXCLUDE_FROM_PERSONAL_ALBUM,
                GO_TO_FILE,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото из альбома вне альбома', () => {
            const ALBUM_ID = '5da60da4e4d70eb354781da6';
            store = createStore(Object.assign({}, defaultState, {
                page: { idContext: '/disk' },
                environment: Object.assign({}, defaultState.environment, {
                    session: { experiment: { } }
                }),
                personalAlbums: {
                    albumsByIds: {
                        [ALBUM_ID]: {
                            id: ALBUM_ID
                        }
                    }
                }
            }));

            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, Object.assign({}, TEST_RESOURCES.photo, {
                albumIds: [ALBUM_ID]
            })));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото в гео-альбоме', () => {
            const ALBUM_ID = 'myTestGeoAlbumId';
            store = createStore(Object.assign({}, defaultState, {
                page: { idContext: '/album/', albumId: ALBUM_ID },
                personalAlbums: {
                    albumsByIds: {
                        [ALBUM_ID]: { id: ALBUM_ID, album_type: 'geo' }
                    }
                }
            }));

            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, Object.assign({}, TEST_RESOURCES.photo, {
                albumIds: [ALBUM_ID]
            })));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                EXCLUDE_FROM_GEO_ALBUM,
                GO_TO_FILE,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото из гео-альбома в персональных альбомах', () => {
            const ALBUM_ID = '5da60da4e4d70eb354781da6';
            store = createStore(Object.assign({}, defaultState, {
                page: { idContext: '/disk' },
                personalAlbums: {
                    albumsByIds: {
                        [ALBUM_ID]: { id: ALBUM_ID, albumType: null }
                    }
                }
            }));

            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, Object.assign({}, TEST_RESOURCES.photo, {
                albumIds: [ALBUM_ID]
            })));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото из папки Сканы в Доксах', () => {
            store = createStore(Object.assign({}, defaultState, {
                page: { page: 'docs', idPage: 'scans', idContext: '/disk/Сканы' }
            }));

            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photo));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                GO_TO_FILE
            ]);
        });
    });

    describe('действия в особых окружениях', () => {
        beforeEach(() => {
            store = createStore(defaultState);
        });

        it('действия над фото на телефонах', () => {
            store = createStore(Object.assign({}, defaultState, {
                environment: Object.assign({}, defaultState.environment, {
                    agent: {
                        hasAdblock: false,
                        BrowserBase: 'Safari',
                        BrowserBaseVersion: '604.1',
                        BrowserEngine: 'WebKit',
                        BrowserEngineVersion: '604.1.38',
                        BrowserName: 'MobileSafari',
                        BrowserVersion: '11.0',
                        DeviceName: 'iPhone',
                        DeviceVendor: 'Apple',
                        MultiTouch: true,
                        OSFamily: 'iOS',
                        OSVersion: '11.0',
                        inAppBrowser: false,
                        isBrowser: true,
                        isMobile: true,
                        isTablet: false,
                        isTouch: true,
                        isSmartphone: true,
                        botSocial: false,
                        isSupported: true,
                        osId: 'ios'
                    }
                })
            }));

            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photo));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над фото на планшетах', () => {
            store = createStore(Object.assign({}, defaultState, {
                environment: Object.assign({}, defaultState.environment, {
                    agent: {
                        hasAdblock: false,
                        BrowserBase: 'Safari',
                        BrowserBaseVersion: '604.1',
                        BrowserEngine: 'WebKit',
                        BrowserEngineVersion: '604.1.34',
                        BrowserName: 'MobileSafari',
                        BrowserVersion: '11.0',
                        DeviceName: 'iPad',
                        DeviceVendor: 'Apple',
                        MultiTouch: true,
                        OSFamily: 'iOS',
                        OSVersion: '11.0',
                        inAppBrowser: false,
                        isBrowser: true,
                        isMobile: true,
                        isTablet: true,
                        isTouch: true,
                        isSmartphone: false,
                        botSocial: false,
                        isSupported: true,
                        osId: 'ios'
                    }
                })
            }));

            const id = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.photo));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                SHOW_FULLSIZE,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                COPY,
                ADD_TO_ALBUM,
                TOGGLE_FAVORITES
            ]);
        });

        it('действия над документом на телефонах', () => {
            store = createStore(Object.assign({}, defaultState, {
                environment: Object.assign({}, defaultState.environment, {
                    agent: {
                        hasAdblock: false,
                        BrowserBase: 'Safari',
                        BrowserBaseVersion: '604.1',
                        BrowserEngine: 'WebKit',
                        BrowserEngineVersion: '604.1.38',
                        BrowserName: 'MobileSafari',
                        BrowserVersion: '11.0',
                        DeviceName: 'iPhone',
                        DeviceVendor: 'Apple',
                        MultiTouch: true,
                        OSFamily: 'iOS',
                        OSVersion: '11.0',
                        inAppBrowser: false,
                        isBrowser: true,
                        isMobile: true,
                        isTablet: false,
                        isTouch: true,
                        isSmartphone: true,
                        botSocial: false,
                        isSupported: true,
                        osId: 'ios'
                    }
                })
            }));

            const id = normalize(TEST_RESOURCES.document.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.document));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                VIEW,
                RENAME,
                MOVE,
                COPY,
                VERSIONS
            ]);
        });

        it('действия над документом на планшетах', () => {
            store = createStore(Object.assign({}, defaultState, {
                environment: Object.assign({}, defaultState.environment, {
                    agent: {
                        hasAdblock: false,
                        BrowserBase: 'Safari',
                        BrowserBaseVersion: '604.1',
                        BrowserEngine: 'WebKit',
                        BrowserEngineVersion: '604.1.34',
                        BrowserName: 'MobileSafari',
                        BrowserVersion: '11.0',
                        DeviceName: 'iPad',
                        DeviceVendor: 'Apple',
                        MultiTouch: true,
                        OSFamily: 'iOS',
                        OSVersion: '11.0',
                        inAppBrowser: false,
                        isBrowser: true,
                        isMobile: true,
                        isTablet: true,
                        isTouch: true,
                        isSmartphone: false,
                        botSocial: false,
                        isSupported: true,
                        osId: 'ios'
                    }
                })
            }));

            const id = normalize(TEST_RESOURCES.document.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.document));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                EDIT,
                VIEW,
                RENAME,
                MOVE,
                COPY,
                VERSIONS
            ]);
        });

        it('действия над папкой на мобилке', () => {
            store = createStore(Object.assign({}, defaultState, {
                environment: Object.assign({}, defaultState.environment, {
                    agent: {
                        BrowserBase: 'Safari',
                        BrowserBaseVersion: '602.1',
                        BrowserEngine: 'WebKit',
                        BrowserEngineVersion: '602.1.50',
                        BrowserName: 'ChromeMobile',
                        BrowserVersion: '56.0.2924',
                        OSFamily: 'iPhone',
                        OSVersion: '10.3',
                        isBrowser: true,
                        isMobile: true,
                        isTablet: false,
                        isSmartphone: true,
                        osId: 'ios'
                    }
                })
            }));

            const id = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.privateFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                MOVE_INTO,
                COPY,
                CREATE_INSIDE
            ]);
        });

        it('действия над папкой на планшете', () => {
            store = createStore(Object.assign({}, defaultState, {
                environment: Object.assign({}, defaultState.environment, {
                    agent: {
                        BrowserBase: 'Safari',
                        BrowserBaseVersion: '601.1',
                        BrowserEngine: 'WebKit',
                        BrowserEngineVersion: '601.1.46',
                        BrowserName: 'MobileSafari',
                        BrowserVersion: '9.0',
                        OSFamily: 'iOS',
                        OSVersion: '9.1',
                        isBrowser: true,
                        isMobile: true,
                        isTablet: true,
                        isSmartphone: false,
                        osId: 'ios'
                    }
                })
            }));

            const id = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.privateFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                MOVE_INTO,
                COPY,
                CREATE_INSIDE
            ]);
        });

        it('действия над папкой в разделе "общие папки" на мобилке', () => {
            store = createStore(Object.assign({}, defaultState, {
                page: { idContext: '/shared' },
                environment: Object.assign({}, defaultState.environment, {
                    agent: {
                        BrowserBase: 'Safari',
                        BrowserBaseVersion: '601.1',
                        BrowserEngine: 'WebKit',
                        BrowserEngineVersion: '601.1.46',
                        BrowserName: 'MobileSafari',
                        BrowserVersion: '9.0',
                        OSFamily: 'iOS',
                        OSVersion: '9.1',
                        isBrowser: true,
                        isMobile: true,
                        isTablet: false,
                        isSmartphone: true,
                        osId: 'ios'
                    }
                })
            }));

            const id = normalize(TEST_RESOURCES.sharedOwnedFolder.id);
            store.dispatch(setResourceData(id, TEST_RESOURCES.sharedOwnedFolder));

            const state = store.getState();
            expect(getAvailableActions([getResourceData(state, id)], state)).toEqual([
                PUBLISH,
                DOWNLOAD,
                DELETE,
                RENAME,
                MOVE,
                MOVE_INTO,
                COPY,
                GO_TO_FOLDER,
                CREATE_INSIDE
            ]);
        });
    });

    describe('перенос ресурсов в папку', () => {
        beforeEach(() => {
            store = createStore(defaultState);
        });

        it('в обычную папку - пачка обычных ресурсов', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const photoId = normalize(TEST_RESOURCES.photo.id);
            store.dispatch(setResourceData(photoId, TEST_RESOURCES.photo));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            const photo = getResourceData(state, photoId);
            expect(can(MOVE_INTO, folder, state, [file, photo])).toEqual(true);
        });

        it('в обычную папку - пачка обычных ресурсов, в том числе сама папка', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            expect(can(MOVE_INTO, folder, state, [file, folder])).toEqual(false);
        });

        it('в обычную папку - пачка обычных ресурсов, в том числе вложенная в эту папку папка', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const folderInFolderId = normalize(TEST_RESOURCES.folder + '/folder');

            store.dispatch(setResourceData(folderInFolderId, Object.assign({}, TEST_RESOURCES.folder, {
                id: folderInFolderId
            })));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            const folderInFolder = getResourceData(state, folderInFolderId);
            expect(can(MOVE_INTO, folder, state, [file, folderInFolder]))
                .toEqual(true);
        });

        it('в обычную папку - пачка обычных ресурсов, в том родительская папка', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const folderInFolderId = normalize(TEST_RESOURCES.folder + '/folder');

            store.dispatch(setResourceData(folderInFolderId, Object.assign({}, TEST_RESOURCES.folder, {
                id: folderInFolderId
            })));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            const folderInFolder = getResourceData(state, folderInFolderId);
            expect(can(MOVE_INTO, folderInFolder, state, [file, folder]))
                .toEqual(false);
        });

        it('в обычную папку - пачка ресурсов, содержащих общую папку', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const sharedFolderId = normalize(TEST_RESOURCES.sharedOwnedFolder.id);
            store.dispatch(setResourceData(sharedFolderId, TEST_RESOURCES.sharedOwnedFolder));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            const sharedFolder = getResourceData(state, sharedFolderId);
            expect(can(MOVE_INTO, folder, state, [file, sharedFolder]))
                .toEqual(true);
        });

        it('в свою общую папку - пачка обычных ресурсов', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const sharedFolderId = normalize(TEST_RESOURCES.sharedOwnedFolder.id);
            store.dispatch(setResourceData(sharedFolderId, TEST_RESOURCES.sharedOwnedFolder));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            const sharedFolder = getResourceData(state, sharedFolderId);
            expect(can(MOVE_INTO, sharedFolder, state, [file, folder]))
                .toEqual(true);
        });

        it('в свою общую папку - пачка ресурсов, содержащих общую папку', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const sharedFolderId = normalize(TEST_RESOURCES.sharedOwnedFolder.id);
            store.dispatch(setResourceData(sharedFolderId, TEST_RESOURCES.sharedOwnedFolder));

            const sharedFolderId2 = normalize(

                TEST_RESOURCES.sharedOwnedFolder.id + '2');
            store.dispatch(setResourceData(sharedFolderId2, Object.assign({}, TEST_RESOURCES.sharedOwnedFolder, {
                id: sharedFolderId2
            })));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            const sharedFolder = getResourceData(state, sharedFolderId);
            const sharedFolder2 = getResourceData(state, sharedFolderId2);
            expect(
                can(
                    MOVE_INTO, sharedFolder, state,
                    [file, folder, sharedFolder2]
                )
            ).toEqual(false);
        });

        it('в гостевую общую папку - пачка обычных ресурсов', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const sharedGuestFolderId = normalize(TEST_RESOURCES.sharedGuestFolder.id);
            store.dispatch(setResourceData(sharedGuestFolderId, TEST_RESOURCES.sharedGuestFolder));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            const sharedGuestFolder = getResourceData(state, sharedGuestFolderId);
            expect(
                can(MOVE_INTO, sharedGuestFolder, state, [file, folder])
            ).toEqual(true);
        });

        it('в гостевую RO-общую папку - пачка обычных ресурсов', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const sharedGuestReadonlyFolderId = normalize(TEST_RESOURCES.sharedGuestReadonlyFolder.id);
            store.dispatch(setResourceData(sharedGuestReadonlyFolderId, TEST_RESOURCES.sharedGuestReadonlyFolder));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            const sharedGuestReadonlyFolder = getResourceData(state, sharedGuestReadonlyFolderId);
            expect(
                can(MOVE_INTO, sharedGuestReadonlyFolder, state, [file, folder])
            ).toEqual(false);
        });

        it('в гостевую общую папку - пачка ресурсов, содержащих общую папку', () => {
            const folderId = normalize(TEST_RESOURCES.privateFolder.id);
            store.dispatch(setResourceData(folderId, TEST_RESOURCES.privateFolder));

            const fileId = normalize(TEST_RESOURCES.file.id);
            store.dispatch(setResourceData(fileId, TEST_RESOURCES.file));

            const sharedFolderId = normalize(TEST_RESOURCES.sharedOwnedFolder.id);
            store.dispatch(setResourceData(sharedFolderId, TEST_RESOURCES.sharedOwnedFolder));

            const sharedGuestFolderId = normalize(TEST_RESOURCES.sharedGuestFolder.id);
            store.dispatch(setResourceData(sharedGuestFolderId, TEST_RESOURCES.sharedGuestFolder));

            const state = store.getState();
            const folder = getResourceData(state, folderId);
            const file = getResourceData(state, fileId);
            const sharedFolder = getResourceData(state, sharedFolderId);
            const sharedGuestFolder = getResourceData(state, sharedGuestFolderId);
            expect(
                can(
                    MOVE_INTO, sharedGuestFolder, state,
                    [file, folder, sharedFolder]
                )
            ).toEqual(false);
        });

        it('одну общую папку в другую общую папку', () => {
            const sharedFolderId = normalize(TEST_RESOURCES.sharedOwnedFolder.id);
            store.dispatch(setResourceData(sharedFolderId, TEST_RESOURCES.sharedOwnedFolder));

            const sharedGuestFolderId = normalize(TEST_RESOURCES.sharedGuestFolder.id);
            store.dispatch(setResourceData(sharedGuestFolderId, TEST_RESOURCES.sharedGuestFolder));

            const state = store.getState();
            const sharedFolder = getResourceData(state, sharedFolderId);
            const sharedGuestFolder = getResourceData(state, sharedGuestFolderId);
            expect(can(MOVE_INTO, sharedFolder, state, [sharedGuestFolder])).toEqual(false);
            expect(can(MOVE_INTO, sharedGuestFolder, state, [sharedFolder])).toEqual(false);
        });
    });

    describe('батчевое обновление ресурсов', () => {
        beforeEach(() => {
            store = createStore(defaultState);
        });

        it('должен создать ресурсы в сторе', () => {
            expect(store.getState().resources).toEqual({});

            store.dispatch(batchSetResourceData([
                ['/resource1', { id: '/resource1' }],
                ['/resource2', { id: '/resource2' }]
            ]));

            expect(Object.keys(store.getState().resources)).toEqual(['/resource1', '/resource2']);
        });

        it('должен присвоить значение ресурсам по ключу `field`', () => {
            store.dispatch(batchSetResourceData([
                ['/resource1', { id: '/resource1' }],
                ['/resource2', { id: '/resource2' }]
            ]));

            store.dispatch(batchSetResourceData([
                ['/resource1', { data: 'data' }, 'field'],
                ['/resource2', { data: 'data' }, 'field']
            ]));

            ['/resource1', '/resource2'].forEach((resourceId) => {
                const resource = store.getState().resources[resourceId];
                expect(resource.field).toEqual({ data: 'data' });
            });
        });
    });

    describe('редактирование', () => {
        const getStoreFor = (editor) => createStore(merge({}, defaultState, {
            environment: { agent: {
                isSmartphone: true
            } },
            user: { officeEditorType: editor }
        }));

        const editors = [{
            title: 'MS Office',
            editor: 'microsoft_online',
            result: false
        }, {
            title: 'Only Office',
            editor: 'only_office',
            result: true
        }];

        editors.forEach(({ title, editor, result }) => {
            it(`в ${title} на смартфоне ${result ? 'разрешено' : 'запрещено'}`, () => {
                const store = getStoreFor(editor);
                const id = normalize(TEST_RESOURCES.document.id);

                store.dispatch(setResourceData(id, TEST_RESOURCES.document));

                const state = store.getState();
                const resource = getResourceData(state, id);

                expect(can(EDIT, resource, state)).toBe(result);
            });
        });
    });
});
