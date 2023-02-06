import 'mocha';
import { expect } from 'chai';
import j from 'jscodeshift';
import * as recast from 'recast';
import type { Program } from 'jscodeshift';
import type { Test } from '../../../../src';
import { HermioneRenameQueueItem, Update } from '../../../../src/plugins/tide-renamer/types';
import {
    makeUpdateQueue,
    applyChanges,
} from '../../../../src/plugins/tide-renamer/renamer/hermione';
import { mkAST } from '../fixtures';
import { HermioneAstParser } from '../../../../src';

describe('tide-renamer / renamer / hermione', () => {
    describe('makeUpdateQueue', () => {
        it('should return queue with test updates', () => {
            const updates: Array<Update> = [
                {
                    from: { feature: 'F-1', experiment: 'E-3' },
                    to: { feature: 'Feature-2', experiment: 'Exp-0' },
                },
                { from: 'P-4', to: 'Part-6' },
                { from: 'P-5', to: 'Part-7' },
            ];
            const b = recast.types.builders;
            const ast = mkAST();
            const test = {
                files: {
                    hermione: {
                        ast: j(ast as Program),
                    },
                },
            };
            const constants = {
                hermione: {
                    TITLE_KEYS: ['feature', 'type', 'experiment'],
                    AST_CALLEE_NAMES: ['specs', 'describe', 'it'],
                },
            };
            const parser = new HermioneAstParser();

            const expectedQueue: Array<HermioneRenameQueueItem> = [
                {
                    node: b.objectExpression([
                        b.property('init', b.identifier('feature'), b.literal('F-1')),
                        b.property('init', b.identifier('experiment'), b.literal('E-3')),
                    ]),
                    key: 'properties',
                    value: [
                        b.property('init', b.stringLiteral('feature'), b.literal('Feature-2')),
                        b.property('init', b.stringLiteral('experiment'), b.literal('Exp-0')),
                    ],
                },
                { node: b.literal('P-4'), key: 'value', value: 'Part-6' },
                { node: b.literal('P-5'), key: 'value', value: 'Part-7' },
            ].sort();

            const actualQueue = makeUpdateQueue(
                updates,
                test as unknown as Test,
                constants,
                parser,
            ).sort();

            const prepareQueue = (queue: HermioneRenameQueueItem[]): void => {
                for (const item of queue) {
                    if (Array.isArray(item.value)) {
                        item.value = item.value.map((property) =>
                            JSON.parse(j(j.objectExpression([property])).toSource()),
                        );
                    }
                    item.node = j(j.expressionStatement(item.node as any)).toSource() as any;
                }
            };

            prepareQueue(actualQueue);
            prepareQueue(expectedQueue);
            expect(actualQueue).deep.equal(expectedQueue);
        });
    });

    describe('applyChanges', () => {
        it('should apply changes from the queue', () => {
            const b = recast.types.builders;
            const literalsToChange = [
                b.literal('F-1'),
                b.literal('T-2'),
                b.literal('E-3'),
                b.literal('P-7'),
            ];
            const renameQueue: Array<HermioneRenameQueueItem> = [
                { node: literalsToChange[0], key: 'value', value: 'Feature-2' },
                { node: literalsToChange[1], key: 'value', value: 'Type-5' },
                { node: literalsToChange[2], key: 'value', value: 'Exp-1' },
                { node: literalsToChange[3], key: 'value', value: 'Part-9' },
            ];

            applyChanges(renameQueue);

            expect(literalsToChange[0]).deep.equal(b.literal('Feature-2'));

            expect(literalsToChange[1]).deep.equal(b.literal('Type-5'));

            expect(literalsToChange[2]).deep.equal(b.literal('Exp-1'));

            expect(literalsToChange[3]).deep.equal(b.literal('Part-9'));
        });
    });
});
