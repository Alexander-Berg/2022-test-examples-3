/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import * as mem from 'mem';
import { TimeoutError } from 'promise-timeout';
import * as unistat from '../../../../services/unistat';
import * as s3 from '../../../../services/s3';
import { CategoryType } from '../../../../fixtures/categories';
import { Channel, SkillAccess } from '../../../../db/tables/settings';
import * as skillRepository from '../../../../db/repositories/skill';
import {
    createImage,
    createSkill,
    createUser,
    wipeDatabase,
} from '../../../../test/functional/_helpers';
import { ImageType } from '../../../../db/tables/image';
import * as dialogBySlug from '../../../../services/catalogue/dialogBySlug';
import * as categoryListing from '../../../../services/catalogue/categoryListing';
import { getPoolSet } from '../../../../lib/pgPool';

test.before(async() => {
    // waiting for pg service startup
    await getPoolSet();
});

test.beforeEach(wipeDatabase);

// --- getDialogsByCategory ---

test('test getDialogsByCategory (from db. all is fine)', async t => {
    // init db

    await createUser();
    const skill = await createSkill({
        channel: Channel.AliceSkill,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
        publishingSettings: {
            category: CategoryType.business_finance,
        },
        onAir: true,
    });
    const image = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    await skill.update({ logoId: image.id });

    // test

    const getDialogsByCategoryImpl = sinon.spy(categoryListing, 'getDialogsByCategoryImpl');
    const getDialogsByCategoryPumpkin = sinon.spy(categoryListing, 'getDialogsByCategoryPumpkin');

    const res = await categoryListing.getDialogsByCategory(CategoryType.business_finance, 10, 0);
    t.true(res.hasMore === false);
    t.true(res.items[0].category === CategoryType.business_finance);
    t.true(getDialogsByCategoryImpl.calledOnce);
    t.true(getDialogsByCategoryPumpkin.notCalled);

    getDialogsByCategoryImpl.restore();
    getDialogsByCategoryPumpkin.restore();
});

test('test getDialogsByCategory (from pumpkin. db fails)', async t => {
    sinon.stub(skillRepository, 'findDeployedSkills').throws(new Error());
    sinon.stub(s3, 'downloadJSONCached').value(async() => {
        return {
            items: [
                {
                    // Full skill entity should be here.
                    // But in fact the rest items except "category" is not mandatory in this test
                    category: 'business_finance',
                },
            ],
            total: 1,
            hasMore: false,
        };
    });

    mem.clear(categoryListing.getDialogsByCategoryImplCached);

    const getDialogsByCategoryImpl = sinon.spy(categoryListing, 'getDialogsByCategoryImpl');
    const incStorePumpkin = sinon.spy(unistat, 'incStorePumpkin');
    const getDialogsByCategoryPumpkin = sinon.spy(categoryListing, 'getDialogsByCategoryPumpkin');

    const res = await categoryListing.getDialogsByCategory(CategoryType.business_finance, 10, 0);
    t.true(res.hasMore === false);
    t.true(res.items[0].category === CategoryType.business_finance);

    t.true(getDialogsByCategoryImpl.calledOnce);
    t.true(incStorePumpkin.calledOnceWith('getDialogsByCategory'));
    t.true(getDialogsByCategoryPumpkin.calledOnce);

    getDialogsByCategoryImpl.restore();
    incStorePumpkin.restore();
    getDialogsByCategoryPumpkin.restore();
    sinon.restore();
});

