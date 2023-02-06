/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { callApi, createStoreSkillWithLogo, respondsWithResult, respondsWithError } from './_helpers';
import { wipeDatabase, createUser } from '../../_helpers';
import { getUserTicket, testUser } from '../_helpers';
import { createSkillUserShare, getSharedSkills, getSkillUserShare } from '../../../../db/entities/skillUserShare';
import {
    createOrUpdatePublicShare,
    createOneTimeShare,
    isShareExpired,
    invalidateShare,
} from '../../../../db/entities/shares';
import { serializeSkillForCatalog } from '../../../../serializers/skills';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

const sharedSkillsRoute = '/personal/shared-skills';
const sharesRoute = (shareKey: string) => `/personal/shares/${shareKey}/apply`;

test('sharedSkills: should get shared skills', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });

    await createSkillUserShare({ for: testUser.uid, with: skill.id });

    const res = await callApi('get', sharedSkillsRoute, t.context);

    t.true(res.body.result.map(({ id }: any) => id).includes(skill.id));
});

test('sharedSkills: should get shared skills only', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });
    const nonSharedSkill = await createStoreSkillWithLogo({
        publishingSettings: {},
        backendSettings: {},
        userId: '0001',
    });

    await createSkillUserShare({ for: testUser.uid, with: skill.id });

    const res = await callApi('get', sharedSkillsRoute, t.context);

    t.false(res.body.result.map(({ id }: any) => id).includes(nonSharedSkill.id));
});

test('sharedSkills: should delete share', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });

    await createSkillUserShare({ for: testUser.uid, with: skill.id });

    const res = await callApi('delete', `${sharedSkillsRoute}/${skill.id}`, t.context);

    const sharedSkills = await getSharedSkills({ for: testUser.uid });

    t.true(res.ok);
    t.is(sharedSkills.length, 0);
});

test('sharedSkills: should not delete non-existing share and reply with 200', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });

    const res = await callApi('delete', `${sharedSkillsRoute}/${skill.id}`, t.context);

    const sharedSkills = await getSharedSkills({ for: testUser.uid });

    t.true(res.ok);
    t.is(sharedSkills.length, 0);
});

test('sharedSkills: apply share', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });

    await createOrUpdatePublicShare({ skillId: skill.id });

    await skill.reload();

    const res = await callApi('post', sharesRoute(skill.publicShareKey!), t.context);

    const userShare = await getSkillUserShare({ for: testUser.uid, with: skill.id });

    t.not(userShare, null);

    respondsWithResult(
        {
            result: {
                status: 'CREATED',
                skill: JSON.parse(JSON.stringify(serializeSkillForCatalog(skill))),
            },
        },
        res,
        t,
        201,
    );
});

test('sharedSkills: not apply existing share', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });

    await createOrUpdatePublicShare({ skillId: skill.id });

    await skill.reload();

    const res = await callApi('post', sharesRoute(skill.publicShareKey!), t.context);

    const userShare = await getSkillUserShare({ for: testUser.uid, with: skill.id });

    t.true(res.ok);
    t.not(userShare, null);

    const res2 = await callApi('post', sharesRoute(skill.publicShareKey!), t.context);

    respondsWithResult(
        {
            result: {
                status: 'EXISTS',
                skill: JSON.parse(JSON.stringify(serializeSkillForCatalog(skill))),
            },
        },
        res2,
        t,
        201,
    );
});

test('sharedSkills: apply onetime share', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });

    const onetimeShare = await createOneTimeShare({ skillId: skill.id });

    const res = await callApi('post', sharesRoute(onetimeShare.id), t.context);

    const userShare = await getSkillUserShare({ for: testUser.uid, with: skill.id });

    t.not(userShare, null);
    respondsWithResult(
        {
            result: {
                status: 'CREATED',
                skill: JSON.parse(JSON.stringify(serializeSkillForCatalog(skill))),
            },
        },
        res,
        t,
        201,
    );

    await onetimeShare.reload();

    t.true(isShareExpired(onetimeShare));
});

test('sharedSkills: not apply existing onetime share', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });

    const onetimeShare = await createOneTimeShare({ skillId: skill.id });

    const res = await callApi('post', sharesRoute(onetimeShare.id), t.context);

    const userShare = await getSkillUserShare({ for: testUser.uid, with: skill.id });

    t.not(userShare, null);
    t.true(res.ok);

    const res2 = await callApi('post', sharesRoute(onetimeShare.id), t.context);

    respondsWithResult(
        {
            result: {
                status: 'EXISTS',
                skill: JSON.parse(JSON.stringify(serializeSkillForCatalog(skill))),
            },
        },
        res2,
        t,
        201,
    );

    await onetimeShare.reload();

    t.true(isShareExpired(onetimeShare));
});

test('sharedSkills: not apply expired', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });

    const onetimeShare = await createOneTimeShare({ skillId: skill.id });
    await invalidateShare({ key: onetimeShare.id });

    const res = await callApi('post', sharesRoute(onetimeShare.id), t.context);

    respondsWithError(403, 'Expired share key', res, t);
});

test('sharedSkills: check share invalidation', async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0001' });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {}, userId: '0001' });

    const onetimeShare = await createOneTimeShare({ skillId: skill.id });

    await callApi('post', sharesRoute(onetimeShare.id), t.context);

    const userShare = await getSkillUserShare({ for: testUser.uid, with: skill.id });

    t.not(userShare, null);

    await onetimeShare.reload();

    t.true(isShareExpired(onetimeShare));
});
