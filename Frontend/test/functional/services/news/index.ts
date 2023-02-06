/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as moment from 'moment';
import * as s3 from '../../../../services/s3';
import { testUser } from '../../api/_helpers';
import { wipeDatabase, createUser, createSkill, createFeed, createNewsContent } from '../../_helpers';
import { Channel } from '../../../../db/tables/settings';
import { Sound, NewsContent } from '../../../../db';
import { SkillInstance } from '../../../../db/tables/skill';
import * as soundService from '../../../../services/soundService';
import { clearOutdatedNewsContent } from '../../../../services/news';

const test = anyTest as TestInterface<{
    skill: SkillInstance;
    s3Stub: {
        remove: sinon.SinonStub;
    };
    soundServiceStub: {
        deleteSkillSound: sinon.SinonStub;
    };
}>;

test.before(async t => {
    const s3Stub = {
        remove: sinon.stub(s3, 'remove'),
    };

    const soundServiceStub = {
        deleteSkillSound: sinon.stub(soundService, 'deleteSkillSound'),
    };

    Object.assign(t.context, { s3Stub, soundServiceStub });
});

test.after(async t => {
    sinon.restore();
});

test.beforeEach(async t => {
    await wipeDatabase();

    t.context.s3Stub.remove.resetHistory();
    t.context.soundServiceStub.deleteSkillSound.resetHistory();

    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id, channel: Channel.NewsSkill });

    Object.assign(t.context, { skill });
});

test('Test correct removing', async t => {
    const { skill, s3Stub, soundServiceStub } = t.context;

    const feed = await createFeed({
        skillId: skill.id,
    });

    const sound = await Sound.create({
        originalSize: 1,
        originalPath: 'path',
        originalName: 'name',
        maxDurationSec: 10,
        skillId: skill.id,
    });

    const content = await createNewsContent({
        feedId: feed.id,
        uid: '1',
        soundId: sound.id,
        pubDate: moment()
            .subtract({ milliseconds: 7 * 24 * 60 * 60 * 1000 + 1000 })
            .toDate(),
    });

    await clearOutdatedNewsContent();

    const deletedContent = await NewsContent.findOne({
        where: {
            id: content.id,
        },
    });

    const deletedSound = await Sound.findOne({
        where: {
            id: sound.id,
        },
    });

    t.true(s3Stub.remove.called);
    t.true(soundServiceStub.deleteSkillSound.called);

    t.is(deletedContent, null);
    t.is(deletedSound, null);
});

test('Test correct ignoring', async t => {
    const { skill, s3Stub, soundServiceStub } = t.context;

    const feed = await createFeed({
        skillId: skill.id,
    });

    const sound = await Sound.create({
        originalSize: 1,
        originalPath: 'path',
        originalName: 'name',
        maxDurationSec: 10,
        skillId: skill.id,
    });

    const content = await createNewsContent({
        feedId: feed.id,
        uid: '1',
        soundId: sound.id,
        pubDate: moment()
            .subtract({ milliseconds: 7 * 24 * 60 * 60 * 1000 - 1000 })
            .toDate(),
    });

    await clearOutdatedNewsContent();

    const notDeletedContent = await NewsContent.findOne({
        where: {
            id: content.id,
        },
    });

    const notDeletedSound = await Sound.findOne({
        where: {
            id: sound.id,
        },
    });

    t.false(s3Stub.remove.called);
    t.false(soundServiceStub.deleteSkillSound.called);

    t.not(notDeletedContent, null);
    t.not(notDeletedSound, null);
});
