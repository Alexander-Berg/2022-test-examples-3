/* eslint-disable */
import test from 'ava';
import { createImageForSkill, createSkill, createUser, wipeDatabase } from '../_helpers';
import * as CollectionRepository from '../../../db/repositories/skillCollection';
import { BunkerData } from '../../../types/BunkerData';
import { prepareMainPageData } from '../../../services/catalogue/mainPageService';

test.beforeEach(async t => {
    await wipeDatabase();
    await createUser();
});

// Теперь мы не исключаем навыки подборок из топов категорий
test.skip('categoryTops do not include any skills from skill collections', async t => {
    const collectionSkill = await createSkill({
        onAir: true,
        publishingSettings: {
            category: 'kids',
        },
        slug: 'collection-skill',
        score: 2,
        isRecommended: true,
    });
    const categoryTopSkill = await createSkill({
        onAir: true,
        publishingSettings: {
            category: 'kids',
        },
        slug: 'category-top-skill',
        score: 1,
        isRecommended: true,
    });
    const logo = await createImageForSkill(collectionSkill);
    await collectionSkill.update({
        logoId: logo.id,
    });
    await categoryTopSkill.update({
        logoId: logo.id,
    });

    const bunkerData = {
        mainpage: {
            skillCollections: [
                {
                    id: 'popular',
                    title: 'title',
                    skills: [collectionSkill.slug],
                },
            ],
            categoryTops: [
                {
                    slug: 'kids',
                    size: 6,
                },
            ],
        },
    } as BunkerData;

    const mainPageData = await prepareMainPageData(bunkerData);

    t.is(mainPageData.skillCollections.length, 1);
    const collectionIds = mainPageData.skillCollections[0].skills.map(skill => skill.id);
    t.deepEqual(collectionIds, [collectionSkill.id]);
    t.is(mainPageData.categoryTops.length, 1);
    const categoryTopIds = mainPageData.categoryTops[0].skills.map(skill => skill.id);
    t.deepEqual(categoryTopIds, [categoryTopSkill.id]);
});

test('handpicked collections have priority over automatic', async t => {
    const handpickedSkill = await createSkill({
        onAir: true,
        publishingSettings: {
            category: 'kids',
        },
        slug: 'collection-skill',
        score: 2,
        isRecommended: true,
    });
    const automaticCollectionSkill = await createSkill({
        onAir: true,
        publishingSettings: {
            category: 'kids',
        },
        slug: 'automatic-collection-skill',
        score: 2,
        isRecommended: true,
    });
    const logo = await createImageForSkill(handpickedSkill);
    await handpickedSkill.update({
        logoId: logo.id,
    });
    await automaticCollectionSkill.update({
        logoId: logo.id,
    });
    await CollectionRepository.update([
        {
            id: 'popular',
            skillSlugs: [handpickedSkill.slug, automaticCollectionSkill.slug],
            categorySlug: null,
            description: '',
        },
    ]);
    const bunkerData = {
        mainpage: {
            skillCollections: [
                {
                    id: 'popular',
                    title: 'title',
                    skills: ['', handpickedSkill.slug],
                },
            ],
        },
    } as BunkerData;

    const mainPageData = await prepareMainPageData(bunkerData);
    t.is(mainPageData.skillCollections.length, 1);
    const collectionIds = mainPageData.skillCollections[0].skills.map(skill => skill.id);
    t.deepEqual(collectionIds, [automaticCollectionSkill.id, handpickedSkill.id]);
});
