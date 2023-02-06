/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import {
    createUpdatableCache,
    enableUpdatableCache,
    disableUpdatableCache,
} from '../../../services/cacheUtils';
import { sleep } from '../../../utils';

test.afterEach.always(() => {
    disableUpdatableCache();
});

test.serial('createUpdatableCache: do not update cache by default', async t => {
    const spy = sinon.fake.resolves(undefined);

    createUpdatableCache(spy, {
        updateInterval: 100,
    });

    await sleep(200);

    t.true(spy.notCalled);
});

test.serial('createUpdatableCache: update cache after enableCall', async t => {
    const spy = sinon.fake.resolves(undefined);

    createUpdatableCache(spy, {
        updateInterval: 100,
    });

    enableUpdatableCache();

    await sleep(200);

    t.true(spy.called);
});
