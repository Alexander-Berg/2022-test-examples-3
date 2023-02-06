/* eslint-disable */
import test from 'ava';
import {
    createFeed,
    createSkill,
    createUser,
    createImage,
    wipeDatabase,
} from '../../../../functional/_helpers';
import { serializeNewsSourceFeed } from '../../../../../services/news/settings/serializers';
import { Channel } from '../../../../../db/tables/settings';
import { ImageType } from '../../../../../db/tables/image';
import { NewsSourceFeed } from '../../../../../types/NewsSourceFeed';

test.beforeEach(async() => {
    await wipeDatabase();
});

test('serializeNewsSourceFeed: serialize singular feed with skill logo', async t => {
    await createUser();
    const skill = await createSkill({
        name: 'test skill',
        channel: Channel.NewsSkill,
    });
    const feed = await createFeed({
        skillId: skill.id,
        name: 'test feed',
    });
    const logo = await createImage({
        skillId: skill.id,
        type: ImageType.SkillSettings,
        url: 'https://example2.com',
    });

    skill.logo2 = logo;
    feed.skill = skill;

    const counts = {
        [skill.id]: 1,
    };

    const serialized = serializeNewsSourceFeed(feed as NewsSourceFeed, counts);

    t.deepEqual(serialized, {
        id: feed.id,
        name: skill.name,
        description: undefined,
        logoUrl: 'https://example2.com',
    });
});

test('serializeNewsSourceFeed: serialize singular feed with feed logo', async t => {
    await createUser();
    const skill = await createSkill({
        name: 'test skill',
        channel: Channel.NewsSkill,
    });
    const feed = await createFeed({
        skillId: skill.id,
        iconUrl: 'https://example.com',
        name: 'test feed',
    });
    const logo = await createImage({ skillId: skill.id, type: ImageType.SkillSettings, url: '' });

    skill.logo2 = logo;
    feed.skill = skill;

    const counts = {
        [skill.id]: 1,
    };

    const serialized = serializeNewsSourceFeed(feed as NewsSourceFeed, counts);

    t.deepEqual(serialized, {
        id: feed.id,
        name: 'test skill',
        description: undefined,
        logoUrl: 'https://example.com',
    });
});

test('serializeNewsSourceFeed: serialize feed with feed logo', async t => {
    await createUser();
    const skill = await createSkill({
        name: 'test skill',
        channel: Channel.NewsSkill,
    });
    const feed = await createFeed({
        skillId: skill.id,
        iconUrl: 'https://example.com',
        topic: 'test1',
        name: 'test feed',
    });
    await createFeed({
        skillId: skill.id,
        iconUrl: 'https://example.com',
        topic: 'test2',
    });
    const logo = await createImage({ skillId: skill.id, type: ImageType.SkillSettings, url: '' });

    skill.logo2 = logo;
    feed.skill = skill;

    const counts = {
        [skill.id]: 2,
    };

    const serialized = serializeNewsSourceFeed(feed as NewsSourceFeed, counts);

    t.deepEqual(serialized, {
        id: feed.id,
        name: 'test feed',
        description: 'test skill',
        logoUrl: 'https://example.com',
    });
});
