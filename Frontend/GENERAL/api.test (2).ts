import { Types } from '@yandex-int/trendbox-ci._db';
import { getBuildGraph, ParsedWorkflow } from './api';

type FixtureNames = 'build' | 'build_master' | 'mq' | 'after_merge_pr' | 'after_decline_pr' | 'DEFAULT_WORKFLOW' | 'build_release';
type ParsedWorkflows = Record<FixtureNames, ParsedWorkflow>;

const workflows:ParsedWorkflows = require('./api.test.fixture.json');

function createJob(job) {
    const props = {
        status: Types.JobStatus.created,
        resolution: null,
        cloudId: null,
        cloudTaskId: null,
        cloudCopyOf: null,
        sourceJobId: null,
        output: null,
        key: null,
        title: null,
        input: null,
        hashKey: '<hashKey>',
        id: '<jobId>',
        buildId: '<buildId>',
    };

    return { ...props, ...job };
}

describe('Graph', () => {
    it('should return correct graph', () => {
        const workflowData = workflows.build;
        const jobs:Array<Types.IJob> = [
            { status: 'finished', resolution: 'skipped', hashKey: 'onSNJbu', title: 'Hermione e2e' },
            { status: 'finished', resolution: 'success', hashKey: 'XmVRX9u', title: 'Создание эксперимента на 0% для залипания' },
            { status: 'finished', resolution: 'success', hashKey: '2L9YmkV', title: 'PulseShooter/Desktop' },
            { status: 'finished', resolution: 'skipped', hashKey: 'BGfgDAD', title: 'Check templates' },
            { status: 'finished', resolution: 'skipped', hashKey: '7mKkH73', title: 'Unit' },
            { status: 'finished', resolution: 'skipped', hashKey: 'x15Xj9w', title: 'Hermione large' },
            { status: 'finished', resolution: 'skipped', hashKey: '2UM4fsQ', title: 'Hermione' },
            { status: 'finished', resolution: 'success', hashKey: '3rn31tg', title: 'Deploy' },
            { status: 'finished', resolution: 'success', hashKey: '4EkF19n', title: 'Testpalm: validate' },
            { status: 'finished', resolution: 'success', hashKey: '39nXJP4', title: 'PulseShooter/Touch' },
            { status: 'finished', resolution: 'success', hashKey: 'HbZz454', title: 'PulseStatic' },
            { status: 'finished', resolution: 'success', hashKey: 'jmX2wP3', title: 'Update PR description' },
            { status: 'finished', resolution: 'success', hashKey: 'N3muNtm', title: 'ExpFlags: upload' },
            { status: 'finished', resolution: 'success', hashKey: '2qnDYBh', title: 'Linters' },
            { status: 'finished', resolution: 'success', hashKey: 'fEj13or', title: 'Publish list of changed packages' },
            { status: 'finished', resolution: 'skipped', hashKey: '2GA3prj', title: 'Drone' },
            { status: 'finished', resolution: 'success', hashKey: '4ZAihBZ', title: 'Bootstrap task' },
            { status: 'finished', resolution: 'success', hashKey: '47oNNgQ', title: 'Setup task' },
            { status: 'finished', resolution: 'success', hashKey: 'tWzP5Ei', title: 'Publish' },
        ].map(createJob);
        const graph = getBuildGraph({ workflowData, jobs });

        const hermioneNode = graph.get('2UM4fsQ');
        const bootstrapNode = graph.get('4ZAihBZ');
        const setupNode = graph.get('47oNNgQ');
        const group = graph.get('Bootstrap task + Setup task');

        expect(hermioneNode?.parents.map(d => d.job.title)).toEqual(expect.arrayContaining(['Bootstrap task + Setup task']));
        expect(bootstrapNode?.children.map(d => d.job.title)).toEqual(expect.arrayContaining(['Bootstrap task + Setup task']));
        expect(bootstrapNode?.children.map(d => d.job.title)).toEqual(expect.not.arrayContaining(['Hermione']));
        expect(setupNode?.children.map(d => d.job.title)).toEqual(expect.arrayContaining(['Bootstrap task + Setup task']));
        expect(group?.children.map(d => d.job.title)).toEqual(expect.arrayContaining(['Hermione']));
    });
    it('chunks', () => {
        const workflowData = workflows.build;
        const jobs:Array<Types.IJob> = [
            { status: 'running', resolution: null, hashKey: '2UM4fsQ', title: 'Hermione', id: '1' },
            { status: 'finished', resolution: 'success', hashKey: '2UM4fsQ', title: 'Hermione', id: '2' },
            { status: 'finished', resolution: 'exception', hashKey: '2UM4fsQ', title: 'Hermione', id: '3' },
            { status: 'finished', resolution: 'failure', hashKey: '2UM4fsQ', title: 'Hermione', id: '4' },
            { status: 'finished', resolution: 'success', hashKey: '2UM4fsQ', title: 'Hermione', id: '5' },
            { status: 'finished', resolution: 'success', hashKey: '4ZAihBZ', title: 'Bootstrap task' },
            { status: 'finished', resolution: 'success', hashKey: '47oNNgQ', title: 'Setup task' },
        ].map(createJob);
        const graph = getBuildGraph({ workflowData, jobs });
        const hermioneNode = graph.get('2UM4fsQ');
        const group = graph.get('Bootstrap task + Setup task');

        expect(hermioneNode?.children.length).toEqual(5);
        expect(group?.children.map(d => d.job.title)).toEqual(expect.arrayContaining(['Hermione']));
    });

    it('max parents count is 1', () => {
        const workflowData = workflows.build;
        const jobs:Array<Types.IJob> = [
            { status: 'running', resolution: null, hashKey: '2UM4fsQ', title: 'Hermione', id: '1' },
            { status: 'finished', resolution: 'success', hashKey: '2UM4fsQ', title: 'Hermione', id: '2' },
            { status: 'finished', resolution: 'exception', hashKey: '2UM4fsQ', title: 'Hermione', id: '3' },
            { status: 'finished', resolution: 'failure', hashKey: '2UM4fsQ', title: 'Hermione', id: '4' },
            { status: 'finished', resolution: 'success', hashKey: '2UM4fsQ', title: 'Hermione', id: '5' },
            { status: 'finished', resolution: 'success', hashKey: '4ZAihBZ', title: 'Bootstrap task' },
            { status: 'finished', resolution: 'success', hashKey: '47oNNgQ', title: 'Setup task' },
        ].map(createJob);
        const graph = getBuildGraph({ workflowData, jobs });

        expect([...graph.entries()].filter(([, node]) => node.type !== 'group').every(([, node]) => node.parents.length <= 1)).toEqual(true);
    });
});
