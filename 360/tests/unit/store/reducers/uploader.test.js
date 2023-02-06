import '../../noscript';
import uploader from '../../../../components/redux/store/reducers/uploader';
import {
    START_UPLOAD,
    MOVE_CLONED_RESOURCES,
    DESTROY_RESOURCE,
    HIDE_UPLOADER,
    INIT_UPLOADING_STATES,
    UPDATE_UPLOADING_PROGRESS,
    HIDE_UPLOAD_NOTIFICATION,
    UPDATE_UPLOADING_STATE
} from '../../../../components/redux/store/actions/types';
import deepFreeze from 'deep-freeze';
import { UPLOAD_STATUSES, UPLOAD_ERRORS, UPLOAD_NOTIFICATION_TYPES } from '../../../../components/helpers/upload';
import _ from 'lodash';

describe('uploader reducer', () => {
    describe('HIDE_UPLOADER', () => {
        it('должен скрывать загрузчик и очищать листинг загрузчика', () => {
            const state = {
                visible: true,
                opened: true,
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg'],
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.DONE },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.DONE }
                },
                totalUploadingSize: 100,
                totalUploadedSize: 100,
            };
            deepFreeze(state);
            const newState = uploader(state, { type: HIDE_UPLOADER });

            expect(newState).toEqual({
                visible: false,
                opened: false,
                resourcesToUpload: [],
                uploadingStatesByIds: { },
                totalUploadingSize: 0,
                totalUploadedSize: 0,
            });
        });
    });

    describe('START_UPLOAD', () => {
        it('По умолчанию должен добавлять ресурсы в начало', () => {
            const state = {
                totalUploadingSize: 300,
                totalUploadedSize: 30,
                resourcesToUpload: ['/disk/1', '/disk/2', '/disk/3'],
                uploadingStatesByIds: {
                    '/disk/1': { status: UPLOAD_STATUSES.DONE },
                    '/disk/2': { status: UPLOAD_STATUSES.DONE },
                    '/disk/3': { status: UPLOAD_STATUSES.DONE }
                },
                notofications: []
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: START_UPLOAD,
                payload: {
                    states: [
                        { id: '/disk/4.jpg', status: UPLOAD_STATUSES.ENQUEUED, size: 100 },
                        { id: '/disk/5.jpg', status: UPLOAD_STATUSES.ENQUEUED, size: 100 }
                    ]
                }
            });
            expect(newState.resourcesToUpload).toEqual(['/disk/4.jpg', '/disk/5.jpg', '/disk/1', '/disk/2', '/disk/3']);
            expect(newState.uploadingStatesByIds['/disk/4.jpg']).toEqual({ status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 });
            expect(newState.uploadingStatesByIds['/disk/5.jpg']).toEqual({ status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 });
            expect(newState.totalUploadingSize).toBe(500);
            expect(newState.totalUploadedSize).toBe(30);
            expect(newState.notofications).toEqual([]);
        });

        it('Должен добавлять ресурсы в конкретную позицию', () => {
            const state = {
                totalUploadingSize: 300,
                totalUploadedSize: 30,
                resourcesToUpload: ['/disk/1', '/disk/2', '/disk/3'],
                uploadingStatesByIds: {
                    '/disk/1': { status: UPLOAD_STATUSES.UPLOADING, total: 100, loaded: 10 },
                    '/disk/2': { status: UPLOAD_STATUSES.UPLOADING, total: 100, loaded: 10 },
                    '/disk/3': { status: UPLOAD_STATUSES.UPLOADING, total: 100, loaded: 10 }
                }
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: START_UPLOAD,
                payload: {
                    position: 1,
                    states: [
                        { id: '/disk/4.jpg', status: UPLOAD_STATUSES.ENQUEUED, size: 100 },
                        { id: '/disk/5.jpg', status: UPLOAD_STATUSES.ENQUEUED, size: 100 }
                    ]
                }
            });
            expect(newState.resourcesToUpload).toEqual(['/disk/1', '/disk/4.jpg', '/disk/5.jpg', '/disk/2', '/disk/3']);
            expect(newState.uploadingStatesByIds['/disk/4.jpg']).toEqual({ status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 });
            expect(newState.uploadingStatesByIds['/disk/5.jpg']).toEqual({ status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 });
            expect(newState.totalUploadingSize).toBe(500);
            expect(newState.totalUploadedSize).toBe(30);
        });

        it('Должен удалять дубли', () => {
            const state = {
                totalUploadingSize: 0,
                totalUploadedSize: 0,
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg', '/disk/3.jpg'],
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.DONE, total: 100, loaded: 100 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.DONE, total: 100, loaded: 100 },
                    '/disk/3.jpg': { status: UPLOAD_STATUSES.DONE, total: 100, loaded: 100 }
                }
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: START_UPLOAD,
                payload: {
                    states: [
                        { id: '/disk/2.jpg', status: UPLOAD_STATUSES.ENQUEUED, size: 100 }
                    ]
                }
            });
            expect(newState.resourcesToUpload).toEqual(['/disk/2.jpg', '/disk/1.jpg', '/disk/3.jpg']);
            expect(newState.uploadingStatesByIds['/disk/2.jpg']).toEqual({ status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 });
            expect(newState.totalUploadingSize).toBe(100);
            expect(newState.totalUploadedSize).toBe(0);
        });
    });

    describe('MOVE_CLONED_RESOURCES', () => {
        it('должен заменять ресурс, если такой есть среди загружамых', () => {
            const state = {
                visible: true,
                opened: true,
                resourcesToUpload: ['/disk/1', '/disk/2', '/disk/3'],
                uploadingStatesByIds: {
                    '/disk/1': { status: UPLOAD_STATUSES.DONE },
                    '/disk/2': { status: UPLOAD_STATUSES.DONE },
                    '/disk/3': { status: UPLOAD_STATUSES.DONE }
                }
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/2' },
                        dst: { id: '/disk/4' }
                    }]
                }
            });

            expect(newState.resourcesToUpload).toEqual(['/disk/1', '/disk/4', '/disk/3']);
            expect(newState.uploadingStatesByIds).toEqual({
                '/disk/1': { status: UPLOAD_STATUSES.DONE },
                '/disk/4': { status: UPLOAD_STATUSES.DONE },
                '/disk/3': { status: UPLOAD_STATUSES.DONE }
            });
        });

        it('не должен заменять ресурс, если такого нет среди загружаемых', () => {
            const state = {
                visible: true,
                opened: true,
                resourcesToUpload: ['/disk/1', '/disk/2'],
                uploadingStatesByIds: {
                    '/disk/1': { status: UPLOAD_STATUSES.DONE },
                    '/disk/2': { status: UPLOAD_STATUSES.DONE }
                }
            };
            deepFreeze(state);
            const newState = uploader(state, {
                visible: true,
                opened: true,
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/3' },
                        dst: { id: '/disk/4' }
                    }]
                }
            });

            expect(newState.resourcesToUpload).toEqual(['/disk/1', '/disk/2']);
            expect(newState.uploadingStatesByIds).toEqual({
                '/disk/1': { status: UPLOAD_STATUSES.DONE },
                '/disk/2': { status: UPLOAD_STATUSES.DONE }
            });
        });

        it('должен скрывать загрузчик, при удалении последнего ресурса', () => {
            const state = {
                visible: true,
                opened: true,
                notifications: [],
                resourcesToUpload: ['/disk/1'],
                uploadingStatesByIds: {
                    '/disk/1': { status: UPLOAD_STATUSES.DONE }
                }
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/1' },
                        dst: { id: '/trash/1', isInTrash: true }
                    }]
                }
            });

            expect(newState.resourcesToUpload).toEqual([]);
            expect(newState.visible).toBe(false);
        });

        it('должен скрывать нотификацию для ресурса перемещённого в корзину', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 100,
                totalUploadedSize: 0,
                uploadingStatus: UPLOAD_STATUSES.ERROR,
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg'],
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.CONFLICT, loaded: 0, total: 100 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.DONE, loaded: 100, total: 100 }
                },
                notifications: [{
                    id: '/disk/1.jpg',
                    type: 'service',
                    data: { resourceId: '/disk/1.jpg', reason: UPLOAD_ERRORS.CONFLICT }
                }]
            };

            deepFreeze(state);
            const newState = uploader(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    resources: [{
                        src: { id: '/disk/1.jpg' },
                        dst: { id: '/trash/1.jpg', isInTrash: true }
                    }]
                }
            });

            expect(newState.resourcesToUpload).toEqual(['/disk/2.jpg']);
            expect(newState.notifications).toEqual([]);
            expect(_.pick(newState, ['totalUploadingSize', 'totalUploadedSize', 'uploadingStatus', 'visible', 'opened']))
                .toEqual({
                    visible: true,
                    opened: true,
                    totalUploadingSize: 0,
                    totalUploadedSize: 0,
                    uploadingStatus: UPLOAD_STATUSES.DONE,
                });
        });
    });

    describe('DESTROY_RESOURCE', () => {
        it('должен удалить ресурс, если такой есть среди загружамых', () => {
            const state = {
                visible: true,
                opened: true,
                notifications: [],
                resourcesToUpload: ['/disk/1', '/disk/2', '/disk/3'],
                uploadingStatesByIds: {
                    '/disk/1': { status: UPLOAD_STATUSES.DONE },
                    '/disk/2': { status: UPLOAD_STATUSES.DONE },
                    '/disk/3': { status: UPLOAD_STATUSES.DONE }
                }
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: DESTROY_RESOURCE,
                payload: { id: '/disk/2' }
            });

            expect(newState.resourcesToUpload).toEqual(['/disk/1', '/disk/3']);
            expect(newState.uploadingStatesByIds['/disk/2']).toBeUndefined();
        });

        it('не должен удалять ничего, если ресурса нет среди загружаемых', () => {
            const state = {
                visible: true,
                opened: true,
                notifications: [],
                resourcesToUpload: ['/disk/1', '/disk/2'],
                uploadingStatesByIds: {
                    '/disk/1': { status: UPLOAD_STATUSES.DONE },
                    '/disk/2': { status: UPLOAD_STATUSES.DONE }
                }
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: DESTROY_RESOURCE,
                payload: { id: '/disk/3' }
            });

            expect(newState.resourcesToUpload).toEqual(['/disk/1', '/disk/2']);
        });

        it('при удалении последнего ресурса - должен закрыть загрузчик', () => {
            const state = {
                visible: true,
                opened: true,
                notifications: [],
                resourcesToUpload: ['/disk/1'],
                uploadingStatesByIds: {
                    '/disk/1': { status: UPLOAD_STATUSES.DONE }
                }
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: DESTROY_RESOURCE,
                payload: { id: '/disk/1' }
            });

            expect(newState.resourcesToUpload).toEqual([]);
            expect(newState.uploadingStatesByIds).toEqual({});
            expect(newState.visible).toBe(false);
            expect(newState.opened).toBe(false);
        });

        it('должен отменять загрузку дочерних ресурсов', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 200,
                totalUploadedSize: 100,
                notifications: [],
                resourcesToUpload: ['/disk/file', '/disk/folder'],
                uploadingStatesByIds: {
                    '/disk/file': { loaded: 50, total: 100, status: UPLOAD_STATUSES.UPLOADING },
                    '/disk/folder': { loaded: 50, total: 100, status: UPLOAD_STATUSES.UPLOADING, isDirectory: true },
                    '/disk/folder/1': { loaded: 50, total: 100, status: UPLOAD_STATUSES.UPLOADING, rootFolderId: '/disk/folder' }
                }
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: DESTROY_RESOURCE,
                payload: { id: '/disk/folder' }
            });

            expect(newState.resourcesToUpload).toEqual(['/disk/file']);
            expect(newState.uploadingStatesByIds['/disk/folder'].status).toBe(UPLOAD_STATUSES.CANCELED);
            expect(newState.uploadingStatesByIds['/disk/folder/1'].status).toBe(UPLOAD_STATUSES.CANCELED);

            expect(newState.totalUploadingSize).toEqual(100);
            expect(newState.totalUploadedSize).toEqual(50);
        });

        it('должен скрывать нотификацию для удалённого ресурса', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 200,
                totalUploadedSize: 100,
                uploadingStatus: UPLOAD_STATUSES.ERROR,
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg'],
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.CONFLICT, loaded: 0, total: 100 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.DONE, loaded: 100, total: 100 }
                },
                notifications: [{
                    id: '/disk/1.jpg',
                    type: 'service',
                    data: { resourceId: '/disk/1.jpg', reason: UPLOAD_ERRORS.CONFLICT }
                }]
            };

            deepFreeze(state);
            const newState = uploader(state, {
                type: DESTROY_RESOURCE,
                payload: { id: '/disk/1.jpg' }
            });

            expect(newState.resourcesToUpload).toEqual(['/disk/2.jpg']);
            expect(newState.uploadingStatesByIds['/disk/1.jpg'].status).toBe(UPLOAD_STATUSES.CANCELED);
            expect(newState.notifications).toEqual([]);
            expect(_.pick(newState, ['totalUploadingSize', 'totalUploadedSize', 'uploadingStatus', 'visible', 'opened']))
                .toEqual({
                    visible: true,
                    opened: true,
                    totalUploadingSize: 0,
                    totalUploadedSize: 0,
                    uploadingStatus: UPLOAD_STATUSES.DONE,
                });
        });
    });

    describe('INIT_UPLOADING_STATES', () => {
        it('Должен иниициализировать ошибкой entry которую не удалось прочитать', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 0,
                totalUploadedSize: 0,
                uploadingStatesByIds: {
                    '/disk/folder': {
                        isDirectory: true,
                        status: UPLOAD_STATUSES.CREATING_FOLDER,
                        total: 0,
                        loaded: 0,
                        files: 0,
                        folders: 0,
                        loadedFiles: 0,
                        loadedFolders: 0,
                        filesErrors: 0,
                        foldersErrors: 0,
                        retriableErrors: 0
                    }
                },
                resourcesToUpload: ['/disk/folder']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: INIT_UPLOADING_STATES,
                payload: {
                    rootFolderId: '/disk/folder',
                    states: [
                        { id: '/disk/folder/subfolder', isDirectory: true, size: 0 },
                        { id: '/disk/folder/subfolder/file.jpg', isDirectory: false, size: 20 },
                        { id: '/disk/folder/subfolder2', isDirectory: true, readFail: true, size: 0 }
                    ]
                }
            });
            expect(newState.uploadingStatesByIds['/disk/folder']).toEqual({
                isDirectory: true,
                status: UPLOAD_STATUSES.CREATING_FOLDER,
                total: 20,
                loaded: 0,
                files: 1,
                folders: 2,
                loadedFiles: 0,
                loadedFolders: 0,
                filesErrors: 0,
                foldersErrors: 1,
                retriableErrors: 0
            });
            expect(newState.uploadingStatesByIds['/disk/folder/subfolder2']).toEqual({
                rootFolderId: '/disk/folder',
                loaded: 0,
                total: 0,
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.ENTRY_READ_FAIL
            });
            expect(newState.totalUploadingSize).toBe(20);
        });
    });

    describe('UPDATE_UPLOADING_PROGRESS', () => {
        it('должен обновить прогресс ресурса и общий прогресс', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 1100,
                totalUploadedSize: 100,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 200, loaded: 100 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 400, loaded: 0 },
                    '/disk/3.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 500, loaded: 0 }
                },
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg', '/disk/3.jpg']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_PROGRESS,
                payload: { id: '/disk/1.jpg', loaded: 120 }
            });
            expect(newState.uploadingStatesByIds['/disk/1.jpg'].loaded).toBe(120);
            expect(newState.totalUploadedSize).toBe(120);
        });

        it('Должен обновить прогресс ресурса и корневой папки', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 900,
                totalUploadedSize: 100,
                uploadingStatesByIds: {
                    '/disk/folder': { status: UPLOAD_STATUSES.UPLOADING, total: 900, loaded: 100 },
                    '/disk/subfolder': { status: UPLOAD_STATUSES.DONE },
                    '/disk/folder/subfolder/1.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 400, loaded: 100, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder/2.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 500, loaded: 0 }
                },
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg', '/disk/3.jpg']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_PROGRESS,
                payload: { id: '/disk/folder/subfolder/1.jpg', loaded: 120 }
            });
            expect(newState.uploadingStatesByIds['/disk/folder/subfolder/1.jpg'].loaded).toBe(120);
            expect(newState.uploadingStatesByIds['/disk/folder'].loaded).toBe(120);
            expect(newState.totalUploadedSize).toBe(120);
        });

        it('Не должен обновлять прогресс ресурса из папки и общий прогресс, если загрузка папки, в которой находится этот ресурс отменена', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 400,
                totalUploadedSize: 100,
                uploadingStatesByIds: {
                    '/disk/folder': { status: UPLOAD_STATUSES.CANCELED, total: 400, loaded: 100 },
                    '/disk/folder/1.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 400, loaded: 100, rootFolderId: '/disk/folder' },
                },
                resourcesToUpload: ['/disk/folder']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_PROGRESS,
                payload: { id: '/disk/folder/1.jpg', loaded: 120 }
            });
            expect(newState.uploadingStatesByIds['/disk/folder/1.jpg'].loaded).toBe(100);
            expect(newState.totalUploadedSize).toBe(100);
        });

        it('Не должен обновлять прогресс ресурса и общий прогресс, если загрузка ресурса была отменена', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 400,
                totalUploadedSize: 100,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.CANCELED, total: 400, loaded: 100 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 400, loaded: 100 },
                },
                resourcesToUpload: ['/disk/2.jpg']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_PROGRESS,
                payload: { id: '/disk/1.jpg', loaded: 120, total: 400 }
            });
            expect(newState.uploadingStatesByIds['/disk/1.jpg'].loaded).toBe(100);
            expect(newState.totalUploadedSize).toBe(100);
        });

        it('Не должен обновлять прогресс ресурса и общий прогресс, если ресурс был удалён', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 400,
                totalUploadedSize: 100,
                uploadingStatesByIds: {
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 400, loaded: 100 }
                },
                resourcesToUpload: ['/disk/2.jpg']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_PROGRESS,
                payload: { id: '/disk/1.jpg', loaded: 120, total: 400 }
            });
            expect(newState.uploadingStatesByIds['/disk/1.jpg']).toBeUndefined();
            expect(newState.totalUploadedSize).toBe(100);
        });

        it('Должен переводить в статус UPLOADED, если со стороны клиента всё было отправлено', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 400,
                totalUploadedSize: 200,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 400, loaded: 200 }
                },
                resourcesToUpload: ['/disk/1.jpg']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_PROGRESS,
                payload: { id: '/disk/1.jpg', loaded: 400, total: 400 }
            });
            expect(newState.uploadingStatesByIds['/disk/1.jpg'].status).toBe(UPLOAD_STATUSES.UPLOADED);
        });

        it('Не должен переводить в статус UPLOADED, если ресурс уже находится в статусе DONE', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 400,
                totalUploadedSize: 400,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.DONE, total: 400, loaded: 400 }
                },
                resourcesToUpload: ['/disk/1.jpg']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_PROGRESS,
                payload: { id: '/disk/1.jpg', loaded: 400, total: 400 }
            });
            expect(newState.uploadingStatesByIds['/disk/1.jpg'].status).toBe(UPLOAD_STATUSES.DONE);
        });
    });

    describe('UPDATE_UPLOADING_STATE', () => {
        it('должен обновлять статус ресурса', () => {
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 100,
                totalUploadedSize: 0,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 }
                },
                resourcesToUpload: ['/disk/1.jpg']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/1.jpg', status: UPLOAD_STATUSES.GETTING_URL }
            });
            expect(newState.uploadingStatesByIds['/disk/1.jpg']).toEqual({
                status: UPLOAD_STATUSES.GETTING_URL,
                total: 100,
                loaded: 0
            });
            expect(newState.uploadingStatus).toBe(UPLOAD_STATUSES.UPLOADING);
        });

        it('должен обновлять несколько статусов', () => {
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 100,
                totalUploadedSize: 0,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 }
                },
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: [
                    { id: '/disk/1.jpg', status: UPLOAD_STATUSES.GETTING_URL },
                    { id: '/disk/2.jpg', status: UPLOAD_STATUSES.GETTING_URL }
                ]
            });
            expect(newState.uploadingStatesByIds['/disk/1.jpg'].status).toBe(UPLOAD_STATUSES.GETTING_URL);
            expect(newState.uploadingStatesByIds['/disk/2.jpg'].status).toBe(UPLOAD_STATUSES.GETTING_URL);
        });

        it('должен обновить общий статус загрузки на DONE если все ресурсы перешли в DONE', () => {
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 200,
                totalUploadedSize: 200,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.DONE, total: 100, loaded: 200 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.PROCESSING, total: 100, loaded: 100 }
                },
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg'],
                notifications: []
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/2.jpg', status: UPLOAD_STATUSES.DONE }
            });

            expect(newState.uploadingStatesByIds['/disk/2.jpg']).toEqual({
                status: UPLOAD_STATUSES.DONE,
                total: 100,
                loaded: 100
            });
            expect(newState.uploadingStatus).toBe(UPLOAD_STATUSES.DONE);
        });

        it('должен обновить общий статус загрузки на ERROR если часть ресурсов в DONE а остальные в ERROR', () => {
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 200,
                totalUploadedSize: 200,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.DONE, total: 100, loaded: 200 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.PROCESSING, total: 100, loaded: 100 }
                },
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg'],
                notifications: []
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/2.jpg', status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.UNKNOWN }
            });

            expect(newState.uploadingStatesByIds['/disk/2.jpg']).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.UNKNOWN,
                total: 100,
                loaded: 100
            });
            expect(newState.uploadingStatus).toBe(UPLOAD_STATUSES.ERROR);
        });

        it('должен показать нотификацию в случае ошибки INSUFFICIENT_STORAGE', () => {
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 200,
                totalUploadedSize: 0,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 }
                },
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg'],
                notifications: []
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/1.jpg', status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }
            });
            expect(newState.uploadingStatesByIds['/disk/1.jpg']).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE,
                total: 100,
                loaded: 0
            });
            expect(newState.uploadingStatus).toBe(UPLOAD_STATUSES.UPLOADING);
            expect(newState.notifications).toEqual([{
                id: '/disk/1.jpg',
                type: 'service',
                data: { resourceId: '/disk/1.jpg', reason: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }
            }]);
            expect(newState.insufficientStorageNotificationShown).toBe(true);
        });

        it('не должен показать нотификацию INSUFFICIENT_STORAGE, если она уже была показана', () => {
            let state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 100,
                totalUploadedSize: 0,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 },
                },
                resourcesToUpload: ['/disk/1.jpg'],
                notifications: []
            };
            state = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/1.jpg', status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }
            });
            expect(state.notifications).toEqual([{
                id: '/disk/1.jpg',
                type: 'service',
                data: { resourceId: '/disk/1.jpg', reason: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }
            }]);
            state = uploader(state, {
                type: HIDE_UPLOAD_NOTIFICATION,
                payload: '/disk/1.jpg'
            });
            expect(state.notifications).toEqual([]);
            expect(state.insufficientStorageNotificationShown).toBe(true);
            state = Object.assign({}, state, {
                uploadingStatesByIds: Object.assign({}, state.uploadingStatesByIds, {
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 },
                })
            });
            state = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/2.jpg', status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }
            });
            expect(state.notifications).toEqual([]);
        });

        it('должен показать только одну нотификацию в случае нескольких ошибок INSUFFICIENT_STORAGE', () => {
            const pics = ['/disk/1.jpg', '/disk/2.jpg'];
            const uploadingStatesByIds = pics.reduce((state, pic) =>
                Object.assign(state, { [pic]: { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 } }), {});
            const action = {
                type: UPDATE_UPLOADING_STATE,
                payload: pics.map((pic) => ({ id: pic, status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }))
            };
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 200,
                totalUploadedSize: 0,
                uploadingStatesByIds,
                resourcesToUpload: pics,
                notifications: []
            };
            deepFreeze(state);
            const newState = uploader(state, action);
            pics.forEach((pic) => {
                expect(newState.uploadingStatesByIds[pic]).toEqual({
                    status: UPLOAD_STATUSES.ERROR,
                    error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE,
                    total: 100,
                    loaded: 0
                });
            });
            expect(newState.uploadingStatus).toBe(UPLOAD_STATUSES.ERROR);
            expect(newState.notifications).toEqual([{
                id: '/disk/1.jpg',
                type: 'service',
                data: { resourceId: '/disk/1.jpg', reason: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }
            }]);
        });

        it('должен показать только одну нотификацию в случае нескольких ошибок INSUFFICIENT_STORAGE', () => {
            const pics = ['/disk/1.jpg', '/disk/2.jpg'];
            const uploadingStatesByIds = pics.reduce((state, pic) =>
                Object.assign(state, { [pic]: { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 } }), {});
            const action = {
                type: UPDATE_UPLOADING_STATE,
                payload: pics.map((pic) => ({ id: pic, status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }))
            };
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 200,
                totalUploadedSize: 0,
                uploadingStatesByIds,
                resourcesToUpload: pics,
                notifications: []
            };
            deepFreeze(state);
            const newState = uploader(state, action);
            pics.forEach((pic) => {
                expect(newState.uploadingStatesByIds[pic]).toEqual({
                    status: UPLOAD_STATUSES.ERROR,
                    error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE,
                    total: 100,
                    loaded: 0
                });
            });
            expect(newState.uploadingStatus).toBe(UPLOAD_STATUSES.ERROR);
            expect(newState.notifications).toEqual([{
                id: '/disk/1.jpg',
                type: 'service',
                data: { resourceId: '/disk/1.jpg', reason: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }
            }]);
        });

        it('должен показать нотификацию при конфликте', () => {
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 200,
                totalUploadedSize: 0,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 }
                },
                resourcesToUpload: ['/disk/1.jpg'],
                notifications: []
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/1.jpg', status: UPLOAD_STATUSES.CONFLICT }
            });
            expect(newState.uploadingStatesByIds['/disk/1.jpg']).toEqual({
                status: UPLOAD_STATUSES.CONFLICT,
                total: 100,
                loaded: 0
            });
            expect(newState.uploadingStatus).toBe(UPLOAD_STATUSES.ERROR);
            expect(newState.notifications).toEqual([{
                id: '/disk/1.jpg',
                type: 'service',
                data: { resourceId: '/disk/1.jpg', reason: UPLOAD_ERRORS.CONFLICT }
            }]);
        });

        it('Если папка не создалась, должен перевести всех её потомков в состояние ошибки', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 300,
                totalUploadedSize: 0,
                uploadingStatesByIds: {
                    '/disk/folder': { status: UPLOAD_STATUSES.UPLOADING, total: 300, loaded: 0, files: 3, folders: 2, loadedFiles: 0, loadedFolders: 0, filesErrors: 0, foldersErrors: 0, retriableErrors: 0, isDirectory: true },
                    '/disk/folder/file.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 100, loaded: 0, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder': { status: UPLOAD_STATUSES.CREATING_FOLDER, rootFolderId: '/disk/folder', isDirectory: true },
                    '/disk/folder/subfolder/1.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder/subfolder2': { status: UPLOAD_STATUSES.ENQUEUED, rootFolderId: '/disk/folder', isDirectory: true },
                    '/disk/folder/subfolder/subfolder2/2.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 100, loaded: 0, rootFolderId: '/disk/folder' }
                },
                resourcesToUpload: ['/disk/folder']
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/folder/subfolder', status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.FOLDER_NOT_CREATED }
            });

            expect(newState.uploadingStatesByIds['/disk/folder']).toEqual({
                total: 300,
                loaded: 0,
                status: UPLOAD_STATUSES.UPLOADING,
                files: 3,
                folders: 2,
                loadedFiles: 0,
                loadedFolders: 0,
                filesErrors: 2,
                foldersErrors: 2,
                retriableErrors: 1,
                isDirectory: true
            });

            expect(_.pick(newState.uploadingStatesByIds['/disk/folder/subfolder'], ['status', 'error']))
                .toEqual({ status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.FOLDER_NOT_CREATED });
            expect(_.pick(newState.uploadingStatesByIds['/disk/folder/subfolder/1.jpg'], ['status', 'error']))
                .toEqual({ status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.PARENT_FOLDER_CREATE_FAIL });
            expect(_.pick(newState.uploadingStatesByIds['/disk/folder/subfolder/subfolder2'], ['status', 'error']))
                .toEqual({ status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.PARENT_FOLDER_CREATE_FAIL });
            expect(_.pick(newState.uploadingStatesByIds['/disk/folder/subfolder/subfolder2/2.jpg'], ['status', 'error']))
                .toEqual({ status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.PARENT_FOLDER_CREATE_FAIL });
        });

        it('Должен проставить статус CANCELED только тем ресурсам которые можно отменить', () => {
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 300,
                totalUploadedSize: 100,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.PROCESSING, total: 100, loaded: 100 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.CONFLICT, total: 100, loaded: 0 },
                    '/disk/3.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 }
                },
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg', '/disk/3.jpg'],
                notifications: [{
                    id: '/disk/2.jpg',
                    type: 'service',
                    data: { resourceId: '/disk/2.jpg', reason: UPLOAD_ERRORS.CONFLICT }
                }]
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: [
                    { id: '/disk/1.jpg', status: UPLOAD_STATUSES.CANCELED },
                    { id: '/disk/2.jpg', status: UPLOAD_STATUSES.CANCELED },
                    { id: '/disk/3.jpg', status: UPLOAD_STATUSES.CANCELED }
                ]
            });

            expect(newState.uploadingStatesByIds['/disk/1.jpg'].status).toBe(UPLOAD_STATUSES.PROCESSING);
            expect(newState.uploadingStatesByIds['/disk/2.jpg'].status).toBe(UPLOAD_STATUSES.CANCELED);
            expect(newState.uploadingStatesByIds['/disk/3.jpg'].status).toBe(UPLOAD_STATUSES.CANCELED);
            expect(newState.resourcesToUpload).toEqual(['/disk/1.jpg']);
            expect(newState.notifications).toEqual([]);
        });

        it('Должен при выставлении статуса CANCELED - проставить флаг needCancel или статус CANCELED для соответствущих статусов', () => {
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 300,
                totalUploadedSize: 100,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.GETTING_HASH, total: 100, loaded: 0 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.GETTING_URL, total: 100, loaded: 0, needCancel: true },
                },
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg'],
                notifications: []
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: [
                    { id: '/disk/1.jpg', status: UPLOAD_STATUSES.CANCELED },
                    { id: '/disk/2.jpg', status: UPLOAD_STATUSES.CANCELED }
                ]
            });

            expect(newState.uploadingStatesByIds['/disk/1.jpg'].status).toBe(UPLOAD_STATUSES.GETTING_HASH);
            expect(newState.uploadingStatesByIds['/disk/2.jpg'].status).toBe(UPLOAD_STATUSES.CANCELED);
            expect(newState.uploadingStatesByIds['/disk/1.jpg'].needCancel).toBe(true);
            expect(newState.uploadingStatesByIds['/disk/2.jpg'].needCancel).toBe(undefined);
        });

        it('Должен проставлять статус CANCELED или needCancel всем потомкам папки, для которых это возможно, при отмене загрузки папки', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 550,
                totalUploadedSize: 280,
                uploadingStatesByIds: {
                    '/disk/file.jpg': { status: UPLOAD_STATUSES.GETTING_URL, total: 50, loaded: 0 },
                    '/disk/folder': { status: UPLOAD_STATUSES.UPLOADING, total: 500, loaded: 280, files: 5, folders: 2, loadedFiles: 1, loadedFolders: 2, filesErrors: 0, foldersErrors: 0, isDirectory: true },
                    '/disk/folder/file.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 100, loaded: 80, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder': { status: UPLOAD_STATUSES.DONE, rootFolderId: '/disk/folder', isDirectory: true },
                    '/disk/folder/subfolder/1.jpg': { status: UPLOAD_STATUSES.PROCESSING, total: 100, loaded: 100, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder/subfolder2': { status: UPLOAD_STATUSES.DONE, rootFolderId: '/disk/folder', isDirectory: true },
                    '/disk/folder/subfolder/subfolder2/2.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder/subfolder2/3.jpg': { status: UPLOAD_STATUSES.GETTING_HASH, total: 100, loaded: 0, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder/subfolder2/4.jpg': { status: UPLOAD_STATUSES.DONE, total: 100, loaded: 100, rootFolderId: '/disk/folder' }
                },
                resourcesToUpload: ['/disk/file.jpg', '/disk/folder'],
                notifications: []
            };

            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/folder', status: UPLOAD_STATUSES.CANCELED }
            });

            expect(_.pick(newState, ['totalUploadingSize', 'totalUploadedSize', 'resourcesToUpload', 'visible', 'opened']))
                .toEqual({
                    totalUploadingSize: 350,
                    totalUploadedSize: 200,
                    resourcesToUpload: ['/disk/file.jpg'],
                    visible: true,
                    opened: true
                });

            expect(newState.uploadingStatesByIds['/disk/folder']).toEqual({
                isDirectory: true,
                total: 500,
                loaded: 280,
                status: UPLOAD_STATUSES.CANCELED,
                files: 5,
                folders: 2,
                loadedFiles: 1,
                loadedFolders: 2,
                filesErrors: 0,
                foldersErrors: 0
            });

            expect(newState.uploadingStatesByIds['/disk/folder/file.jpg'].status).toBe(UPLOAD_STATUSES.CANCELED);
            expect(newState.uploadingStatesByIds['/disk/folder/subfolder'].status).toBe(UPLOAD_STATUSES.DONE);
            expect(newState.uploadingStatesByIds['/disk/folder/subfolder/1.jpg'].status).toBe(UPLOAD_STATUSES.PROCESSING);
            expect(newState.uploadingStatesByIds['/disk/folder/subfolder/subfolder2'].status).toBe(UPLOAD_STATUSES.DONE);
            expect(newState.uploadingStatesByIds['/disk/folder/subfolder/subfolder2/2.jpg'].status).toBe(UPLOAD_STATUSES.CANCELED);
            expect(newState.uploadingStatesByIds['/disk/folder/subfolder/subfolder2/3.jpg'].status).toBe(UPLOAD_STATUSES.GETTING_HASH);
            expect(newState.uploadingStatesByIds['/disk/folder/subfolder/subfolder2/3.jpg'].needCancel).toBe(true);
            expect(newState.uploadingStatesByIds['/disk/folder/subfolder/subfolder2/4.jpg'].status).toBe(UPLOAD_STATUSES.DONE);
        });

        it('Должен проставить папке ошибку INSUFFICIENT_STORAGE если хотя бы один ресурс не смог загрузиться по этой причине', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 300,
                totalUploadedSize: 100,
                uploadingStatesByIds: {
                    '/disk/folder': { status: UPLOAD_STATUSES.UPLOADING, total: 300, loaded: 100, files: 3, folders: 2, loadedFiles: 1, loadedFolders: 1, filesErrors: 1, foldersErrors: 1, retriableErrors: 1, isDirectory: true },
                    '/disk/folder/file.jpg': { status: UPLOAD_STATUSES.DONE, total: 100, loaded: 100, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder': { status: UPLOAD_STATUSES.DONE, rootFolderId: '/disk/folder', isDirectory: true },
                    '/disk/folder/subfolder/1.jpg': { status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.UNKNOWN, total: 100, loaded: 0, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder/2.jpg': { status: UPLOAD_STATUSES.UPLOADING, total: 100, loaded: 0, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder/subfolder2': { status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.ENTRY_READ_FAIL, rootFolderId: '/disk/folder', isDirectory: true }
                },
                resourcesToUpload: ['/disk/folder'],
                notifications: []
            };

            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/folder/subfolder/2.jpg', status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE }
            });

            expect(_.pick(newState.uploadingStatesByIds['/disk/folder'], 'status', 'error')).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE
            });
        });

        it('Должен проставить папке ошибку FOLDER_UPLOAD_INCOMPLETE если все ошибки подресурсов ENTRY_READ_FAIL или FILE_TOO_BIG', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 53687091300,
                totalUploadedSize: 100,
                uploadingStatesByIds: {
                    '/disk/folder': { status: UPLOAD_STATUSES.UPLOADING, total: 500, loaded: 200, files: 3, folders: 2, loadedFiles: 1, loadedFolders: 1, filesErrors: 1, foldersErrors: 1, retriableErrors: 0, isDirectory: true },
                    '/disk/folder/file.jpg': { status: UPLOAD_STATUSES.DONE, total: 100, loaded: 100, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder': { status: UPLOAD_STATUSES.DONE, rootFolderId: '/disk/folder', isDirectory: true },
                    '/disk/folder/subfolder/1.jpg': { status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.FILE_TOO_BIG, total: 53687091200, loaded: 0, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder/subfolder2': { status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.ENTRY_READ_FAIL, rootFolderId: '/disk/folder', isDirectory: true }
                },
                resourcesToUpload: ['/disk/folder'],
                notifications: []
            };

            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/folder/subfolder/1.jpg', status: UPLOAD_STATUSES.DONE }
            });

            expect(_.pick(newState.uploadingStatesByIds['/disk/folder'], 'status', 'error')).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.FOLDER_UPLOAD_INCOMPLETE
            });
        });

        it('Должен проставить папке ошибку UNKNOWN если нет ошибок INSUFFICIENT_STORAGE и есть ошибки которые можно ретраить', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 200,
                totalUploadedSize: 200,
                uploadingStatesByIds: {
                    '/disk/folder': { status: UPLOAD_STATUSES.UPLOADING, total: 500, loaded: 200, files: 3, folders: 2, loadedFiles: 1, loadedFolders: 1, filesErrors: 1, foldersErrors: 1, retriableErrors: 0, isDirectory: true },
                    '/disk/folder/file.jpg': { status: UPLOAD_STATUSES.DONE, total: 100, loaded: 100, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder': { status: UPLOAD_STATUSES.DONE, rootFolderId: '/disk/folder', isDirectory: true },
                    '/disk/folder/subfolder/1.jpg': { status: UPLOAD_STATUSES.UPLOADED, total: 100, loaded: 100, rootFolderId: '/disk/folder' },
                    '/disk/folder/subfolder/subfolder2': { status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.ENTRY_READ_FAIL, rootFolderId: '/disk/folder', isDirectory: true }
                },
                resourcesToUpload: ['/disk/folder'],
                notifications: []
            };

            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/folder/subfolder/1.jpg', status: UPLOAD_STATUSES.ERROR, error: UPLOAD_ERRORS.UNKNOWN }
            });

            expect(_.pick(newState.uploadingStatesByIds['/disk/folder'], 'status', 'error')).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.UNKNOWN
            });
        });

        it('Должен проставить статус DONE вместо UPLOADING для пустой папки после ее создания', () => {
            const state = {
                visible: true,
                opened: true,
                totalUploadingSize: 0,
                totalUploadedSize: 0,
                uploadingStatesByIds: {
                    '/disk/emptyfolder': { status: UPLOAD_STATUSES.CREATING_FOLDER, total: 0, loaded: 0, files: 0, folders: 0, loadedFiles: 0, loadedFolders: 0, filesErrors: 0, foldersErrors: 0, isDirectory: true }
                },
                resourcesToUpload: ['/disk/emptyfolder'],
                notifications: []
            };

            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/emptyfolder', status: UPLOAD_STATUSES.UPLOADING }
            });

            expect(newState.uploadingStatesByIds['/disk/emptyfolder'].status).toBe(UPLOAD_STATUSES.DONE);
            expect(newState.uploadingStatus).toBe(UPLOAD_STATUSES.DONE);
        });

        it('Должен вставлять новую нотификацию о конфликте перед первой нотификацией о конфликте', () => {
            const state = {
                visible: true,
                opened: true,
                uploadingStatus: UPLOAD_STATUSES.UPLOADING,
                totalUploadingSize: 100,
                totalUploadedSize: 0,
                uploadingStatesByIds: {
                    '/disk/1.jpg': { status: UPLOAD_STATUSES.CONFLICT, total: 100, loaded: 0 },
                    '/disk/2.jpg': { status: UPLOAD_STATUSES.CONFLICT, total: 100, loaded: 0 },
                    '/disk/3.jpg': { status: UPLOAD_STATUSES.ENQUEUED, total: 100, loaded: 0 }
                },
                resourcesToUpload: ['/disk/1.jpg', '/disk/2.jpg', '/disk/3.jpg'],
                notifications: [
                    { id: UPLOAD_NOTIFICATION_TYPES.PROMO, type: UPLOAD_NOTIFICATION_TYPES.PROMO },
                    { id: '/disk/2.jpg', type: UPLOAD_NOTIFICATION_TYPES.SERVICE, data: { resourceId: '/disk/2.jpg', reason: UPLOAD_ERRORS.CONFLICT } },
                    { id: '/disk/1.jpg', type: UPLOAD_NOTIFICATION_TYPES.SERVICE, data: { resourceId: '/disk/1.jpg', reason: UPLOAD_ERRORS.CONFLICT } }
                ]
            };
            deepFreeze(state);
            const newState = uploader(state, {
                type: UPDATE_UPLOADING_STATE,
                payload: { id: '/disk/3.jpg', status: UPLOAD_STATUSES.CONFLICT }
            });

            expect(newState.notifications).toEqual([
                { id: UPLOAD_NOTIFICATION_TYPES.PROMO, type: UPLOAD_NOTIFICATION_TYPES.PROMO },
                { id: '/disk/3.jpg', type: UPLOAD_NOTIFICATION_TYPES.SERVICE, data: { resourceId: '/disk/3.jpg', reason: UPLOAD_ERRORS.CONFLICT } },
                { id: '/disk/2.jpg', type: UPLOAD_NOTIFICATION_TYPES.SERVICE, data: { resourceId: '/disk/2.jpg', reason: UPLOAD_ERRORS.CONFLICT } },
                { id: '/disk/1.jpg', type: UPLOAD_NOTIFICATION_TYPES.SERVICE, data: { resourceId: '/disk/1.jpg', reason: UPLOAD_ERRORS.CONFLICT } }
            ]);
        });
    });
});
