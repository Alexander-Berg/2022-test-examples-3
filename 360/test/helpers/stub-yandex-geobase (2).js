'use strict';

const mockRequire = require('mock-require');

const geobase = () => ({
    regionByIp: () => ({ id: 213 }),
    getCountryInfoByRegionId: () => ({ id: 134 }),
    getParentsIds: () => [10590, 134, 183, 10001, 10000]
});

const mock = {
    default: {
        v6: geobase
    }
};

mockRequire('@yandex-int/yandex-geobase', mock);
