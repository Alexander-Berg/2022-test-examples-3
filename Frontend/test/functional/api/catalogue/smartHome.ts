import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { Channel, SkillAccess } from '../../../../db/tables/settings';
import typograf from '../../../../services/typograf';
import * as blackboxService from '../../../../services/blackbox';
import * as tvm from '../../../../services/tvm';
import { callApi, respondsWithResult } from './_helpers';
import { getUserTicket, testUser } from '../_helpers';
import { createImageForSkill, createShareForSkill, createSkill, createUser, wipeDatabase } from '../../_helpers';
import { DraftStatus } from '../../../../db/tables/draft';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test.afterEach.always(async() => {
    sinon.restore();
});

test('Должен вернуть публичные и приватные навыки user1', async t => {
    const user1 = await createUser({ id: '001' });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user1.id,
    });

    // --- user1 ---

    const skill11 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 1.1',
        },
    });
    skill11.logoId = (await createImageForSkill(skill11)).id;
    await skill11.save();
    const skill12 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 1.2',
        },
    });
    skill12.logoId = (await createImageForSkill(skill12)).id;
    await skill12.save();

    const res = await callApi('get', '/smart_home/get_native_skills', {
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            result: [
                {
                    id: skill11.id,
                    name: typograf('smart home skill 1.1'),
                    secondary_title: typograf('This is a public smart home skill 1.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill12.id,
                    name: typograf('smart home skill 1.2 (private)'),
                    secondary_title: typograf('This is a private smart home skill 1.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
            ],
        },
        res,
        t,
    );
});

test('Должен вернуть все публичные навыки и приватные навыки user2', async t => {
    const user1 = await createUser({ id: '001' });
    const user2 = await createUser({ id: '002' });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user2.id,
    });

    // --- user1 ---

    const skill11 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 1.1',
        },
    });
    skill11.logoId = (await createImageForSkill(skill11)).id;
    await skill11.save();
    const skill12 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 1.2',
        },
    });
    skill12.logoId = (await createImageForSkill(skill12)).id;
    await skill12.save();

    // --- user2 ---

    const skill21 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.1',
        },
    });
    skill21.logoId = (await createImageForSkill(skill21)).id;
    await skill21.save();
    const skill22 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 2.2',
        },
    });
    skill22.logoId = (await createImageForSkill(skill22)).id;
    await skill22.save();

    const res = await callApi('get', '/smart_home/get_native_skills', {
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            result: [
                {
                    id: skill11.id,
                    name: typograf('smart home skill 1.1'),
                    secondary_title: typograf('This is a public smart home skill 1.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill21.id,
                    name: typograf('smart home skill 2.1'),
                    secondary_title: typograf('This is a public smart home skill 2.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill22.id,
                    name: typograf('smart home skill 2.2 (private)'),
                    secondary_title: typograf('This is a private smart home skill 2.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
            ],
        },
        res,
        t,
    );
});

test('Должен вернуть все публичные навыки и приватные навыки user2 + навык user1 через шаринг', async t => {
    const user1 = await createUser({ id: '001' });
    const user2 = await createUser({ id: '002' });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user2.id,
    });

    // --- user1 ---

    const skill11 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 1.1',
        },
    });
    skill11.logoId = (await createImageForSkill(skill11)).id;
    await skill11.save();
    const skill12 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.2 (private) with sharing',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 1.2',
        },
    });
    skill12.logoId = (await createImageForSkill(skill12)).id;
    await skill12.save();

    // share skill with user1
    await createShareForSkill(skill12, user2);

    // --- user2 ---

    const skill21 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.1',
        },
    });
    skill21.logoId = (await createImageForSkill(skill21)).id;
    await skill21.save();
    const skill22 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 2.2',
        },
    });
    skill22.logoId = (await createImageForSkill(skill22)).id;
    await skill22.save();

    const res = await callApi('get', '/smart_home/get_native_skills', {
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            result: [
                {
                    id: skill11.id,
                    name: typograf('smart home skill 1.1'),
                    secondary_title: typograf('This is a public smart home skill 1.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill12.id,
                    name: typograf('smart home skill 1.2 (private) with sharing'),
                    secondary_title: typograf('This is a private smart home skill 1.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill21.id,
                    name: typograf('smart home skill 2.1'),
                    secondary_title: typograf('This is a public smart home skill 2.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill22.id,
                    name: typograf('smart home skill 2.2 (private)'),
                    secondary_title: typograf('This is a private smart home skill 2.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
            ],
        },
        res,
        t,
    );
});

test('Должен вернуть все публичные навыки и приватные навыки user2 (isYandexoid теперь не влияет никак)', async t => {
    const user1 = await createUser({ id: '001' });
    const user2 = await createUser({ id: '002' });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user2.id,
    });

    sinon.stub(blackboxService, 'isYandexoid').resolves(true);

    // --- user1 ---

    const skill11 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 1.1',
        },
    });
    skill11.logoId = (await createImageForSkill(skill11)).id;
    await skill11.save();
    const skill12 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 1.2',
        },
    });
    skill12.logoId = (await createImageForSkill(skill12)).id;
    await skill12.save();

    // --- user2 ---

    const skill21 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.1',
        },
    });
    skill21.logoId = (await createImageForSkill(skill21)).id;
    await skill21.save();
    const skill22 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 2.2',
        },
    });
    skill22.logoId = (await createImageForSkill(skill22)).id;
    await skill22.save();

    const res = await callApi('get', '/smart_home/get_native_skills', {
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            result: [
                {
                    id: skill11.id,
                    name: typograf('smart home skill 1.1'),
                    secondary_title: typograf('This is a public smart home skill 1.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill21.id,
                    name: typograf('smart home skill 2.1'),
                    secondary_title: typograf('This is a public smart home skill 2.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill22.id,
                    name: typograf('smart home skill 2.2 (private)'),
                    secondary_title: typograf('This is a private smart home skill 2.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
            ],
        },
        res,
        t,
    );
});

/**
 * При удалении фичи (см. ниже) нужно удалить этот тест
 */
test('Должен вернуть все публичные и приватные навыки всех пользователей', async t => {
    const user1 = await createUser({ id: '001' });
    const user2 = await createUser({ id: '002' });
    const user3 = await createUser({ id: '003', featureFlags: { showPrivateAndPublicSmartHomeSkills: true } });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user3.id,
    });

    // --- user1 ---

    const skill11 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 1.1',
        },
    });
    skill11.logoId = (await createImageForSkill(skill11)).id;
    await skill11.save();
    const skill12 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 1.2',
        },
    });
    skill12.logoId = (await createImageForSkill(skill12)).id;
    await skill12.save();

    // --- user2 ---

    const skill21 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.1',
        },
    });
    skill21.logoId = (await createImageForSkill(skill21)).id;
    await skill21.save();
    const skill22 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 2.2',
        },
    });
    skill22.logoId = (await createImageForSkill(skill22)).id;
    await skill22.save();

    const res = await callApi('get', '/smart_home/get_native_skills', {
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            result: [
                {
                    id: skill11.id,
                    name: typograf('smart home skill 1.1'),
                    secondary_title: typograf('This is a public smart home skill 1.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill12.id,
                    name: typograf('smart home skill 1.2 (private)'),
                    secondary_title: typograf('This is a private smart home skill 1.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill21.id,
                    name: typograf('smart home skill 2.1'),
                    secondary_title: typograf('This is a public smart home skill 2.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill22.id,
                    name: typograf('smart home skill 2.2 (private)'),
                    secondary_title: typograf('This is a private smart home skill 2.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
            ],
        },
        res,
        t,
    );
});

/**
 * При удалении фичи (см. ниже) нужно удалить этот тест
 */
test('Должен вернуть все публичные и приватные навыки всех пользователей с флагом showPrivateAndPublicSmartHomeSkills', async t => {
    const user1 = await createUser({ id: '001' });
    const user2 = await createUser({ id: '002' });
    const user3 = await createUser({ id: '003', featureFlags: { showPrivateAndPublicSmartHomeSkills: true } });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user3.id,
    });

    // --- user1 ---

    const skill11 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 1.1',
        },
    });
    skill11.logoId = (await createImageForSkill(skill11)).id;
    await skill11.save();
    const skill12 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 1.2',
        },
    });
    skill12.logoId = (await createImageForSkill(skill12)).id;
    await skill12.save();

    // --- user2 ---

    const skill21 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.1',
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.1',
        },
    });
    skill21.logoId = (await createImageForSkill(skill21)).id;
    await skill21.save();
    const skill22 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 2.2',
        },
    });
    skill22.logoId = (await createImageForSkill(skill22)).id;
    await skill22.save();

    const res = await callApi('get', '/smart_home/get_native_skills', {
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            result: [
                {
                    id: skill11.id,
                    name: typograf('smart home skill 1.1'),
                    secondary_title: typograf('This is a public smart home skill 1.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill12.id,
                    name: typograf('smart home skill 1.2 (private)'),
                    secondary_title: typograf('This is a private smart home skill 1.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill21.id,
                    name: typograf('smart home skill 2.1'),
                    secondary_title: typograf('This is a public smart home skill 2.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: false,
                    averageRating: 0,
                },
                {
                    id: skill22.id,
                    name: typograf('smart home skill 2.2 (private)'),
                    secondary_title: typograf('This is a private smart home skill 2.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
            ],
        },
        res,
        t,
    );
});

test('Должен вернуть публичные и приватные навыки user1 (с флагами trusted и диплинками)', async t => {
    const user1 = await createUser({ id: '001' });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user1.id,
    });

    // --- user1 ---

    const skill11 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.1',
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'This is a trusted public smart home skill 1.1 (with deeplinks)',
        },
    });
    skill11.logoId = (await createImageForSkill(skill11)).id;
    await skill11.save();
    const skill12 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.2 (private)',
        hideInStore: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 1.2',
        },
    });
    skill12.logoId = (await createImageForSkill(skill12)).id;
    await skill12.save();

    const res = await callApi('get', '/smart_home/get_native_skills', {
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            result: [
                {
                    id: skill11.id,
                    name: typograf('smart home skill 1.1'),
                    secondary_title: typograf('This is a trusted public smart home skill 1.1 (with deeplinks)'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: true,
                    averageRating: 0,
                },
                {
                    id: skill12.id,
                    name: typograf('smart home skill 1.2 (private)'),
                    secondary_title: typograf('This is a private smart home skill 1.2'),
                    logo_url: 'http://localhost',
                    private: true,
                    trusted: false,
                    averageRating: 0,
                },
            ],
        },
        res,
        t,
    );
});

