const config = require('config');
const proxyquire = require('proxyquire');

const uploadDataModuleStub = { uploadDataAsFile: sinon.stub() };
const { tryToMoveQueriesToMds, shouldQueriesBeMovedToMds } = proxyquire.load('../../../src/server/helpers/move-queries-to-mds', {
    './upload-data-to-mds': uploadDataModuleStub,
});

describe('helpers/move-queries-to-mds', () => {
    let sandbox, params, logger;

    beforeEach(() => {
        sandbox = sinon.createSandbox();
        logger = { error: () => {} };
        params = {
            queries: { source: 'text', lines: 1000, val: 'q' },
        };
    });

    afterEach(() => sandbox.restore());

    describe('shouldQueriesBeMovedToMds:', () => {
        it('должен вернуть false, если source не text', () => {
            const queriesStub = { source: 'test' };
            const result = shouldQueriesBeMovedToMds(queriesStub);
            assert.isFalse(result);
        });

        it('должен вернуть false, если lines меньше разрешенного значения', () => {
            params.queries.lines = config.queriesLimits.linesCount - 10;
            const result = shouldQueriesBeMovedToMds(params.queries);
            assert.isFalse(result);
        });

        it('должен вернуть true, если lines больше разрешенного значения', () => {
            params.queries.lines = config.queriesLimits.linesCount + 10;
            const result = shouldQueriesBeMovedToMds(params.queries);
            assert.isTrue(result);
        });

        it('должен вернуть true, если вес строки в байтах больше разрешенного значения', () => {
            params.queries.val = new Array(config.queriesLimits.bytesCount + 1000).fill('q').join('');
            const result = shouldQueriesBeMovedToMds(params.queries);
            assert.isTrue(result);
        });
    });

    describe('tryToMoveQueriesToMds:', () => {
        it('должен вернуть params, с измененными значениями queries', async() => {
            const URL = 'https://url.com';
            uploadDataModuleStub.uploadDataAsFile.returns(Promise.resolve(URL));

            const updatedQueries = await tryToMoveQueriesToMds(1, params.queries, logger);

            assert.equal(updatedQueries.source, 'url');
            assert.equal(updatedQueries.val, URL);
            assert.exists(updatedQueries.lines);
        });

        it('должен вернуть неизмененный params в случае ошибки во время загрузки данных на mds', async() => {
            uploadDataModuleStub.uploadDataAsFile.returns(Promise.reject());
            const result = await tryToMoveQueriesToMds(1, params.queries, logger);

            assert.deepEqual(result, params.queries);
        });

        it('должен залогировать ошибку в случае ошибки во время загрузки данных на mds', async() => {
            uploadDataModuleStub.uploadDataAsFile.returns(Promise.reject());
            logger.error = sinon.spy();

            await tryToMoveQueriesToMds(1, params.queries, logger);

            assert.isTrue(logger.error.calledOnce);
        });
    });
});

