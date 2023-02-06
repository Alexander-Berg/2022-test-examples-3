/* eslint-disable */
import test from 'ava';
import { wipeDatabase } from '../../_helpers';
import * as ferrymanJobs from '../../../../db/entities/ferrymanJobs';
import { setJobStatus } from '../../../../db/entities/ferrymanJobs';
import { FerrymanJob } from '../../../../db';
import { JobStatus, JobType } from '../../../../db/tables/ferrymanJob';

test.beforeEach(async() => {
    await wipeDatabase();
});

test('getActiveJobs() returns zero on empty database', async t => {
    const activeJobs = await ferrymanJobs.getActiveJobs(JobType.KvSaaS);
    t.deepEqual(activeJobs.length, 0);
});

test('getActiveJobs() does not count jobs in Done status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.Done,
        tableName: '',
    });

    const activeJobs = await ferrymanJobs.getActiveJobs(JobType.KvSaaS);
    t.deepEqual(activeJobs.length, 0);
});

test('getActiveJobs() does not count jobs in Error status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.Error,
        tableName: '',
    });

    const activeJobs = await ferrymanJobs.getActiveJobs(JobType.KvSaaS);
    t.deepEqual(activeJobs.length, 0);
});

test('getActiveJobs() does not count jobs in Indexing status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.Indexing,
        tableName: '',
    });

    const activeJobs = await ferrymanJobs.getActiveJobs(JobType.KvSaaS);
    t.deepEqual(activeJobs.length, 0);
});

test('getActiveJobs() counts jobs in Created status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.Created,
        tableName: '',
    });

    const activeJobs = await ferrymanJobs.getActiveJobs(JobType.KvSaaS);
    t.deepEqual(activeJobs.length, 1);
});

test('getActiveJobs() counts jobs in UploadedToYT status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.UploadedToYT,
        tableName: '',
    });

    const activeJobs = await ferrymanJobs.getActiveJobs(JobType.KvSaaS);
    t.deepEqual(activeJobs.length, 1);
});

test('getActiveJobs() counts jobs in SubmittedToFerryman status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.SubmittedToFerryman,
        tableName: '',
    });

    const activeJobs = await ferrymanJobs.getActiveJobs(JobType.KvSaaS);
    t.deepEqual(activeJobs.length, 1);
});

test('getActiveJobs() ignores jobs of other types', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.SubmittedToFerryman,
        tableName: '',
    });
    await FerrymanJob.create({
        type: JobType.SaaS,
        status: JobStatus.SubmittedToFerryman,
        tableName: '',
    });

    const activeJobs = await ferrymanJobs.getActiveJobs(JobType.KvSaaS);

    t.deepEqual(activeJobs.length, 1);
});

test('setJobStatus() saves job status in database', async t => {
    const job = await FerrymanJob.create({
        type: JobType.KvSaaS,
        tableName: '',
    });

    await setJobStatus(job, JobStatus.Done);
    await job.reload();

    t.deepEqual(job.status, JobStatus.Done);
});

test('getActiveJobCount() returns zero on empty database', async t => {
    const activeJobCount = await ferrymanJobs.getActiveJobCount(JobType.KvSaaS);
    t.deepEqual(activeJobCount, 0);
});

test('getActiveJobCount() does not count jobs in Done status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.Done,
        tableName: '',
    });

    const activeJobCount = await ferrymanJobs.getActiveJobCount(JobType.KvSaaS);
    t.deepEqual(activeJobCount, 0);
});

test('getActiveJobCount() does not count jobs in Error status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.Error,
        tableName: '',
    });

    const activeJobCount = await ferrymanJobs.getActiveJobCount(JobType.KvSaaS);
    t.deepEqual(activeJobCount, 0);
});

test('getActiveJobCount() does not count jobs in Indexing status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.Indexing,
        tableName: '',
    });

    const activeJobCount = await ferrymanJobs.getActiveJobCount(JobType.KvSaaS);
    t.deepEqual(activeJobCount, 0);
});

test('getActiveJobCount() counts jobs in Created status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.Created,
        tableName: '',
    });

    const activeJobCount = await ferrymanJobs.getActiveJobCount(JobType.KvSaaS);
    t.deepEqual(activeJobCount, 1);
});

test('getActiveJobCount() counts jobs in UploadedToYT status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.UploadedToYT,
        tableName: '',
    });

    const activeJobCount = await ferrymanJobs.getActiveJobCount(JobType.KvSaaS);
    t.deepEqual(activeJobCount, 1);
});

test('getActiveJobCount() counts jobs in SubmittedToFerryman status', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.SubmittedToFerryman,
        tableName: '',
    });

    const activeJobCount = await ferrymanJobs.getActiveJobCount(JobType.KvSaaS);
    t.deepEqual(activeJobCount, 1);
});

test('getActiveJobCount() ignores jobs of other types', async t => {
    await FerrymanJob.create({
        type: JobType.KvSaaS,
        status: JobStatus.SubmittedToFerryman,
        tableName: '',
    });
    await FerrymanJob.create({
        type: JobType.SaaS,
        status: JobStatus.SubmittedToFerryman,
        tableName: '',
    });

    const activeJobCount = await ferrymanJobs.getActiveJobCount(JobType.KvSaaS);
    t.deepEqual(activeJobCount, 1);
});

test('createNewJob() sets job parameters', async t => {
    const type = JobType.SaaS;
    const tableName = '//home/paskills/ferryman/unittests';

    const job = await ferrymanJobs.createNewJob(type, tableName);

    t.deepEqual(job.type, type);
    t.deepEqual(job.tableName, tableName);
});

