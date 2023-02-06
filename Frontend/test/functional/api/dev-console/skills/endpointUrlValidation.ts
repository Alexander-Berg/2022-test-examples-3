/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as endpointValidation from '../../../../../services/endpointUrlValidation';
import * as skillValidation from '../../../../../services/skill-validation';
import { UserInstance } from '../../../../../db/tables/user';
import { getUserTicket, testUser } from '../../_helpers';
import { wipeDatabase, createUser, createSkill } from '../../../_helpers';
import { callApi, respondsWithError } from '../_helpers';
import { Channel } from '../../../../../db/tables/settings';
import { EndpointUrlValidationError } from '../../../../../services/endpointUrlValidation/error';
import { approveReview } from '../../../../../services/skill-lifecycle';
import cloudClient from '../../../../../services/cloud';
import config from '../../../../../services/config';

interface TestContext {
    userTicket: string;
    user: UserInstance;
    initialShouldValidateEnpointUrl: boolean;
}

const test = anyTest as TestInterface<TestContext>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    t.context.userTicket = userTicket;
    t.context.initialShouldValidateEnpointUrl = config.iot.shouldValidateEndpointUri;
    config.iot.shouldValidateEndpointUri = true;
});

test.after(t => {
    config.iot.shouldValidateEndpointUri = t.context.initialShouldValidateEnpointUrl;
});

test.beforeEach(async t => {
    await wipeDatabase();
    sinon.replace(skillValidation, 'isDraftAllowedForDeploy', sinon.fake.returns(true));
    t.context.user = await createUser({ id: testUser.uid });
});

test.afterEach.always(async() => {
    sinon.restore();
});

test('Do not validate endpoint url for non smart home skills', async t => {
    const fake = sinon.fake.resolves(undefined);
    sinon.replace(endpointValidation, 'validateEndpointUrl', fake);

    const skill = await createSkill({
        userId: testUser.uid,
        channel: Channel.AliceSkill,
        backendSettings: { uri: 'https://example.com', backendType: 'webhook' },
    });

    await approveReview(skill, { user: t.context.user });

    const res = await callApi('post', `/skills/${skill.id}/release`, t.context);

    t.is(res.status, 201);
    t.true(fake.notCalled);
});

test('Do not validate endpoint url for cloud functions smart home skills', async t => {
    const fake = sinon.fake.resolves(undefined);
    sinon.replace(endpointValidation, 'validateEndpointUrl', fake);
    const validationFake = sinon.fake.resolves(undefined);
    sinon.replace(cloudClient, 'validateFunction', validationFake);

    const skill = await createSkill({
        userId: testUser.uid,
        channel: Channel.SmartHome,
        backendSettings: { functionId: '123', backendType: 'function' },
    });

    await approveReview(skill, { user: t.context.user });

    const res = await callApi('post', `/skills/${skill.id}/release`, t.context);

    t.is(res.status, 201);
    t.true(fake.notCalled);
    t.true(validationFake.called);
});

test('Successful publication', async t => {
    sinon.replace(endpointValidation, 'validateEndpointUrl', sinon.fake.resolves(undefined));

    const skill = await createSkill({
        userId: testUser.uid,
        channel: Channel.SmartHome,
        backendSettings: { uri: 'https://example.com', backendType: 'webhook' },
    });

    await approveReview(skill, { user: t.context.user });

    const res = await callApi('post', `/skills/${skill.id}/release`, t.context);
    await skill.draft.reload();

    t.is(res.status, 201);
    // smart home skills deploys immediately
    t.is(skill.draft.status, 'inDevelopment');
});

test('Failed publication (bad request)', async t => {
    sinon.replace(
        endpointValidation,
        'validateEndpointUrl',
        sinon.fake.rejects(
            EndpointUrlValidationError.fromErrorBody({
                request_id: '',
                status: 'error'
            }),
        ),
    );

    const skill = await createSkill({
        channel: Channel.SmartHome,
        userId: testUser.uid,
        backendSettings: { uri: 'https://example.com', backendType: 'webhook' },
    });

    await approveReview(skill, { user: t.context.user });

    const res = await callApi('post', `/skills/${skill.id}/release`, t.context);
    await skill.draft.reload();

    // return to review approved after unsuccesful publication
    t.is(skill.draft.status, 'reviewApproved');
    respondsWithError(
        {
            code: 400,
            message: 'Bad request for endpoint validation',
            payload: {
                errorCode: 'ERROR',
            },
        },
        res,
        t,
    );
});

const fieldError = {
    status: 'ERROR',
    url: 'example.com',
    http_code: 400,
    http_method: 'POST',
}
test('Failed publication (validation error)', async t => {
    sinon.replace(
        endpointValidation,
        'validateEndpointUrl',
        sinon.fake.rejects(
            EndpointUrlValidationError.fromErrorBody({
                request_id: '',
                status: 'error',
                devices: fieldError,
            }),
        ),
    );

    const skill = await createSkill({
        channel: Channel.SmartHome,
        userId: testUser.uid,
        backendSettings: { uri: 'https://example.com', backendType: 'webhook' },
    });

    await approveReview(skill, { user: t.context.user });

    const res = await callApi('post', `/skills/${skill.id}/release`, t.context);
    await skill.draft.reload();

    // return to review approved after unsuccesful publication
    t.is(skill.draft.status, 'reviewApproved');
    respondsWithError(
        {
            code: 400,
            message: 'Validation error in endpoint validation',
            payload: {
                errorCode: 'ERROR',
            },
            fields: {
                uri: 'Невалидный url бэкенда\ndevices: ERROR',
            },
        },
        res,
        t,
    );
});
