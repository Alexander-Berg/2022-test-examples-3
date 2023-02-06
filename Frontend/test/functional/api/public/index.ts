/* eslint-disable */
import * as fs from 'fs';
import { resolve } from 'path';
import { promisify } from 'util';
import { match, SinonStub, stub } from 'sinon';
import anyTest, { TestInterface } from 'ava';
import { ImageType } from '../../../../db/tables/image';
import { SkillInstance } from '../../../../db/tables/skill';
import * as avatars from '../../../../services/avatars';
import { createImage, createSkill, createUser, wipeDatabase } from '../../_helpers';
import { getUserTicket, testUser } from '../_helpers';
import { callApi } from './_helpers';

const readFile = promisify(fs.readFile);

const ns = 'dialogs-skill-card';
const validImageId = '000000/00000000000000000000';
const nonexistentImageId = '000000/00000000000000000001';
const rateLimitImageId = '000000/00000000000000000002';

const validImage = {
    size: 9136,
    url: `https://example.com/${ns}/${validImageId}/orig`,
};

const nonexistentImage = {
    size: 9136,
    url: `https://avatars.mdst.yandex.net/get-${ns}/${nonexistentImageId}/orig`,
};

const validImageAvatarResponse = {
    size: validImage.size,
    url: `https://avatars.mdst.yandex.net/get-${ns}/${validImageId}/orig`,
    origUrl: validImage.url,
};

const rateLimitImage = {
    size: validImage.size,
    url: `https://avatars.mdst.yandex.net/get-${ns}/${rateLimitImageId}/orig`,
};

const matchImageOfSize = (size: number) => ({
    fieldname: 'file',
    originalname: 'image.png',
    encoding: '7bit',
    mimetype: 'image/png',
    buffer: match.instanceOf(Buffer),
    size,
});

async function getImage() {
    const contentType = 'image/png';
    const filename = resolve(__dirname, '../../../../../testResources/orig.png');
    const body = await readFile(filename);
    return { contentType, body };
}

const test = anyTest as TestInterface<{
    skill: SkillInstance;
    userTicket: string;
    avatarsStub: {
        uploadImage: SinonStub;
        deleteImage: SinonStub;
    };
}>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);
    const avatarsStub = {
        uploadImage: stub(avatars, 'uploadImage'),
        deleteImage: stub(avatars, 'deleteImage'),
    };
    const badRequestError = Object.assign(new Error(), { response: { statusCode: 400 }});
    const imageTooSmall = Object.assign(new Error(), {
        statusCode: 403,
        response: {
            body: JSON.stringify({
                attrs: {},
                description: 'Image size 631 bytes less than minimum allowed 1024 bytes',
                status: 'error',
            }),
            statusCode: 403,
        },
    });
    const imageTooBig = Object.assign(new Error(), {
        response: {
            body: JSON.stringify({
                attrs: {},
                description: 'Image size 10174706 bytes more than maximum allowed 1048576 bytes',
                status: 'error',
            }),
            statusCode: 403,
        },
    });
    const imageUnavailable = Object.assign(new Error(), {
        response: {
            body: JSON.stringify({
                attrs: {},
                description:
                    'util::curl::attemps_are_over: file downloader ran out of attempts: url="https://path-to-image.png"',
                status: 'error',
            }),
            statusCode: 434,
        },
    });
    const rateLimitExceeded = Object.assign(new Error(), {
        response: {
            body: JSON.stringify({
                attrs: {},
                description: 'too many requests to cluster',
                status: 'error',
            }),
            statusCode: 429,
        },
    });

    const error404 = Object.assign(new Error(), { response: { statusCode: 404 }});
    const imageNotFoundError = avatars.AvatarError.fromError(error404);
    const invalidImageError = avatars.AvatarError.fromError(badRequestError);
    const imageTooSmallError = avatars.AvatarError.fromError(imageTooSmall);
    const imageTooBigError = avatars.AvatarError.fromError(imageTooBig);
    const imageUnavailableError = avatars.AvatarError.fromError(imageUnavailable);
    const rateLimitExceededError = avatars.AvatarError.fromError(rateLimitExceeded);

    const internalError = avatars.AvatarError.fromError(new Error());

    avatarsStub.uploadImage.withArgs(ns, 'https://yandex.ru/').rejects(invalidImageError);
    avatarsStub.uploadImage
        .withArgs(ns, 'https://path-to-image.png/')
        .rejects(imageUnavailableError);
    avatarsStub.uploadImage.withArgs(ns, matchImageOfSize(1) as any).rejects(imageTooSmallError);
    avatarsStub.uploadImage
        .withArgs(ns, matchImageOfSize(10 * 1024 * 1024) as any)
        .rejects(imageTooBigError);
    avatarsStub.uploadImage
        .withArgs(ns, matchImageOfSize(validImage.size) as any)
        .resolves(validImageAvatarResponse as any);
    avatarsStub.uploadImage.withArgs(ns, validImage.url).resolves(validImageAvatarResponse as any);
    avatarsStub.uploadImage.withArgs(ns, rateLimitImage.url).rejects(rateLimitExceededError);
    avatarsStub.uploadImage.rejects(internalError);

    avatarsStub.deleteImage.withArgs(ns, rateLimitImageId).rejects(rateLimitExceededError);
    avatarsStub.deleteImage.withArgs(ns, validImageId).resolves();
    avatarsStub.deleteImage.withArgs(ns, nonexistentImageId).rejects(imageNotFoundError);
    avatarsStub.deleteImage.rejects(internalError);

    Object.assign(t.context, { userTicket, avatarsStub });
});

