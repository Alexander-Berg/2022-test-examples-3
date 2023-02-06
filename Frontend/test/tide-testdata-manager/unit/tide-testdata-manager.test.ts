import 'mocha';
import { assert } from 'chai';
import sinon from 'sinon';
import { Test, Tide, File } from '../../../src';
import { TideTestdataManager } from '../../../src/plugins/tide-testdata-manager/tide-testdata-manager';
import { UrlPlain } from '../../../src/plugins/tide-testdata-manager/types';
import { parseUrlPlain } from '../../../src/plugins/tide-testdata-manager/utils';

function mkTide(tests: Test[]): Tide {
    return {
        constants: {
            hermione: {},
            testpalm: {},
        },
        testCollection: {
            eachTest(cb: (test: Test) => void): void {
                tests.forEach(cb);
            },
        },
    } as Tide;
}

describe('tide-testdata-manager', () => {
    describe('update', () => {
        let tide: Tide;
        let testdataManager: TideTestdataManager;
        let test: Test;
        let oldUrl: UrlPlain;
        let newUrl: UrlPlain;
        let updateHermioneFileStub;

        beforeEach(() => {
            test = { files: { testdata: [] }, titlePath: ['123', '456'] } as unknown as Test;
            tide = mkTide([test]);

            oldUrl = parseUrlPlain('?param=value');
            newUrl = parseUrlPlain('?another=new-value');

            testdataManager = new TideTestdataManager(tide, {
                oldUrl,
                newUrl,
            });

            updateHermioneFileStub = sinon.stub(testdataManager, 'updateHermioneFile');
        });

        afterEach(() => {
            sinon.restore();
        });

        it('should call updateHermioneFile for hermione files', () => {
            testdataManager.updateUrl(
                {} as UrlPlain,
                {} as UrlPlain,
                {
                    tool: 'hermione',
                } as File,
                test.titlePath,
            );

            assert(
                updateHermioneFileStub.calledOnceWith(
                    {} as UrlPlain,
                    {} as UrlPlain,
                    {
                        tool: 'hermione',
                    } as File,
                    test.titlePath,
                ),
            );
        });
    });
});
