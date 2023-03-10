const fs = require('fs');
const path = require('path');
const assert = require('assert');
const BJSON = require('buffer-json');
const { matchContent, matchBinary } = require('./utils/test');
const { preparePng, prepareSvg, processHandler, parseQuery } = require('./handlers');

const dirPath = path.join(process.cwd(), 'tests', 'fixtures', 'responses');

const mockLogger = {
    info: () => {},
    debug: () => {},
    error: () => {},
};

const matchSnapshot = type => {
    switch (type) {
        case 'binary':
            return matchBinary(dirPath);
    }

    return matchContent(dirPath);
};

describe('handlers', () => {
    describe('workflow', () => {
        let svgContent;

        beforeEach(() => {
            svgContent = fs.readFileSync(path.join(process.cwd(), 'tests', 'fixtures', 'svg', 'prof_math.svg'), 'utf8');
        });

        it('svg workflow', async() => {
            const result = await prepareSvg(svgContent);

            assert(result.svg);
            await matchSnapshot()('prof_math_optim_svg.json', JSON.stringify(result));
        });

        it('png workflow', async() => {
            const result = await preparePng(svgContent, {
                height: 100,
                width: 700,
            });

            assert(result.png);
            await matchSnapshot('binary')('prof_math_optim.png', result.png);
        });
    });

    describe('handlers', () => {
        const fakeResponse = () => ({
            json: function(response) {
                this.response = response;
            },
            send: function(response) {
                this.response = response;
            },
            type: () => {},
        });

        const checkResponse = async(expectedResponsePath, res) => {
            if (res.response instanceof Buffer) {
                await matchSnapshot('binary')(expectedResponsePath, res.response);
                return;
            }

            await matchSnapshot()(
                expectedResponsePath,
                typeof res.response !== 'string' ? BJSON.stringify(res.response) : res.response
            );
        };

        it('get svg', async() => {
            const res = fakeResponse();

            await processHandler(
                {
                    query: {
                        tex: 'E=mc^{2}',
                        format: 'svg',
                    },
                    logger: mockLogger,
                },
                res
            );

            await checkResponse('svg_response.svg', res);
        });

        it('get png', async() => {
            const res = fakeResponse();

            await processHandler(
                {
                    query: {
                        tex: 'E=mc^{2}',
                        format: ['png'],
                    },
                    logger: mockLogger,
                },
                res
            );

            await checkResponse('png_response.png', res);
        });

        it('get svg and png', async() => {
            const res = fakeResponse();

            await processHandler(
                {
                    query: {
                        tex: 'E=mc^{2}',
                        format: ['png', 'svg'],
                    },
                    logger: mockLogger,
                },
                res
            );

            await checkResponse('png-svg.json', res);
        });

        it('without tex', async() => {
            const res = fakeResponse();

            await processHandler(
                {
                    query: {
                        tex: '',
                        format: ['png'],
                    },
                    logger: mockLogger,
                },
                res
            );

            await checkResponse('without-tex.json', res);
        });
    });

    describe('parseQuery', () => {
        it('empty query', () => {
            assert.deepEqual(parseQuery({}), { format: new Set(['svg']), inline: false });
        });

        it('custom format', () => {
            assert.deepEqual(
                parseQuery({
                    format: ['svg', 'png'],
                }),
                {
                    format: new Set(['svg', 'png']),
                    inline: false,
                }
            );
        });

        it('custom font size', () => {
            assert.deepEqual(
                parseQuery({
                    ex_size: '10',
                }),
                {
                    exSize: 10,
                    format: new Set(['svg']),
                    inline: false,
                }
            );
        });

        it('invalid font size', () => {
            assert.deepEqual(
                parseQuery({
                    ex_size: 'aaa',
                }),
                {
                    format: new Set(['svg']),
                    inline: false,
                }
            );
        });
    });
});
