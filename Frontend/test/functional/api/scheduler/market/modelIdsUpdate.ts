/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import { callApi } from '../_helpers';
import * as market from '../../../../../services/market/yt';
import * as yt from '../../../../../services/yt';
import { NewModelIdWithMetaRow } from '../../../../../services/market/types';
import { createUser, createSkill, wipeDatabase, createMarketDevice } from '../../../_helpers';
import { Channel } from '../../../../../db/tables/settings';
import { Skill } from '../../../../../entities/skill';

test.beforeEach(async() => {
    await wipeDatabase();
    sinon.restore();
});

test('Should update published market devices', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        onAir: true,
        channel: Channel.SmartHome,
    });

    await createMarketDevice({
        skillId: skill.id,
        marketId: '123',
    });

    const stubUpdateMetadata: NewModelIdWithMetaRow[] = [
        {
            is_draft: false,
            old_model_id: '123',
            new_model_id: '234',
            skill_id: skill.id,
        },
    ];

    sinon.replace(yt, 'getUnprocessedTables', sinon.fake.resolves(['test']));
    sinon.replace(yt, 'markTableProcessed', sinon.fake.resolves(undefined));
    sinon.replace(market, 'downloadUpdatedMarketModelIdsFromYt', sinon.fake.resolves(stubUpdateMetadata));

    await callApi('/skills/smart-home-market-model-ids/download-from-yt');

    const [device] = await skill.getPublishedMarketDevices();

    t.deepEqual(device.marketId, '234');
});

test('Should update draft market devices', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
    });

    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '123',
        },
        { isDraft: true },
    );

    const stubUpdateMetadata: NewModelIdWithMetaRow[] = [
        {
            is_draft: true,
            old_model_id: '123',
            new_model_id: '234',
            skill_id: skill.id,
        },
    ];

    sinon.replace(yt, 'getUnprocessedTables', sinon.fake.resolves(['test']));
    sinon.replace(yt, 'markTableProcessed', sinon.fake.resolves(undefined));
    sinon.replace(market, 'downloadUpdatedMarketModelIdsFromYt', sinon.fake.resolves(stubUpdateMetadata));

    await callApi('/skills/smart-home-market-model-ids/download-from-yt');

    const [device] = await skill.getDraftMarketDevices();

    t.deepEqual(device.marketId, '234');
});

test('Should update market devices in several entities', async t => {
    const user = await createUser();
    const skill1 = await createSkill({ userId: user.id, channel: Channel.SmartHome });
    const skill2 = await createSkill({ userId: user.id, channel: Channel.SmartHome });
    const skill3 = await createSkill({ userId: user.id, channel: Channel.SmartHome });

    await createMarketDevice({
        skillId: skill1.id,
        marketId: '123',
    });
    await createMarketDevice({
        skillId: skill2.id,
        marketId: '123',
    });

    await createMarketDevice(
        {
            skillId: skill3.id,
            marketId: '123',
        },
        { isDraft: true },
    );

    const stubUpdateMetadata: NewModelIdWithMetaRow[] = [
        {
            is_draft: false,
            old_model_id: '123',
            new_model_id: '234',
            skill_id: skill1.id,
        },
        {
            is_draft: false,
            old_model_id: '123',
            new_model_id: '234',
            skill_id: skill2.id,
        },
        {
            is_draft: true,
            old_model_id: '123',
            new_model_id: '234',
            skill_id: skill3.id,
        },
    ];

    sinon.replace(yt, 'getUnprocessedTables', sinon.fake.resolves(['test']));
    sinon.replace(yt, 'markTableProcessed', sinon.fake.resolves(undefined));
    sinon.replace(market, 'downloadUpdatedMarketModelIdsFromYt', sinon.fake.resolves(stubUpdateMetadata));

    await callApi('/skills/smart-home-market-model-ids/download-from-yt');

    const [device1] = await skill1.getPublishedMarketDevices();
    const [device2] = await skill2.getPublishedMarketDevices();
    const [device3] = await skill3.getDraftMarketDevices();

    t.deepEqual(device1.marketId, '234');
    t.deepEqual(device2.marketId, '234');
    t.deepEqual(device3.marketId, '234');
});

test('Should upload correct data to yt', async t => {
    const fake = sinon.fake();
    sinon.replace(yt, 'writeYtTableWithTTL', fake);

    const user = await createUser();
    const skill1 = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
    });
    await createMarketDevices(skill1, ['123', '234']);

    const skill2 = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
    });
    await createMarketDevices(skill2, ['123']);

    const skill3 = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
    });
    await createMarketDevices(skill3, ['123', '456']);
    await createMarketDevices(skill3, ['123'], true);

    await market.uploadMarketModelIdsToYt();

    const expectedPayload = [
        {
            skill_id: skill1.id,
            model_id: '123',
            is_draft: false,
        },
        {
            skill_id: skill1.id,
            model_id: '234',
            is_draft: false,
        },
        {
            skill_id: skill2.id,
            model_id: '123',
            is_draft: false,
        },
        {
            skill_id: skill3.id,
            model_id: '123',
            is_draft: false,
        },
        {
            skill_id: skill3.id,
            model_id: '456',
            is_draft: false,
        },
        {
            skill_id: skill3.id,
            model_id: '123',
            is_draft: true,
        },
    ];

    const arg = fake.getCall(0).args[2];

    t.true(Array.isArray(arg));
    t.is(arg.length, expectedPayload.length);

    expectedPayload.forEach(entity => {
        t.true(arg.some((x: any) => isMarketModelEntityEqual(x, entity)));
    });
});

function isMarketModelEntityEqual(value: any, other: any) {
    return value.skill_id === other.skill_id && value.model_id === other.model_id && value.is_draft === other.is_draft;
}

async function createMarketDevices(skill: Skill, ids: string[], isDraft?: boolean) {
    for (const id of ids) {
        await createMarketDevice({ skillId: skill.id, marketId: id }, { isDraft });
    }
}
