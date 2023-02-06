import getMockLocation from '../__mocks__/location';
import { makeYaConf } from '../__mocks__/yaConf';
import { initRoutingVariables } from './routingVars';
import isHomeLocation from './isHomeLocation';

const location = getMockLocation();

jest.mock('../lib/rum');

describe('helpers', () => {
    describe('isHomeLocation', () => {
        const lat = 55;
        const lon = 37;
        const geoId = 213;

        it('Голый url - домашняя', () => {
            expect(isHomeLocation(initRoutingVariables({ location }))).toBeTruthy();
            expect(isHomeLocation(initRoutingVariables({ location, params: { slug: '' } }))).toBeTruthy();
            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: '' },
                        location: { ...location, search: '?lat=&lon=' },
                    })
                )
            ).toBeTruthy();
            expect(
                isHomeLocation(
                    initRoutingVariables({
                        location: { ...location, search: '?lat=foo&lon=bar' },
                    })
                )
            ).toBeTruthy();
        });

        it('Не домашняя без yaConf', () => {
            expect(isHomeLocation(initRoutingVariables({ location, params: { slug: 'foo' } }))).toBeFalsy();
            expect(
                isHomeLocation(
                    initRoutingVariables({
                        location: {
                            ...location,
                            search: `?lat=${lat}&lon=${lon}`,
                        },
                    })
                )
            ).toBeFalsy();
            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: geoId.toString(10) },
                        location: {
                            ...location,
                            search: `?lat=${lat}&lon=${lon}`,
                        },
                    })
                )
            ).toBeFalsy();
        });

        it('yaConf - домашняя', () => {
            expect(
                isHomeLocation(
                    initRoutingVariables({
                        location: { ...location, search: `?yaConf=${makeYaConf({})}` },
                    })
                )
            ).toBeTruthy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: '' },
                        location: { ...location, search: `?lat=${lat}&lon=${lon}&yaConf=${makeYaConf({ lat, lon })}` },
                    })
                )
            ).toBeTruthy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        location: { ...location, search: `?lat=${lat}&lon=${lon}&yaConf=${makeYaConf({ lat, lon })}` },
                    })
                )
            ).toBeTruthy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: `${geoId}` },
                        location: {
                            ...location,
                            search: `?lat=${lat}&lon=${lon}&yaConf=${makeYaConf({ lat, lon, geoId })}`,
                        },
                    })
                )
            ).toBeTruthy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: `${geoId}` },
                        location: { ...location, search: `?yaConf=${makeYaConf({ geoId })}` },
                    })
                )
            ).toBeTruthy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        location: { ...location, search: `?yaConf=${makeYaConf({ geoId })}` },
                    })
                )
            ).toBeTruthy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        location: { ...location, search: `?yaConf=${makeYaConf({ lat, lon })}` },
                    })
                )
            ).toBeTruthy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        location: { ...location, search: `?yaConf=${makeYaConf({ lat, lon, geoId })}` },
                    })
                )
            ).toBeTruthy();
        });

        it('yaConf - не домашняя', () => {
            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: `${geoId}` },
                        location: { ...location, search: `?yaConf=${makeYaConf({})}` },
                    })
                )
            ).toBeFalsy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        location: { ...location, search: `?lat=${lat}&lon=${lon}yaConf=${makeYaConf({})}` },
                    })
                )
            ).toBeFalsy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: `${geoId}` },
                        location: { ...location, search: `?lat=${lat}&lon=${lon}yaConf=${makeYaConf({})}` },
                    })
                )
            ).toBeFalsy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: `${geoId}` },
                        location: { ...location, search: `?lat=${lat}&lon=${lon}&yaConf=${makeYaConf({ lat, lon })}` },
                    })
                )
            ).toBeFalsy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        location: {
                            ...location,
                            search: `?lat=${lat - 1}&lon=${lon}&yaConf=${makeYaConf({ lat, lon })}`,
                        },
                    })
                )
            ).toBeFalsy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: `${geoId - 1}` },
                        location: {
                            ...location,
                            search: `?lat=${lat}&lon=${lon}&yaConf=${makeYaConf({ lat, lon, geoId })}`,
                        },
                    })
                )
            ).toBeFalsy();

            expect(
                isHomeLocation(
                    initRoutingVariables({
                        params: { slug: `${geoId - 1}` },
                        location: { ...location, search: `?yaConf=${makeYaConf({ geoId })}` },
                    })
                )
            ).toBeFalsy();
        });
    });
});
