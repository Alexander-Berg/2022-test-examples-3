import { assert } from 'chai';
import { getAliceType } from '../.';

const livesReason = { id: 'alisa_lives_here' };
const compatibleReason = { id: 'compatible_with_alisa' };

describe('"Alice type" mapper', () => {
    describe('Mapping Alice\'s type for product', () => {
        it('should handle fake Alice type', () => {
            assert.equal(getAliceType({}), 'fake-alice');
            assert.equal(getAliceType({reasonsToBuy: []}), 'fake-alice');
            assert.equal(getAliceType({reasonsToBuy: [{id: 'some-another-reason'}]}), 'fake-alice');
        });

        it('should handle "Alisa lives here" reason', () => {
            assert.equal(getAliceType({
                reasonsToBuy: [
                    {id: 'some-another-reason'},
                    livesReason
                ]
            }), 'live-in');
            assert.equal(getAliceType({reasonsToBuy: [livesReason]}), 'live-in');
        });

        it('should handle "Compatible with Alisa" reason', () => {
            assert.equal(getAliceType({
                reasonsToBuy: [
                    {id: 'some-another-reason'},
                    compatibleReason
                ]
            }), 'work-with');
            assert.equal(getAliceType({reasonsToBuy: [compatibleReason]}), 'work-with');
        });
    });

    describe('Mapping Alice\'s type to the purchase reason', () => {
        it('should handle fake Alice type', () => {
            assert.equal(getAliceType(''), 'fake-alice');
            assert.equal(getAliceType('some-another-reason'), 'fake-alice');
        });

        it('should handle "Alisa lives here" reason', () => {
            assert.equal(getAliceType(livesReason.id), 'live-in');
        });

        it('should handle "Compatible with Alisa" reason', () => {
            assert.equal(getAliceType(compatibleReason.id), 'work-with');
        });
    });
});

