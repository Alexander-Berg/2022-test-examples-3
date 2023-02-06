import { assert } from 'chai';
import sinon from 'sinon';
import { Tide, TideConfig } from '../../../src';
import * as Config from '../../../src/config';
import { ConflictMode } from '../../../src/types';
import FileCollection = require('../../../src/file-collection');
import TestCollection = require('../../../src/test-collection');

function createTideStub(): Tide {
    sinon.stub(Tide.prototype as any, '_addDebugEventListeners');
    sinon.stub(Tide.prototype as any, '_loadPlugins');
    sinon.stub(Tide.prototype as any, '_addExitEmitter');
    sinon.stub(Config, 'parseConfig').returns({
        parsers: {},
        plugins: {},
        conflictMode: ConflictMode.SaveNew,
    });
    return new Tide({} as TideConfig);
}

describe('tide', () => {
    afterEach(() => {
        sinon.restore();
    });

    describe('extendCli', () => {
        it('should emit CLI event', () => {
            const tide = createTideStub();
            const emitStub = sinon.stub(tide, 'emit');

            tide.extendCli({} as any);

            assert(emitStub.calledOnceWith(tide.events.CLI, {}));
        });
    });

    describe('run', () => {
        beforeEach(() => {
            sinon.stub(FileCollection as any, 'default');
            sinon.stub(TestCollection as any, 'default');
        });

        it('should emit events in a correct order', async () => {
            const tide = createTideStub();
            const initStub = sinon.stub(tide as any, '_init');
            const emitStub = sinon.stub(tide, 'emit');
            const emitAsyncStub = sinon.stub(tide, 'emitAsync');
            const emitAsyncSerialStub = sinon.stub(tide, 'emitAsyncSerial');
            const readStub = sinon.stub(tide, 'read');
            const writeStub = sinon.stub(tide, 'write');

            await tide.run();

            const [start] = emitStub.getCalls();
            const [beforeFilesRead, afterFilesRead, beforeFilesWrite, afterFilesWrite] =
                emitAsyncSerialStub.getCalls();
            const [end] = emitAsyncStub.getCalls();

            const read = readStub.getCall(0);
            const write = writeStub.getCall(0);

            assert(initStub.calledOnce);
            assert(emitStub.calledOnce);
            assert(emitAsyncSerialStub.callCount === 4);
            assert(emitAsyncStub.calledOnce);

            assert(beforeFilesRead.calledAfter(start));
            assert(read.calledAfter(beforeFilesRead));
            assert(afterFilesRead.calledAfter(read));
            assert(beforeFilesWrite.calledAfter(afterFilesRead));
            assert(write.calledAfter(beforeFilesWrite));
            assert(afterFilesWrite.calledAfter(write));
            assert(end.calledAfter(afterFilesWrite));
        });
    });
});
