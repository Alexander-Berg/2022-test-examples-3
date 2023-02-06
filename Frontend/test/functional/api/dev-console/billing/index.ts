/* eslint-disable */
import * as nock from 'nock';
import anyTest, { TestInterface, ExecutionContext } from 'ava';
import * as uuid from 'uuid';
import config from '../../../../../services/config';
import {
    merchantsFixture,
    merchantFixture,
    incorrectMerchantsFixture1,
    incorrectMerchantsFixture2,
    incorrectMerchantFixture,
} from './_fixtures';
import { wipeDatabase, createSkill, createUser } from '../../../_helpers';
import { callApi, respondsWithError } from '../_helpers';
import { getUserTicket, testUser } from '../../_helpers';
import * as tvm from '../../../../../services/tvm';
import { completeDeploy } from '../../../../../services/skill-lifecycle';
import { SkillsCrypto } from '../../../../../db';
import sinon = require('sinon');

const test = anyTest as TestInterface<{ userTicket: string }>;

const scope = nock(config.billing.url);

test.before(async t => {
    t.context.userTicket = await getUserTicket(testUser.oauthToken);
});

test.beforeEach(async() => {
    nock.cleanAll();
    sinon.restore();
    await wipeDatabase();
    await createUser({ id: testUser.uid });
});

test('billing: getMerchants with correct fixture', async t => {
    const { res } = await callMerchants(t, {
        responseBody: { merchants: merchantsFixture },
        responseCode: 200,
    });

    t.true(scope.isDone());
    t.deepEqual(res.body, { result: merchantsFixture });
});

test('billing: getMerchants with incorrect fixture 1', async t => {
    const { res } = await callMerchants(t, {
        responseBody: { merchants: incorrectMerchantsFixture1 },
        responseCode: 200,
    });

    t.true(scope.isDone());
    respondsWithError(
        {
            code: 500,
            message: 'Unexpected response body',
        },
        res,
        t,
    );
});

test('billing: getMerchants with incorrect fixture 2', async t => {
    const { res } = await callMerchants(t, {
        responseBody: incorrectMerchantsFixture2,
        responseCode: 200,
    });

    t.true(scope.isDone());
    respondsWithError(
        {
            code: 500,
            message: 'Unexpected response body',
        },
        res,
        t,
    );
});

test('billing: getMerchants with 404 response from billing', async t => {
    const { res, skill } = await callMerchants(t, { responseCode: 404 });

    t.true(scope.isDone());

    respondsWithError(
        {
            code: 400,
            message: `Billing is not enabled for skill: ${skill.id}`,
            payload: { errorCode: 'BILLING_NOT_ENABLED' },
        },
        res,
        t,
    );
});

test('billing: requestMerchantAccess positive 1', async t => {
    const { res } = await callMerchantRequestAccess(t, {
        responseBody: merchantFixture,
        responseCode: 200,
        requestBody: {
            token: 'token',
            description: 'description',
        },
    });

    t.true(scope.isDone());
    t.deepEqual(res.body, { result: merchantFixture });
});

test('billing: requestMerchantAccess postitive 2', async t => {
    const { res } = await callMerchantRequestAccess(t, {
        responseBody: merchantFixture,
        responseCode: 200,
        requestBody: {
            token: 'token',
        },
    });

    t.true(scope.isDone());
    t.deepEqual(res.body, { result: merchantFixture });
});

test('billing: requestMerchantAccess without token', async t => {
    const { res } = await callMerchantRequestAccess(t, {
        responseBody: merchantFixture,
        responseCode: 200,
        requestBody: {
            description: 'description',
        },
    });

    respondsWithError(
        {
            code: 400,
            message: 'Bad request',
        },
        res,
        t,
    );
});

test('billing: requestMerchantAccess with wrong response', async t => {
    const { res } = await callMerchantRequestAccess(t, {
        responseBody: incorrectMerchantFixture,
        responseCode: 200,
        requestBody: {
            token: 'token',
            description: 'description',
        },
    });

    t.true(scope.isDone());
    respondsWithError(
        {
            code: 500,
            message: 'Unexpected response body',
        },
        res,
        t,
    );
});

