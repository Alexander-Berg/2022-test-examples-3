'use strict';

// const joinSettings = require('../settings/_filters/join-settings.js');
const weatherRegion = require('./weather-region.js');

const regions = {
    13: {
        id: 13,
        services: [ 'other' ],
        parent_id: 3
    },
    3: {
        id: 3,
        services: [ 'weather' ],
        parent_id: 2
    },
    2: {
        id: 2,
        services: [ 'other' ],
        capital_id: 1,
        parent_id: 1
    },
    1: {
        id: 1,
        services: [],
        parent_id: 0
    }
};

let core;

describe('models/geobase/weather-region', () => {
    beforeEach(() => {
        core = {
            request: jest.fn().mockImplementation((meth, params) => {
                return regions[params.id];
            }),
            config: {
                USER_IP: 'FAKE_IP'
            }
        };
    });

    describe('с параметром id', () => {
        it('возвращает регион', async () => {
            const region = await weatherRegion({ id: 3 }, core);
            expect(region.services).toContain('weather');
            expect(region.id).toEqual(3);
        });

        it('возвращает родительский регион', async () => {
            const region = await weatherRegion({ id: 13 }, core);
            expect(region.services).toContain('weather');
            expect(region.id).toEqual(3);
        });

        it('возвращает null', async () => {
            const region = await weatherRegion({ id: 2 }, core);
            expect(region).toBeNull();
        });
    });

    describe('без параметра id', () => {
        it('возвращает регион', async () => {
            core.request.mockResolvedValueOnce(regions['3']);
            const region = await weatherRegion({}, core);
            expect(region.services).toContain('weather');
            expect(region.id).toEqual(3);
            expect(core.request.mock.calls).toMatchSnapshot();
        });

        it('возвращает родительсий регион', async () => {
            core.request.mockResolvedValueOnce(regions['13']);
            const region = await weatherRegion({}, core);
            expect(region.services).toContain('weather');
            expect(region.id).toEqual(3);
            expect(core.request.mock.calls).toMatchSnapshot();
        });

        it('возвращает null', async () => {
            core.request.mockResolvedValueOnce(regions['2']);
            const region = await weatherRegion({}, core);
            expect(region).toBeNull();
        });
    });
});
