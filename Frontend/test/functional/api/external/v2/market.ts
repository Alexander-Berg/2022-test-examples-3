/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { createUser, createSkill, createMarketDevice, wipeDatabase } from '../../../_helpers';
import { Channel, SkillAccess } from '../../../../../db/tables/settings';
import { callApi } from './_helpers';
import { getServiceTicket } from '../../_helpers';
import { completeDeploy } from '../../../../../services/skill-lifecycle';

const test = anyTest as TestInterface<{ serviceTicket: string }>;

test.before(async t => {
    const serviceTicket = await getServiceTicket();

    Object.assign(t.context, { serviceTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test('Should get devices from trusted skills', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    await completeDeploy(skill);
    const device1 = await createMarketDevice({
        skillId: skill.id,
        marketId: '123',
    });
    const device2 = await createMarketDevice({
        skillId: skill.id,
        marketId: '234',
    });

    const res = await callApi('get', '/skills/smart-home/market-device-ids', t.context);

    t.true(res.body.product_ids.map((a: any) => a.id).includes(device1.marketId));
    t.true(res.body.product_ids.map((a: any) => a.id).includes(device2.marketId));
});

test('Should get same devices from different trusted skills', async t => {
    const user = await createUser();
    const skill1 = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    const skill2 = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    await completeDeploy(skill1);
    await completeDeploy(skill2);

    const device1 = await createMarketDevice({
        skillId: skill1.id,
        marketId: '123',
    });
    const device2 = await createMarketDevice({
        skillId: skill2.id,
        marketId: '123',
    });

    const res = await callApi('get', '/skills/smart-home/market-device-ids', t.context);

    t.deepEqual(
        res.body.product_ids.map((a: any) => a.id),
        [device1.marketId, device2.marketId],
    );
});

test('Should not get devices from unpublished skills', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    await createMarketDevice({
        skillId: skill.id,
        marketId: '123',
    });
    await createMarketDevice({
        skillId: skill.id,
        marketId: '234',
    });

    const res = await callApi('get', '/skills/smart-home/market-device-ids', t.context);

    t.deepEqual(res.body, {
        product_ids: [],
    });
});

test('Should not get devices from private skills', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        hideInStore: true,
        skillAccess: SkillAccess.Public,
    });

    await createMarketDevice({
        skillId: skill.id,
        marketId: '123',
    });
    await createMarketDevice({
        skillId: skill.id,
        marketId: '234',
    });

    const res = await callApi('get', '/skills/smart-home/market-device-ids', t.context);

    t.deepEqual(res.body, {
        product_ids: [],
    });
});

test('Should not get devices from untrusted skills', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    await createMarketDevice({
        skillId: skill.id,
        marketId: '123',
    });
    await createMarketDevice({
        skillId: skill.id,
        marketId: '234',
    });

    const res = await callApi('get', '/skills/smart-home/market-device-ids', t.context);

    t.deepEqual(res.body, {
        product_ids: [],
    });
});
