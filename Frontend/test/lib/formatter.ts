import 'mocha';

import assert from 'assert';
import {
    ICUMessagePart,
    StringType,
    ArgumentType,
    SelectType,
    CaseType,
    OctothorpeType,
} from '../../lib/formatter';

describe('Lib. Formatter. ICUMessagePart', () => {
    describe('#getPartType', () => {
        it('should return instance of StringType', () => {
            const stringType = ICUMessagePart.getPartType('test string');

            assert.strictEqual(stringType instanceof StringType, true);
        });

        it('should return instance of OctothorpeType', () => {
            const octothorpeType = ICUMessagePart.getPartType({ type: 'octothorpe' });

            assert.strictEqual(octothorpeType instanceof OctothorpeType, true);
        });

        it('should return instance of ArgumentType', () => {
            const argumentParsedPart = { type: 'argument', arg: 'value' };
            const argumentType = ICUMessagePart.getPartType(argumentParsedPart);

            assert.strictEqual(argumentType instanceof ArgumentType, true);
        });

        it('should return instance of SelectType', () => {
            const selectParsedPart = {
                type: 'select',
                arg: 'eventType',
                cases: [
                    { key: 'concert', tokens: ['Концерт'] },
                ],
            };
            const selectType = ICUMessagePart.getPartType(selectParsedPart);

            assert.strictEqual(selectType instanceof SelectType, true);
        });
    });
});

describe('Lib. Formatter. StringType', () => {
    describe('#toString', () => {
        it('should return string', () => {
            const stringType = new StringType('test string');

            assert.strictEqual(stringType.toString(), 'test string');
        });
    });
});

describe('Lib. Formatter. OctothorpeType', () => {
    describe('#toString', () => {
        it('should return string', () => {
            const octothorpeType = new OctothorpeType({ type: 'octothorpe' });

            assert.strictEqual(octothorpeType.toString(), '#');
        });
    });
});

describe('Lib. Formatter. ArgumentType', () => {
    describe('#toString', () => {
        it('should return string', () => {
            const argType = new ArgumentType({ type: 'argument', arg: 'some arg' });

            assert.strictEqual(argType.toString(), '{some arg}');
        });
    });
});

describe('Lib. Formatter. CaseType', () => {
    describe('#toString', () => {
        it('should return array of string', () => {
            const caseType = new CaseType({
                key: 'key', tokens: [
                    'first value',
                    'second value',
                    'third value',
                ],
            });

            assert.deepStrictEqual(caseType.toString(), [
                'key {', 'first value', 'second value', 'third value', '}',
            ]);
        });
    });
});

describe('Lib. Formatter. SelectType', () => {
    describe('#toString', () => {
        it('should return array of string', () => {
            const selectType = new SelectType({
                type: 'select',
                arg: 'select arg',
                cases: [
                    { key: 'first key', tokens: ['first value'] },
                    { key: 'second key', tokens: ['second value'] },
                ],
            });

            assert.deepStrictEqual(selectType.toString(), [
                '{select arg, select,',
                ['first key {', 'first value', '}'],
                ['second key {', 'second value', '}'],
                '}',
            ]);
        });
    });
});
