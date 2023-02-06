/* eslint-disable */
import { resolve } from 'path';
import * as fs from 'fs';
import { promisify } from 'util';
import anyTest, { TestInterface } from 'ava';
import { SinonStub, stub, replace } from 'sinon';
import { v4 as uuid } from 'uuid';
import { Sound } from '../../../../db';
import { SkillInstance } from '../../../../db/tables/skill';
import { SoundAttributes } from '../../../../db/tables/sounds';
import * as s3 from '../../../../services/s3';
import * as soundService from '../../../../services/soundService';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';
import { getUserTicket, testUser } from '../_helpers';
import { callApi } from './_helpers';
import { Status } from '../../../../routes/public/v1/status';
import config from '../../../../services/config';
import { callApi as callDevConsoleApi } from '../dev-console/_helpers';

const readFile = promisify(fs.readFile);

const test = anyTest as TestInterface<{
    skill: SkillInstance;
    userTicket: string;
    s3Stub: {
        upload: SinonStub;
        remove: SinonStub;
    };
    soundServiceStub: {
        deleteSkillSound: SinonStub;
    };
}>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);
    const s3Stub = {
        upload: stub(s3, 'upload'),
        remove: stub(s3, 'remove'),
    };

    const soundServiceStub = {
        deleteSkillSound: stub(soundService, 'deleteSkillSound'),
    };

    Object.assign(t.context, { userTicket, s3Stub, soundServiceStub });

    replace(config.sounds, 'enableUpload', true);
});

test.after(async t => {
    t.context.s3Stub.upload.restore();
    t.context.s3Stub.remove.restore();
    t.context.soundServiceStub.deleteSkillSound.restore();
});

test.beforeEach(async t => {
    await wipeDatabase();

    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });

    Object.assign(t.context, { skill });
});

test('call api with no ticket', async t => {
    const res = await callApi('get', `/skills/${t.context.skill.id}/sounds`);

    t.is(res.status, 403);
    t.deepEqual(res.body, {
        message: 'Forbidden (no credentials)',
    });
});

test('call api with malformed ticket', async t => {
    const res = await callApi('get', `/skills/${t.context.skill.id}/sounds`, { userTicket: '123' });

    t.is(res.status, 403);
    t.deepEqual(res.body, {
        message: 'Forbidden (authentication error)',
    });
});

test('GET /skill/:id/sound for own skill without sounds', async t => {
    const res = await callApi('get', `/skills/${t.context.skill.id}/sounds`, { userTicket: t.context.userTicket });

    t.is(res.status, 200);
    t.deepEqual(res.body, { sounds: [], total: 0 });
});

test('GET /skill/:id/sound for own skill', async t => {
    await createSound({
        skillId: t.context.skill.id,
        originalPath: 'path/sound',
    });

    const res = await callApi('get', `/skills/${t.context.skill.id}/sounds`, { userTicket: t.context.userTicket });

    t.is(res.status, 200);
    t.is(res.body.sounds.length, 1);

    const {
        sounds: [actual],
    } = res.body;
    t.truthy(actual.id);
    t.truthy(actual.createdAt);
    delete actual.id;
    delete actual.createdAt;
    const expected = {
        error: null,
        isProcessed: false,
        size: null,
        originalName: null,
        skillId: t.context.skill.id,
    };
    t.deepEqual(actual, expected);
});

test("GET /skill/:id/sounds for another's skill", async t => {
    await createSound({
        skillId: t.context.skill.id,
        originalPath: 'path/sound',
    });

    const res = await callApi('get', `/skills/${uuid()}/sounds`, { userTicket: t.context.userTicket });

    t.is(res.status, 404);
});

test('GET /skill/:id/sounds for deleted skill', async t => {
    await createSound({
        skillId: t.context.skill.id,
        originalPath: 'path/sound',
    });

    await t.context.skill.destroy();

    const res = await callApi('get', `/skills/${t.context.skill.id}/sounds`, { userTicket: t.context.userTicket });

    t.is(res.status, 404);
});

test('GET /skill/:id/sounds with malformed id', async t => {
    const res = await callApi('get', '/skills/123/sounds', { userTicket: t.context.userTicket });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'skillId must be a valid UUID',
    });
});

test('POST /skill/:id/sounds with 10 MB file', async t => {
    const res = await callApi('post', `/skills/${t.context.skill.id}/sounds`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file',
            filename: 'sound.mp3',
            contentType: 'audio/mp3',
            buffer: Buffer.alloc(10 * 1024 * 1024),
        },
    });

    t.is(res.status, 403);
    t.deepEqual(res.body, {
        message: 'File is too large',
    });
});

test('POST /skill/:id/sounds with valid file', async t => {
    const sound = await getSound();
    const res = await callApi('post', `/skills/${t.context.skill.id}/sounds`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file',
            filename: 'sound.png',
            contentType: sound.contentType,
            buffer: sound.body,
        },
    });

    t.is(res.status, 201);
    t.truthy(res.body.sound);
    t.truthy(res.body.sound.id);
});

test('Invalid attachment filed name causes response code 400', async t => {
    const sound = await getSound();
    const res = await callApi('post', `/skills/${t.context.skill.id}/sounds`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file123',
            filename: 'sound.png',
            contentType: sound.contentType,
            buffer: sound.body,
        },
    });

    t.deepEqual(res.status, 400);
});

