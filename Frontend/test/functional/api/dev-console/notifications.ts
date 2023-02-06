/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';
import { getUserTicket, testUser } from '../_helpers';
import { callApi, respondsWithExistingModelContains } from './_helpers';
import { defaultNotificationOptions, defaultNotificationSettings } from '../../../../types';
import * as tycoon from '../../../../services/tycoon';
import * as confirmations from '../../../../db/entities/phoneConfirmations';
import { PhoneConfirmation } from '../../../../db';

const test = anyTest as TestInterface<{ userTicket: string; sandbox: sinon.SinonSandbox }>;

const testUUID = 'a334b9bc-4f4f-47a0-8bdb-78dc93cc9a83';

const findPhoneConfirmation = async(id: string) => (await PhoneConfirmation.findById(id))!;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async t => {
    t.context.sandbox = sinon.createSandbox();
    await wipeDatabase();
});

test.afterEach.always(t => {
    t.context.sandbox.restore();
});

test('Patch notifications settings', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ id: testUUID, userId: user.id });
    const res = await callApi('patch', `/skills/${skill.id}/notifications/options`, t.context).send(
        defaultNotificationOptions,
    );

    respondsWithExistingModelContains(
        {
            notificationSettings: {
                options: defaultNotificationOptions,
            },
        },
        res,
        t,
    );
});

test('Creates confirmation record when confirmationId is not specified', async t => {
    const phoneNumber = '+78005553535';
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ id: testUUID, userId: user.id });

    const spy = sinon.fake.resolves(undefined);
    t.context.sandbox.replace(tycoon, 'sendVerificationCode', spy);

    const createConfirmationSpy = t.context.sandbox.spy(confirmations, 'createPhoneConfirmation');

    const res = await callApi('patch', `/skills/${skill.id}/notifications/phone/confirmation`, t.context).send({
        phoneNumber,
    });

    const confirmation = await findPhoneConfirmation(res.body.result.confirmationId);

    t.is(confirmation.skillId, testUUID);
    t.true(
        spy.calledOnceWithExactly({
            phoneNumber,
            code: confirmation.code,
            uid: testUser.uid,
        }),
    );
    t.true(createConfirmationSpy.calledOnceWithExactly({ phoneNumber, skillId: testUUID }));
});

test('Find existing confirmation when confirmationId is specified', async t => {
    const phoneNumber = '+78005553535';
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ id: testUUID, userId: user.id });

    const sendCodeSpy = sinon.fake.resolves(undefined);
    t.context.sandbox.replace(tycoon, 'sendVerificationCode', sendCodeSpy);

    const findConfirmationSpy = t.context.sandbox.spy(confirmations, 'findPhoneConfirmation');
    const createConfirmationSpy = t.context.sandbox.spy(confirmations, 'createPhoneConfirmation');

    const res1 = await callApi('patch', `/skills/${skill.id}/notifications/phone/confirmation`, t.context).send({
        phoneNumber,
    });

    t.true(createConfirmationSpy.calledOnceWithExactly({ phoneNumber, skillId: testUUID }));

    await callApi('patch', `/skills/${skill.id}/notifications/phone/confirmation`, t.context).send({
        phoneNumber, // phone is ignored
        confirmationId: res1.body.result.confirmationId,
    });

    t.true(findConfirmationSpy.calledOnceWithExactly(res1.body.result.confirmationId));
});

test('Confirm phone', async t => {
    const phoneNumber = '+78005553535';
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ id: testUUID, userId: user.id });

    const sendCodeSpy = sinon.fake.resolves(undefined);
    t.context.sandbox.replace(tycoon, 'sendVerificationCode', sendCodeSpy);

    const res = await callApi('patch', `/skills/${skill.id}/notifications/phone/confirmation`, t.context).send({
        phoneNumber,
    });
    const confirmation = await findPhoneConfirmation(res.body.result.confirmationId);

    await callApi('patch', `/skills/${skill.id}/notifications/phone`, t.context).send({
        code: confirmation.code,
        confirmationId: confirmation.id,
    });

    await skill.reload();

    t.is(skill.notificationSettings.phoneNumber!, phoneNumber);
});

test('Not confirm phone with wrong code', async t => {
    const phoneNumber = '+78005553535';
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ id: testUUID, userId: user.id });

    const sendCodeSpy = sinon.fake.resolves(undefined);
    t.context.sandbox.replace(tycoon, 'sendVerificationCode', sendCodeSpy);

    const res1 = await callApi('patch', `/skills/${skill.id}/notifications/phone/confirmation`, t.context).send({
        phoneNumber,
    });
    const confirmation = await findPhoneConfirmation(res1.body.result.confirmationId);

    const res2 = await callApi('patch', `/skills/${skill.id}/notifications/phone`, t.context).send({
        code: '1',
        confirmationId: confirmation.id,
    });

    t.is(res2.body.error.code, 403);
});

test('Delete phone number', async t => {
    const phoneNumber = '+78005553535';
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({
        id: testUUID,
        userId: user.id,
        notificationSettings: {
            ...defaultNotificationSettings,
            phoneNumber,
        },
    });

    t.deepEqual(skill.notificationSettings, {
        ...defaultNotificationSettings,
        phoneNumber,
    });

    await callApi('delete', `/skills/${skill.id}/notifications/phone`, t.context);
    await skill.reload();

    t.is(skill.notificationSettings.phoneNumber, undefined);
});
