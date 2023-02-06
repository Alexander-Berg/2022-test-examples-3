import Response from 'Response';

import {initRegionMock, fallbackMock, checkGeobaseAvailabilityMock, nodulesUser} from './mock';

const mockCheck = new Response();
mockCheck.resolve({});
const mockInit = new Response();
mockInit.resolve({region: {}});

const mockRegionInitialization = {
    init: () => {
        const response = new Response();
        response.resolve(initRegionMock);
        return response;
    },
    geobaseFallbackInit: () => fallbackMock,
    checkGeobaseAvailability: () => {
        const response = new Response();
        response.resolve(checkGeobaseAvailabilityMock);
        return response;
    },
};

jest.mock('@yandex-market/region-initialization', () => mockRegionInitialization);

const MockNodulesUser = function() {
    Object.assign(this, nodulesUser);
    this.init = () => Promise.resolve(this);
};

MockNodulesUser.registerComponent = () => {};

jest.mock('nodules-user', () => MockNodulesUser);
