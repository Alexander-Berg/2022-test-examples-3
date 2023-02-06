import '../../../noscript';
import { queue, addToQueue, cancelUploadResource } from '../../../../../components/redux/store/actions/uploader/queue';
import { startUpload } from '../../../../../components/redux/store/actions/uploader';
import { createResource } from '../../../../../components/redux/store/actions/resources';

import getStore from '../../../../../components/redux/store';

import deepFreeze from 'deep-freeze';

jest.mock('../../../../../components/redux/store/actions/uploader/upload-file', () => ({
    uploadFileResource: jest.fn(() => () => Promise.resolve())
}));

const testResources = [{ id: '/disk/0.jpg', size: 5278350, notPart: true }, { id: '/disk/1.jpg', size: 5278350 }, { id: '/disk/2.jpg', size: 5278350 }];

const queueItems = testResources.map((resource) => {
    return {
        id: resource.id,
        entry: {
            type: 'image/jpg',
            size: resource.size
        },
        force: 0,
        dependency: null,
        abortables: {}
    };
});

let store;

/**
 * Есть ли элемент в очереди
 *
 * @param {string} id
 * @returns {boolean}
 */
export const isInQueue = (id) => queue.some((item) => item.id === id);

describe('cancelUploadResource', () => {
    beforeEach(() => {
        store = getStore();
        deepFreeze(store);

        queueItems.forEach((queueItem, i) => {
            if (testResources[i].notPart) {
                getStore().dispatch(createResource({
                    id: queueItem.id,
                    type: 'file',
                    meta: {
                        size: queueItem.entry.size
                    }
                }));
            } else {
                getStore().dispatch(createResource({
                    id: queueItem.id,
                    type: 'file',
                    meta: {
                        size: queueItem.entry.size
                    }
                }, { part: true }));
            }
        });
    });

    it('Отмена загрузки единственного загружаемого файла', () => {
        const queueItem = queueItems[1];

        store.dispatch(startUpload([queueItem]));
        store.dispatch(addToQueue(queueItem));
        store.dispatch(cancelUploadResource(queueItem.id));

        const { resources, uploader: { visible, resourcesToUpload } } = store.getState();
        const resource = resources[queueItem.id];

        // отмененная загрузка удалена из очереди загрузки
        expect(isInQueue(queueItem.id)).toBe(false);
        // ресурс соответствующий загружаемому файлу удален
        expect(resource).toBe(undefined);
        // загружаемых ресурсов не осталось
        expect(resourcesToUpload).toEqual([]);
        // загрузчик закрывается
        expect(visible).toBe(false);
    });

    it('Отмена загрузки единственного загружаемого файла при конфликте(есть ресурс)', () => {
        const queueItem = queueItems[0];

        store.dispatch(startUpload([queueItem]));
        store.dispatch(addToQueue(queueItem));
        store.dispatch(cancelUploadResource(queueItem.id));

        const { resources, uploader: { visible, resourcesToUpload, notifications } } = getStore().getState();
        const resource = resources[queueItem.id];

        // отмененная загрузка удалена из очереди загрузки
        expect(isInQueue(queueItem.id)).toBe(false);
        // ресурс соответствующий загружаемому не удален
        expect(resource).not.toBe(undefined);
        // загружаемых ресурсов не осталось
        expect(resourcesToUpload).toEqual([]);
        // загрузчик закрывается
        expect(visible).toBe(false);
        // нет нотифайки о конфликте
        expect(notifications).toEqual([]);
    });

    it('Отмена загрузки загружаемого файла при загрузке нескольких ресурсов', () => {
        const queueItem = queueItems[1];

        store.dispatch(startUpload(queueItems));
        store.dispatch(addToQueue(queueItems));
        store.dispatch(cancelUploadResource(queueItem.id));

        const { resources, uploader: { visible, resourcesToUpload } } = store.getState();
        const resource = resources[queueItem.id];

        // отмененная загрузка удалена из очереди загрузки
        expect(isInQueue(queueItem.id)).toBe(false);
        // ресурс соответствующий загружаемому файлу удален
        expect(resource).toBe(undefined);
        // остальные ресурсы в загрузчике остались
        expect(resourcesToUpload).toEqual([queueItems[0].id, queueItems[2].id]);
        // загрузчик не закрывается, т.к. он не пуст
        expect(visible).toBe(true);
    });
});
