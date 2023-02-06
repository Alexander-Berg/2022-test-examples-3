import { assert } from 'chai';
import { checkSomeHasAlice } from '../.';

const livesReason = { id: 'alisa_lives_here' };
const compatibleReason = { id: 'compatible_with_alisa' };

describe('checkSomeHasAlice', () => {
    it('should return true', () => {
        assert.isTrue(checkSomeHasAlice([
            { reasonsToBuy: [{ id: 'some_other_reason' }] },
            { reasonsToBuy: [livesReason] },
            { reasonsToBuy: [compatibleReason] },
        ]));

        assert.isTrue(checkSomeHasAlice([
            { reasonsToBuy: [] },
            { reasonsToBuy: [] },
            { reasonsToBuy: [livesReason] },
        ]));

        assert.isTrue(checkSomeHasAlice([
            { reasonsToBuy: [] },
            { reasonsToBuy: [] },
            { reasonsToBuy: [compatibleReason] },
        ]));
    });

    it('should return false', () => {
        assert.isFalse(checkSomeHasAlice([]));

        assert.isFalse(checkSomeHasAlice([
            { reasonsToBuy: [] },
            { reasonsToBuy: [] },
        ]));

        assert.isFalse(checkSomeHasAlice([
            { reasonsToBuy: [] },
            { reasonsToBuy: [] },
            { reasonsToBuy: [{ id: 'some_another_reason' }] },
        ]));
    });
});