test('test getDialogsByCategory (from pumpkin. db fails. check that downloadJSON is under a cache)', async t => {
    sinon.stub(skillRepository, 'findDeployedSkills').throws(new Error());
    sinon.stub(s3, 'downloadJSON').value(() => {
        return {
            items: [
                {
                    // Full skill entity should be here.
                    // But in fact the rest items except "category" is not mandatory in this test
                    category: 'business_finance',
                },
            ],
            total: 1,
            hasMore: false,
        };
    });

    const getDialogsByCategoryImpl = sinon.spy(categoryListing, 'getDialogsByCategoryImpl');
    const incStorePumpkin = sinon.spy(unistat, 'incStorePumpkin');
    const getDialogsByCategoryPumpkin = sinon.spy(categoryListing, 'getDialogsByCategoryPumpkin');
    const downloadJSON = sinon.spy(s3, 'downloadJSON');
    const downloadJSONCached = sinon.spy(s3, 'downloadJSONCached');

    // first obtaining (not from cache)

    let res = await categoryListing.getDialogsByCategory(CategoryType.business_finance, 10, 0);
    t.true(res.hasMore === false);
    t.true(res.items[0].category === CategoryType.business_finance);

    t.true(getDialogsByCategoryImpl.calledOnce);
    t.true(incStorePumpkin.calledOnceWith('getDialogsByCategory'));
    t.true(getDialogsByCategoryPumpkin.calledOnce);
    t.true(downloadJSON.calledOnce);
    t.true(downloadJSONCached.calledOnce);

    // second obtaining (from cache)

    getDialogsByCategoryImpl.resetHistory();
    incStorePumpkin.resetHistory();
    getDialogsByCategoryPumpkin.resetHistory();
    downloadJSON.resetHistory();
    downloadJSONCached.resetHistory();

    res = await categoryListing.getDialogsByCategory(CategoryType.business_finance, 10, 0);
    t.true(res.hasMore === false);
    t.true(res.items[0].category === CategoryType.business_finance);

    t.true(getDialogsByCategoryImpl.calledOnce);
    t.true(incStorePumpkin.calledOnceWith('getDialogsByCategory'));
    t.true(getDialogsByCategoryPumpkin.calledOnce);
    t.true(downloadJSON.notCalled);
    t.true(downloadJSONCached.calledOnce);

    getDialogsByCategoryImpl.restore();
    incStorePumpkin.restore();
    getDialogsByCategoryPumpkin.restore();
    downloadJSON.restore();
    downloadJSONCached.restore();
    sinon.restore();
});

test('test getDialogsByCategory (from pumpkin. db request was too long)', async t => {
    sinon.stub(skillRepository, 'findDeployedSkills').throws(new TimeoutError());
    sinon.stub(s3, 'downloadJSONCached').value(async() => {
        return {
            items: [
                {
                    // Full skill entity should be here.
                    // But in fact the rest items except "category" is not mandatory in this test
                    category: 'business_finance',
                },
            ],
            total: 1,
            hasMore: false,
        };
    });

    const getDialogsByCategoryImpl = sinon.spy(categoryListing, 'getDialogsByCategoryImpl');
    const incDbTimeout = sinon.spy(unistat, 'incDbTimeout');
    const incStorePumpkin = sinon.spy(unistat, 'incStorePumpkin');
    const getDialogsByCategoryPumpkin = sinon.spy(categoryListing, 'getDialogsByCategoryPumpkin');

    const res = await categoryListing.getDialogsByCategory(CategoryType.business_finance, 10, 0);
    t.true(res.hasMore === false);
    t.true(res.items[0].category === CategoryType.business_finance);

    t.true(getDialogsByCategoryImpl.calledOnce);
    t.true(incDbTimeout.calledOnceWith('getDialogsByCategory [store]'));
    t.true(incStorePumpkin.calledOnceWith('getDialogsByCategory'));
    t.true(getDialogsByCategoryPumpkin.calledOnce);

    getDialogsByCategoryImpl.restore();
    incDbTimeout.restore();
    incStorePumpkin.restore();
    getDialogsByCategoryPumpkin.restore();
    sinon.restore();
});