test.after(async t => {
    t.context.avatarsStub.deleteImage.restore();
    t.context.avatarsStub.uploadImage.restore();
});

test.beforeEach(async t => {
    await wipeDatabase();

    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });

    Object.assign(t.context, { skill });
});

test('call api with no ticket', async t => {
    const res = await callApi('get', `/skills/${t.context.skill.id}/images`);

    t.is(res.status, 403);
    t.deepEqual(res.body, {
        message: 'Forbidden (no credentials)',
    });
});

test('call api with malformed ticket', async t => {
    const res = await callApi('get', `/skills/${t.context.skill.id}/images`, { userTicket: '123' });

    t.is(res.status, 403);
    t.deepEqual(res.body, {
        message: 'Forbidden (authentication error)',
    });
});

test('ask for unknown resource', async t => {
    const res = await callApi('get', '/unknown-resource', { userTicket: t.context.userTicket });

    t.is(res.status, 404);
    t.deepEqual(res.body, {
        message: 'Resource not found',
    });
});

test('GET /skill/:id/images for own skill', async t => {
    const { createdAt } = await createImage({
        type: ImageType.SkillCard,
        skillId: t.context.skill.id,
        url: validImage.url,
        origUrl: validImage.url,
    });

    const res = await callApi('get', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
    });

    t.is(res.status, 200);
    t.deepEqual(res.body, {
        images: [
            {
                id: validImageId,
                origUrl: validImage.url,
                size: 0,
                createdAt: createdAt.toISOString(),
            },
        ],
        total: 1,
    });
});

test("GET /skill/:id/images for another's skill", async t => {
    const unknownSkillId = t.context.skill.id.slice(0, -4) + '0000';
    const res = await callApi('get', `/skills/${unknownSkillId}/images`, {
        userTicket: t.context.userTicket,
    });

    t.is(res.status, 404);
    t.deepEqual(res.body, {
        message: 'Resource not found',
    });
});

test('GET /skill/:id/images with malformed id', async t => {
    const res = await callApi('get', '/skills/123/images', { userTicket: t.context.userTicket });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'skillId must be a valid UUID',
    });
});

test('POST /skill/:id/images with malformed id', async t => {
    const res = await callApi('post', '/skills/123/images', { userTicket: t.context.userTicket });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'skillId must be a valid UUID',
    });
});

test('POST /skill/:id/images without url', async t => {
    const res = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
    });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'URL or FILE is needed',
    });
});

test('POST /skill/:id/images with malformed url', async t => {
    const res = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
        body: {
            url: '123',
        },
    });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'Invalid url',
    });
});

test('POST /skill/:id/images with with url of wrong content type', async t => {
    const res = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
        body: {
            url: 'https://yandex.ru',
        },
    });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'Invalid image',
    });
});

test('POST /skill/:id/images with unavailable image', async t => {
    const res = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
        body: {
            url: 'https://path-to-image.png',
        },
    });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'Image unavailable',
    });
});

test('POST /skill/:id/images with 1 byte file', async t => {
    const res = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file',
            filename: 'image.png',
            contentType: 'image/png',
            buffer: Buffer.alloc(1),
        },
    });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'Invalid image size',
        payload: {
            type: 'LessThenMinimum',
        },
    });
});

test('POST /skill/:id/images with 10 MB file', async t => {
    const res = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file',
            filename: 'image.png',
            contentType: 'image/png',
            buffer: Buffer.alloc(10 * 1024 * 1024),
        },
    });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'Invalid image size',
        payload: {
            type: 'MoreThenMaximum',
        },
    });
});

