import maskPhoneNumber, { isValidPhone } from '../maskPhoneNumber';

describe('Mask phone number', () => {
    it('+79261234567', () => {
        expect(maskPhoneNumber('+79261234567')).toEqual(
            '+7 926 <sup>*</sup><sup>*</sup><sup>*</sup>-<sup>*</sup><sup>*</sup>-67',
        );
    });

    it('+7              926 1 2 3  45 fgfg hfjgfjg 6 fgfg7', () => {
        expect(maskPhoneNumber('+7              926 1 2 3  45 fgfg hfjgfjg 6 fgfg7')).toEqual(
            '+7 926 <sup>*</sup><sup>*</sup><sup>*</sup>-<sup>*</sup><sup>*</sup>-67',
        );
    });

    it('8 926 -- 100 --- 67 56    ', () => {
        expect(maskPhoneNumber('8 926 -- 100 --- 67 56    ')).toEqual(
            '8 926 <sup>*</sup><sup>*</sup><sup>*</sup>-<sup>*</sup><sup>*</sup>-56',
        );
    });

    it('8 88 123 123 123', () => {
        expect(maskPhoneNumber('8 88 123 123 123')).toEqual(
            '88 812 <sup>*</sup><sup>*</sup><sup>*</sup>-<sup>*</sup><sup>*</sup>-23',
        );
    });

    it('812 888 888 12 12', () => {
        expect(maskPhoneNumber('812 888 888 12 12')).toEqual(
            '812 888 <sup>*</sup><sup>*</sup><sup>*</sup>-<sup>*</sup><sup>*</sup>-12',
        );
    });

    it('+812 888 888 12 12', () => {
        expect(maskPhoneNumber('+812 888 888 12 12')).toEqual(
            '+812 888 <sup>*</sup><sup>*</sup><sup>*</sup>-<sup>*</sup><sup>*</sup>-12',
        );
    });

    it('+7 926 -- 100 --- 67 56    ', () => {
        expect(maskPhoneNumber('+7 926 -- 100 --- 67 56    ')).toEqual(
            '+7 926 <sup>*</sup><sup>*</sup><sup>*</sup>-<sup>*</sup><sup>*</sup>-56',
        );
    });

    it('+8 88 123 123 123', () => {
        expect(maskPhoneNumber('+8 88 123 123 123')).toEqual(
            '+88 812 <sup>*</sup><sup>*</sup><sup>*</sup>-<sup>*</sup><sup>*</sup>-23',
        );
    });

    describe('#isValidPhone', () => {
        it('check be valid with 8', () => {
            expect(isValidPhone('891693033')).toBeFalsy();
            expect(isValidPhone('8916930335')).toBeFalsy();

            expect(isValidPhone('89169303351')).toBeTruthy();
            expect(isValidPhone('891693033335')).toBeTruthy();
            expect(isValidPhone('8916930333352')).toBeTruthy();

            expect(isValidPhone('891693033335211')).toBeFalsy();
            expect(isValidPhone('8916930333352114')).toBeFalsy();

            expect(isValidPhone('8 916 930 33 51')).toBeTruthy();

            expect(isValidPhone('+79169303351')).toBeTruthy();
            expect(isValidPhone('+7 916 930 33 51')).toBeTruthy();
            expect(isValidPhone('+7916 93033 51')).toBeTruthy();
        });
    });
});
