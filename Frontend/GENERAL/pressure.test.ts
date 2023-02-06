import { getJumpType, getType, JumpType, PressureType } from './pressure';

describe('helpers', () => {
    describe('pressure', () => {
        it('getType', () => {
            expect(getType(734, 760, 30)).toEqual(PressureType.NORM);
            expect(getType(744, 750, 0)).toEqual(PressureType.LOWERING);
            expect(getType(794, 740, 20)).toEqual(PressureType.INCREASE);
        });

        it('getJumpType', () => {
            expect(getJumpType(734, 734, 3)).toEqual(JumpType.NOTHING);
            expect(getJumpType(754, 749, 0)).toEqual(JumpType.DOWN);
            expect(getJumpType(728, 759, 10)).toEqual(JumpType.UP);
        });
    });
});
