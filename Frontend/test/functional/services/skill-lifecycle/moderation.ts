/* eslint-disable */
import test from 'ava';
import { createUser, createSkill, createImageForSkill, wipeDatabase, createMarketDevice } from '../../_helpers';
import { Channel } from '../../../../db/tables/settings';
import { DraftStatus } from '../../../../db/tables/draft';
import { createModerationTicket, updateModerationTicket } from '../../../../services/skill-lifecycle';
import { getDeviceUrlsForModeration, createMarketDeviceUrl } from '../../../../services/market/utils';
import { findUserSkillWithIdForModeration } from '../../../../db/entities';

test.beforeEach(async t => {
    await wipeDatabase();
});

const createModerationTicketFixture = async() => {
    await createUser();

    const skill = await createSkill({
        channel: Channel.SmartHome,
        onAir: false,
        hideInStore: false,
        isTrustedSmartHomeSkill: true,
    });

    skill.logoId = (await createImageForSkill(skill)).id;
    await skill.save();
    skill.draft.logoId = (await createImageForSkill(skill)).id;
    await skill.draft.save();
    await skill.draft.update({
        name: 'Тестовый навык УД (by emvolkov)',
        status: DraftStatus.ReviewRequested,
        hideInStore: false,
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill',
            developerName: 'emvolkov',
            email: 'emvolkov@yandex-team.ru',
            brandVerificationWebsite: 'https://ya.ru',
            description: 'Тестовый навык',
        },
        backendSettings: {
            uri: 'https://localhost/',
        },
        noteForModerator: 'Заметка для модератора',
    });
    await skill.draft.save();

    return {
        skill,
        response: await createModerationTicket(skill),
    };
};

/**
 * Это по сути интеграционные тесты. Нужно подумать как есть сделать правильно.
 * Проверил один раз - работает. Но бомбить ST тикетами просто так не хорошо.
 * Нужно по идее ходить в тестинг ST, но сейчас нет времени.
 */
test.skip('create moderation ticket', async t => {
    const { response } = await createModerationTicketFixture();

    t.true(response.indexOf('SUPSKILL') > -1);
});

test.skip('update moderation ticket. cancel review', async t => {
    const { skill } = await createModerationTicketFixture();

    await updateModerationTicket(skill, 'cancel', 'Описание ошибки');

    t.true(true); // нет эксепшна - значит всё ок
});

test.skip('update moderation ticket. approve review', async t => {
    const { skill } = await createModerationTicketFixture();

    await updateModerationTicket(skill, 'approve');

    t.true(true); // нет эксепшна - значит всё ок
});

test.skip('update moderation ticket. test required', async t => {
    const { skill } = await createModerationTicketFixture();

    await updateModerationTicket(skill, 'requireTest');

    t.true(true); // нет эксепшна - значит всё ок
});

test('Get updated device ids without old', async t => {
    const user = await createUser();
    let skill = await createSkill({ channel: Channel.SmartHome });
    await createMarketDevice({
        skillId: skill.id,
        marketId: '123',
    });
    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '123',
        },
        { isDraft: true },
    );
    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '234',
        },
        { isDraft: true },
    );
    // Заново ищу скилл со всеми устройствами
    skill = await findUserSkillWithIdForModeration(skill.id, user);

    const urls = getDeviceUrlsForModeration(skill);

    t.deepEqual(urls, [createMarketDeviceUrl('234')]);
});

test('Get updated device ids without removed', async t => {
    const user = await createUser();
    let skill = await createSkill({ channel: Channel.SmartHome });
    await createMarketDevice({
        skillId: skill.id,
        marketId: '123',
    });
    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '234',
        },
        { isDraft: true },
    );
    // Заново ищу скилл со всеми устройствами
    skill = await findUserSkillWithIdForModeration(skill.id, user);

    const urls = getDeviceUrlsForModeration(skill);

    t.deepEqual(urls, [createMarketDeviceUrl('234')]);
});

test('Get all device when there was no old', async t => {
    const user = await createUser();
    let skill = await createSkill({ channel: Channel.SmartHome });
    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '123',
        },
        { isDraft: true },
    );
    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '234',
        },
        { isDraft: true },
    );
    // Заново ищу скилл со всеми устройствами
    skill = await findUserSkillWithIdForModeration(skill.id, user);

    const urls = getDeviceUrlsForModeration(skill);

    t.deepEqual(urls, [createMarketDeviceUrl('123'), createMarketDeviceUrl('234')]);
});

test('Get non empty list for private draft', async t => {
    const user = await createUser();
    let skill = await createSkill({
        channel: Channel.SmartHome,
        hideInStore: true,
    });
    await createMarketDevice({
        skillId: skill.id,
        marketId: '123',
    });
    await createMarketDevice(
        {
            skillId: skill.id,
            marketId: '234',
        },
        { isDraft: true },
    );
    // Заново ищу скилл со всеми устройствами
    skill = await findUserSkillWithIdForModeration(skill.id, user);

    const urls = getDeviceUrlsForModeration(skill);

    t.deepEqual(urls, [createMarketDeviceUrl('234')]);
});