test('billing: getPublicKey positive', async t => {
    const { res } = await callPublicKey(t, {
        publicKey: 'publicKey',
        privateKey: 'privateKey',
    });

    t.true(scope.isDone());
    t.deepEqual(res.body, { result: 'publicKey' });
});

test('billing: registerBilling positive', async t => {
    const { res } = await callRegisterBilling(t, {
        responseBody: { result: 'ok' },
        responseCode: 200,
    });

    t.true(scope.isDone());
    t.deepEqual(res.body, { result: 'ok' });
});

test('billing: registerBilling bad response body', async t => {
    const { res } = await callRegisterBilling(t, {
        responseBody: { unexpectedField: 'value' },
        responseCode: 200,
    });

    t.true(scope.isDone());
    respondsWithError(
        {
            code: 500,
            message: 'Unexpected response',
        },
        res,
        t,
    );
});

test('billing: request to missing skill', async t => {
    const res = await callApi('put', `/skills/${uuid()}`, { userTicket: t.context.userTicket });

    respondsWithError(
        {
            code: 404,
            message: 'Resource not found',
        },
        res,
        t,
    );
});

test('billing: request with invalid skill id', async t => {
    const res = await callApi('put', '/skills/invalid_id', { userTicket: t.context.userTicket });

    respondsWithError(
        {
            code: 400,
            message: 'Invalid skill id',
        },
        res,
        t,
    );
});

const callMerchants = async(
    t: ExecutionContext<{ userTicket: string }>,
    params: { responseBody?: any; responseCode: number },
) => {
    sinon.replace(tvm, 'getServiceTickets', sinon.fake.resolves({ billing: { ticket: '' } }));
    const skill = await createSkill({ userId: testUser.uid });
    await completeDeploy(skill);

    scope.get(`/skill/${skill.id}/merchants`).reply(params.responseCode, params.responseBody);

    return {
        res: await callApi('get', `/billing/skills/${skill.id}/merchants`, {
            userTicket: t.context.userTicket,
        }),
        skill,
    };
};

const callMerchantRequestAccess = async(
    t: ExecutionContext<{ userTicket: string }>,
    params: { responseBody?: any; responseCode: number; requestBody?: any },
) => {
    sinon.replace(tvm, 'getServiceTickets', sinon.fake.resolves({ billing: { ticket: '' } }));
    const skill = await createSkill({ userId: testUser.uid });
    await completeDeploy(skill);

    scope
        .put(`/skill/${skill.id}/request_merchant_access`)
        .reply(params.responseCode, params.responseBody);

    return {
        res: await callApi('put', `/billing/skills/${skill.id}/request_merchant_access`, {
            userTicket: t.context.userTicket,
        }).send(params.requestBody),
        skill,
    };
};

const callPublicKey = async(
    t: ExecutionContext<{ userTicket: string }>,
    keyPair: { privateKey: string; publicKey: string },
) => {
    sinon.replace(tvm, 'getServiceTickets', sinon.fake.resolves({ billing: { ticket: '' } }));
    const skill = await createSkill({ userId: testUser.uid });
    await completeDeploy(skill);
    await SkillsCrypto.create({
        skillId: skill.id,
        ...keyPair,
    });

    return {
        res: await callApi('get', `/billing/skills/${skill.id}/public_key`, {
            userTicket: t.context.userTicket,
        }),
        skill,
    };
};

const callRegisterBilling = async(
    t: ExecutionContext<{ userTicket: string }>,
    params: { responseBody?: any; responseCode: number },
) => {
    sinon.replace(tvm, 'getServiceTickets', sinon.fake.resolves({ billing: { ticket: '' } }));
    const skill = await createSkill({ userId: testUser.uid });
    await completeDeploy(skill);

    scope.put(`/skill/${skill.id}`).reply(params.responseCode, params.responseBody);

    return {
        res: await callApi('put', `/billing/skills/${skill.id}`, {
            userTicket: t.context.userTicket,
        }),
        skill,
    };
};
