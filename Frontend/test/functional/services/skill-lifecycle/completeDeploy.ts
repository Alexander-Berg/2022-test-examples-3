/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import { pick } from 'lodash';
import {
    createUser,
    createSkill,
    wipeDatabase,
    createMarketDevices,
    isSameContent,
    createIntents,
} from '../../_helpers';
import { completeDeploy, requestDeploy } from '../../../../services/skill-lifecycle';
import { Channel, SkillAccess } from '../../../../db/tables/settings';
import * as intentService from '../../../../services/intents';
import * as endpointUrlValidation from '../../../../services/endpointUrlValidation';
import { DraftUserAgreement, PublishedUserAgreement } from '../../../../db';
import { userAgreementFields } from '../../../../db/userAgreement';

test.before(() => {
    sinon.replace(endpointUrlValidation, 'validateEndpointUrl', sinon.fake.resolves(undefined));
});

test.beforeEach(async t => {
    await wipeDatabase();
});

test.after(() => {
    sinon.restore();
});

test('completeDeploy() initializes isReommended with true', async t => {
    const user = await createUser();
    const skill = await createSkill({ isRecommended: null });
    await requestDeploy(skill, { user });
    await completeDeploy(skill);
    await skill.reload();
    t.true(skill.onAir);
    t.true(skill.isRecommended);
});

test("completeDeploy doesn't override isRecommended=true", async t => {
    const user = await createUser();
    const skill = await createSkill({ isRecommended: true });
    await requestDeploy(skill, { user });
    await completeDeploy(skill);
    await skill.reload();
    t.true(skill.onAir);
    t.true(skill.isRecommended);
});

test("completeDeploy doesn't override isRecommended=false", async t => {
    const user = await createUser();
    const skill = await createSkill({ isRecommended: false });
    await requestDeploy(skill, { user });
    await completeDeploy(skill);
    await skill.reload();
    t.true(skill.onAir);
    t.false(skill.isRecommended);
});

test("completeDeploy doesn't override market devices when skill is private", async t => {
    const user = await createUser();
    const skill = await createSkill({
        hideInStore: true,
        skillAccess: SkillAccess.Private,
        isTrustedSmartHomeSkill: true,
        channel: Channel.SmartHome,
    });

    await createMarketDevices(
        [
            {
                marketId: '123',
                isDraft: false,
            },
            {
                marketId: '123',
                isDraft: true,
            },
            {
                marketId: '234',
                isDraft: true,
            },
        ],
        skill.id,
    );

    await requestDeploy(skill, { user });
    await completeDeploy(skill);

    const draftMarketDevicesIds = (await skill.getDraftMarketDevices()).map(
        device => device.marketId,
    );
    const publishedMarketDeviceIds = (await skill.getPublishedMarketDevices()).map(
        device => device.marketId,
    );

    t.true(isSameContent(['123', '234'], draftMarketDevicesIds));
    t.true(isSameContent(['123'], publishedMarketDeviceIds));
});

test("completeDeploy doesn't override market devices when skill is not trusted", async t => {
    const user = await createUser();
    const skill = await createSkill({
        hideInStore: false,
        skillAccess: SkillAccess.Public,
        isTrustedSmartHomeSkill: false,
        channel: Channel.SmartHome,
    });

    await createMarketDevices(
        [
            {
                marketId: '123',
                isDraft: false,
            },
            {
                marketId: '123',
                isDraft: true,
            },
            {
                marketId: '234',
                isDraft: true,
            },
        ],
        skill.id,
    );

    await requestDeploy(skill, { user });
    await completeDeploy(skill);

    const draftMarketDevicesIds = (await skill.getDraftMarketDevices()).map(
        device => device.marketId,
    );
    const publishedMarketDeviceIds = (await skill.getPublishedMarketDevices()).map(
        device => device.marketId,
    );

    t.true(isSameContent(['123', '234'], draftMarketDevicesIds));
    t.true(isSameContent(['123'], publishedMarketDeviceIds));
});

test('completeDeploy override market devices when skill is public', async t => {
    const user = await createUser();
    const skill = await createSkill({
        isTrustedSmartHomeSkill: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
        channel: Channel.SmartHome,
    });

    await createMarketDevices(
        [
            {
                marketId: '123',
                isDraft: false,
            },
            {
                marketId: '123',
                isDraft: true,
            },
            {
                marketId: '234',
                isDraft: true,
            },
        ],
        skill.id,
    );

    await requestDeploy(skill, { user });
    await completeDeploy(skill);

    const draftMarketDevicesIds = (await skill.getDraftMarketDevices()).map(
        device => device.marketId,
    );
    const publishedMarketDeviceIds = (await skill.getPublishedMarketDevices()).map(
        device => device.marketId,
    );

    t.true(isSameContent(['123', '234'], draftMarketDevicesIds));
    t.true(isSameContent(['123', '234'], publishedMarketDeviceIds));
});

