import '../../../../../tests/unit/noscript';
import { testSaga } from 'redux-saga-test-plan';
import { takeLeading, takeEvery } from 'redux-saga/effects';

import pageHelper from '../../../../helpers/page';
import resourceHelper from '../../../../helpers/resource';
import rootSaga from './index';
import {
    fetchAlbumWithItems,
    selectAllItemsInCluster,
} from '../../actions/albums/index';
import { selectAllItemsInClusterSaga } from './selectAllItemsInCluster';
import { fetchAlbumWithItemsSaga } from './fetchAlbumWithItems';

describe('Sagas/albums/rootSaga', () => {
    test('fetchAlbumWithItemsSaga should be triggered with takeLeading effect ', () => {
        testSaga(rootSaga)
            .next()
            .setContext({
                pageHelper,
                resourceHelper,
            })
            .next()
            .all([
                takeLeading(fetchAlbumWithItems.type, fetchAlbumWithItemsSaga),
                takeEvery(selectAllItemsInCluster, selectAllItemsInClusterSaga),
            ])
            .next()
            .isDone();
    });
});
