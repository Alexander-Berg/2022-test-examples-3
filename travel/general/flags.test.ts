import {
    createFlags,
    enable,
    disable,
    toggle,
    isEnabled,
    toArray,
} from './flags';

describe('flags', () => {
    it('creates flags', () => {
        const flags = createFlags();

        expect(flags).toEqual(0);
    });

    it('enables flag', () => {
        const flags = createFlags<number>(0b100);
        expect(enable(flags, 0b010)).toEqual(0b110);
    });

    it('disables flag', () => {
        const flags = createFlags<number>(0b100);
        expect(disable(flags, 0b100)).toEqual(0b000);
    });

    it('toggles flag', () => {
        const flags = createFlags<number>(0b100);
        expect(toggle(flags, 0b010)).toEqual(0b110);
        expect(toggle(toggle(flags, 0b010), 0b010)).toEqual(0b100);
    });

    it('checks if flag is enabled', () => {
        const flags = createFlags<number>(0b100);
        expect(isEnabled(flags, 0b010)).toEqual(false);
        expect(isEnabled(flags, 0b100)).toEqual(true);
    });

    it('converts flags to array', () => {
        expect(toArray(createFlags<number>(0b1101))).toEqual([
            0b0001, 0b0100, 0b1000,
        ]);
        expect(toArray(createFlags<number>(0b1000))).toEqual([0b1000]);
    });
});
