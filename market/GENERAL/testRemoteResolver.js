import {EMPTY, from, of, concat} from 'rxjs';
import {mergeMap, catchError} from 'rxjs/operators';
import {ofType} from 'redux-observable';
import context from '@yandex-market/mandrel/clientContext';
import * as logger from '@yandex-market/logger';
import {resolveCurrentUser} from '@root/src/resolvers/demo/resolveCurrentUser';
import {LOAD_REMOTE_RESOLVER, loadRemoteResolverSuccess} from '../actions';
import {updateCollections} from '@yandex-market/apiary/common/actions';

const testRemoteResolverEpic = action$ => action$.pipe(
    ofType(LOAD_REMOTE_RESOLVER),
    mergeMap(
        () => from(resolveCurrentUser(context))
            .pipe(
                mergeMap(({result, collections}) => concat(
                    of(updateCollections(collections)),
                    of(loadRemoteResolverSuccess(result)),
                )),
                catchError(error => {
                    logger.error(error);
                    return EMPTY;
                })
            )
    )
);
export default testRemoteResolverEpic;
