import fs from 'fs';
import chai from 'chai';

import TestStubParser from '../../../src/parsers/tide-hermione-stub-parser/test-stub-parser';

const className = '.serp-list';

describe('tide-hermione-parser / test-stub-parser', () => {
    let parser: TestStubParser;
    const filePath = 'test/tide-hermione-parser/fixtures/basic/basic.hermione.js';

    beforeEach(() => {
        parser = new TestStubParser({
            thisStubs: {
                PO: {
                    serpList: (): string => className,
                },
            },
        });
    });

    describe('parse', () => {
        it('should read and parse hermione file', async () => {
            const expected = {
                feature: 'Feature-name',
                specs: {
                    'Describe 1': {
                        beforeEach: [
                            {
                                name: 'yaOpenSerp',
                                arguments: [{ text: 'text' }, className],
                                object: 'browser',
                                path: ['browser', 'yaOpenSerp'],
                            },
                        ],
                        'It 1': [
                            {
                                name: 'assertView',
                                arguments: ['plain', className],
                                object: 'browser',
                                path: ['browser', 'assertView'],
                            },
                            {
                                name: 'assertView',
                                arguments: ['plain 2', className],
                                object: 'browser',
                                path: ['browser', 'assertView'],
                            },
                        ],
                        'It 2': [
                            {
                                name: 'assertView',
                                arguments: ['plain', className],
                                object: 'browser',
                                path: ['browser', 'assertView'],
                            },
                            {
                                name: 'assertView',
                                arguments: ['plain 2', className],
                                object: 'browser',
                                path: ['browser', 'assertView'],
                            },
                        ],
                    },
                    'It 3': [
                        {
                            name: 'yaOpenSerp',
                            arguments: [{ text: 'text' }, className],
                            object: 'browser',
                            path: ['browser', 'yaOpenSerp'],
                        },
                        {
                            name: 'assertView',
                            arguments: ['plain', className],
                            object: 'browser',
                            path: ['browser', 'assertView'],
                        },
                    ],
                },
                files: [filePath],
            };

            const raw = fs.readFileSync(filePath, 'utf-8');
            const actual = await parser.parse(raw, filePath);

            chai.assert.deepEqual(actual, expected);
        });
    });
});
