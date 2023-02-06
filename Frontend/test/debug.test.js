const { parseMdKeepPosition } = require('./utils/parse-md');

/**
 * This test run for fast debug from IDE
 */
describe('Test for debug purposes', () => {
    it('will run debug session', () => {
        const md = '<placeholder>';
        const result = parseMdKeepPosition(md);

        expect(result).toEqual({
            type: 'root',
            children: [
                {
                    type: 'html',
                    value: '<placeholder>',
                    position: {
                        start: {
                            offset: 0,
                            line: 1,
                            column: 1,
                        },
                        end: {
                            offset: 13,
                            line: 1,
                            column: 14,
                        },
                    },
                },
            ],
            position: {
                start: {
                    offset: 0,
                    line: 1,
                    column: 1,
                },
                end: {
                    offset: 13,
                    line: 1,
                    column: 14,
                },
            },
        });
    });
});
