import { assert } from 'chai';
import { describe, it } from 'mocha';

import { ROOT_SCOPE, getPlatformClassName, getPlatformClassList, getLevelFromFilename } from '../src/getters';

describe('getters', () => {
    describe('getScopeClass', () => {
        it('should return class name for level', () => {
            const actual = getPlatformClassName('desktop');
            const expected = `${ROOT_SCOPE}__desktop`;

            assert.strictEqual(actual, expected);
        });
    });

    describe('getPlatformClassList', () => {
        it('should return array of class names for levels', () => {
            const actual = getPlatformClassList(['touch', 'touch-phone']);
            const expected = [ROOT_SCOPE, `${ROOT_SCOPE}__touch`, `${ROOT_SCOPE}__touch-phone`];

            assert.deepEqual(actual, expected);
        });
    });

    describe('getLevelFromFilename', () => {
        it('should return level from @-filename', () => {
            const actual = getLevelFromFilename('path/to/file@desktop.scss');
            const expected = 'desktop';

            assert.strictEqual(actual, expected);
        });

        it('should return undefined from other filename', () => {
            assert.isUndefined(
                getLevelFromFilename('path/to/file.scss'),
            );
        });
    });
});
