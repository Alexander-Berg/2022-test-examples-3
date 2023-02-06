/* eslint-disable */
import anyTest, { TestInterface, ExecutionContext } from 'ava';
import sinon = require('sinon');
import { getServiceTicket } from '../../_helpers';
import { wipeDatabase, createSkill, createUser } from '../../../_helpers';
import { callApi, respondsWithResult, respondsWithError } from './_helpers';
import { OrganizationChatFormSettings } from '../../../../../types/FormSettings';
import { findSkillWithId } from '../../../../../db/entities';
import { DraftStatus, DraftInstance } from '../../../../../db/tables/draft';
import { Channel, SkillAccess } from '../../../../../db/tables/settings';
import { requestReview, requestDeploy } from '../../../../../services/skill-lifecycle';
import * as avatars from '../../../../../services/avatars';
import * as nlu from '../../../../../services/nlu';
import * as webmaster from '../../../../../services/webmaster';
import * as organizationChatApi from '../../../../../services/organizationChatApi';

const test = anyTest as TestInterface<{ serviceTicket: string }>;

test.before(async t => {
    const serviceTicket = await getServiceTicket();

    sinon.replace(
        avatars,
        'uploadImage',
        sinon.fake.resolves({
            meta: '1',
            size: 1,
            url: 'https://via.placeholder.com/1',
        }),
    );

    sinon.replace(nlu, 'inflect', sinon.fake.resolves([]));

    Object.assign(t.context, { serviceTicket });
});

test.after(() => {
    sinon.restore();
});

test.beforeEach(() => {
    sinon.stub(organizationChatApi, 'getOwnedOrganizations').value(async() => []);
    return wipeDatabase();
});

test('Should create draft', async t => {
    const settings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
    };

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
    });

    t.truthy(res.body.skillId);
    t.false(res.body.isAllowedForDeploy);

    const skill = await findSkillWithId(res.body.skillId);

    checkSettings(settings, skill.draft, t);
});

test('Should create draft with valid settings 1', async t => {
    const settings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        bannerMessage: 'Message',
        brandVerificationWebsite: 'https://test.com',
        category: 'utilities',
        developerName: 'Name',
        email: 'test@test.com',
        explicitContent: false,
        provider: 'yandex',
        openingHoursType: 'always',
        suggests: ['Test', 'Test'],
        urls: ['https://test.com'],
        useWithYandexMarket: false,
        hideInStore: true,
    };

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    checkSettings(settings, skill.draft, t);
    t.is(skill.draft.skillAccess, SkillAccess.Hidden);
    t.true(skill.draft.backendSettings.forceCreateFloydOrganization);
});

test('Should create draft with valid settings 1.1', async t => {
    sinon.stub(organizationChatApi, 'getOwnedOrganizations').value(async() => [
        {
            name: 'Name',
            id: '1',
        },
    ]);

    const settings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        bannerMessage: 'Message',
        brandVerificationWebsite: 'https://test.com',
        category: 'utilities',
        developerName: 'Name',
        email: 'test@test.com',
        explicitContent: false,
        provider: 'yandex',
        openingHoursType: 'always',
        suggests: ['Test', 'Test'],
        urls: ['https://test.com'],
        useWithYandexMarket: false,
    };

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.falsy(skill.draft.backendSettings.forceCreateFloydOrganization);
    checkSettings(settings, skill.draft, t);
});

test('Should create draft with valid settings 2', async t => {
    const settings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        bannerMessage: 'Message',
        organizationId: '111',
        category: 'utilities',
        developerName: 'Name',
        email: 'test@test.com',
        explicitContent: false,
        provider: 'yandex',
        openingHoursType: 'always',
        suggests: ['Test', 'Test'],
        urls: ['https://test.com'],
        useWithYandexMarket: false,
    };

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    checkSettings(settings, skill.draft, t);
});

test('Should create draft with valid settings 3', async t => {
    const settings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        bannerMessage: 'Message',
        organizationId: '111',
        brandVerificationWebsite: 'https://test.com',
        category: 'utilities',
        developerName: 'Name',
        email: 'test@test.com',
        explicitContent: false,
        provider: 'jivosite',
        openingHoursType: 'always',
        suggests: ['Test', 'Test'],
        urls: ['https://test.com'],
        useWithYandexMarket: false,
        jivositeId: '111',
    };

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    checkSettings(settings, skill.draft, t);
});

