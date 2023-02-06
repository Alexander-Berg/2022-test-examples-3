/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as grpc from 'grpc';
import { getUserTicket, testUser } from '../../_helpers';
import { wipeDatabase, createUser, createSkill } from '../../../_helpers';
import cloudClient from '../../../../../services/cloud';
import * as endpointValidation from '../../../../../services/endpointUrlValidation';
import { FunctionMetadata } from '../../../../../services/cloud/types';
import { callApi, respondsWithError } from '../_helpers';
import { CloudError } from '../../../../../services/cloud/error';
import * as unistat from '../../../../../services/unistat';
import * as skillValidation from '../../../../../services/skill-validation';
import { DraftStatus } from '../../../../../db/tables/draft';
import { Channel } from '../../../../../db/tables/settings';
const { UNKNOWN, INTERNAL, ABORTED } = grpc.status;

const test = anyTest as TestInterface<{ userTicket: string }>;
const userId = testUser.uid;

test.before(async t => {
    t.context.userTicket = await getUserTicket(testUser.oauthToken);
});

test.beforeEach(async() => {
    sinon.restore();
    await wipeDatabase();
    await createUser({ id: userId });
});

const testFunction: FunctionMetadata = {
    cloudId: 'cloudId',
    cloudName: 'cloudName',
    folderId: 'folderId',
    folderName: 'folderName',
    functionId: 'functionId',
    functionName: 'functionName',
};

test('getUserFunctionsHandler: gets functions', async t => {
    const functions = [testFunction, testFunction, testFunction];
    sinon.replace(cloudClient, 'listUserFunctions', sinon.fake.resolves({ functions }));

    const res = await callApi('get', '/cloud-functions', { userTicket: t.context.userTicket });

    t.deepEqual(res.body.result, functions);
});

test('getUserFunctionsHandler: responds with error', async t => {
    sinon.replace(
        cloudClient,
        'listUserFunctions',
        sinon.fake.rejects(new CloudError('', UNKNOWN, 'listUserFunctions')),
    );

    const spy = sinon.spy(unistat, 'incCloudServerlessFunctionsError');
    const res = await callApi('get', '/cloud-functions', { userTicket: t.context.userTicket });

    t.true(spy.calledOnce);
    respondsWithError(
        {
            code: 500,
            message: '',
            payload: {
                errorCode: 2,
            },
        },
        res,
        t,
    );
});

test('getUserFunctionsHandler: responds with error and returns unknown error code if error message is not supported', async t => {
    sinon.replace(
        cloudClient,
        'listUserFunctions',
        // Передаем неподдерживаемый код ошибки ABORTED
        sinon.fake.rejects(new CloudError('', ABORTED, 'listUserFunctions')),
    );

    const spy = sinon.spy(unistat, 'incCloudServerlessFunctionsError');
    const res = await callApi('get', '/cloud-functions', { userTicket: t.context.userTicket });

    t.true(spy.calledOnce);
    respondsWithError(
        {
            code: 500,
            message: '',
            payload: {
                // В ответе код ошибки UNKNOWN - 2
                errorCode: 2,
            },
        },
        res,
        t,
    );
});

test('validateUserFunctionHandler: validates correctly', async t => {
    sinon.replace(cloudClient, 'validateFunction', sinon.fake.resolves(undefined));

    const res = await callApi('post', '/cloud-functions/functionId/validate', { userTicket: t.context.userTicket });

    t.is(res.body.result, 'ok');
});

test('validateUserFunctionHandler: throws error', async t => {
    sinon.replace(
        cloudClient,
        'validateFunction',
        sinon.fake.rejects(new CloudError('', INTERNAL, 'validateFunction')),
    );

    const spy = sinon.spy(unistat, 'incCloudServerlessFunctionsError');
    const res = await callApi('post', '/cloud-functions/functionId/validate', { userTicket: t.context.userTicket });

    t.true(spy.calledOnce);
    respondsWithError(
        {
            code: 500,
            message: '',
            payload: {
                errorCode: 13,
            },
            fields: {
                functionId: 'Ошибка валидации функции из Яндекс Облака. Проверьте доступность функции',
            },
        },
        res,
        t,
    );
});

