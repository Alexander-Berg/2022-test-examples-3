import '../../../noscript';
jest.mock('@ps-int/ps-components/src/helpers/offline', () => ({
    waitOnline: () => Promise.resolve(),
    defer: () => Promise.resolve()
}));

jest.mock('../../../../../components/redux/store/actions/space', () => ({
    fetchSpace: jest.fn(() => {
        return () => Promise.resolve({
            type: 'UPDATE_SPACE',
            payload: {
                space: {
                    loaded: 1048576,
                    status: 'DONE',
                    total: 1048576
                }
            }
        });
    })
}));

import { uploadFileResource } from '../../../../../components/redux/store/actions/uploader/upload-file';
import { createUploadingResources, startUpload } from '../../../../../components/redux/store/actions/uploader';
import * as queueActions from '../../../../../components/redux/store/actions/uploader/queue';
import * as rawFetchModel from '../../../../../components/redux/store/lib/raw-fetch-model';
import getStore from '../../../../../components/redux/store';
import createStore from '../../../../../components/redux/store/create-store';
import { UPDATE_UPLOADING_PROGRESS } from '../../../../../components/redux/store/actions/types';
import { fetchSpace } from '../../../../../components/redux/store/actions/space';
import { setResourceData } from '../../../../../components/redux/store/actions/resources';

jest.mock('../../../../../components/redux/store/actions/uploader/queue-hash', () => ({
    calculateHash: () => Promise.resolve({
        md5: '8f4e33f3dc3e414ff94e5fb6905cba8c',
        sha256: 'cd52d81e25f372e6fa4db2c0dfceb59862c1969cab17096da352b34950c973cc'
    })
}));

import { UPLOAD_STATUSES, UPLOAD_ERRORS } from '../../../../../components/helpers/upload';
import _ from 'lodash';