test('getLatestJobInIndexingStatus() returns latest timestamp', async t => {
    const earlierJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 1000000,
        tableName: '',
        status: JobStatus.Indexing,
    });
    const laterJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 2000000,
        tableName: '',
        status: JobStatus.Indexing,
    });

    const latestIndexingJobCreatedAt = await ferrymanJobs.getLatestJobInIndexingStatus(JobType.KvSaaS, 2);

    t.notDeepEqual(latestIndexingJobCreatedAt, earlierJob.createdAt);
    t.deepEqual(latestIndexingJobCreatedAt, laterJob.createdAt);
});

test('getLatestJobInIndexingStatus() ignores jobs not in Indexing status', async t => {
    const earlierJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 1000000,
        tableName: '',
        status: JobStatus.Indexing,
    });
    const laterJobWithWrongStatus = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 2000000,
        tableName: '',
        status: JobStatus.Done,
    });

    const latestIndexingJobCreatedAt = await ferrymanJobs.getLatestJobInIndexingStatus(JobType.KvSaaS, 2);

    t.deepEqual(latestIndexingJobCreatedAt, earlierJob.createdAt);
    t.notDeepEqual(latestIndexingJobCreatedAt, laterJobWithWrongStatus.createdAt);
});

test('getLatestJobInIndexingStatus() filters jobs by ferrymanTimestamp', async t => {
    const earlierJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 1000000,
        tableName: '',
        status: JobStatus.Indexing,
    });
    const laterJobWithLargeFerrymanTimestamp = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 2000000,
        tableName: '',
        status: JobStatus.Indexing,
    });

    const latestIndexingJobCreatedAt = await ferrymanJobs.getLatestJobInIndexingStatus(JobType.KvSaaS, 1);

    t.deepEqual(latestIndexingJobCreatedAt, earlierJob.createdAt);
    t.notDeepEqual(latestIndexingJobCreatedAt, laterJobWithLargeFerrymanTimestamp.createdAt);
});

test('getLatestJobInIndexingStatus() ignores jobs of other types', async t => {
    const earlierJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 1000000,
        tableName: '',
        status: JobStatus.Indexing,
    });
    const laterJobWithWrongType = await FerrymanJob.create({
        type: JobType.SaaS,
        ferrymanTimestamp: 2000000,
        tableName: '',
        status: JobStatus.Indexing,
    });

    const latestIndexingJobCreatedAt = await ferrymanJobs.getLatestJobInIndexingStatus(JobType.KvSaaS, 2);

    t.deepEqual(latestIndexingJobCreatedAt, earlierJob.createdAt);
    t.notDeepEqual(latestIndexingJobCreatedAt, laterJobWithWrongType.createdAt);
});

test('markIndexingJobsAsDone() marks all Indexing Jobs as done', async t => {
    const earlierJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 1000000,
        tableName: '',
        status: JobStatus.Indexing,
    });
    const laterJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 2000000,
        tableName: '',
        status: JobStatus.Indexing,
    });

    await ferrymanJobs.markIndexingJobsAsDone(JobType.KvSaaS, 2);
    await earlierJob.reload();
    await laterJob.reload();

    t.deepEqual(earlierJob.status, JobStatus.Done);
    t.deepEqual(laterJob.status, JobStatus.Done);
});

test('markIndexingJobsAsDone() ignores jobs not in Indexing status', async t => {
    const earlierJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 1000000,
        tableName: '',
        status: JobStatus.Indexing,
    });
    const laterJobWithWrongStatus = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 2000000,
        tableName: '',
        status: JobStatus.Error,
    });

    await ferrymanJobs.markIndexingJobsAsDone(JobType.KvSaaS, 2);
    await earlierJob.reload();
    await laterJobWithWrongStatus.reload();

    t.deepEqual(earlierJob.status, JobStatus.Done);
    t.deepEqual(laterJobWithWrongStatus.status, JobStatus.Error);
});

test('markIndexingJobsAsDone() filters jobs by ferrymanTimestamp', async t => {
    const earlierJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 1000000,
        tableName: '',
        status: JobStatus.Indexing,
    });
    const laterJobWithLargeFerrymanTimestamp = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 2000000,
        tableName: '',
        status: JobStatus.Indexing,
    });

    await ferrymanJobs.markIndexingJobsAsDone(JobType.KvSaaS, 1);
    await earlierJob.reload();
    await laterJobWithLargeFerrymanTimestamp.reload();

    t.deepEqual(earlierJob.status, JobStatus.Done);
    t.deepEqual(laterJobWithLargeFerrymanTimestamp.status, JobStatus.Indexing);
});

test('markIndexingJobsAsDone() ignores jobs of other types', async t => {
    const earlierJob = await FerrymanJob.create({
        type: JobType.KvSaaS,
        ferrymanTimestamp: 1000000,
        tableName: '',
        status: JobStatus.Indexing,
    });
    const laterJobWithWrongType = await FerrymanJob.create({
        type: JobType.SaaS,
        ferrymanTimestamp: 2000000,
        tableName: '',
        status: JobStatus.Indexing,
    });

    await ferrymanJobs.markIndexingJobsAsDone(JobType.KvSaaS, 2);
    await earlierJob.reload();
    await laterJobWithWrongType.reload();

    t.deepEqual(earlierJob.status, JobStatus.Done);
    t.deepEqual(laterJobWithWrongType.status, JobStatus.Indexing);
});
