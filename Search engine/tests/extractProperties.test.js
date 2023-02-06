import assert from 'assert';

import deepFreeze from 'deep-freeze-strict';

import {extractProperties} from '../h';

describe('Extract properties', () => {
    test('without properties', () => {
        const options = {
            key: 'key',
            otherProperty: 'value',
        };
        const {props, restOptions} = extractProperties('span', options);
        assert.deepStrictEqual(restOptions, options);
        assert.deepStrictEqual(props, {});
    });

    test('with properties, without tag', () => {
        const options = {
            key: 'key',
            otherProperty: 'value',
            id: 'id',
            className: 'className',
        };
        const {props, restOptions} = extractProperties(null, options);
        assert.deepStrictEqual(restOptions, {
            key: 'key',
            otherProperty: 'value',
        });
        assert.deepStrictEqual(props, {
            id: 'id',
            className: 'className',
        });
    });

    test('with properties, with unknown tag', () => {
        const options = {
            key: 'key',
            otherProperty: 'value',
            id: 'id',
            className: 'className',
        };
        const {props, restOptions} = extractProperties('unknown', options);
        assert.deepStrictEqual(restOptions, {
            key: 'key',
            otherProperty: 'value',
        });
        assert.deepStrictEqual(props, {
            id: 'id',
            className: 'className',
        });
    });

    test('with properties, with known tag', () => {
        const options = {
            key: 'key',
            otherProperty: 'value',
            id: 'id',
            className: 'className',
            colSpan: 1,
        };
        const {props, restOptions} = extractProperties('td', options);
        assert.deepStrictEqual(restOptions, {
            key: 'key',
            otherProperty: 'value',
        });
        assert.deepStrictEqual(props, {
            id: 'id',
            className: 'className',
            colSpan: 1,
        });
    });

    test('options object is not mutated', () => {
        const options = {
            key: 'key',
            otherProperty: 'value',
            id: 'id',
            className: 'className',
        };
        extractProperties(null, deepFreeze(options));
    });
});
