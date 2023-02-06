import {checkUnstaged} from 'make-me-a-content';
// @ts-expect-error(TS7016) найдено в рамках MARKETPARTNER-16237
import npmRunAll from 'npm-run-all';
import assert from 'assert';

describe('generated content up to date', () => {
    it('generate:source:*', async () => {
        await npmRunAll('generate:source:*');
        const result = checkUnstaged();
        // @ts-expect-error(TS7034) найдено в рамках MARKETPARTNER-16237
        const expected = [];

        expect(() => {
            // pretty error message
            assert.deepEqual(
                result,
                // @ts-expect-error(TS7005) найдено в рамках MARKETPARTNER-16237
                expected,
                `run "npm run generate:source:docs" and commit the changes, found files\n${result
                    .map(x => `- ${x}`)
                    .join('\n')}`,
            );
        }).not.toThrow();
    });
});
