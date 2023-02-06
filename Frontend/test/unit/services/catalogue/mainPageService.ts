/* eslint-disable */
import * as sinon from 'sinon';
import anyTest, { TestInterface } from 'ava';
import * as skillRepository from '../../../../db/repositories/skill';
import * as skillCollectionRepository from '../../../../db/repositories/skillCollection';
import { BunkerData } from '../../../../types/BunkerData';
import * as bunker from '../../../../services/bunker';
import { getMainPageData } from '../../../../services/catalogue';
import * as alicePrizeNomenees from '../../../../db/entities/alicePrizeNominees';

interface TestContext {
    fetchFromBunker: sinon.SinonStub;
    findDeployedSkillsBySlug: sinon.SinonStub;
    findAllSkillCollections: sinon.SinonStub;
    getAlicePrizeNomenees: sinon.SinonStub;
}

const test = anyTest as TestInterface<TestContext>;

const bunkerData = {
    mainpage: {
        skillCollections: [
            {
                skills: ['slug1', 'slug2'],
            },
        ],
    },
} as BunkerData;

test.beforeEach(async t => {
    t.context.findDeployedSkillsBySlug = sinon
        .stub(skillRepository, 'findDeployedSkillsBySlug')
        .resolves([]);
    t.context.fetchFromBunker = sinon.stub(bunker, 'getBunkerData').resolves(bunkerData);
    t.context.findAllSkillCollections = sinon
        .stub(skillCollectionRepository, 'findAll')
        .resolves([]);
    t.context.getAlicePrizeNomenees = sinon.stub(alicePrizeNomenees, 'getAlicePrizeNomineesCached').resolves([]);
});

test.afterEach.always(async t => {
    t.context.findDeployedSkillsBySlug.restore();
    t.context.fetchFromBunker.restore();
    t.context.findAllSkillCollections.restore();
    t.context.getAlicePrizeNomenees.restore();
});

test('findDeployedSkillsBySlug is called only once in timeout', async t => {
    await getMainPageData();
    await getMainPageData();
    await getMainPageData();
    t.is(t.context.findDeployedSkillsBySlug.callCount, 1);
});