test('upload sound, check quota and delete skill', async t => {
    const resStatus = await callApi('get', '/status', {
        userTicket: t.context.userTicket,
    });

    t.is(resStatus.status, 200);
    const status1 = resStatus.body as Status;
    t.truthy(status1.sounds);
    t.truthy(status1.sounds!.quota);
    t.is(typeof status1.sounds!.quota.total, 'number');
    t.is(typeof status1.sounds!.quota.used, 'number');

    t.is(status1.sounds!.quota.used, 0);

    const initialQuota = status1.sounds!.quota;

    const sound = await getSound();
    const resUpload = await callApi('post', `/skills/${t.context.skill.id}/sounds`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file',
            filename: 'sound.png',
            contentType: sound.contentType,
            buffer: sound.body,
        },
    });

    t.is(resUpload.status, 201);

    const resStatus2 = await callApi('get', '/status', {
        userTicket: t.context.userTicket,
    });

    t.is(resStatus2.status, 200);
    const status2 = resStatus2.body as Status;
    t.is(status2.sounds!.quota.total, initialQuota.total);
    t.is(status2.sounds!.quota.used, sound.body.byteLength);

    const resDelete = await callDevConsoleApi('delete', `/skills/${t.context.skill.id}`, {
        userTicket: t.context.userTicket,
    });

    t.is(resDelete.status, 200);
    t.is(resDelete.body.result, 'ok');

    const resStatus3 = await callApi('get', '/status', {
        userTicket: t.context.userTicket,
    });

    t.is(resStatus3.status, 200);
    const status3 = resStatus.body as Status;
    t.is(status3.sounds!.quota.total, initialQuota.total);
    t.is(status3.sounds!.quota.used, 0);
});

test('upload sound, check quota and delete sound', async t => {
    const resStatus = await callApi('get', '/status', {
        userTicket: t.context.userTicket,
    });

    t.is(resStatus.status, 200);
    const status1 = resStatus.body as Status;
    t.truthy(status1.sounds);
    t.truthy(status1.sounds!.quota);
    t.is(typeof status1.sounds!.quota.total, 'number');
    t.is(typeof status1.sounds!.quota.used, 'number');

    t.is(status1.sounds!.quota.used, 0);

    const initialQuota = status1.sounds!.quota;

    const sound = await getSound();
    const resUpload = await callApi('post', `/skills/${t.context.skill.id}/sounds`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file',
            filename: 'sound.png',
            contentType: sound.contentType,
            buffer: sound.body,
        },
    });

    t.is(resUpload.status, 201);

    const resStatus2 = await callApi('get', '/status', {
        userTicket: t.context.userTicket,
    });

    t.is(resStatus2.status, 200);
    const status2 = resStatus2.body as Status;
    t.is(status2.sounds!.quota.total, initialQuota.total);
    t.is(status2.sounds!.quota.used, sound.body.byteLength);

    const resDelete = await callApi('delete', `/skills/${t.context.skill.id}/sounds/${resUpload.body.sound.id}`, {
        userTicket: t.context.userTicket,
    });

    t.is(resDelete.status, 200);
    t.is(resDelete.body.result, 'ok');

    const resStatus3 = await callApi('get', '/status', {
        userTicket: t.context.userTicket,
    });

    t.is(resStatus3.status, 200);
    const status3 = resStatus.body as Status;
    t.is(status3.sounds!.quota.total, initialQuota.total);
    t.is(status3.sounds!.quota.used, 0);
});

test('DELETE /skill/:skillId/sounds/:soundId with malformed skillId', async t => {
    const res = await callApi('delete', '/skills/123/sounds/---', { userTicket: t.context.userTicket });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'skillId must be a valid UUID',
    });
});

test('DELETE /skill/:skillId/sounds/:soundId for deleted skill', async t => {
    const sound = await getSound();
    const resUpload = await callApi('post', `/skills/${t.context.skill.id}/sounds`, {
        userTicket: t.context.userTicket,
        attachment: {
            field: 'file',
            filename: 'sound.png',
            contentType: sound.contentType,
            buffer: sound.body,
        },
    });

    t.is(resUpload.status, 201);

    await t.context.skill.destroy();

    const resDelete = await callApi('delete', `/skills/${t.context.skill.id}/sounds/${resUpload.body.sound.id}`, {
        userTicket: t.context.userTicket,
    });

    t.is(resDelete.status, 404);
});

test('DELETE /skill/:skillId/sounds/:soundId with malformed soundId', async t => {
    const res = await callApi('delete', `/skills/${t.context.skill.id}/sounds/---`, {
        userTicket: t.context.userTicket,
    });

    t.is(res.status, 400);
    t.deepEqual(res.body, {
        message: 'Invalid sound ID',
    });
});

test('DELETE /skill/:skillId/sounds/:soundId with unknown skill', async t => {
    const res = await callApi('delete', `/skills/${uuid()}/sounds/${uuid()}`, { userTicket: t.context.userTicket });

    t.is(res.status, 404);
    t.deepEqual(res.body, {
        message: 'Resource not found',
    });
});

const createSound = async(props: SoundAttributes = {}) => {
    const sound = await Sound.create({
        ...props,
    });

    return sound;
};

const getSound = async() => {
    const contentType = 'media/mp3';
    const filename = resolve(__dirname, '../../../../../testResources/sounds/sound1.mp3');
    const body = await readFile(filename);
    return { contentType, body };
};
