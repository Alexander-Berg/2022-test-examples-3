import 'mocha';

import assert from 'assert';
import { printTabs, printStrings } from '../../lib/helpers';

describe('Helpers. printTabs', () => {
    it('should return 4 whitespace for each number', () => {
        assert.strictEqual(printTabs(2), '        ');
    });
});

describe('Helpers. printStrings', () => {
    it('should return string', () => {
        assert.strictEqual(printStrings('simple str'), 'simple str');
    });

    it('should return string for array', () => {
        const strings = [
            '{select arg, select,',
            [
                'first key {',
                [
                    'value1',
                    [['more ', 'depth ', 'value'], 'value'],
                ],
                ['value2'],
                '}',
            ],
            ['second key {', ['value1'], ['value2'], '}'],
            '}',
        ];
        assert.strictEqual(printStrings(strings), [
            '{select arg, select,',
            '    first key {',
            '        value1',
            '                more depth value',
            '            value',
            '        value2',
            '    }',
            '    second key {',
            '        value1',
            '        value2',
            '    }',
            '}',
        ].join('\n'));
    });
});
