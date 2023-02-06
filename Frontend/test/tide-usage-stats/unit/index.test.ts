import { EventEmitter } from 'events';
import child_process, { ChildProcess } from 'child_process';
import path from 'path';
import sinon from 'sinon';
import { assert } from 'chai';
import usageStatsPlugin from '../../../src/plugins/tide-usage-stats';
import * as events from '../../../src/constants/events';
import * as utils from '../../../src/plugins/tide-usage-stats/utils';
import { Tide } from '../../../src';
import { UsageStatsConfig } from '../../../src/plugins/tide-usage-stats/types';

describe('tide-usage-stats', () => {
    it('should not launch if disabled', () => {
        const tide: { [key: string]: any } = { config: {}, events };
        tide.on = sinon.stub();

        usageStatsPlugin(tide as Tide, {} as UsageStatsConfig);

        assert(tide.on.notCalled);
    });

    it('should not launch if project was not provided', () => {
        sinon.stub(console, 'warn');
        const tide: { [key: string]: any } = { config: {}, events };
        tide.on = sinon.stub();
        const options = { enabled: true };

        usageStatsPlugin(tide as Tide, options as UsageStatsConfig);

        assert(tide.on.notCalled);
    });

    it('should listen to init, after files read and end events', () => {
        const tide: { [key: string]: any } = new EventEmitter();
        tide.events = events;
        tide.config = {};
        sinon.spy(tide, 'on');
        const options = {
            enabled: true,
            project: 'test',
        };

        usageStatsPlugin(tide as Tide, options as UsageStatsConfig);

        assert(tide.on.calledWith(events.INIT));
        assert(tide.on.calledWith(events.AFTER_FILES_READ));
        assert(tide.on.calledWith(events.END));
    });

    describe('handling of tide events', () => {
        let tide: Record<string, any>;
        const options = {
            enabled: true,
            project: 'test',
        };
        let childProcessStub;
        let spawnStub;
        beforeEach(() => {
            tide = new EventEmitter();
            tide.events = events;
            tide.version = '0.0.0';
            tide.config = {};

            tide.testCollection = {
                mapTests: sinon.stub().returns([1, 2, 3]),
            };

            sinon.stub(process, 'argv').get(() => ['command', 'arg1', 'arg2']);
            sinon.stub(utils, 'getLocalBranch').returns('some-branch');

            childProcessStub = {
                unref: sinon.stub(),
            };
            spawnStub = sinon
                .stub(child_process, 'spawn')
                .returns(childProcessStub as unknown as ChildProcess);

            usageStatsPlugin(tide as Tide, options as UsageStatsConfig);
        });

        afterEach(() => {
            sinon.restore();
        });

        [
            {
                name: 'should spawn a process to handle INIT event',
                events: [events.INIT],
                expectedArgs: [
                    path.resolve(__dirname, '../../../src/plugins/tide-usage-stats/worker'),
                    '--event=init',
                    '--project=test',
                    '--branch=some-branch',
                    "--cmd='command arg1 arg2'",
                    '--test_count=-1',
                    '--tide_version=0.0.0',
                ].sort(),
            },
            {
                name: 'should spawn a process to handle END event',
                events: [events.AFTER_FILES_READ, events.END],
                expectedArgs: [
                    path.resolve(__dirname, '../../../src/plugins/tide-usage-stats/worker'),
                    '--event=end',
                    '--project=test',
                    '--branch=some-branch',
                    "--cmd='command arg1 arg2'",
                    '--test_count=3',
                    '--tide_version=0.0.0',
                ].sort(),
            },
        ].forEach((testCase) => {
            it(testCase.name, () => {
                testCase.events.forEach((event) => tide.emit(event));

                assert(spawnStub.calledOnce);
                assert(spawnStub.firstCall.args[0] === 'node');
                assert.deepEqual(
                    spawnStub.firstCall.args[1].concat().sort(),
                    testCase.expectedArgs,
                );

                assert(childProcessStub.unref.calledOnce);
            });
        });
    });
});
