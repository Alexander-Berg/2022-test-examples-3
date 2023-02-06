import 'mocha';
import { expect } from 'chai';

import { Command } from 'commander';
import { addOptionsFromInput } from '../../../src/plugins/tide-renamer/options';
import { TideRenamerOptions } from '../../../src/plugins/tide-renamer/types';

describe('tide-renamer / options', () => {
    describe('addOptionsFromInput', () => {
        [
            {
                name: 'should parse JSON input',
                options: {},
                inputOptions: {
                    oldName: '["Feature / Type", "Part"]',
                    newName: '["NewFeature / Type", "Part"]',
                    args: [],
                },
                input: {
                    args: [],
                },
                expectedOptions: {
                    fromTitlePath: [/^Feature \/ Type$|Feature \/ Type/, /^Part$|Part/],
                    toTitlePath: ['NewFeature / Type', 'Part'],
                },
            },
            {
                name: 'should not escape regexp characters',
                options: {},
                inputOptions: {
                    oldName: '[{"feature": "[]+ (Feature){2}", "type": ".*"}, "$"]',
                    newName: '[{"feature": "$1 $named", "type": ""}, ""]',
                },
                input: {
                    args: [],
                },
                expectedOptions: {
                    fromTitlePath: [
                        {
                            feature: /^\[\]\+ \(Feature\)\{2\}$|[]+ (Feature){2}/,
                            type: /^\.\*$|.*/,
                        },
                        /^\$$|$/,
                    ],
                    toTitlePath: [{ feature: '$1 $named', type: '' }, ''],
                },
            },
        ].forEach((test) =>
            it(test.name, () => {
                addOptionsFromInput(
                    test.options as TideRenamerOptions,
                    test.input as unknown as Command,
                    test.inputOptions as Record<string, any>,
                );

                expect(test.options).deep.equal(test.expectedOptions);
            }),
        );

        it('should throw an error if oldName and newName are of different types', () => {
            const input = {
                args: [],
            };
            const inputOptions = {
                oldName: '[]',
                newName: 'String value',
            };

            expect(
                addOptionsFromInput.bind(
                    undefined,
                    {} as TideRenamerOptions,
                    input as unknown as Command,
                    inputOptions as Record<string, any>,
                ),
            ).to.throw();
        });
    });
});