test('completeDeploy not publish market devices on first publishing when skill is private', async t => {
    const user = await createUser();
    const skill = await createSkill({
        isTrustedSmartHomeSkill: true,
        hideInStore: true,
        skillAccess: SkillAccess.Private,
        channel: Channel.SmartHome,
    });

    await createMarketDevices(
        [
            {
                marketId: '123',
                isDraft: true,
            },
            {
                marketId: '234',
                isDraft: true,
            },
        ],
        skill.id,
    );

    await requestDeploy(skill, { user });
    await completeDeploy(skill);

    const draftMarketDevicesIds = (await skill.getDraftMarketDevices()).map(
        device => device.marketId,
    );
    const publishedMarketDeviceIds = (await skill.getPublishedMarketDevices()).map(
        device => device.marketId,
    );

    t.true(isSameContent(['123', '234'], draftMarketDevicesIds));
    t.true(isSameContent([], publishedMarketDeviceIds));
});

test('completeDeploy not publish market devices on first publishing when skill is not trusted', async t => {
    const user = await createUser();
    const skill = await createSkill({
        isTrustedSmartHomeSkill: false,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
        channel: Channel.SmartHome,
    });

    await createMarketDevices(
        [
            {
                marketId: '123',
                isDraft: true,
            },
            {
                marketId: '234',
                isDraft: true,
            },
        ],
        skill.id,
    );

    await requestDeploy(skill, { user });
    await completeDeploy(skill);

    const draftMarketDevicesIds = (await skill.getDraftMarketDevices()).map(
        device => device.marketId,
    );
    const publishedMarketDeviceIds = (await skill.getPublishedMarketDevices()).map(
        device => device.marketId,
    );

    t.true(isSameContent(['123', '234'], draftMarketDevicesIds));
    t.true(isSameContent([], publishedMarketDeviceIds));
});

test('completeDeploy publish market devices on first publishing', async t => {
    const user = await createUser();
    const skill = await createSkill({
        isTrustedSmartHomeSkill: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
        channel: Channel.SmartHome,
    });

    await createMarketDevices(
        [
            {
                marketId: '123',
                isDraft: true,
            },
            {
                marketId: '234',
                isDraft: true,
            },
        ],
        skill.id,
    );

    await requestDeploy(skill, { user });
    await completeDeploy(skill);

    const draftMarketDevicesIds = (await skill.getDraftMarketDevices()).map(
        device => device.marketId,
    );
    const publishedMarketDeviceIds = (await skill.getPublishedMarketDevices()).map(
        device => device.marketId,
    );

    t.true(isSameContent(['123', '234'], draftMarketDevicesIds));
    t.true(isSameContent(['123', '234'], publishedMarketDeviceIds));
});

test('completeDeploy: publish only valid intents', async t => {
    await createUser();
    const skill = await createSkill();

    await createIntents(
        [
            {
                formName: 'hello1',
                humanReadableName: 'hello',
                sourceText: 'root: hello',
                base64: 'base64',
                isDraft: true,
            },
            {
                formName: 'hello2',
                humanReadableName: 'hello',
                sourceText: 'root: hello',
                isDraft: true,
            },
            {
                formName: 'hello3',
                humanReadableName: 'hello',
                sourceText: 'root: hello',
                isDraft: true,
            },
            { isDraft: true },
        ],
        skill.id,
    );

    await completeDeploy(skill);

    const publishedIntents = await intentService.getSkillIntents(skill.id, false);

    t.is(publishedIntents.length, 1);
    t.is(publishedIntents[0].formName, 'hello1');
});

test('completeDeploy: publish user agreements', async t => {
    await createUser();
    const skill = await createSkill();
    const draftUserAgreement = await DraftUserAgreement.create({
        skillId: skill.id,
        url: 'https://ya.ru',
        name: 'name',
        order: 0,
    });
    await completeDeploy(skill);
    const publishedUserAgreements = await PublishedUserAgreement.findAll({
        where: {
            skillId: skill.id,
        },
    });
    t.is(publishedUserAgreements.length, 1);
    t.deepEqual(
        pick(draftUserAgreement, userAgreementFields),
        pick(publishedUserAgreements[0], userAgreementFields),
    );
});
