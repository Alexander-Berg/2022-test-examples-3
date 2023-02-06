import { safeSaga } from '@/utils/sagas';
import { takeLatest, all } from 'typed-redux-saga';

import * as actions from '../actions';
import * as testSend from './testSend';

import { FEATURE_NAME } from '../constants';

const safe = safeSaga(FEATURE_NAME);

export function* testSendSaga() {
    yield* all([
        takeLatest(actions.testSendStart, safe(testSend)),
    ]);
}