test('test getDialogsByCategory (pumpkin error)', async t => {
    sinon.stub(skillRepository, 'findDeployedSkills').throws(new Error());
    sinon.stub(s3, 'downloadJSONCached').throws(new s3.UnableToDownload('', new Error()));

    const getDialogsByCategoryImpl = sinon.spy(categoryListing, 'getDialogsByCategoryImpl');
    const incStorePumpkin = sinon.spy(unistat, 'incStorePumpkin');
    const getDialogsByCategoryPumpkin = sinon.spy(categoryListing, 'getDialogsByCategoryPumpkin');
    const incPumpkinError = sinon.spy(unistat, 'incPumpkinError');

    const res = await categoryListing.getDialogsByCategory(CategoryType.business_finance, 10, 0);
    t.true(res.hasMore === false);
    t.true(res.items.length === 0);

    t.true(getDialogsByCategoryImpl.calledOnce);
    t.true(incStorePumpkin.calledOnceWith('getDialogsByCategory'));
    t.true(getDialogsByCategoryPumpkin.calledOnce);
    t.true(incPumpkinError.calledOnceWith('getDialogsByCategory [store]'));

    getDialogsByCategoryImpl.restore();
    incStorePumpkin.restore();
    getDialogsByCategoryPumpkin.restore();
    incPumpkinError.restore();
    sinon.restore();
});

// --- getDialogBySlug ---
test('test getDialogBySlug (from db. all is fine)', async t => {
    // init db

    await createUser();
    const skill = await createSkill({
        channel: Channel.AliceSkill,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
        publishingSettings: {
            category: CategoryType.business_finance,
        },
        onAir: true,
    });
    const image = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    await skill.update({
        logoId: image.id,
        slug: 'xxxxxxx-fake-slug',
    });

    // test

    const getDialogBySlugImpl = sinon.spy(dialogBySlug, 'getDialogBySlugImpl');
    const getDialogBySlugPumpkin = sinon.spy(dialogBySlug, 'getDialogBySlugPumpkin');

    const res = await dialogBySlug.getDialogBySlug(skill.slug);
    t.true(res.category === CategoryType.business_finance);
    t.true(res.slug === skill.slug);
    t.true(getDialogBySlugImpl.calledOnce);
    t.true(getDialogBySlugPumpkin.notCalled);

    getDialogBySlugImpl.restore();
    getDialogBySlugPumpkin.restore();
});

test('test getDialogBySlug (from pumpkin. db fails)', async t => {
    sinon.stub(skillRepository, 'findDeployedSkillsBySlug').throws(new Error());
    sinon.stub(s3, 'downloadJSONCached').value(async() => {
        return {
            // Full skill entity should be here.
            // But in fact the rest items except "category" is not mandatory in this test
            category: 'business_finance',
        };
    });

    const getDialogBySlugImpl = sinon.spy(dialogBySlug, 'getDialogBySlugImpl');
    const incStorePumpkin = sinon.spy(unistat, 'incStorePumpkin');
    const getDialogBySlugPumpkin = sinon.spy(dialogBySlug, 'getDialogBySlugPumpkin');

    const res = await dialogBySlug.getDialogBySlug('xxxxxxx-fake-slug-no-cache');
    t.true(res.category === CategoryType.business_finance);

    t.true(getDialogBySlugImpl.calledOnce);
    t.true(incStorePumpkin.calledOnceWith('getDialogBySlug'));
    t.true(getDialogBySlugPumpkin.calledOnce);

    getDialogBySlugImpl.restore();
    incStorePumpkin.restore();
    getDialogBySlugPumpkin.restore();
    sinon.restore();
});