test('Should create draft with valid settings 4', async t => {
    const settings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        bannerMessage: 'Message',
        organizationId: '111',
        brandVerificationWebsite: 'https://test.com',
        category: 'utilities',
        developerName: 'Name',
        email: 'test@test.com',
        explicitContent: false,
        provider: 'jivosite',
        openingHoursType: 'schedule',
        timezone: 'Europe/Moscow',
        openingHours: {
            Monday: null,
            Tuesday: null,
            Wednesday: null,
            Thursday: null,
            Friday: null,
            Saturday: null,
            Sunday: { from: '10:00', to: '11:00' },
        },
        suggests: ['Test', 'Test'],
        urls: ['https://test.com'],
        useWithYandexMarket: false,
        jivositeId: '111',
    };

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    checkSettings(settings, skill.draft, t);
});

test('Should respond with error on invalid settings 1', async t => {
    const settings: Partial<OrganizationChatFormSettings> = {
        name: 'Яндекс',
    };

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    t.is(res.status, 400);
    t.truthy(res.body.error.fields.name);
    t.is(res.body.error.message, 'Validation error');
});

test('Should respond with error on invalid settings 2', async t => {
    const settings: Partial<OrganizationChatFormSettings> = {
        brandVerificationWebsite: 'not-a-website',
    };

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    t.is(res.status, 400);
    t.truthy(res.body.error.fields.brandVerificationWebsite);
    t.is(res.body.error.message, 'Validation error');
});

test('Should get draft status', async t => {
    await createUser();
    const skill = await createSkill({ name: 'Test', channel: Channel.OrganizationChat });

    const res = await callApi('get', '/chats/' + skill.id + '/draft/status', {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithResult(
        {
            status: DraftStatus.InDevelopment,
            isAllowedForDeploy: false,
            settings: {
                name: 'Test',
            },
            uid: '0001',
        },
        res,
        t,
    );
});

test('Status should serialize settings correctly 1', async t => {
    const settings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        bannerMessage: 'Message',
        organizationId: '111',
        brandVerificationWebsite: 'https://test.com',
        category: 'utilities',
        developerName: 'Name',
        email: 'test@test.com',
        explicitContent: false,
        provider: 'jivosite',
        openingHoursType: 'schedule',
        timezone: 'Europe/Moscow',
        openingHours: {
            Monday: null,
            Tuesday: null,
            Wednesday: null,
            Thursday: null,
            Friday: null,
            Saturday: null,
            Sunday: { from: '10:00', to: '11:00' },
        },
        suggests: ['Test', 'Test'],
        urls: ['https://test.com'],
        useWithYandexMarket: false,
        jivositeId: '111',
    };

    const res1 = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
    });

    const skill = await findSkillWithId(res1.body.skillId);

    const res2 = await callApi('get', '/chats/' + skill.id + '/draft/status', {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithResult(
        {
            status: DraftStatus.InDevelopment,
            isAllowedForDeploy: false,
            settings,
            uid: '1',
        },
        res2,
        t,
    );
});

test('Status should serialize settings correctly 2', async t => {
    const settings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        bannerMessage: 'Message',
        organizationId: '111',
        brandVerificationWebsite: 'https://test.com',
        category: 'utilities',
        developerName: 'Name',
        email: 'test@test.com',
        explicitContent: false,
        provider: 'jivosite',
        openingHoursType: 'schedule',
        timezone: 'Europe/Moscow',
        openingHours: {
            Monday: null,
            Tuesday: null,
            Wednesday: null,
            Thursday: null,
            Friday: null,
            Saturday: null,
            Sunday: { from: '10:00', to: '11:00' },
        },
        suggests: ['Test', 'Test'],
        urls: ['https://test.com'],
        useWithYandexMarket: false,
        jivositeId: '111',
    };

    const res1 = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    const skill = await findSkillWithId(res1.body.skillId);

    const res2 = await callApi('get', '/chats/' + skill.id + '/draft/status', {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithResult(
        {
            status: DraftStatus.InDevelopment,
            isAllowedForDeploy: true,
            settings: {
                ...settings,
                logoId: skill.logo2.id,
            },
            uid: '1',
        },
        res2,
        t,
    );
});

test('Should delete chat', async t => {
    await createUser();
    const skill = await createSkill({ name: 'Test', channel: Channel.OrganizationChat });

    const res = await callApi('delete', '/chats/' + skill.id, {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithResult('ok', res, t);
});

test('Should not deploy chat when on moderation', async t => {
    const user = await createUser();
    const skill = await createSkill({ name: 'Test', channel: Channel.OrganizationChat });

    await requestReview(skill, { user });

    const res = await callApi('post', '/chats/' + skill.id + '/draft/actions/deploy', {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithError(409, 'Chat is already publishing or sent on moderation', res, t);
});

test('Should not deploy chat when is deploying', async t => {
    const user = await createUser();
    const skill = await createSkill({ name: 'Test', channel: Channel.OrganizationChat });

    await requestDeploy(skill, { user });

    const res = await callApi('post', '/chats/' + skill.id + '/draft/actions/deploy', {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithError(409, 'Chat is already publishing or sent on moderation', res, t);
});

test('Should patch draft correctly', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        bannerMessage: 'Message',
        organizationId: '111',
        brandVerificationWebsite: 'https://test.com',
        category: 'utilities',
        developerName: 'Name',
        email: 'test@test.com',
        explicitContent: false,
        provider: 'jivosite',
        openingHoursType: 'schedule',
        timezone: 'Europe/Moscow',
        openingHours: {
            Monday: null,
            Tuesday: null,
            Wednesday: null,
            Thursday: null,
            Friday: null,
            Saturday: null,
            Sunday: { from: '10:00', to: '11:00' },
        },
        suggests: ['Test', 'Test'],
        urls: ['https://test.com'],
        useWithYandexMarket: false,
        jivositeId: '111',
    };

    const res1 = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });
    const skill = await findSkillWithId(res1.body.skillId);

    t.false(skill.draft.hideInStore);
    t.is(skill.draft.skillAccess, SkillAccess.Public);

    const changedSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test 1',
        bannerMessage: 'Message 1',
        organizationId: '1111',
        brandVerificationWebsite: 'https://test.com/test',
        category: 'communication',
        developerName: 'Name1',
        email: 'test1@test.com',
        explicitContent: false,
        provider: 'verbox',
        openingHoursType: 'always',
        suggests: ['Test', 'Test', 'Test'],
        urls: ['https://test.com/test', 'https://test.com/test1'],
        useWithYandexMarket: true,
        jivositeId: '1111',
        hideInStore: true,
    };

    await callApi('patch', '/chats/' + res1.body.skillId + '/draft', { serviceTicket: t.context.serviceTicket }).send({
        settings: changedSettings,
    });

    await skill.draft.reload();

    t.true(skill.draft.hideInStore);
    t.is(skill.draft.skillAccess, SkillAccess.Hidden);

    checkSettings(changedSettings, skill.draft, t);
});

test('Should patch draft correctly when flag changes', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        explicitContent: true,
    };

    const res1 = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    const changedSettings: Partial<OrganizationChatFormSettings> = {
        explicitContent: false,
    };

    await callApi('patch', '/chats/' + res1.body.skillId + '/draft', { serviceTicket: t.context.serviceTicket }).send({
        settings: changedSettings,
    });

    const skill = await findSkillWithId(res1.body.skillId);

    checkSettings(changedSettings, skill.draft, t);
});

test('Should change logo correctly', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
    };

    const res1 = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
        logoUrl: 'https://via.placeholder.com/1',
    });

    const skill = await findSkillWithId(res1.body.skillId);

    const savedLogoId = skill.draft.logoId;

    await callApi('patch', '/chats/' + skill.id + '/draft', { serviceTicket: t.context.serviceTicket }).send({
        logoUrl: 'https://via.placeholder.com/1',
    });

    await skill.reload();

    t.not(savedLogoId, skill.logoId);
});

