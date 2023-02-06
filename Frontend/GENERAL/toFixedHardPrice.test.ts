import { toFixedHardPrice } from './toFixedHardPrice';

describe('toFixedHardPrice', () => {
    it('don\'t fractional in int', () => {
        const actual = toFixedHardPrice(10);

        expect(actual).toEqual('10');
    });

    it('fractional in float', () => {
        const actual = toFixedHardPrice(10.22);

        expect(actual).toEqual('10,22');
    });

    it('fractional in float > 2 number', () => {
        const actual = toFixedHardPrice(10.2222);

        expect(actual).toEqual('10,22');
    });

    it('fractional in float < 2 number', () => {
        const actual = toFixedHardPrice(10.2);

        expect(actual).toEqual('10,20');
    });

    it('fractional not around', () => {
        const actual = toFixedHardPrice(10.999999);

        expect(actual).toEqual('10,99');
    });
});
