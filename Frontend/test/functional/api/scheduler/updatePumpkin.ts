/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { ImageType } from '../../../../db/tables/image';
import { createSkill, createUser, wipeDatabase, createImage } from '../../_helpers';
import { callApi } from './_helpers';
import * as s3 from '../../../../services/s3';
import { approveReview, requestReview, requestDeploy, completeDeploy } from '../../../../services/skill-lifecycle';

const test = anyTest as TestInterface<{ clock: sinon.SinonFakeTimers }>;

test.beforeEach(wipeDatabase);

test('test update pumpkin', async t => {
    sinon.stub(s3, 'uploadJSON').value(() => {});

    const user = await createUser();
    const skill = await createSkill();
    const image = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    await skill.update({ logoId: image.id });

    await requestReview(skill, { user });
    await approveReview(skill, { user, comment: '' });
    await requestDeploy(skill, { user });
    await completeDeploy(skill);

    const res = await callApi('/skills/updatePumpkin');
    t.deepEqual(res.body, {});
    t.truthy(res.status === 200);

    sinon.restore();
});
