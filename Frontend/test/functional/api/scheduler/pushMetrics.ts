/* eslint-disable */
import '../_helpers';
import test from 'ava';
import got from 'got';
import * as sinon from 'sinon';
import config from '../../../../services/config';
import { wipeDatabase } from '../../_helpers';
import { callApi } from './_helpers';

test.beforeEach(async t => {
    await wipeDatabase();
});

test.skip('returns zeros on empty database', async t => {
    const gotPost = sinon.stub(got, 'post').resolves({
        statusCode: 200,
    } as any);

    const response = await callApi('/statistics/deploy-requested');
    t.truthy(response.status === 200);

    t.truthy(gotPost.callCount === 1);
    const sensors = ((gotPost as any).getCall(0).args[1].body).sensors;
    // 8 возможных комбинаций лейблов * (6 перцентилей + 2 count) = 64 метрики
    t.truthy(sensors.length === 64);
    sensors.map((s: any) => {
        t.truthy(s.value === 0);
    });

    gotPost.restore();
});

test.skip('throws error on Solomon 4xx', async t => {
    const gotPost = sinon.stub(got, 'post').resolves({
        statusCode: 400,
    } as any);

    const response = await callApi('/statistics/deploy-requested');
    t.truthy(gotPost.callCount === 1);
    t.truthy(response.status === 500);

    gotPost.restore();
});

test.skip('throws error on Solomon 5xx', async t => {
    const gotPost = sinon.stub(got, 'post').resolves({
        statusCode: 400,
    } as any);

    const response = await callApi('/statistics/deploy-requested');
    t.truthy(gotPost.callCount === 1);
    t.truthy(response.status === 500);

    gotPost.restore();
});

test.skip('test cluster name substituion', async t => {
    const gotPost = sinon.stub(got, 'post').resolves({
        statusCode: 200,
    } as any);

    config.qloud.environment = 'integtrationtest';

    const response = await callApi('/statistics/deploy-requested');
    t.truthy(response.status === 200);

    t.truthy(gotPost.callCount === 1);
    t.truthy(
        (gotPost as any).getCall(0).args[0] ===
            'http://solomon.yandex.net/push?project=paskills&service=api_push&cluster=integtrationtest',
    );
});