test('Должен вернуть публичные навыки и драфты на модерации', async t => {
    const user1 = await createUser({ id: '001', featureFlags: { canTestSmartHomeDrafts: true } });
    const user2 = await createUser({ id: '002' });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user1.id,
    });

    // --- user1 ---

    // В выдаче должна быть только "Опубликованная версия". У драфта не тот статус.
    const skill11 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.1',
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'This is a trusted public smart home skill 1.1',
        },
    });
    skill11.logoId = (await createImageForSkill(skill11)).id;
    await skill11.save();

    // --- user2 ---

    // Ничего не должно быть в выдаче. Приватный навык и у драфта не тот статус.
    const skill21 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: false,
        name: 'smart home skill 2.1 (draft only, not in production, private)',
        hideInStore: true,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 2.1',
        },
    });
    skill21.logoId = (await createImageForSkill(skill21)).id;
    await skill21.save();

    // Должна быть в выдаче только "Опубликованная версия". У Драфта не тот статус.
    const skill22 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.2 (draft only, not in production, public)',
        hideInStore: false,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.2',
        },
    });
    skill22.logoId = (await createImageForSkill(skill22)).id;
    await skill22.save();

    // Должна быть в выдаче "Опубликованная версия" и драфт отличный от неё
    const skill23 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.3 (new draft, in production, public)',
        hideInStore: false,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.3',
        },
    });
    skill23.logoId = (await createImageForSkill(skill23)).id;
    await skill23.save();
    skill23.draft.logoId = (await createImageForSkill(skill23, 'http://localhost/new')).id;
    await skill23.draft.save();
    await skill23.draft.update({
        name: 'smart home skill 2.3a (new draft, in production, public)',
        status: DraftStatus.ReviewRequested,
        hideInStore: false,
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.3a',
        },
    });
    await skill23.draft.save();

    const res = await callApi('get', '/smart_home/get_native_skills', {
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            result: [
                {
                    id: skill11.id,
                    name: typograf('smart home skill 1.1'),
                    secondary_title: typograf('This is a trusted public smart home skill 1.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: true,
                    averageRating: 0,
                },
                {
                    id: skill22.id,
                    name: typograf('smart home skill 2.2 (draft only, not in production, public)'),
                    secondary_title: typograf('This is a public smart home skill 2.2'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: true,
                    averageRating: 0,
                },
                {
                    id: skill23.id,
                    name: typograf('smart home skill 2.3 (new draft, in production, public)'),
                    secondary_title: typograf('This is a public smart home skill 2.3'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: true,
                    averageRating: 0,
                },
                {
                    id: `${skill23.id}-draft`,
                    name: `${typograf('smart home skill 2.3a (new draft, in production, public)')} (draft)`,
                    secondary_title: typograf('This is a public smart home skill 2.3a'),
                    logo_url: 'http://localhost/new',
                    private: false,
                    trusted: true,
                    averageRating: 0,
                },
            ],
        },
        res,
        t,
    );
});

test('Должен вернуть публичные навыки, драфты на модерации нет (нет фичи canTestSmartHomeDrafts)', async t => {
    const user1 = await createUser({ id: '001' });
    const user2 = await createUser({ id: '002' });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user1.id,
    });

    // --- user1 ---

    // В выдаче должна быть только "Опубликованная версия". У драфта не тот статус.
    const skill11 = await createSkill({
        channel: Channel.SmartHome,
        userId: user1.id,
        onAir: true,
        name: 'smart home skill 1.1',
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'This is a trusted public smart home skill 1.1',
        },
    });
    await skill11.update({
        rating: [7, 2, 3, 5, 8],
    });
    skill11.logoId = (await createImageForSkill(skill11)).id;
    await skill11.save();

    // --- user2 ---

    // Ничего не должно быть в выдаче. Приватный навык и у драфта не тот статус.
    const skill21 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: false,
        name: 'smart home skill 2.1 (draft only, not in production, private)',
        hideInStore: true,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'This is a private smart home skill 2.1',
        },
    });
    skill21.logoId = (await createImageForSkill(skill21)).id;
    await skill21.save();

    // Должна быть в выдаче только "Опубликованная версия". У Драфта не тот статус.
    const skill22 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.2 (draft only, not in production, public)',
        hideInStore: false,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.2',
        },
    });
    skill22.logoId = (await createImageForSkill(skill22)).id;
    await skill22.save();

    // Должна быть в выдаче "Опубликованная версия" и драфт отличный от неё
    const skill23 = await createSkill({
        channel: Channel.SmartHome,
        userId: user2.id,
        onAir: true,
        name: 'smart home skill 2.3 (new draft, in production, public)',
        hideInStore: false,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.3',
        },
    });
    await skill23.update({
        rating: [7, 2, 3, 5, 8],
    });
    skill23.logoId = (await createImageForSkill(skill23)).id;
    await skill23.save();
    skill23.draft.logoId = (await createImageForSkill(skill23, 'http://localhost/new')).id;
    await skill23.draft.save();
    await skill23.draft.update({
        name: 'smart home skill 2.3a (new draft, in production, public)',
        status: DraftStatus.ReviewRequested,
        hideInStore: false,
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 2.3a',
        },
    });
    await skill23.draft.save();

    const res = await callApi('get', '/smart_home/get_native_skills', {
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            result: [
                {
                    id: skill11.id,
                    name: typograf('smart home skill 1.1'),
                    secondary_title: typograf('This is a trusted public smart home skill 1.1'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: true,
                    averageRating: 3.2,
                },
                {
                    id: skill22.id,
                    name: typograf('smart home skill 2.2 (draft only, not in production, public)'),
                    secondary_title: typograf('This is a public smart home skill 2.2'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: true,
                    averageRating: 0,
                },
                {
                    id: skill23.id,
                    name: typograf('smart home skill 2.3 (new draft, in production, public)'),
                    secondary_title: typograf('This is a public smart home skill 2.3'),
                    logo_url: 'http://localhost',
                    private: false,
                    trusted: true,
                    averageRating: 3.2,
                },
            ],
        },
        res,
        t,
    );
});

test('Должен вернуть приватные навыки user', async t => {
    const user = await createUser({ id: '001' });

    sinon.stub(tvm, 'checkUserTicket').resolves({
        default_uid: user.id,
    });

    const skill = await createSkill({
        channel: Channel.SmartHome,
        userId: user.id,
        onAir: true,
        name: 'smart home skill 1.1',
        skillAccess: SkillAccess.Private,
        publishingSettings: {
            secondaryTitle: 'This is a public smart home skill 1.1',
        },
    });
    skill.logoId = (await createImageForSkill(skill)).id;
    await skill.save();

    const res = await callApi('get', '/dialogs/private', {
        userTicket: t.context.userTicket,
    });

    t.truthy(res.body.result.items[0].id === skill.id);
});
