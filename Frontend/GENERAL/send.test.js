const SendCommand = require('./send');

describe('SendCommand', () => {
    const reselectiveMock = {
        directlyAffected: [
            {
                entity: {
                    block: 'Attach',
                },
                tech: 'css',
                layer: 'common',
            },
            {
                entity: {
                    block: 'Button',
                    mod: { name: 'theme', val: 'normal' },
                },
                tech: 'css',
                layer: 'common',
            },
            {
                entity: {
                    block: 'Button',
                    mod: { name: 'pseudo', val: true },
                },
                tech: 'css',
                layer: 'common',
            },
        ],
        affectedDependents: [
            {
                entity: {
                    block: 'Button',
                },
                tech: 'css',
                layer: 'common',
            },
        ],
        plainBlocksList: [
            'Attach',
            'Button',
        ],
    };

    let instance;

    beforeEach(() => {
        instance = new SendCommand([], {});
    });

    describe('#createReport', () => {
        it('should correct parse input json', () => {
            expect(instance.parseInput(JSON.stringify(reselectiveMock))).toEqual(reselectiveMock);
        });

        it('should throw the syntax error', () => {
            expect(() => instance.parseInput('')).toThrow(SyntaxError);
        });

        it('should throw the schema error', () => {
            expect(() => instance.parseInput(JSON.stringify({ blah: [] })))
                .toThrow(/Reselective output doesn't match/);
        });
    });
});
