/* eslint-disable */
import anyTest, { TestInterface, Assertions } from 'ava';
import { getServiceTicket } from '../../_helpers';
import { wipeDatabase, createUser } from '../../../_helpers';
import { Channel, SkillAccess } from '../../../../../db/tables/settings';
import { callApi } from './_helpers';
import { Interface } from '../../../../../services/surface';
import { completeDeploy } from '../../../../../services/skill-lifecycle';
import { createStoreSkillWithLogo } from '../../catalogue/_helpers';

const test = anyTest as TestInterface<{ serviceTicket: string }>;

test.before(async t => {
    const serviceTicket = await getServiceTicket();

    Object.assign(t.context, { serviceTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();

    await createUser();
});

const createHomepageAppropriateTaggedSkill = async(tags: string[], requireScreen?: boolean) => {
    const skill = await createStoreSkillWithLogo({
        userId: '0001',
        channel: Channel.AliceSkill,
        tags,
        skillAccess: SkillAccess.Public,
        requiredInterfaces: requireScreen ? [Interface.Screen] : [],
    });

    await completeDeploy(skill);

    return skill;
};

const retrieveSkills = (res: any) => {
    return res.body.skills;
};

const checkSkillPresense = (skills: any[], id: string) => {
    return skills.map((s: any) => s.id).includes(id);
};

const checkRequiredFields = (skill: any, t: Assertions) => {
    t.true('tags' in skill);
    t.true('editorDescription' in skill);
    t.true('editorName' in skill);
    t.true('homepageBadgeTypes' in skill);
};

test('Should fetch skills by tags', async t => {
    const skill1 = await createHomepageAppropriateTaggedSkill(['test1']);
    const skill2 = await createHomepageAppropriateTaggedSkill(['test2']);

    const res = await callApi('get', '/skills?tags=test1,test2', { serviceTicket: t.context.serviceTicket });

    t.true(res.ok);

    const skills = retrieveSkills(res);

    t.true(checkSkillPresense(skills, skill1.id));
    t.true(checkSkillPresense(skills, skill2.id));

    skills.forEach((skill: any) => {
        checkRequiredFields(skill, t);
    });
});

test('Should not fetch skills by not requested tags', async t => {
    const skill1 = await createHomepageAppropriateTaggedSkill(['test1']);
    const skill2 = await createHomepageAppropriateTaggedSkill(['test2']);

    const res = await callApi('get', '/skills?tags=test1', { serviceTicket: t.context.serviceTicket });

    t.true(res.ok);

    const skills = retrieveSkills(res);

    t.true(checkSkillPresense(skills, skill1.id));
    t.false(checkSkillPresense(skills, skill2.id));

    skills.forEach((skill: any) => {
        checkRequiredFields(skill, t);
    });
});

test('Should fetch skills which has several tags by each of tags 1', async t => {
    const skill = await createHomepageAppropriateTaggedSkill(['test1', 'test2']);

    const res = await callApi('get', '/skills?tags=test1', { serviceTicket: t.context.serviceTicket });
    t.true(res.ok);
    const skills = retrieveSkills(res);

    t.true(checkSkillPresense(skills, skill.id));

    skills.forEach((s: any) => {
        checkRequiredFields(skill, t);
    });
});

test('Should fetch skills which has several tags by each of tags 2', async t => {
    const skill = await createHomepageAppropriateTaggedSkill(['test1', 'test2']);

    const res = await callApi('get', '/skills?tags=test2', { serviceTicket: t.context.serviceTicket });
    t.true(res.ok);
    const skills = retrieveSkills(res);

    t.true(checkSkillPresense(skills, skill.id));

    skills.forEach((s: any) => {
        checkRequiredFields(skill, t);
    });
});

test('Should fetch skills which has several tags by each of tags 3', async t => {
    const skill = await createHomepageAppropriateTaggedSkill(['test1', 'test2']);

    const res = await callApi('get', '/skills?tags=test1,test2', { serviceTicket: t.context.serviceTicket });
    t.true(res.ok);
    const skills = retrieveSkills(res);

    t.true(checkSkillPresense(skills, skill.id));

    skills.forEach((s: any) => {
        checkRequiredFields(skill, t);
    });
});

test('Should fetch skills which has several tags by each of tags 4', async t => {
    const skill = await createHomepageAppropriateTaggedSkill(['test1', 'test2']);

    const res = await callApi('get', '/skills?tags=test2,test1', { serviceTicket: t.context.serviceTicket });
    t.true(res.ok);
    const skills = retrieveSkills(res);

    t.true(checkSkillPresense(skills, skill.id));

    skills.forEach((s: any) => {
        checkRequiredFields(skill, t);
    });
});

test('Should filter by surfaces 1', async t => {
    const skill1 = await createHomepageAppropriateTaggedSkill(['test1']);
    const skill2 = await createHomepageAppropriateTaggedSkill(['test2'], true);

    const res = await callApi('get', '/skills?tags=test1,test2&surfaces=station', {
        serviceTicket: t.context.serviceTicket,
    });

    t.true(res.ok);

    const skills = retrieveSkills(res);

    t.true(checkSkillPresense(skills, skill1.id));
    t.false(checkSkillPresense(skills, skill2.id));

    skills.forEach((skill: any) => {
        checkRequiredFields(skill, t);
    });
});

test('Should filter by surfaces 2', async t => {
    const skill1 = await createHomepageAppropriateTaggedSkill(['test1']);
    const skill2 = await createHomepageAppropriateTaggedSkill(['test2'], true);

    const res = await callApi('get', '/skills?tags=test1,test2&surfaces=station,mobile', {
        serviceTicket: t.context.serviceTicket,
    });

    t.true(res.ok);

    const skills = retrieveSkills(res);

    t.true(checkSkillPresense(skills, skill1.id));
    t.true(checkSkillPresense(skills, skill2.id));

    skills.forEach((skill: any) => {
        checkRequiredFields(skill, t);
    });
});
