/* eslint-disable */
import * as fs from 'fs';
import * as path from 'path';
import { promisify } from 'util';
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as uuid from 'uuid';
import { SkillInstance } from '../../../../../db/tables/skill';
import * as s3 from '../../../../../services/s3';
import { testUser } from '../../../api/_helpers';
import { wipeDatabase, createUser, createSkill, createFeed } from '../../../_helpers';
import { Channel } from '../../../../../db/tables/settings';
import { Sound } from '../../../../../db';
import { FeedSoundUploader } from '../../../../../services/news/soundUploader';

const writeFile = promisify(fs.writeFile);

const test = anyTest as TestInterface<{
    skill: SkillInstance;
    userTicket: string;
    s3Stub: {
        upload: sinon.SinonSpy;
        remove: sinon.SinonStub;
    };
}>;

const writeStreamFileName = path.join(__dirname, 'sound');

test.before(async t => {
    const replacement = sinon.spy(async(key: string, buffer: Buffer, type: string) => {
        await writeFile(writeStreamFileName, buffer);
    });

    sinon.replace(s3, 'upload', replacement);

    const s3Stub = {
        upload: replacement,
        remove: sinon.stub(s3, 'remove'),
    };

    Object.assign(t.context, { s3Stub });
});

test.after(async t => {
    sinon.restore();

    const unlink = promisify(fs.unlink);
    await unlink(writeStreamFileName);
});

test.beforeEach(async t => {
    await wipeDatabase();

    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id, channel: Channel.NewsSkill });

    Object.assign(t.context, { skill });
});

test('test', async t => {
    const { skill, s3Stub } = t.context;
    const soundId = uuid();
    const feed = await createFeed({
        skillId: skill.id,
    });

    const soundUploader = FeedSoundUploader.fromBuffer(Buffer.alloc(1024 * 1024), feed, soundId, 'test');

    const sound = await soundUploader.upload();

    t.is(sound.originalPath, `dialogs-upload/sounds/orig/${skill.id}/${soundId}`);
    t.is(sound.originalSize, 1024 * 1024);
    t.is(sound.originalName, 'test');

    t.true(s3Stub.upload.called);

    t.true(soundUploader.firedEvents.includes(0));
    t.true(soundUploader.firedEvents.includes(1));

    await soundUploader.rollback();

    t.is(
        await Sound.findOne({
            where: { id: soundId },
        }),
        null,
    );
    t.true(s3Stub.remove.called);
});
