import 'mocha';

import assert from 'assert';
import { format } from '..';

describe('format', () => {
    it('should print simple message without new lines and tabs', () => {
        const msg = 'So {wow}.';

        assert.strictEqual(format(msg), 'So {wow}.');
    });

    it('should print message with selectordinal type', () => {
        const msg = 'Such { thing }. { count, selectordinal, one {First} two {Second} few {Third} other {#th} } word.';

        assert.strictEqual(format(msg), [
            'Such {thing}. {count, selectordinal,',
            '    one {First}',
            '    two {Second}',
            '    few {Third}',
            '    other {#th}',
            '} word.',
        ].join('\n'));
    });

    it('should print message with plural type', () => {
        const msg = 'Many{type,select,plural{ numbers}selectordinal{ counting} select{ choices}other{ some {type}}}.';

        assert.strictEqual(format(msg), [
            'Many{type, select,',
            '    plural { numbers}',
            '    selectordinal { counting}',
            '    select { choices}',
            '    other { some {type}}',
            '}.',
        ].join('\n'));
    });

    it('should print message with nested select', () => {
        const msg = '{eventType, select, concert {{tagCase, select, many {Концерты {inCity}} other {Концерт {inCity}}}} other {Расписание {inCity}}}';

        assert.strictEqual(format(msg), [
            '{eventType, select,',
            '    concert {{tagCase, select,',
            '        many {Концерты {inCity}}',
            '        other {Концерт {inCity}}',
            '    }}',
            '    other {Расписание {inCity}}',
            '}',
        ].join('\n'));
    });

    it('should save line breaks', () => {
        const msg = 'So {wow}.\nSo {hard}';

        assert.strictEqual(format(msg), [
            'So {wow}.',
            'So {hard}',
        ].join('\n'));
    });

    it('should throw error on invalid syntax', () => {
        const msg = '{Such compliance';
        let err;

        try {
            format(msg);
        } catch (error) {
            err = error;
        }

        assert.strictEqual(err instanceof Error, true);
    });
});