test('Should not verify url when created', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        brandVerificationWebsite: 'https://test.com',
    };

    const sb = sinon.createSandbox();

    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(false));

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.false(skill.draft.publishingSettings.brandIsVerified);

    sb.restore();
});

test('Should verify url when created', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        brandVerificationWebsite: 'https://test.com',
    };

    const sb = sinon.createSandbox();

    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(true));

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.true(skill.draft.publishingSettings.brandIsVerified);

    sb.restore();
});

test('Should not verify url when patched', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
    };

    const sb = sinon.createSandbox();

    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(false));

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
    });

    await callApi('patch', '/chats/' + res.body.skillId + '/draft', { serviceTicket: t.context.serviceTicket }).send({
        settings: {
            brandVerificationWebsite: 'https://test.com',
        },
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.false(skill.draft.publishingSettings.brandIsVerified);

    sb.restore();
});

test('Should verify url when patched', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
    };

    const sb = sinon.createSandbox();

    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(true));

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
    });

    await callApi('patch', '/chats/' + res.body.skillId + '/draft', { serviceTicket: t.context.serviceTicket }).send({
        settings: {
            brandVerificationWebsite: 'https://test.com',
        },
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.true(skill.draft.publishingSettings.brandIsVerified);

    sb.restore();
});

test('Brand verification should initially be undefined', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
    };

    const sb = sinon.createSandbox();

    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(true));

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.is(skill.draft.publishingSettings.brandIsVerified, undefined);

    sb.restore();
});

