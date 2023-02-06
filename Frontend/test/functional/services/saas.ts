/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as saas from '../../../services/saas';
import * as operations from '../../../db/entities/operations';
import { createSkill, createUser, wipeDatabase } from '../_helpers';
import { FerrymanJob } from '../../../db';
import { DraftStatus } from '../../../db/tables/draft';
import { JobStatus, JobType } from '../../../db/tables/ferrymanJob';

const test = anyTest as TestInterface<{ updateIndexingJobs: sinon.SinonStub }>;

test.beforeEach(async t => {
    await wipeDatabase();
    t.context.updateIndexingJobs = sinon.stub(saas, 'updateIndexingJobs');
});

test.afterEach.always(async t => {
    t.context.updateIndexingJobs.restore();
});

test('uploadAllSkills starts with force=true and zero delta size', async t => {
    const uploadSkillActivationPhrases = sinon.stub(saas, 'uploadSkillActivationPhrases');
    const countSkillsWithChangedDeployStatus = sinon.stub(operations, 'countSkillsWithChangedDeployStatus').resolves(0);

    await saas.uploadToFerryman(true);
    t.true(uploadSkillActivationPhrases.calledOnce);

    countSkillsWithChangedDeployStatus.restore();
    uploadSkillActivationPhrases.restore();
});

test('uploadAllSkills starts with force=true', async t => {
    const uploadSkillActivationPhrases = sinon.stub(saas, 'uploadSkillActivationPhrases');
    const countSkillsWithChangedDeployStatus = sinon.stub(operations, 'countSkillsWithChangedDeployStatus').resolves(0);

    await saas.uploadToFerryman(true);
    t.true(uploadSkillActivationPhrases.calledOnce);

    countSkillsWithChangedDeployStatus.restore();
    uploadSkillActivationPhrases.restore();
});

test('uploadAllSkills starts with force=true and active jobs in progress', async t => {
    const uploadSkillActivationPhrases = sinon.stub(saas, 'uploadSkillActivationPhrases');
    const countSkillsWithChangedDeployStatus = sinon.stub(operations, 'countSkillsWithChangedDeployStatus').resolves(1);

    await saas.uploadToFerryman(true);
    t.true(uploadSkillActivationPhrases.calledOnce);

    countSkillsWithChangedDeployStatus.restore();
    uploadSkillActivationPhrases.restore();
});

test('uploadAllSkills starts with force=false, no active jobs in progress and nonzero delta size', async t => {
    const uploadSkillActivationPhrases = sinon.stub(saas, 'uploadSkillActivationPhrases');
    const countSkillsWithChangedDeployStatus = sinon.stub(operations, 'countSkillsWithChangedDeployStatus').resolves(1);
    const updateJobStatuses = sinon.stub(saas, 'updateJobStatuses').resolves(0);

    await saas.uploadToFerryman(false);
    t.true(uploadSkillActivationPhrases.calledOnce);

    countSkillsWithChangedDeployStatus.restore();
    uploadSkillActivationPhrases.restore();
    updateJobStatuses.restore();
});

test("uploadAllSkills doesn't start with force=false and active jobs in progress", async t => {
    const uploadSkillActivationPhrases = sinon.stub(saas, 'uploadSkillActivationPhrases');
    const countSkillsWithChangedDeployStatus = sinon.stub(operations, 'countSkillsWithChangedDeployStatus').resolves(0);
    const updateJobStatuses = sinon.stub(saas, 'updateJobStatuses').resolves(1);

    await saas.uploadToFerryman(false);
    t.deepEqual(uploadSkillActivationPhrases.callCount, 0);

    updateJobStatuses.restore();
    countSkillsWithChangedDeployStatus.restore();
    uploadSkillActivationPhrases.restore();
});

test("uploadAllSkills doesn't start with force=false and zero delta size", async t => {
    const uploadSkillActivationPhrases = sinon.stub(saas, 'uploadSkillActivationPhrases');
    const countSkillsWithChangedDeployStatus = sinon.stub(operations, 'countSkillsWithChangedDeployStatus').resolves(0);
    const updateJobStatuses = sinon.stub(saas, 'updateJobStatuses').resolves(0);

    await saas.uploadToFerryman(false);
    t.deepEqual(uploadSkillActivationPhrases.callCount, 0);

    updateJobStatuses.restore();
    countSkillsWithChangedDeployStatus.restore();
    uploadSkillActivationPhrases.restore();
});

test("deployIndexedDrafts() doesn't deploy skills created after ferryman upload", async t => {
    await FerrymanJob.create({
        ferrymanTimestamp: 1000000,
        status: JobStatus.Indexing,
        type: JobType.KvSaaS,
        tableName: '',
    });
    await createUser();
    const skill = await createSkill();
    await skill.draft.update({
        status: DraftStatus.DeployRequested,
    });
    await saas.deployIndexedDrafts(1000000);
    await skill.draft.reload();
    t.true(skill.draft.status === DraftStatus.DeployRequested);
});

test('deployIndexedDrafts() deploy skills created before ferryman upload', async t => {
    await createUser();
    const skill = await createSkill();
    await skill.draft.update({
        status: DraftStatus.DeployRequested,
    });
    await FerrymanJob.create({
        ferrymanTimestamp: 1000000,
        status: JobStatus.Indexing,
        type: JobType.KvSaaS,
        tableName: '',
    });
    await saas.deployIndexedDrafts(1000000);
    await skill.reload();
    await skill.draft.reload();
    t.true(skill.draft.status === DraftStatus.InDevelopment);
    t.true(skill.onAir);
});