test('POST /skill/:id/images with valid file', async t => {
    const image = await getImage();
    const res = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file',
            filename: 'image.png',
            contentType: image.contentType,
            buffer: image.body,
        },
    });

    t.is(res.status, 201);
    t.is(res.body.image.origUrl, undefined);
    t.regex(res.body.image.id, /^[0-9]+\/[0-9a-f]+$/);
});

test('Invalid attachment filed name causes response code 400', async t => {
    const image = await getImage();
    const res = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file123',
            filename: 'image.png',
            contentType: image.contentType,
            buffer: image.body,
        },
    });

    t.deepEqual(res.status, 400);
});

test('upload image, check quota and delete', async t => {
    const resStatus = await callApi('get', '/status', {
        userTicket: t.context.userTicket,
    });

    t.is(resStatus.status, 200);
    t.deepEqual(resStatus.body.images, {
        quota: {
            total: 104857600,
            used: 0,
        },
    });

    const resUpload = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
        body: { url: validImage.url },
    });

    t.is(resUpload.status, 201);
    t.is(resUpload.body.image.origUrl, validImage.url);
    t.regex(resUpload.body.image.id, /^[0-9]+\/[0-9a-f]+$/);

    const resStatus2 = await callApi('get', '/status', {
        userTicket: t.context.userTicket,
    });

    t.is(resStatus2.status, 200);
    t.deepEqual(resStatus2.body.images, {
        quota: {
            total: 104857600,
            used: 9136,
        },
    });

    const resDelete = await callApi(
        'delete',
        `/skills/${t.context.skill.id}/images/${resUpload.body.image.id}`,
        {
            userTicket: t.context.userTicket,
        },
    );

    t.is(resDelete.status, 200);
    t.is(resDelete.body.result, 'ok');

    const resStatus3 = await callApi('get', '/status', {
        userTicket: t.context.userTicket,
    });

    t.is(resStatus3.status, 200);
    t.deepEqual(resStatus3.body.images, {
        quota: {
            total: 104857600,
            used: 0,
        },
    });
});

test('DELETE /skill/:skillId/images/:imageId with malformed skillId', async t => {
    const res = await callApi('delete', '/skills/123/images/---/---', {
        userTicket: t.context.userTicket,
    });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'skillId must be a valid UUID',
    });
});

test('DELETE /skill/:skillId/images/:imageId with malformed imageId', async t => {
    const res = await callApi('delete', `/skills/${t.context.skill.id}/images/---/---`, {
        userTicket: t.context.userTicket,
    });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'Invalid image ID',
    });
});

test('DELETE /skill/:skillId/images/:imageId with unknown skill', async t => {
    const unknownSkillId = t.context.skill.id.slice(0, -4) + '0000';
    const res = await callApi('delete', `/skills/${unknownSkillId}/images/123`, {
        userTicket: t.context.userTicket,
    });

    t.is(res.status, 404);
    t.deepEqual(res.body, {
        message: 'Resource not found',
    });
});

test('DELETE /skill/:skillId/images/:imageId with unknown image', async t => {
    const res = await callApi(
        'delete',
        `/skills/${t.context.skill.id}/images/0000/429f04ace2d88763a581`,
        {
            userTicket: t.context.userTicket,
        },
    );

    t.is(res.status, 404);
    t.deepEqual(res.body, {
        message: 'Image not found',
    });
});

test('Delete image that is already deleted from avatar storage', async t => {
    await createImage({
        skillId: t.context.skill.id,
        url: nonexistentImage.url,
        origUrl: nonexistentImage.url,
        type: ImageType.SkillCard,
    });

    const res = await callApi(
        'delete',
        `/skills/${t.context.skill.id}/images/000000/00000000000000000001`,
        {
            userTicket: t.context.userTicket,
        },
    );

    t.is(res.status, 200);
});

test('delete image rejects with 429 error', async t => {
    await createImage({
        type: ImageType.SkillCard,
        skillId: t.context.skill.id,
        url: rateLimitImage.url,
        origUrl: rateLimitImage.url,
        id: rateLimitImageId,
    });

    const res = await callApi(
        'delete',
        `/skills/${t.context.skill.id}/images/${rateLimitImageId}`,
        { userTicket: t.context.userTicket },
    );

    t.is(res.status, 429);
    t.deepEqual(res.body, {
        message: 'Rate limit exceeded',
    });
});

test('upload image rejects with 429 error', async t => {
    const res = await callApi('post', `/skills/${t.context.skill.id}/images`, {
        userTicket: t.context.userTicket,
        body: { url: rateLimitImage.url },
    });

    t.is(res.status, 429);
    t.deepEqual(res.body, {
        message: 'Rate limit exceeded',
    });
});
