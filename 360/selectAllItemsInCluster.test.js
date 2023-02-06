import '../../../../../tests/unit/noscript';
import { testSaga } from 'redux-saga-test-plan';

import { selectAllItemsWorker } from './selectAllItemsInCluster';
import { fetchAlbumWithItemsSaga } from './fetchAlbumWithItems';
import { setIsSelectingPhotos } from '../../actions/albums/index';
import { getCurrentAlbum } from '../../selectors/albums';
import { addToSelected } from '../../actions/selection/index';
import { getSelectedResources } from '../../selectors/states-context';
import { MAX_ACTIONABLE_SELECTION } from '../../../../consts';

describe('Sagas/selectAllItemsInCluster', () => {
    const clusterId = 'cluster-1';

    it('should select all items in cluster', () => {
        const album = {
            isCompleted: false,
            clusters: [
                {
                    id: clusterId,
                    items: [
                        {
                            id: 'resource-1',
                        },
                        {
                            id: 'resource-2',
                        },
                    ],
                },
                {
                    id: 'cluster-2',
                    items: [
                        {
                            id: 'resource-1',
                        },
                    ],
                },
            ],
        };

        testSaga(selectAllItemsWorker, clusterId)
            .next()
            .put(setIsSelectingPhotos(true))
            .next()
            .select(getCurrentAlbum)
            .next(album)
            .put(addToSelected(['resource-1', 'resource-2']))
            .next()
            .select(getSelectedResources)
            .next(['resource-1', 'resource-2'])
            .put(setIsSelectingPhotos(false))
            .next()
            .isDone();
    });

    it('should exit loop if cluster not found', () => {
        const album = {
            isCompleted: false,
            clusters: [],
        };

        testSaga(selectAllItemsWorker, clusterId)
            .next()
            .put(setIsSelectingPhotos(true))
            .next()
            .select(getCurrentAlbum)
            .next(album)
            .put(setIsSelectingPhotos(false))
            .next()
            .isDone();
    });

    it('should fetch another portion till album is loaded', () => {
        const album = {
            isCompleted: false,
            clusters: [
                {
                    id: clusterId,
                    items: [
                        {
                            id: 'resource-1',
                        },
                        {
                            id: 'resource-2',
                        },
                    ],
                },
            ],
        };

        testSaga(selectAllItemsWorker, clusterId)
            .next()
            .put(setIsSelectingPhotos(true))
            .next()
            .select(getCurrentAlbum)
            .next(album)
            .put(addToSelected(['resource-1', 'resource-2']))
            .next()
            .select(getSelectedResources)
            .next(['resource-1', 'resource-2'])
            .call(fetchAlbumWithItemsSaga)
            .next()
            .select(getCurrentAlbum)
            .next({ ...album, isCompleted: true })
            .put(addToSelected(['resource-1', 'resource-2']))
            .next()
            .select(getSelectedResources)
            .next(['resource-1', 'resource-2'])
            .put(setIsSelectingPhotos(false))
            .next()
            .isDone();
    });

    it('should fetch another portion till selected cluster is not the last one', () => {
        const album = {
            isCompleted: false,
            clusters: [
                {
                    id: clusterId,
                    items: [
                        {
                            id: 'resource-1',
                        },
                        {
                            id: 'resource-2',
                        },
                    ],
                },
            ],
        };

        /**
         * @type {any}
         */
        const cluster = {};

        testSaga(selectAllItemsWorker, clusterId)
            .next()
            .put(setIsSelectingPhotos(true))
            .next()
            .select(getCurrentAlbum)
            .next(album)
            .put(addToSelected(['resource-1', 'resource-2']))
            .next()
            .select(getSelectedResources)
            .next(['resource-1', 'resource-2'])
            .call(fetchAlbumWithItemsSaga)
            .next()
            .select(getCurrentAlbum)
            .next({ ...album, clusters: album.clusters.concat(cluster) })
            .put(addToSelected(['resource-1', 'resource-2']))
            .next()
            .select(getSelectedResources)
            .next(['resource-1', 'resource-2'])
            .put(setIsSelectingPhotos(false))
            .next()
            .isDone();
    });

    it('should fetch another portion till MAX_ACTIONABLE_SELECTION is selected', () => {
        const album = {
            isCompleted: false,
            clusters: [
                {
                    id: clusterId,
                    items: [
                        {
                            id: 'resource-1',
                        },
                        {
                            id: 'resource-2',
                        },
                    ],
                },
            ],
        };

        testSaga(selectAllItemsWorker, clusterId)
            .next()
            .put(setIsSelectingPhotos(true))
            .next()
            .select(getCurrentAlbum)
            .next(album)
            .put(addToSelected(['resource-1', 'resource-2']))
            .next()
            .select(getSelectedResources)
            .next(['resource-1', 'resource-2'])
            .call(fetchAlbumWithItemsSaga)
            .next()
            .select(getCurrentAlbum)
            .next(album)
            .put(addToSelected(['resource-1', 'resource-2']))
            .next()
            .select(getSelectedResources)
            .next(Array(MAX_ACTIONABLE_SELECTION))
            .put(setIsSelectingPhotos(false))
            .next()
            .isDone();
    });
});
