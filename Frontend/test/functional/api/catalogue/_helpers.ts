/* eslint-disable */
import { Assertions } from 'ava';
import * as request from 'supertest';
import { makeRestApiCaller, testUser } from '../_helpers';
import { createSkill, createImage } from '../../_helpers';
import { SkillAttributes } from '../../../../db/tables/skill';
import { ImageType } from '../../../../db/tables/image';

export const callApi = makeRestApiCaller('catalogue/v1');

export const respondsWithError = (errorCode: number, errorMessage: string, res: request.Response, t: Assertions) => {
    t.is(res.status, errorCode);
    t.deepEqual(res.body, { error: { message: errorMessage, code: errorCode } });
};

export const respondsWithResult = (result: any, res: request.Response, t: Assertions, status?: number) => {
    t.is(res.status, status ?? 200);
    t.deepEqual(res.body, result);
};

export const createStoreSkill = (props: SkillAttributes = {}) => {
    const { userId = testUser.uid, ...restProps } = props;

    return createSkill({
        userId,
        onAir: true,
        hideInStore: false,
        ...restProps,
    });
};

export const createStoreSkillWithLogo = async(params: SkillAttributes = {}) => {
    const skill = await createStoreSkill(params);
    const image = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    await skill.update({ logoId: image.id, backendSettings: { uri: 'https://example.com/webhook' } });
    await skill.draft.update({ logoId: image.id });
    skill.logo2 = image;

    return skill;
};
