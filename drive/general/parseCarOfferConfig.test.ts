import { parseCarOfferConfig } from 'entities/Car/helpers/parseCarOfferConfig/parseCarOfferConfig';
import { CarOfferSchema } from 'entities/Car/types/CarOfferSchema';

/*eslint-disable @typescript-eslint/no-magic-numbers*/
const CAR_OFFER: CarOfferSchema = {
    secondary: [
        {
            title: 'Child seat',
            id: 'child_seat',
        },
        {
            title: 'Roof box',
            id: 'roof_rack',
        },
        {
            title: 'GPS',
            id: 'gps',
        },
        {
            title: 'Snow chains',
            id: 'snow_chains',
        },
        {
            title: 'Ecological zones in Germany',
            id: 'entry_to_eco_zones_in_germany',
        },
    ],
    insurance_type: 'id_silver',
    primary: [
        {
            types: [
                {
                    title: 'GOLD',
                    id: 'id_gold',
                },
                {
                    title: 'SILVER',
                    id: 'id_silver',
                },
                {
                    title: 'BASIC',
                    id: 'id_basic',
                },
            ],
            id: 'insurance_types',
        },
        {
            statuses: [
                {
                    title: 'Draft',
                    id: 'draft',
                },
                {
                    title: 'Confirmed',
                    id: 'confirmed',
                },
                {
                    title: 'Paid',
                    id: 'paid',
                },
            ],
            id: 'statuses',
        },
        {
            locations: [
                {
                    lat: 50.0824852,
                    lon: 14.42966175,
                    location_name: 'Politických Vězňů, 912/10, Praha, Česko',
                    id: '10511',
                },
                {
                    lat: 50.10686111,
                    lon: 14.26646137,
                    location_name: 'Prague Václav Havel airport, Praha, Česko',
                    id: '10512',
                },
            ],
            id: 'delivery_locations',
        },
        {
            locations: [
                {
                    lat: 50.0824852,
                    lon: 14.42966175,
                    location_name: 'Politických Vězňů, 912/10, Praha, Česko',
                    id: '10511',
                },
                {
                    lat: 50.10686111,
                    lon: 14.26646137,
                    location_name: 'Prague Václav Havel airport, Praha, Česko',
                    id: '10512',
                },
            ],
            id: 'return_locations',
        },
        {
            currencies: [
                {
                    name: 'CZK',
                    id: 'id_czk',
                },
            ],
            id: 'currencies',
        },
        {
            id: 'limit_km_per_day',
            default_value: 0,
        },
        {
            id: 'overrun_cost_per_km',
            default_value: 0,
        },
    ],
    status: 'paid',
    currency: 'id_czk',
    comment: 'test test',
    return_location_name: 'Prague Václav Havel airport - Praha, Česko',
    return_location: [50.10686111, 14.26646137],
    deposit: 10000,
    since: 1650088800,
    until: 1650693600,
    total_payment: 10000,
    overrun_cost_per_km: 5,
    limit_km_per_day: 300,
    offer_id: '58e9a0ee-2a26-0c58-0029-29888451b7ac',
    car_id: '1059ea00-7d98-75b9-d3a9-d4ea4b15828f',
    offer_options: {
        child_seat: false,
        roof_rack: true,
        snow_chains: true,
        entry_to_eco_zones_in_germany: false,
        gps: false,
    },
    delivery_location_name: 'Prague Václav Havel airport - Praha, Česko',
    delivery_location: [50.10686111, 14.26646137],
};

describe('parseCarOfferConfig', function () {
    it('should parse correct structure', function () {
        expect(parseCarOfferConfig(CAR_OFFER)).toMatchInlineSnapshot(`
            Object {
              "currencies": Array [
                Object {
                  "id": "id_czk",
                  "title": "CZK",
                },
              ],
              "delivery_locations": Array [
                Object {
                  "geoid": 10511,
                  "lat": 50.0824852,
                  "lon": 14.42966175,
                  "name": "Politických Vězňů, 912/10, Praha, Česko",
                },
                Object {
                  "geoid": 10512,
                  "lat": 50.10686111,
                  "lon": 14.26646137,
                  "name": "Prague Václav Havel airport, Praha, Česko",
                },
              ],
              "insurance_types": Array [
                Object {
                  "id": "id_gold",
                  "title": "GOLD",
                },
                Object {
                  "id": "id_silver",
                  "title": "SILVER",
                },
                Object {
                  "id": "id_basic",
                  "title": "BASIC",
                },
              ],
              "limit_km_per_day": 0,
              "offer_options": Array [
                Object {
                  "id": "child_seat",
                  "title": "Child seat",
                },
                Object {
                  "id": "roof_rack",
                  "title": "Roof box",
                },
                Object {
                  "id": "gps",
                  "title": "GPS",
                },
                Object {
                  "id": "snow_chains",
                  "title": "Snow chains",
                },
                Object {
                  "id": "entry_to_eco_zones_in_germany",
                  "title": "Ecological zones in Germany",
                },
              ],
              "overrun_cost_per_km": 0,
              "return_locations": Array [
                Object {
                  "geoid": 10511,
                  "lat": 50.0824852,
                  "lon": 14.42966175,
                  "name": "Politických Vězňů, 912/10, Praha, Česko",
                },
                Object {
                  "geoid": 10512,
                  "lat": 50.10686111,
                  "lon": 14.26646137,
                  "name": "Prague Václav Havel airport, Praha, Česko",
                },
              ],
              "statuses": Array [
                Object {
                  "id": "draft",
                  "title": "Draft",
                },
                Object {
                  "id": "confirmed",
                  "title": "Confirmed",
                },
                Object {
                  "id": "paid",
                  "title": "Paid",
                },
              ],
            }
        `);
    });
});
