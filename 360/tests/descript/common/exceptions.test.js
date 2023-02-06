const de = require('@ps-int/ufo-descript');
const path = require('path');

describe('Процесс descript', () => {
    beforeEach(() => {
        process.argv = [];

        de.script.init();
    });

    afterEach(() => {
        de.file.unwatch();
    });

    const throwingHooks = ['guard', 'before', 'key', 'params', 'state', 'after', 'result', 'template'];

    throwingHooks.forEach((hookName) => {
        it(`не падает при возникновении исключения в хуке '${hookName}'`, () => {
            const block = new de.Block.Value(1, {
                [hookName]() {
                    throw new Error('error');
                }
            });

            expect(() => {
                block.run();
            }).not.toThrow();
        });
    });

    const makeFailingBlock = () =>
        new de.Block.Value(1, {
            params() {
                throw new Error('error');
            }
        });

    const compositeBlocks = {
        'de.Block.Array': () => new de.Block.Array([makeFailingBlock()]),
        'de.Block.Object': () => new de.Block.Object({ failingBlock: makeFailingBlock() })
    };

    for (const compositeBlockName in compositeBlocks) {
        it(`не падает при возникновении исключения внутри блока, присутствующего в '${compositeBlockName}'`, (done) => {
            expect(() => {
                compositeBlocks[compositeBlockName]().run().done(() => done());
            }).not.toThrow();
        });
    }

    it('не падает при возникновении исключения внутри блока, подключаемого через de.Block.Include', (done) => {
        const failingBlockAbsolutePath = path.resolve(__dirname, './failingblock.js');
        const failingBlockRelativePath = path.relative(process.cwd(), failingBlockAbsolutePath);

        const block = new de.Block.Include(failingBlockRelativePath);

        expect(() => {
            block.run().done(() => {
                de.events.trigger('loaded-file-changed', failingBlockAbsolutePath);
                done();
            });
        }).not.toThrow();
    });

    it('не падает при возникновении исключения при рендеринге темплейта', (done) => {
        const failingTemplateAbsolutePath = path.resolve(__dirname, './failingtemplate.js');
        const failingTemplateRelativePath = path.resolve(process.cwd(), failingTemplateAbsolutePath);

        const block = new de.Block.Value(1, {
            template() {
                return failingTemplateRelativePath;
            }
        });

        expect(() => {
            block.run().done(() => {
                done();
            });
        }).not.toThrow();
    });
});
