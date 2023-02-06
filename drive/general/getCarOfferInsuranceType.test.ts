import { getCarOfferInsuranceType } from 'entities/Car/helpers/getCarOfferInsuranceType/getCarOfferInsuranceType';

const INSURANCE_TYPE = [
    {
        title: 'GOLD',
        id: 'Gold',
    },
    {
        title: 'SILVER',
        id: 'Silver',
    },
    {
        title: 'BASIC',
        id: 'Basic',
    },
];

describe('getCarOfferInsuranceType', function () {
    it('should works', function () {
        expect(getCarOfferInsuranceType('Gold', INSURANCE_TYPE)).toMatchInlineSnapshot(`"GOLD"`);
        expect(getCarOfferInsuranceType('Silver', INSURANCE_TYPE)).toMatchInlineSnapshot(`"SILVER"`);
        expect(getCarOfferInsuranceType('Basic', INSURANCE_TYPE)).toMatchInlineSnapshot(`"BASIC"`);
        expect(getCarOfferInsuranceType('Platinum', INSURANCE_TYPE)).toMatchInlineSnapshot(`"???"`);
    });
});