test('test getDialogBySlug (from pumpkin. db fails. check that downloadJSON is under a cache)', async t => {
    sinon.stub(skillRepository, 'findDeployedSkillsBySlug').throws(new Error());
    sinon.stub(s3, 'downloadJSON').value(async() => {
        return {
            // Full skill entity should be here.
            // But in fact the rest items except "category" is not mandatory in this test
            category: 'business_finance',
        };
    });

    const getDialogBySlugImpl = sinon.spy(dialogBySlug, 'getDialogBySlugImpl');
    const incStorePumpkin = sinon.spy(unistat, 'incStorePumpkin');
    const getDialogBySlugPumpkin = sinon.spy(dialogBySlug, 'getDialogBySlugPumpkin');
    const downloadJSON = sinon.spy(s3, 'downloadJSON');
    const downloadJSONCached = sinon.spy(s3, 'downloadJSONCached');

    // first obtaining (not from cache)

    let res = await dialogBySlug.getDialogBySlug('xxxxxxx-fake-slug-no-cache');
    t.true(res.category === CategoryType.business_finance);

    t.true(getDialogBySlugImpl.calledOnce);
    t.true(incStorePumpkin.calledOnceWith('getDialogBySlug'));
    t.true(getDialogBySlugPumpkin.calledOnce);
    t.true(downloadJSON.calledOnce);
    t.true(downloadJSONCached.calledOnce);

    // second obtaining (from cache)

    getDialogBySlugImpl.resetHistory();
    incStorePumpkin.resetHistory();
    getDialogBySlugPumpkin.resetHistory();
    downloadJSON.resetHistory();
    downloadJSONCached.resetHistory();

    res = await dialogBySlug.getDialogBySlug('xxxxxxx-fake-slug-no-cache');
    t.true(res.category === CategoryType.business_finance);

    t.true(getDialogBySlugImpl.calledOnce);
    t.true(incStorePumpkin.calledOnceWith('getDialogBySlug'));
    t.true(getDialogBySlugPumpkin.calledOnce);
    t.true(downloadJSON.notCalled);
    t.true(downloadJSONCached.calledOnce);

    getDialogBySlugImpl.restore();
    incStorePumpkin.restore();
    getDialogBySlugPumpkin.restore();
    downloadJSON.restore();
    downloadJSONCached.restore();
    sinon.restore();
});

test('test getDialogBySlug (from pumpkin. db request was too long)', async t => {
    sinon.stub(skillRepository, 'findDeployedSkillsBySlug').throws(new TimeoutError());
    sinon.stub(s3, 'downloadJSONCached').value(async() => {
        return {
            // Full skill entity should be here.
            // But in fact the rest items except "category" is not mandatory in this test
            category: 'business_finance',
        };
    });

    const getDialogBySlugImpl = sinon.spy(dialogBySlug, 'getDialogBySlugImpl');
    const incDbTimeout = sinon.spy(unistat, 'incDbTimeout');
    const incStorePumpkin = sinon.spy(unistat, 'incStorePumpkin');
    const getDialogBySlugPumpkin = sinon.spy(dialogBySlug, 'getDialogBySlugPumpkin');

    getDialogBySlugImpl.resetHistory();

    const res = await dialogBySlug.getDialogBySlug('xxxxxxx-fake-slug-no-cache');
    t.true(res.category === CategoryType.business_finance);

    t.true(getDialogBySlugImpl.calledOnce);
    t.true(incDbTimeout.calledOnceWith('getDialogBySlug [store]'));
    t.true(incStorePumpkin.calledOnceWith('getDialogBySlug'));
    t.true(getDialogBySlugPumpkin.calledOnce);

    getDialogBySlugImpl.restore();
    incDbTimeout.restore();
    incStorePumpkin.restore();
    getDialogBySlugPumpkin.restore();
    sinon.restore();
});

test('test getDialogBySlug (pumpkin error)', async t => {
    sinon.stub(skillRepository, 'findDeployedSkillsBySlug').throws(new Error());
    sinon.stub(s3, 'downloadJSONCached').throws(new s3.UnableToDownload('', new Error()));

    const getDialogBySlugImpl = sinon.spy(dialogBySlug, 'getDialogBySlugImpl');
    const incStorePumpkin = sinon.spy(unistat, 'incStorePumpkin');
    const getDialogBySlugPumpkin = sinon.spy(dialogBySlug, 'getDialogBySlugPumpkin');
    const incPumpkinError = sinon.spy(unistat, 'incPumpkinError');

    try {
        await dialogBySlug.getDialogBySlug('xxxxxxx-fake-slug-no-cache');
    } catch (error) {
        t.true(error instanceof s3.UnableToDownload);
    }

    t.true(getDialogBySlugImpl.calledOnce);
    t.true(incStorePumpkin.calledOnceWith('getDialogBySlug'));
    t.true(getDialogBySlugPumpkin.calledOnce);
    t.true(incPumpkinError.calledOnceWith('getDialogBySlug [store]'));

    getDialogBySlugImpl.restore();
    incStorePumpkin.restore();
    getDialogBySlugPumpkin.restore();
    incPumpkinError.restore();
    sinon.restore();
});
