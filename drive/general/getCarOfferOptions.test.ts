import { getCarOfferOptions } from 'entities/Car/helpers/getCarOfferOptions/getCarOfferOptions';

const OPTIONS = [
    { id: 'child_seat', title: 'Child seat' },
    { id: 'roof_rack', title: 'Roof box' },
    { id: 'gps', title: 'GPS' },
    { id: 'snow_chains', title: 'Snow chains' },
    { id: 'entry_to_eco_zones_in_germany', title: 'Ecological zones in Germany' },
];

describe('getCarOfferOptions', function () {
    it('works with empty params', function () {
        expect(getCarOfferOptions({}, OPTIONS)).toMatchInlineSnapshot(`Array []`);
    });

    it('works with filled params', function () {
        expect(
            getCarOfferOptions(
                {
                    gps: true,
                    snow_chains: false,
                    roof_rack: true,
                },

                OPTIONS,
            ),
        ).toMatchInlineSnapshot(`
            Array [
              "GPS",
              "Roof box",
            ]
        `);

        expect(
            getCarOfferOptions(
                {
                    speed_tracker: true,
                    entry_to_eco_zones_in_germany: true,
                },

                OPTIONS,
            ),
        ).toMatchInlineSnapshot(`
            Array [
              "???",
              "Ecological zones in Germany",
            ]
        `);
    });
});
