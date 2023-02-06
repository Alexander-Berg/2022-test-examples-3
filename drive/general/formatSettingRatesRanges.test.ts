import { formatSettingRatesRanges } from 'features/SettingsDailyRates/helpers/formatSettingRatesRanges/formatSettingRatesRanges';

describe('formatSettingRatesRanges', function () {
    it('works with empty params', function () {
        expect(formatSettingRatesRanges([])).toMatchInlineSnapshot(`Object {}`);
    });

    it('works with filled params', function () {
        expect(
            formatSettingRatesRanges([
                {
                    id: '1-3_days',
                    end: 3,
                },

                {
                    id: '4-7_days',
                    end: 7,
                },

                {
                    id: '8-14_days',
                    end: 14,
                },

                {
                    id: '14-30_days',
                    end: 30,
                },

                {
                    id: 'max',
                    end: 2147483647,
                },
            ]),
        ).toMatchInlineSnapshot(`
            Object {
              "1-3_days": "1–3 days",
              "14-30_days": "14–30 days",
              "4-7_days": "3–7 days",
              "8-14_days": "7–14 days",
              "max": "30+ days",
            }
        `);
    });
});
