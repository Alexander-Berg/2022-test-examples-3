import nock from 'nock';

import config from '@yandex-int/yandex-cfg';

interface GeobaseOptions {
    geobase?: object;
    parents?: object;
    traits?: object;
}

export const nockGeobase = (options: GeobaseOptions = {}) => {
    const { geobase = { region_id: 666 }, parents = {}, traits = {} } = options;

    nock(config.httpGeobase!.server!.toString())
        .post('/v1/pinpoint_geolocation?', { is_trusted: true })
        .times(Infinity)
        .reply(200, geobase)

        .post('/v1/get_traits_by_ip')
        .query({ ip: '::1' })
        .times(Infinity)
        .reply(200, traits)

        .post('/v1/parents')
        .query({ id: /\d+/ })
        .times(Infinity)
        .reply(200, parents);
};
