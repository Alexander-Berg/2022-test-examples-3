/* eslint-disable */
import test from 'ava';
import { createSandbox, fake } from 'sinon';
import { wipeDatabase, createUser, createSkill } from '../_helpers';
import * as cycle from '../../../services/skill-lifecycle';
import config from '../../../services/config';
import * as nlu from '../../../services/nlu';
import * as organizationChatApi from '../../../services/organizationChatApi';

test.beforeEach(async() => {
    await wipeDatabase();
});

test('PASKILLS-2429: publishOrganizationChat not called if nlu throws', async t => {
    const sinon = createSandbox();
    const publishOrganizationChat = fake();
    sinon.replace(organizationChatApi, 'publishOrganizationChat', publishOrganizationChat);
    sinon.replace(nlu, 'inflect', fake.rejects('Boom'));
    sinon.replace(config.app, 'inflectActivationPhrases', true);

    const user = await createUser();
    const skill = await createSkill();
    await t.throwsAsync(() => cycle.requestDeploy(skill, { user }));

    t.true(publishOrganizationChat.notCalled);

    sinon.restore();
});
