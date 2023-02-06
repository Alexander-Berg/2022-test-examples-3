import 'mocha';
import { expect } from 'chai';
import { Test } from '../../../../src';
import {
    getAllPartitionings,
    getOptimalPartitioning,
    isMatchingTitleParts,
    isTitlePartChanged,
    parseByRegExpObject,
    parseTitlePart,
    replaceObject,
    toPlainUpdates,
} from '../../../../src/plugins/tide-renamer/renamer/utils';
import { Update, UpdateRegExp } from '../../../../src/plugins/tide-renamer/types';

const constants = { hermione: { TITLE_KEYS: ['feature', 'type', 'experiment'] } };

describe('tide-renamer / renamer / utils', () => {
    describe('replaceObject', () => {
        it('should replace values in an object', () => {
            const subjectObject = { a: 'test', b: 'unchanged', c: 'modded' };
            const fromObject = { a: /test/, c: /(\w+)/ };
            const toObject = { a: '-test-', c: '$1+$1' };
            const expectedObject = { a: '-test-', b: 'unchanged', c: 'modded+modded' };

            const actualObject = replaceObject(subjectObject, fromObject, toObject);

            expect(actualObject).deep.equal(expectedObject);
        });
    });

    describe('getAllPartitionings', () => {
        it('should generate all possible partitionings', () => {
            const subject = ['A', 'B', 'C', 'D', 'E'];
            const expectedPartitionings = [
                ['A', 'B', 'C / D / E'],
                ['A', 'B / C', 'D / E'],
                ['A', 'B / C / D', 'E'],
                ['A / B', 'C / D', 'E'],
                ['A / B / C', 'D', 'E'],
                ['A / B', 'C', 'D / E'],
            ].sort();

            const actualPartitionings = getAllPartitionings(subject, 3).sort();

            expect(actualPartitionings).deep.equal(expectedPartitionings);
        });

        it('should return one partition if subject.length === partsCount', () => {
            const subject = ['A', 'B', 'C', 'D', 'E'];
            const expectedPartitionings = [['A', 'B', 'C', 'D', 'E']].sort();

            const actualPartitionings = getAllPartitionings(subject, 5).sort();

            expect(actualPartitionings).deep.equal(expectedPartitionings);
        });

        it('should return an empty array if partsCount > subject.length', () => {
            const subject = ['A', 'B', 'C', 'D', 'E'];

            const actualPartitionings = getAllPartitionings(subject, 10);

            expect(actualPartitionings).deep.equal([]);
        });
    });

    describe('getOptimalPartitioning', () => {
        it('should find optimal partitioning', () => {
            // a a b b c c
            const partitionings = [
                ['a a b', 'b', 'c c'],
                ['a a b b', 'c', 'c'],
                ['a a b', 'b c', 'c'],
                ['a a', 'b b c', 'c'],
                ['a a', 'b b', 'c c'],
                ['a', 'a b b', 'c c'],
                ['a', 'a b b c', 'c'],
            ];
            const targetPartitioning = ['a a', 'b e', 'c f'];
            const optimalPartitioning = partitionings[4];

            const actualPartitioning = getOptimalPartitioning(partitionings, targetPartitioning);

            expect(actualPartitioning).deep.equal(optimalPartitioning);
        });
    });

    describe('parseByRegExpObject', () => {
        it('should parse string into object', () => {
            const subjectString = 'Feature-1 / Type-2 / Exp-1';
            const toObject: Array<[string, RegExp]> = [
                ['feature', /.+/],
                ['type', /Type-\d+/],
                ['experiment', /.+/],
            ];
            const expectedObject = { feature: 'Feature-1', type: 'Type-2', experiment: 'Exp-1' };

            const actualObject = parseByRegExpObject(subjectString, toObject);

            expect(actualObject).deep.equal(expectedObject);
        });

        it('should return null if fails to parse', () => {
            const subjectString = 'Feature / Type-2 / Exp-1';
            const toObject: Array<[string, RegExp]> = [
                ['feature', /Feature-\d+/],
                ['type', /Type-\d+/],
                ['experiment', /Exp-\d+/],
            ];

            const actualObject = parseByRegExpObject(subjectString, toObject);

            expect(actualObject).equal(null);
        });
    });

    describe('toPlainUpdates', () => {
        it('should convert regexp updates array to plain updates', () => {
            const updates: Array<UpdateRegExp> = [
                {
                    from: { feature: /Feature-(\d+)/, type: /Type-(\d+)/ },
                    to: { feature: 'F-$1', type: 'T-$1' },
                },
                { from: /Descr-(\d+)/, to: 'D-$1' },
                { from: /It-(\d+)/, to: 'I-$1' },
            ];
            const test = {
                titlePath: [
                    'Feature-3 / Type-1',
                    'Descr-4',
                    'It-6',
                    "Part that doesn't exist at updates",
                ],
                fullTitle: (): string =>
                    "Feature-3 / Type-1 Descr-4 It-6 Part that doesn't exist at updates",
            };
            const expectedPlainUpdates: Array<Update> = [
                {
                    from: { feature: 'Feature-3', type: 'Type-1' },
                    to: { feature: 'F-3', type: 'T-1' },
                },
                { from: 'Descr-4', to: 'D-4' },
                { from: 'It-6', to: 'I-6' },
                {
                    from: "Part that doesn't exist at updates",
                    to: "Part that doesn't exist at updates",
                },
            ];

            const actualPlainUpdates = toPlainUpdates(updates, test as Test, constants);

            expect(actualPlainUpdates).deep.equal(expectedPlainUpdates);
        });

        it('should covert update object to plain updates', () => {
            const test = {
                titlePath: ['Feature-3 / Type-1', 'Descr-4', 'Part-6 Part-8'],
                fullTitle: (): string => 'Feature-3 / Type-1 Descr-4 Part-6 Part-8',
            };
            const update = {
                from: /Feature-3 \/ Type-1 Descr-4 Part-6 Part-8/,
                to: 'Feature-9 / Type Descr-4 Part-9 Part-7',
            };

            const expectedPlainUpdates: Array<Update> = [
                { from: 'Feature-3 / Type-1', to: 'Feature-9 / Type' },
                { from: 'Descr-4', to: 'Descr-4' },
                { from: 'Part-6 Part-8', to: 'Part-9 Part-7' },
            ];

            const actualPlainUpdates = toPlainUpdates(update, test as Test, constants);

            expect(actualPlainUpdates).deep.equal(expectedPlainUpdates);
        });
    });

    describe('isMatchingTitleParts', () => {
        it('should return true for matching string and RegExp', () => {
            const result = isMatchingTitleParts(
                'Feature-1 / Type-2',
                /\w+-\d+ \/ \w+-\d+/,
                constants,
            );

            expect(result).equal(true);
        });

        it('should return false for non-matching string and RegExp', () => {
            const result = isMatchingTitleParts(
                'Feature1 / Type2',
                /\w+-\d+ \/ \w+-\d+/,
                constants,
            );

            expect(result).equal(false);
        });

        it('should return true for matching string and RegExpObject', () => {
            const pattern = { feature: /\w+-\d+/, type: /\w+-\d+/ };

            const result = isMatchingTitleParts('Feature-1 / Type-2', pattern, constants);

            expect(result).equal(true);
        });

        it('should return false for non-matching string and RegExpObject', () => {
            const pattern = { feature: /\w+-\d+/, type: /\w+-\d+/ };

            const result = isMatchingTitleParts('Feature1 / Type2', pattern, constants);

            expect(result).equal(false);
        });
    });

    describe('isTitlePartChanged', () => {
        it("should return false if titlePart won't be changed when update contains an object", () => {
            const subject = 'Feature-1 / Type-2 / Exp-3';
            const update: UpdateRegExp = {
                from: { feature: /(.+)/, type: /(\w+)-(\d+)/, experiment: /Exp-3/ },
                to: { feature: '$1', type: '$1-$2', experiment: 'Exp-3' },
            };

            const result = isTitlePartChanged(subject, update, constants);

            expect(result).equal(false);
        });

        it('should return true if titlePart will be changed when update contains an object', () => {
            const subject = 'Feature-1 / Type-2 / Exp-3';
            const update: UpdateRegExp = {
                from: { feature: /(.+)/, type: /(\w+)-(\d+)/, experiment: /Exp-3/ },
                to: { feature: '$1', type: '$1+$2', experiment: 'Exp-3' },
            };

            const result = isTitlePartChanged(subject, update, constants);

            expect(result).equal(true);
        });

        it("should return false if titlePart won't be changed when update contains a string", () => {
            const subject = 'Feature-1 / Type-2 / Exp-3';
            const update: UpdateRegExp = {
                from: /Feature-(\d+) \/ Type-(\d+) \/ Exp-(\d+)/,
                to: 'Feature-$1 / Type-2 / Exp-$3',
            };

            const result = isTitlePartChanged(subject, update, constants);

            expect(result).equal(false);
        });

        it('should return true if titlePart will be changed when update contains a string', () => {
            const subject = 'Feature-1 / Type-2 / Exp-3';
            const update: UpdateRegExp = {
                from: /Feature-(\d+) \/ Type-(\d+) \/ Exp-(\d+)/,
                to: 'Feature-1 / Type-$3 / Exp-3',
            };

            const result = isTitlePartChanged(subject, update, constants);

            expect(result).equal(true);
        });

        it("should return false if titlePart doesn't match pattern", () => {
            const subject = 'Feature-1 / Type-2 / Exp-3';
            const update: UpdateRegExp = {
                from: {
                    no: /^match$/,
                },
                to: 'Feature-1 / Type-$3 / Exp-3',
            };

            const result = isTitlePartChanged(subject, update, constants);

            expect(result).equal(false);
        });
    });

    describe('parseTitlePart', () => {
        it('should return correct object from string', () => {
            const titlePart = 'F-4 / E-1';
            const oldObject = {
                feature: 'F-1',
                experiment: 'E-3',
            };
            const expectedParsedPart = {
                feature: 'F-4',
                experiment: 'E-1',
            };

            const actualParsedPart = parseTitlePart(titlePart, oldObject, constants);

            expect(actualParsedPart).deep.equal(expectedParsedPart);
        });

        it('should return correct object from string with multiple parts', () => {
            const titlePart = 'F-4 / T-1 / T-2 / E-1 / E-2 / E-3';
            const oldObject = {
                feature: 'F-1',
                type: 'T-5 / T-3',
                experiment: 'E-3 / E-6 / E-0',
            };
            const expectedParsedPart = {
                feature: 'F-4',
                type: 'T-1 / T-2',
                experiment: 'E-1 / E-2 / E-3',
            };

            const actualParsedPart = parseTitlePart(titlePart, oldObject, constants);

            expect(actualParsedPart).deep.equal(expectedParsedPart);
        });

        it('should return empty object on fail', () => {
            const titlePart = 'F-4 / T-3';
            const oldObject = {
                feature: 'F-1',
                type: 'T-2',
                experiment: 'E-3',
            };

            const actualParsedPart = parseTitlePart(titlePart, oldObject, constants);

            expect(actualParsedPart).deep.equal({});
        });

        it('should return object untouched', () => {
            const titlePart = {
                feature: 'F-4',
                type: 'T-3',
            };
            const oldObject = {
                feature: 'F-1',
                type: 'T-2',
            };

            const actualParsedPart = parseTitlePart(titlePart, oldObject, constants);

            expect(actualParsedPart).deep.equal(titlePart);
        });
    });
});