test('Should not change brand url verification if not provided 1', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        brandVerificationWebsite: 'https://test.com',
    };

    const sb = sinon.createSandbox();

    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(true));

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
    });

    await callApi('patch', '/chats/' + res.body.skillId + '/draft', { serviceTicket: t.context.serviceTicket }).send({
        settings: {
            name: 'Test 2',
        },
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.true(skill.draft.publishingSettings.brandIsVerified);

    sb.restore();
});

test('Should not change brand url verification if not provided 2', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        brandVerificationWebsite: 'https://test.com',
    };

    const sb = sinon.createSandbox();

    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(false));

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
    });

    await callApi('patch', '/chats/' + res.body.skillId + '/draft', { serviceTicket: t.context.serviceTicket }).send({
        settings: {
            name: 'Test 2',
        },
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.false(skill.draft.publishingSettings.brandIsVerified);

    sb.restore();
});

test('Should reverify brand url when changed 1', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        brandVerificationWebsite: 'https://test.com',
    };

    const sb = sinon.createSandbox();

    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(false));

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.false(skill.draft.publishingSettings.brandIsVerified);

    sb.restore();
    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(true));

    await callApi('patch', '/chats/' + res.body.skillId + '/draft', { serviceTicket: t.context.serviceTicket }).send({
        settings: {
            name: 'Test 2',
            brandVerificationWebsite: 'https://test.ru',
        },
    });

    await skill.reload();

    t.true(skill.draft.publishingSettings.brandIsVerified);

    sb.restore();
});

test('Should unverify when website removed', async t => {
    const initialSettings: Partial<OrganizationChatFormSettings> = {
        name: 'Test',
        brandVerificationWebsite: 'https://test.com',
    };

    const sb = sinon.createSandbox();

    sb.replace(webmaster, 'verifyUrl', sinon.fake.resolves(true));

    const res = await callApi('post', '/chats', { serviceTicket: t.context.serviceTicket }).send({
        settings: initialSettings,
        uid: '1',
    });

    const skill = await findSkillWithId(res.body.skillId);

    t.true(skill.draft.publishingSettings.brandIsVerified);

    await callApi('patch', '/chats/' + res.body.skillId + '/draft', { serviceTicket: t.context.serviceTicket }).send({
        settings: {
            name: 'Test 2',
            brandVerificationWebsite: '',
        },
    });

    await skill.reload();

    t.false(skill.draft.publishingSettings.brandIsVerified);

    sb.restore();
});

const checkSettings = (settings: Partial<OrganizationChatFormSettings>, draft: DraftInstance, t: ExecutionContext) => {
    if (settings.name !== undefined) {
        t.is(draft.name, settings.name);
    }
    if (settings.bannerMessage !== undefined) {
        t.is(draft.publishingSettings.bannerMessage, settings.bannerMessage);
    }
    if (settings.brandVerificationWebsite !== undefined) {
        t.is(draft.publishingSettings.brandVerificationWebsite, settings.brandVerificationWebsite);
        t.true(draft.publishingSettings.brandIsVerified);
    }
    if (settings.category !== undefined) {
        t.is(draft.publishingSettings.category, settings.category);
    }
    if (settings.developerName !== undefined) {
        t.is(draft.publishingSettings.developerName, settings.developerName);
    }
    if (settings.email !== undefined) {
        t.is(draft.publishingSettings.email, settings.email);
        t.true(draft.publishingSettings.emailIsVerified);
    }
    if (settings.explicitContent !== undefined) {
        t.is(draft.publishingSettings.explicitContent, settings.explicitContent);
    }
    if (settings.provider !== undefined) {
        t.is(draft.backendSettings.provider, settings.provider);
    }
    if (settings.jivositeId !== undefined) {
        t.is(draft.backendSettings.jivositeId, settings.jivositeId);
    }
    if (settings.openingHoursType !== undefined) {
        t.is(draft.backendSettings.openingHoursType, settings.openingHoursType);
    }
    if (settings.openingHours !== undefined) {
        t.deepEqual(draft.backendSettings.openingHours, settings.openingHours);
    }
    if (settings.suggests !== undefined) {
        t.deepEqual(draft.backendSettings.suggests, settings.suggests);
    }
    if (settings.timezone !== undefined) {
        t.is(draft.backendSettings.timezone, settings.timezone);
    }
    if (settings.urls !== undefined) {
        t.deepEqual(draft.backendSettings.urls, settings.urls);
    }
    if (settings.organizationId !== undefined) {
        t.is(draft.publishingSettings.organizationId, settings.organizationId);
        t.true(draft.publishingSettings.organizationIsVerified);
    }
    if (settings.hideInStore !== undefined) {
        t.is(draft.hideInStore, settings.hideInStore);
    }
    if (settings.skillAccess !== undefined) {
        t.is(draft.skillAccess, settings.skillAccess);
    }
};
