/* eslint-disable */
import test from 'ava';
import { createImageForSkill, createSkill, createUser, wipeDatabase } from '../../_helpers';
import { Channel } from '../../../../db/tables/settings';
import { findDeployedSkills } from '../../../../db/entities';

test.beforeEach(async() => {
    await wipeDatabase();
});

test('findDeployedSkills with all channels', async t => {
    await createUser();

    const customSkill = await createSkill({
        channel: Channel.AliceSkill,
        onAir: true,
    });
    customSkill.logoId = (await createImageForSkill(customSkill)).id;
    await customSkill.save();

    const organizationChatSkill = await createSkill({
        channel: Channel.OrganizationChat,
        onAir: true,
    });
    organizationChatSkill.logoId = (await createImageForSkill(organizationChatSkill)).id;
    await organizationChatSkill.save();

    const smartHomeSkill = await createSkill({
        channel: Channel.SmartHome,
        onAir: true,
    });
    smartHomeSkill.logoId = (await createImageForSkill(smartHomeSkill)).id;
    await smartHomeSkill.save();

    let skills = await findDeployedSkills({
        includeLogo: false,
        channels: [Channel.AliceSkill, Channel.OrganizationChat, Channel.SmartHome],
    });
    skills = skills.sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime());

    t.is(skills.length, 3);
    t.is(skills[0].channel, Channel.AliceSkill);
    t.is(skills[1].channel, Channel.OrganizationChat);
    t.is(skills[2].channel, Channel.SmartHome);
});

test('findDeployedSkills with AliceSkill and SmartHomeSkills', async t => {
    await createUser();

    const customSkill = await createSkill({
        channel: Channel.AliceSkill,
        onAir: true,
    });
    customSkill.logoId = (await createImageForSkill(customSkill)).id;
    await customSkill.save();

    const organizationChatSkill = await createSkill({
        channel: Channel.OrganizationChat,
        onAir: true,
    });
    organizationChatSkill.logoId = (await createImageForSkill(organizationChatSkill)).id;
    await organizationChatSkill.save();

    const smartHomeSkill = await createSkill({
        channel: Channel.SmartHome,
        onAir: true,
    });
    smartHomeSkill.logoId = (await createImageForSkill(smartHomeSkill)).id;
    await smartHomeSkill.save();

    let skills = await findDeployedSkills({ includeLogo: false, channels: [Channel.AliceSkill, Channel.SmartHome] });
    skills = skills.sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime());

    t.is(skills.length, 2);
    t.is(skills[0].channel, Channel.AliceSkill);
    t.is(skills[1].channel, Channel.SmartHome);
});

test('findDeployedSkills without SmartHomeSkills', async t => {
    await createUser();

    const customSkill = await createSkill({
        channel: Channel.AliceSkill,
        onAir: true,
    });
    customSkill.logoId = (await createImageForSkill(customSkill)).id;
    await customSkill.save();

    const smartHomeSkill = await createSkill({
        channel: Channel.SmartHome,
        onAir: true,
    });
    smartHomeSkill.logoId = (await createImageForSkill(smartHomeSkill)).id;
    await smartHomeSkill.save();

    let skills = await findDeployedSkills({ includeLogo: false, channels: [Channel.AliceSkill] });
    skills = skills.sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime());

    t.is(skills.length, 1);
    t.is(skills[0].channel, Channel.AliceSkill);
});

test('findDeployedSkills. AliceSkill is only public', async t => {
    await createUser();

    const customSkill = await createSkill({
        channel: Channel.AliceSkill,
        onAir: true,
    });
    customSkill.logoId = (await createImageForSkill(customSkill)).id;
    await customSkill.save();

    const organizationChatSkill = await createSkill({
        channel: Channel.OrganizationChat,
        onAir: false,
    });
    organizationChatSkill.logoId = (await createImageForSkill(organizationChatSkill)).id;
    await organizationChatSkill.save();

    const smartHomeSkill = await createSkill({
        channel: Channel.SmartHome,
        onAir: false,
    });
    smartHomeSkill.logoId = (await createImageForSkill(smartHomeSkill)).id;
    await smartHomeSkill.save();

    let skills = await findDeployedSkills({
        includeLogo: false,
        channels: [Channel.AliceSkill, Channel.OrganizationChat, Channel.SmartHome],
    });
    skills = skills.sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime());

    t.is(skills.length, 1);
    t.is(skills[0].channel, Channel.AliceSkill);
});