test('validateUserFunctionHandler: should throw on requestReview', async t => {
    sinon.replace(endpointValidation, 'validateEndpointUrl', sinon.fake.resolves(undefined));
    sinon.replace(cloudClient, 'validateFunction', sinon.fake.rejects(new CloudError('', UNKNOWN, 'validateFunction')));
    sinon.replace(skillValidation, 'isDraftAllowedForDeploy', sinon.fake.returns(true));

    const skill = await createSkill({
        userId,
        backendSettings: {
            functionId: '123',
        },
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, { userTicket: t.context.userTicket });

    respondsWithError(
        {
            code: 500,
            message: '',
            payload: {
                errorCode: 2,
            },
            fields: {
                functionId: 'Ошибка валидации функции из Яндекс Облака. Проверьте доступность функции',
            },
        },
        res,
        t,
    );
});

test('validateUserFunctionHandler: review should be requested if function not throws', async t => {
    sinon.replace(endpointValidation, 'validateEndpointUrl', sinon.fake.resolves(undefined));
    sinon.replace(cloudClient, 'validateFunction', sinon.fake.resolves(undefined));
    sinon.replace(skillValidation, 'isDraftAllowedForDeploy', sinon.fake.returns(true));

    const skill = await createSkill({
        userId,
        backendSettings: {
            functionId: '123',
        },
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, { userTicket: t.context.userTicket });

    t.true(res.ok);

    await skill.draft.reload();

    t.is(skill.draft.status, DraftStatus.ReviewRequested);
});

test('validateUserFunctionHandler: should throw on requestDeploy', async t => {
    sinon.replace(cloudClient, 'validateFunction', sinon.fake.rejects(new CloudError('', UNKNOWN, 'validateFunction')));
    sinon.replace(skillValidation, 'isDraftAllowedForDeploy', sinon.fake.returns(true));

    const skill = await createSkill({
        userId,
        backendSettings: {
            functionId: '123',
        },
    });

    await skill.draft.update({
        status: DraftStatus.ReviewApproved,
    });

    const res = await callApi('post', `/skills/${skill.id}/release`, { userTicket: t.context.userTicket });

    respondsWithError(
        {
            code: 500,
            message: '',
            payload: {
                errorCode: 2,
            },
            fields: {
                functionId: 'Ошибка валидации функции из Яндекс Облака. Проверьте доступность функции',
            },
        },
        res,
        t,
    );
});

test('validateUserFunctionHandler: deploy should be requested if function not throws', async t => {
    sinon.replace(cloudClient, 'validateFunction', sinon.fake.resolves(undefined));
    sinon.replace(skillValidation, 'isDraftAllowedForDeploy', sinon.fake.returns(true));

    const skill = await createSkill({
        userId,
        backendSettings: {
            functionId: '123',
        },
    });

    await skill.draft.update({
        status: DraftStatus.ReviewApproved,
    });

    const res = await callApi('post', `/skills/${skill.id}/release`, { userTicket: t.context.userTicket });

    t.true(res.ok);

    await skill.draft.reload();

    t.is(skill.draft.status, DraftStatus.DeployRequested);
});

test('patchDraft: should remove backendUri', async t => {
    const skill = await createSkill({ userId });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        backendSettings: {
            backendType: 'function',
            functionId: '123',
            uri: 'https://example.com',
        },
    });

    t.true(res.ok);

    await skill.draft.reload();

    t.is(skill.draft.backendSettings.functionId, '123');
    t.is(skill.draft.backendSettings.backendType, 'function');
    t.falsy(skill.draft.backendSettings.uri);
});

test('patchDraft: should remove functionId', async t => {
    const skill = await createSkill({ userId });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        backendSettings: {
            backendType: 'webhook',
            functionId: '123',
            uri: 'https://example.com',
        },
    });

    t.true(res.ok);

    await skill.draft.reload();

    t.is(skill.draft.backendSettings.uri, 'https://example.com');
    t.is(skill.draft.backendSettings.backendType, 'webhook');
    t.falsy(skill.draft.backendSettings.functionId);
});

test('patchDraft: should set backendType to webhook if not specified', async t => {
    const skill = await createSkill({ userId });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        backendSettings: {
            functionId: '123',
            uri: 'https://example.com',
        },
    });

    t.true(res.ok);

    await skill.draft.reload();

    t.is(skill.draft.backendSettings.uri, 'https://example.com');
    t.is(skill.draft.backendSettings.backendType, 'webhook');
    t.falsy(skill.draft.backendSettings.functionId);
});

test('patchDraft: should deny cloud functions in trusted smart home skills', async t => {
    const skill = await createSkill({
        userId,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
    });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        backendSettings: {
            backendType: 'function',
            functionId: '123',
        },
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                isTrustedSmartHomeSkill: 'Облачные функции недоступны в официальных навыках',
            },
        },
        res,
        t,
    );
});
