import { parseOfferParams } from './parseOfferParams';

describe('parseOfferParams', () => {
  it('works', () => {
    expect(
      parseOfferParams(
        '<?xml version="1.0" encoding="UTF-8"?><offer_params><param name="delivery_weight" unit="кг">0.03</param><param name="vendor" unit="">INTEL</param></offer_params>'
      )
    ).toEqual([
      { name: 'delivery_weight', unit: 'кг', value: '0.03' },
      { name: 'vendor', unit: '', value: 'INTEL' },
    ]);
  });
});
