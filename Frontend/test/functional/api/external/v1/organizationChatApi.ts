/* eslint-disable */
import test from 'ava';
import * as nock from 'nock';
import { Channel } from '../../../../../db/tables/settings';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi } from './_helpers';
import {
    approveReview,
    requestReview,
    requestDeploy,
    completeDeploy,
    stop,
    markDeleted,
} from '../../../../../services/skill-lifecycle';

function stripRequestId(body: string) {
    try {
        const parsedBody = JSON.parse(body);

        delete parsedBody.id;

        return JSON.stringify(parsedBody);
    } catch (e) {
        return body;
    }
}

test.beforeEach(async() => {
    await wipeDatabase();
});

/* TODO: проверять, что в nock пробрасываются заголовки из makeApiRequest:
 * X-Ya-Service-Ticket,
 * X-Ya-User-Ticket
 * X-Real-IP
 * X-Request-Id
 */
test.skip('everything', async t => {
    const user = await createUser();
    const skill = await createSkill({
        channel: Channel.OrganizationChat,
        name: 'skill name',
        logo: 'skill logo',
        backendSettings: {
            urls: ['mask 1', 'mask 2', 'mask 3'],
            provider: 'jivosite',
            jivositeId: 'jivosite-id',
            suggests: ['suggest', 'suggest', 'suggest'],
            openingHoursType: 'schedule',
            timezone: '219',
            openingHours: {
                Monday: { from: '10:00', to: '18:00' },
                Tuesday: { from: '10:00', to: '18:00' },
                Wednesday: { from: '10:00', to: '18:00' },
                Thursday: { from: '10:00', to: '18:00' },
                Friday: { from: '10:00', to: '18:00' },
                Saturday: null,
                Sunday: null,
            },
        },
        publishingSettings: {
            brandVerificationWebsite: 'brand website',
            bannerMessage: 'banner message',
        },
    });

    const publishOrganizationChatRequest = nock('https://chat.ws.test.common.yandex.ru')
        .filteringRequestBody(stripRequestId)
        .post('/dialogs/jsonrpc/', {
            method: 'publishOrganizationChat',
            jsonrpc: '2.0',
            params: [
                {
                    id: skill.id,
                    name: 'skill name',
                    logo: 'skill logo',
                    brandWebsite: 'brand website',
                    providerId: 'jivosite',
                    providerChatId: 'jivosite-id',
                    urls: ['mask 1', 'mask 2', 'mask 3'],
                    welcomeMessage: 'banner message',
                    suggests: ['suggest', 'suggest', 'suggest'],
                    openingHours: {
                        monday: { from: '10:00+03:00', to: '18:00+03:00' },
                        tuesday: { from: '10:00+03:00', to: '18:00+03:00' },
                        wednesday: { from: '10:00+03:00', to: '18:00+03:00' },
                        thursday: { from: '10:00+03:00', to: '18:00+03:00' },
                        friday: { from: '10:00+03:00', to: '18:00+03:00' },
                        saturday: null,
                        sunday: null,
                    },
                },
            ],
        })
        .reply(200, {});

    const stopOrganizationChatRequest = nock('https://chat.ws.test.common.yandex.ru')
        .filteringRequestBody(stripRequestId)
        .post('/dialogs/jsonrpc/', {
            method: 'stopOrganizationChat',
            jsonrpc: '2.0',
            params: [skill.id],
        })
        .reply(200, {});

    const deleteOrganizationChatRequest = nock('https://chat.ws.test.common.yandex.ru')
        .filteringRequestBody(stripRequestId)
        .post('/dialogs/jsonrpc/', {
            method: 'deleteOrganizationChat',
            jsonrpc: '2.0',
            params: [skill.id],
        })
        .reply(200, {});

    // Send skill to deploy
    await requestReview(skill, { user });
    await approveReview(skill, { user });
    await requestDeploy(skill, { user });

    // Check wm is notified
    t.true(publishOrganizationChatRequest.isDone());

    // Approve deploy from wm
    await callApi('confirmOrganizationChatPublication', [skill.id]);

    // Check skill state
    await skill.reload();
    t.true(skill.onAir);

    // Stop skill
    await stop(skill, { user, notifyFeedbackPlatform: false, comment: '' });

    // Check wm is notified
    t.true(stopOrganizationChatRequest.isDone());

    // Delete skill
    await markDeleted(skill);

    // Check wm is notified
    t.true(deleteOrganizationChatRequest.isDone());

    // Send skill to deploy
    const skill2 = await createSkill({
        channel: Channel.OrganizationChat,
        name: 'skill name',
        logo: 'skill logo',
        backendSettings: {
            urls: ['mask 1', 'mask 2', 'mask 3'],
            provider: 'jivosite',
            jivositeId: 'jivosite-id',
            suggests: ['suggest', 'suggest', 'suggest'],
            openingHoursType: 'always',
            timezone: undefined,
            openingHours: undefined,
        },
        publishingSettings: {
            brandVerificationWebsite: 'brand website',
            bannerMessage: 'banner message',
        },
    });

    const publishOrganizationChatRequest2 = nock('https://chat.ws.test.common.yandex.ru')
        .filteringRequestBody(stripRequestId)
        .post('/dialogs/jsonrpc/', {
            method: 'publishOrganizationChat',
            jsonrpc: '2.0',
            params: [
                {
                    id: skill2.id,
                    name: 'skill name',
                    logo: 'skill logo',
                    brandWebsite: 'brand website',
                    providerId: 'jivosite',
                    providerChatId: 'jivosite-id',
                    urls: ['mask 1', 'mask 2', 'mask 3'],
                    welcomeMessage: 'banner message',
                    suggests: ['suggest', 'suggest', 'suggest'],
                    openingHours: {
                        monday: { from: '00:00+00:00', to: '24:00+00:00' },
                        tuesday: { from: '00:00+00:00', to: '24:00+00:00' },
                        wednesday: { from: '00:00+00:00', to: '24:00+00:00' },
                        thursday: { from: '00:00+00:00', to: '24:00+00:00' },
                        friday: { from: '00:00+00:00', to: '24:00+00:00' },
                        saturday: { from: '00:00+00:00', to: '24:00+00:00' },
                        sunday: { from: '00:00+00:00', to: '24:00+00:00' },
                    },
                },
            ],
        })
        .reply(200, {});

    await requestReview(skill2, { user });
    await approveReview(skill2, { user });
    await requestDeploy(skill2, { user });

    // Check wm is notified
    t.true(publishOrganizationChatRequest2.isDone());

    // Reject skill from wm
    await callApi('rejectOrganizationChatPublication', [skill2.id, 'rejection reason']);

    // Check skill state
    await skill2.reload();
    t.false(skill2.onAir);
    t.is(skill2.draft.status, 'deployRejected');

    // Send skill to deploy
    await completeDeploy(skill2);
    t.true(skill2.onAir);

    // Stop skill from wm
    await callApi('stopOrganizationChat', [skill2.id, 'stoppage reason']);

    // Check skill state
    await skill2.reload();
    t.false(skill2.onAir);
});
