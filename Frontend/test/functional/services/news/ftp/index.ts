/* eslint-disable */
import * as fs from 'fs';
import * as path from 'path';
import { promisify } from 'util';
import anyTest, { TestInterface, Implementation } from 'ava';
import * as sinon from 'sinon';
import * as uuid from 'uuid';
import * as nock from 'nock';
import { SkillInstance } from '../../../../../db/tables/skill';
import * as s3 from '../../../../../services/s3';
import { testUser } from '../../../api/_helpers';
import {
    wipeDatabase,
    createUser,
    createSkill,
    createFeed,
    createNewsContent,
} from '../../../_helpers';
import { Channel } from '../../../../../db/tables/settings';
import { downloadSoundResources, HardcodedFtpProviderUrls } from '../../../../../services/news/ftp';
import { NewsContent, Sound } from '../../../../../db';
import { vgtrkResponse } from './resources/_vgtrk-response';
import { NewsFeedType } from '../../../../../db/tables/newsFeed';

const playerScope = nock('https://player.vgtrk.com');

const writeFile = promisify(fs.writeFile);

interface Context {
    skill: SkillInstance;
    userTicket: string;
    s3Stub: {
        upload: sinon.SinonSpy;
        remove: sinon.SinonStub;
    };
}

const test = anyTest as TestInterface<Context>;

const testDirectory = path.join(__dirname, 'test');

const cleanTestDir = () => {
    fs.readdirSync(testDirectory).forEach(file => fs.unlinkSync(path.join(testDirectory, file)));
    fs.rmdirSync(testDirectory);
};

const configureFileDownloading = async() => {
    if (fs.existsSync(testDirectory)) {
        cleanTestDir();
    }
    fs.mkdirSync(testDirectory);

    const replacement = sinon.spy(async(key: string, buffer: Buffer, type: string) => {
        await writeFile(path.join(testDirectory, uuid()), buffer);
    });

    sinon.replace(s3, 'upload', replacement);
    sinon.stub(s3, 'remove');
};

const disableFileDownloading = () => {
    sinon.restore();

    cleanTestDir();
};

const fileDownloadingTest: (implementation: Implementation<Context>) => Implementation<Context> = (
    implementation: Implementation<Context>,
) => async t => {
    try {
        await configureFileDownloading();
        await implementation(t);
    } finally {
        disableFileDownloading();
    }
};

test.beforeEach(async t => {
    nock.cleanAll();

    await wipeDatabase();

    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.NewsSkill,
        id: 'b95c2847-1583-4fe4-a3c4-01877fd28765',
    });

    Object.assign(t.context, { skill });
});

// unskip if needed to test resources downloading
test.skip(
    'Test vgtrk resources download to file',
    fileDownloadingTest(async t => {
        const { skill } = t.context;
        const feed = await createFeed({
            skillId: skill.id,
            url: HardcodedFtpProviderUrls.VESTI,
        });

        await downloadSoundResources();

        const contents = await NewsContent.findAll({
            where: {
                feedId: feed.id,
            },
        });

        t.is(contents.length, 10);
    }),
);

// unskip if need to test s3 uploading
test.skip('Test vgtrk resources uploading to s3', async t => {
    const { skill } = t.context;
    const feed = await createFeed({
        skillId: skill.id,
        url: HardcodedFtpProviderUrls.VESTI,
    });

    await downloadSoundResources();

    const contents = await NewsContent.findAll({
        where: {
            feedId: feed.id,
        },
    });

    const sounds = await Promise.all(
        contents.map(async content => {
            return (await Sound.findOne({
                where: { id: content.soundId! },
                rejectOnEmpty: true,
            }))!;
        }),
    );

    t.is(contents.length, 10);

    const existanceArr = await Promise.all(sounds.map(sound => s3.exists(sound.originalPath!)));

    t.deepEqual(existanceArr, Array(10).fill(true));

    await Promise.all(sounds.map(sound => s3.remove(sound.originalPath!)));
});

test(
    'Ignore text news',
    fileDownloadingTest(async t => {
        const skill = await createSkill({
            userId: testUser.uid,
            channel: Channel.NewsSkill,
            backendSettings: {
                flashBriefingType: 'text_news',
            },
        });
        const feed = await createFeed({
            skillId: skill.id,
            url: HardcodedFtpProviderUrls.VESTI,
        });

        await downloadSoundResources();

        const contents = await NewsContent.findAll({
            where: {
                feedId: feed.id,
            },
        });

        t.is(contents.length, 0);
    }),
);

test(
    'recieving duplicates correctly',
    fileDownloadingTest(async t => {
        nock('http://api.vgtrk.com')
            .get(/\/api\/v1\/audios\/brands\/61154/)
            .reply(200, vgtrkResponse, {
                'content-type': 'application/json',
            });

        [
            '/audio/mp3/test/840/285.mp3',
            '/audio/mp3/test/840/276.mp3',
            '/audio/mp3/test/840/271.mp3',
            '/audio/mp3/test/840/264.mp3',
            '/audio/mp3/test/840/261.mp3',
            '/audio/mp3/test/840/257.mp3',
            '/audio/mp3/test/840/251.mp3',
            '/audio/mp3/test/840/248.mp3',
            '/audio/mp3/test/840/245.mp3',
            '/audio/mp3/test/840/244.mp3',
        ].reduce((scope, p) => {
            scope.get(p).reply(200, Buffer.alloc(1024), { 'content-type': 'audio/mpeg' });

            return scope;
        }, playerScope);

        const { skill } = t.context;
        const feed = await createFeed({
            skillId: skill.id,
            url: HardcodedFtpProviderUrls.VESTI,
            type: NewsFeedType.FTP,
        });

        // create 2/10 resources
        await createNewsContent({
            feedId: feed.id,
            uid: '2490034',
        });
        await createNewsContent({
            feedId: feed.id,
            uid: '2490030',
        });

        // download all 10 resources
        await downloadSoundResources();

        const contents = await NewsContent.findAll({
            where: {
                feedId: feed.id,
            },
        });

        // check, that resulting contents length is 10 not 12, so that duplicates are ignored
        t.is(contents.length, 10);
    }),
);