describe('uploadFileResource', () => {
    let originalRawFetchModel;
    let originalOnUPloadProgress;
    let listeners;
    let uploadListeners;
    let headers;
    const xhrBaseMock = {
        open: () => {},
        setRequestHeader: (key, value) => {
            headers[key] = value;
        },
        addEventListener: (event, callback) => {
            listeners[event] = callback;
        },
        upload: {
            addEventListener: (event, callback) => {
                uploadListeners[event] = callback;
            }
        }
    };

    const RESOURCE_ID = '/disk/test-file.jpg';
    const queueItem = {
        id: RESOURCE_ID,
        entry: {
            type: 'image/jpg',
            name: 'test-file.jpg',
            size: 5278350,
            slice(from, to) {
                return [from, to];
            }
        },
        force: 0,
        dependency: null,
        abortables: {}
    };

    const defaultResponses = {
        'do-resource-upload-url': [{
            promise: 'resolve',
            data: {
                upload_url: 'https://uploader.yandex.net/upload_url',
                oid: 'o1'
            }
        }],
        'do-status-operation': [{
            promise: 'resolve',
            condition: (params) => params.oid === 'o1',
            data: {
                state: 'COMPLETED',
                resource: {
                    id: RESOURCE_ID,
                    isRealFile: true
                }
            }
        }],
        resource: [{
            promise: 'resolve',
            data: {
                id: RESOURCE_ID
            }
        }]
    };

    beforeEach(() => {
        originalRawFetchModel = rawFetchModel.default;
        listeners = {};
        uploadListeners = {};
        headers = {};

        getStore().dispatch(startUpload([queueItem]));
        getStore().dispatch(createUploadingResources([queueItem]));

        originalOnUPloadProgress = queueActions.onUploadProgress;
        queueActions.onUploadProgress = (id, uploadingState) => ({
            type: UPDATE_UPLOADING_PROGRESS,
            payload: Object.assign({ id }, uploadingState)
        });
    });

    afterEach(() => {
        rawFetchModel.default = originalRawFetchModel;
        queueActions.onUploadProgress = originalOnUPloadProgress;
    });

    /**
     * @param {number} status - статус которым ответит кладун
     * @param {Object} [overrideMock] - дополнительные переопределениия мока
     */
    const mockXHR = (status, overrideMock) => {
        const sendMock = {
            send() {
                setTimeout(() => {
                    this.status = status;
                    listeners.load.call(this);
                }, 0);
            }
        };

        window.XMLHttpRequest = jest.fn()
            .mockImplementationOnce(() => Object.assign(xhrBaseMock, sendMock, overrideMock));
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

    it('Успешная загрузка файла', (done) => {
        mockRawFetchModel(defaultResponses);
        const xhrOpen = jest.fn();
        mockXHR(201, { open: xhrOpen });

        getStore().dispatch(uploadFileResource(queueItem)).then((item) => {
            expect(item).toEqual(queueItem);
            expect(xhrOpen).toBeCalledWith('PUT', 'https://uploader.yandex.net/upload_url');
            expect(headers).toEqual({
                'X-Disk-Uploader-Wait-Complete-Upload': 'false',
                'Content-Type': 'image/jpg'
            });
            const { resources, uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(resources[RESOURCE_ID].isRealFile).toBe(true);
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.DONE,
                loaded: 5278350,
                total: 5278350
            });
            done();
        });
    });

    it('Поллинг операции после загрузки', (done) => {
        mockRawFetchModel(Object.assign({}, defaultResponses, {
            'do-status-operation': [{
                promise: 'resolve',
                condition: (params) => params.oid === 'o1',
                data: { state: 'EXECUTING' }
            }, {
                promise: 'resolve',
                condition: (params) => params.oid === 'o1',
                data: { state: 'EXECUTING' }
            }, {
                promise: 'resolve',
                condition: (params) => params.oid === 'o1',
                data: {
                    state: 'COMPLETED',
                    resource: {
                        id: RESOURCE_ID,
                        isRealFile: true
                    }
                }
            }]
        }));
        mockXHR(201);

        getStore().dispatch(uploadFileResource(queueItem, 2)).then((item) => {
            expect(item).toEqual(queueItem);
            const { resources, uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(resources[RESOURCE_ID].isRealFile).toBe(true);
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.DONE,
                loaded: 5278350,
                total: 5278350
            });
            done();
        });
    });

    it('Кладун ответил 507', (done) => {
        mockRawFetchModel(defaultResponses);
        mockXHR(507);

        getStore().dispatch(uploadFileResource(queueItem)).catch((error) => {
            expect(error).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE,
                xhrStatus: 507
            });

            const { uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE,
                loaded: 0,
                total: 5278350
            });
            done();
        });
    });

    it('Ручка /store ответила 507', (done) => {
        mockRawFetchModel({
            'do-resource-upload-url': [{
                promise: 'reject',
                data: { id: 'HTTP_507' }
            }]
        });

        getStore().dispatch(uploadFileResource(queueItem)).catch((error) => {
            expect(error).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE
            });

            const { uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE,
                loaded: 0,
                total: 5278350
            });
            done();
        });
    });

    it('Ручка /store ответила 412 (файл уже существует)', (done) => {
        mockRawFetchModel({
            'do-resource-upload-url': [{
                promise: 'reject',
                data: { id: 'HTTP_412', body: { code: 47 } }
            }]
        });

        getStore().dispatch(uploadFileResource(queueItem)).catch((error) => {
            expect(error).toEqual({
                status: UPLOAD_STATUSES.CONFLICT
            });

            const { uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.CONFLICT,
                loaded: 0,
                total: 5278350
            });
            done();
        });
    });

    it('Загрузка завершилась, но операция провалилась', (done) => {
        mockRawFetchModel(Object.assign({}, defaultResponses, {
            'do-status-operation': [{
                promise: 'resolve',
                condition: (params) => params.oid === 'o1',
                data: { state: 'FAILED' }
            }]
        }));
        mockXHR(201);

        getStore().dispatch(uploadFileResource(queueItem)).catch((error) => {
            expect(error).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.UNKNOWN
            });

            const { uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.UNKNOWN,
                loaded: 5278350,
                total: 5278350
            });
            done();
        });
    });

    it('Загрузка завершилась, но операция провалилась из-за нехватки места', (done) => {
        mockRawFetchModel(Object.assign({}, defaultResponses, {
            'do-status-operation': [{
                promise: 'resolve',
                condition: (params) => params.oid === 'o1',
                data: { state: 'FAILED', error: { response: 507 } }
            }]
        }));
        mockXHR(0, {
            send() {
                setTimeout(() => {
                    uploadListeners.progress.call(this, { loaded: 5278350, total: 5278350 });
                }, 0);
                setTimeout(() => {
                    uploadListeners.load.call(this);
                }, 0);
                setTimeout(() => {
                    this.status = 0;
                    listeners.error.call(this);
                }, 0);
            }
        });

        getStore().dispatch(uploadFileResource(queueItem)).catch((error) => {
            expect(error).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE
            });

            const { uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.INSUFFICIENT_STORAGE,
                loaded: 5278350,
                total: 5278350
            });
            done();
        });
    });

    it('В кладун всё залиилось, но соединение с ним порвалось, а файл всё равно успешно загрузиился', (done) => {
        mockRawFetchModel(defaultResponses);
        mockXHR(0, {
            send() {
                setTimeout(() => {
                    uploadListeners.progress.call(this, { loaded: 5278350, total: 5278350 });
                }, 0);
                setTimeout(() => {
                    uploadListeners.load.call(this);
                }, 0);
                setTimeout(() => {
                    this.status = 0;
                    listeners.error.call(this);
                }, 0);
            }
        });

        getStore().dispatch(uploadFileResource(queueItem)).then((item) => {
            expect(item).toEqual(queueItem);
            const { resources, uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(resources[RESOURCE_ID].isRealFile).toBe(true);
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.DONE,
                loaded: 5278350,
                total: 5278350
            });
            done();
        });
    });

    it('Файл >50гб', (done) => {
        const tooBigItem = _.merge({}, queueItem, { entry: { size: 53687091201 } });
        getStore().dispatch(setResourceData(RESOURCE_ID, 53687091201, 'meta.size'));
        getStore().dispatch(queueActions.onUploadProgress(RESOURCE_ID, { loaded: 0, total: 53687091201 }));

        getStore().dispatch(uploadFileResource(tooBigItem)).catch((error) => {
            expect(error).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.FILE_TOO_BIG
            });

            const { uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.ERROR,
                error: UPLOAD_ERRORS.FILE_TOO_BIG,
                loaded: 0,
                total: 53687091201
            });
            done();
        });
    });

    it('Файл <45МБ и его еще не загружали в диск', (done) => {
        const item1mb = _.merge({}, queueItem, { entry: { size: 1048576 } });
        getStore().dispatch(setResourceData(RESOURCE_ID, 1048576, 'meta.size'));

        mockRawFetchModel(defaultResponses);
        const xhrOpen = jest.fn();
        mockXHR(201, { open: xhrOpen });

        getStore().dispatch(uploadFileResource(item1mb)).then((item) => {
            expect(item).toEqual(item1mb);
            expect(xhrOpen).toBeCalled();
            const { uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.DONE,
                loaded: 1048576,
                total: 1048576
            });
            done();
        });
    });

    it('Файл <45МБ и его уже загружали в диск', (done) => {
        const item1mb = _.merge({}, queueItem, { entry: { size: 1048576 } });
        getStore().dispatch(setResourceData(RESOURCE_ID, 1048576, 'meta.size'));

        mockRawFetchModel(Object.assign({}, defaultResponses, {
            'do-resource-upload-url': [{
                promise: 'resolve',
                data: {
                    status: 'hardlinked'
                }
            }]
        }));

        const xhrOpen = jest.fn();
        mockXHR(201, { open: xhrOpen });
        getStore().dispatch(uploadFileResource(item1mb)).then((item) => {
            expect(item).toEqual(item1mb);
            expect(xhrOpen).not.toBeCalled();
            const { uploader: { uploadingStatesByIds } } = getStore().getState();
            expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.DONE,
                loaded: 1048576,
                total: 1048576
            });
            done();
        });
    });

    it('Файл > 2гб в IE', (done) => {
        mockRawFetchModel(defaultResponses);
        let sendCalls = 0;
        const xhr = {
            open: jest.fn(),
            setRequestHeader: jest.fn(),
            send() {
                setTimeout(() => {
                    sendCalls++;
                    this.status = sendCalls === 1 ? 202 : 201;
                    listeners.load.call(this);
                    mockXHR(undefined, xhr);
                }, 0);
            }
        };
        mockXHR(undefined, xhr);

        const store = createStore({
            environment: {
                agent: { BrowserEngine: 'Trident' },
                session: { experiment: {} }
            }
        });

        const item2_5gb = _.merge({}, queueItem, { entry: { size: 2684354560 } });
        store.dispatch(startUpload([item2_5gb]));
        store.dispatch(createUploadingResources([item2_5gb]));
        const originalFetch = window.fetch;
        window.fetch = jest.fn().mockImplementationOnce(() => Promise.resolve({
            status: 200,
            headers: {
                get(header) {
                    return header === 'content-length' && '2147483648';
                }
            }
        }));

        store.dispatch(uploadFileResource(item2_5gb)).then(() => {
            expect(xhr.open).toBeCalledWith('PUT', 'https://uploader.yandex.net/upload_url');
            expect(xhr.open).toHaveBeenCalledTimes(2);

            // В IE загрузка файла 2.5гб должна произойти в два этапа:
            // первым запросом грузятся первые 2ГБ, вторым - оставшиеся 500Мб
            expect(xhr.setRequestHeader.mock.calls).toEqual([
                ['X-Disk-Uploader-Wait-Complete-Upload', 'false'],
                ['Content-Type', 'image/jpg'],
                ['Content-Range', 'bytes=0-2147483647/2684354560'],

                ['X-Disk-Uploader-Wait-Complete-Upload', 'false'],
                ['Content-Type', 'image/jpg'],
                ['Content-Range', 'bytes=2147483648-2684354559/2684354560']
            ]);

            expect(store.getState().uploader.uploadingStatesByIds[RESOURCE_ID]).toEqual({
                status: UPLOAD_STATUSES.DONE,
                loaded: 2684354560,
                total: 2684354560
            });

            window.fetch = originalFetch;
            done();
        });
    });

    it('should update space after upload', (done) => {
        mockRawFetchModel(defaultResponses);
        const xhrOpen = jest.fn();
        mockXHR(201, { open: xhrOpen });

        getStore().dispatch(uploadFileResource(queueItem)).then(() => {
            expect(fetchSpace).toHaveBeenCalled();
            done();
        });
    });

    describe('failover', () => {
        it('uploader returns 500', (done) => {
            window.XMLHttpRequest = jest.fn()
                .mockImplementation(() => Object.assign(xhrBaseMock, {
                    send() {
                        setTimeout(() => {
                            this.status = 500;
                            listeners.load.call(this);
                        }, 0);
                    }
                })
                );

            rawFetchModel.default = jest.fn()
                .mockImplementation((modelName) => {
                    switch (modelName) {
                        case 'do-resource-upload-url':
                            return Promise.resolve({
                                upload_url: 'https://uploader.yandex.net/upload_url',
                                oid: 'o1'
                            });
                        case 'do-status-operation':
                            return Promise.resolve({
                                state: 'COMPLETED',
                                resource: {
                                    id: '/disk/test-file.jpg',
                                    isRealFile: true
                                }
                            });
                    }
                });

            getStore().dispatch(uploadFileResource(queueItem)).catch((error) => {
                expect(error).toEqual({
                    status: UPLOAD_STATUSES.ERROR,
                    error: UPLOAD_ERRORS.UNKNOWN,
                    xhrStatus: 500
                });

                const { uploader: { uploadingStatesByIds } } = getStore().getState();
                expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                    status: UPLOAD_STATUSES.ERROR,
                    error: UPLOAD_ERRORS.UNKNOWN,
                    loaded: 0,
                    total: 5278350
                });
                done();
            });
        });

        it('uploader returns 500 only once', (done) => {
            window.XMLHttpRequest = jest.fn()
                .mockImplementationOnce(() => Object.assign(xhrBaseMock, {
                    send() {
                        setTimeout(() => {
                            this.status = 500;
                            listeners.load.call(this);
                        }, 0);
                    }
                })).mockImplementation(() => Object.assign(xhrBaseMock, {
                    send() {
                        setTimeout(() => {
                            this.status = 200;
                            listeners.load.call(this);
                        }, 0);
                    }
                }));

            mockRawFetchModel({
                'do-resource-upload-url': [{
                    promise: 'resolve',
                    data: {
                        upload_url: 'https://uploader.yandex.net/upload_url',
                        oid: 'o1'
                    }
                }, {
                    promise: 'resolve',
                    data: {
                        upload_url: 'https://uploader.yandex.net/upload_url',
                        oid: 'o2'
                    }
                }],
                'do-status-operation': [{
                    promise: 'resolve',
                    condition: (params) => params.oid === 'o2',
                    data: {
                        state: 'COMPLETED',
                        resource: {
                            id: '/disk/test-file.jpg',
                            isRealFile: true
                        }
                    }
                }]
            });

            getStore().dispatch(uploadFileResource(queueItem)).then((item) => {
                expect(item).toEqual(queueItem);
                const { resources, uploader: { uploadingStatesByIds } } = getStore().getState();
                expect(resources[RESOURCE_ID].isRealFile).toBe(true);
                expect(uploadingStatesByIds[RESOURCE_ID]).toEqual({
                    status: UPLOAD_STATUSES.DONE,
                    loaded: 5278350,
                    total: 5278350
                });
                done();
            });
        });
    });
});
