import 'mocha';
import fse from 'fs-extra';
import sinon from 'sinon';
import { expect } from 'chai';
import type { Test } from '../../../../src';
import { updateMetricsFile } from '../../../../src/plugins/tide-renamer/renamer/metrics';

describe('tide-renamer / renamer / metrics', () => {
    describe('updateMetricsJson', () => {
        it("shouldn't do anything if metrics file is absent", () => {
            sinon.stub(fse, 'access').throws();
            const test = { filePaths: { hermione: '/some/path/test.hermione.js' } };
            const fseReadFileStub = sinon.stub(fse, 'readFile');
            const fseWriteFileStub = sinon.stub(fse, 'writeFile');

            updateMetricsFile('A', 'B', test as unknown as Test);

            expect(fseReadFileStub.notCalled).equal(true);
            expect(fseWriteFileStub.notCalled).equal(true);
        });

        it('should change and write updated metrics.json', async () => {
            const fakeFileContents = JSON.stringify({
                OldName: 'A-value',
                AnotherTest: 'test-value',
                LastTest: 'last-value',
            });
            const expectedObject = {
                NewName: 'A-value',
                AnotherTest: 'test-value',
                LastTest: 'last-value',
            };
            const test = { filePaths: { hermione: '/some/path/test.hermione.js' } };

            const fseAccessStub = sinon.stub(fse, 'access');
            fseAccessStub.withArgs('/some/path/test.metrics.json').resolves(true);

            const fseReadFileStub = sinon.stub(fse, 'readFile');
            fseReadFileStub.withArgs('/some/path/test.metrics.json').resolves(fakeFileContents);

            const fseWriteFileStub = sinon.stub(fse, 'writeFile');

            await updateMetricsFile('OldName', 'NewName', test as unknown as Test);

            expect(fseReadFileStub.calledOnce).equal(true);
            expect(fseWriteFileStub.calledOnce).equal(true);
            expect('/some/path/test.metrics.json').equal(fseWriteFileStub.firstCall.args[0]);
            expect(JSON.parse(fseWriteFileStub.firstCall.args[1])).deep.equal(expectedObject);
        });
    });
    afterEach(() => {
        sinon.restore();
    });
});
