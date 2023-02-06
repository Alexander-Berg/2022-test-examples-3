/* eslint-disable */
import test from 'ava';
import { chatForPublication } from '../../../services/organizationChatApi/serialize';

const testUUID = '444a7550-59dc-4825-bdbf-be3d2daf791e';
const makeSkillStub = (backendSettings: any, publishingSettings: any): any => {
    return {
        id: testUUID,
        draft: {
            name: 'test skill',
            logo2: {
                url: '',
            },
            publishingSettings: {
                brandVerificationWebsite: '',
                bannerMessage: '',
                ...publishingSettings,
            },
            backendSettings: {
                provider: 'jivosite',
                jivositeId: '123',
                urls: [],
                suggests: [],
                openingHoursType: 'always',
                timezone: 'Europe/Moscow',
                ...backendSettings,
            },
        },
    };
};

test('Serialization should send providerId when provider is not yandex', t => {
    const skill = makeSkillStub(
        {
            provider: 'jivosite',
            jivositeId: '123',
        },
        {},
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.providerChatId, '123');
});

test('Serialization should send skillId as providerId when provider is yandex', t => {
    const skill = makeSkillStub(
        {
            provider: 'yandex',
            jivositeId: '123',
        },
        {},
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.providerChatId, testUUID);
});

test('Serizlization should send selected floyd organization when present', t => {
    const skill = makeSkillStub(
        {
            provider: 'yandex',
            jivositeId: '123',
            floydOrganizationId: '123',
        },
        {},
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.providerChatId, '123');
});

test('Serizlization should send chat id when force create is present', t => {
    const skill = makeSkillStub(
        {
            provider: 'yandex',
            jivositeId: '123',
            floydOrganizationId: '123',
            forceCreateFloydOrganization: true,
        },
        {},
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.providerChatId, testUUID);
});

test('chatForPublication: should include website when verified', async t => {
    const skill = makeSkillStub(
        {},
        {
            brandVerificationWebsite: 'https://example.com',
            brandIsVerified: true,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.brandWebsite, 'https://example.com');
});

test('chatForPublication: should not include website when not verified', async t => {
    const skill = makeSkillStub(
        {},
        {
            brandVerificationWebsite: 'https://example.com',
            brandIsVerified: false,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.brandWebsite, undefined);
});

test('chatForPublication: should include permalink when verified', async t => {
    const skill = makeSkillStub(
        {},
        {
            organizationId: '1234',
            organizationIsVerified: true,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.permalink, '1234');
});

test('chatForPublication: should not include permalink when not verified', async t => {
    const skill = makeSkillStub(
        {},
        {
            organizationId: '1234',
            organizationIsVerified: false,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.permalink, undefined);
});

test('chatForPublication: should include nothing', async t => {
    const skill = await makeSkillStub(
        {},
        {
            organizationId: '1234',
            organizationIsVerified: false,
            brandVerificationWebsite: 'https://example.com',
            brandIsVerified: false,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.permalink, undefined);
    t.is(serializedSkill.brandWebsite, undefined);
});

test('chatForPublication: should include website an not include permalink', async t => {
    const skill = await makeSkillStub(
        {},
        {
            organizationId: '1234',
            organizationIsVerified: false,
            brandVerificationWebsite: 'https://example.com',
            brandIsVerified: true,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.permalink, undefined);
    t.is(serializedSkill.brandWebsite, 'https://example.com');
});

test('chatForPublication: should include permalink an not include website', async t => {
    const skill = await makeSkillStub(
        {},
        {
            organizationId: '1234',
            organizationIsVerified: true,
            brandVerificationWebsite: 'https://example.com',
            brandIsVerified: false,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.brandWebsite, undefined);
    t.is(serializedSkill.permalink, '1234');
});

test('chatForPublication: should include all', async t => {
    const skill = await makeSkillStub(
        {},
        {
            organizationId: '1234',
            organizationIsVerified: true,
            brandVerificationWebsite: 'https://example.com',
            brandIsVerified: true,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.is(serializedSkill.brandWebsite, 'https://example.com');
    t.is(serializedSkill.permalink, '1234');
});

test('chatForPublication: should include useChatOnMarket when useWithYandexMarket is present 1', async t => {
    const skill = await makeSkillStub(
        {},
        {
            useWithYandexMarket: true,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.true(serializedSkill.useChatOnMarket);
});

test('chatForPublication: should include useChatOnMarket when useWithYandexMarket is present 2', async t => {
    const skill = await makeSkillStub(
        {},
        {
            useWithYandexMarket: false,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.false(serializedSkill.useChatOnMarket);
});

test('chatForPublication: should include useChatOnMarket = false when useWithYandexMarket is not present', async t => {
    const skill = await makeSkillStub({}, {});

    const serializedSkill = chatForPublication(skill);

    t.false(serializedSkill.useChatOnMarket);
});

test('chatForPublication: should include hasSerpButton when useWithYandexSearch is present 1', async t => {
    const skill = await makeSkillStub(
        {},
        {
            useWithYandexSearch: true,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.true(serializedSkill.hasSerpButton);
});

test('chatForPublication: should include hasSerpButton when useWithYandexSearch is present 2', async t => {
    const skill = await makeSkillStub(
        {},
        {
            useWithYandexSearch: false,
        },
    );

    const serializedSkill = chatForPublication(skill);

    t.false(serializedSkill.hasSerpButton);
});

test('chatForPublication: should include hasSerpButton = false when useWithYandexSearch is not present', async t => {
    const skill = await makeSkillStub({}, {});

    const serializedSkill = chatForPublication(skill);

    t.true(serializedSkill.hasSerpButton);
});
