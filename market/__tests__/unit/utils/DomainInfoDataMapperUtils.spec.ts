import DomainInfo from '../../../src/domain/models/domain-info/DomainInfo';
import DomainInfoId from '../../../src/domain/models/domain-info/DomainInfoId';
import UUID from '../../../src/utils/UUID';
import DeviceType from '../../../src/domain/models/DeviceType';
import DomainInfoType from '../../../src/domain/models/domain-info/DomainInfoType';
import DomainInfoStatus from '../../../src/domain/models/domain-info/DomainInfoStatus';
import DomainInfoDataMapperUtils from '../../../src/infrastructure/data-mappers/DomainInfoDataMapperUtils';

export const payloadSample = {
    attributes: {
        name: 'name',
    },
    urlTemplates: ['urlTemplates'],
    specifiedValue: {
        keys: ['keys'],
        values: ['values'],
    },
};

export const addPrefix = (obj: Record<string, any> | string[], prefix: string): Record<string, any> | string[] => {
    if (!Array.isArray(obj)) {
        return Object
            .keys(obj)
            .reduce((acc, key) => {
                acc[prefix + key] = obj[key];
                return acc;
                // eslint-disable-next-line @typescript-eslint/no-object-literal-type-assertion
            }, {} as Record<string, any>);
    }

    return obj.map(s => prefix + s);
};

const domainInfoDesktopSample = new DomainInfo({
    id: DomainInfoId.fromDomainKey(new UUID(), DeviceType.DESKTOP, DomainInfoType.SHOP),
    domain: 'sample.ru',
    deviceType: DeviceType.DESKTOP,
    payload: payloadSample,
    rules: ['shop'],
    type: DomainInfoType.SHOP,
    status: DomainInfoStatus.OK,
});

const domainInfoMobileSample = DomainInfo.createFrom(domainInfoDesktopSample, {
    id: DomainInfoId.fromDomainKey(new UUID(), DeviceType.MOBILE, DomainInfoType.SHOP),
    deviceType: DeviceType.MOBILE,
    payload: addPrefix(payloadSample, 'mobile-'),
});

const desktopYtSample = {
    domain: 'sample.ru',
    rules: ['shop'],
    payload: {
        selector: {
            name: 'name',
        },
        urlTemplates: ['urlTemplates'],
        specifiedValue: {
            keys: ['keys'],
            values: ['values'],
        },
    },
    restricted: false,
    signature: '081bb0dd9fa553d6bc0338aeeb2d0f90',
    comments: [],
    status: 'ok',
    type: 'shop',
};

const mobileYtSample = {
    domain: 'sample.ru',
    rules: ['shop'],
    payload: {
        'mobile-selector': {
            name: 'name',
        },
        'mobile-urlTemplates': ['urlTemplates'],
        'mobile-specifiedValue': {
            keys: ['keys'],
            values: ['values'],
        },
    },
    restricted: false,
    signature: 'da9affac2e6bbdd0488a83c90e75c104',
    comments: [],
    status: 'ok',
    type: 'shop',
};

describe('DomainInfoDataMapperUtils', () => {
    describe('ytObjectToDomainInfo', () => {
        test('desktop', () => {
            const domainInfoResult = DomainInfo.createFrom(domainInfoDesktopSample, {
                id: DomainInfoId.fromDomainKey(domainInfoDesktopSample.domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
                payload: payloadSample,
            });
            expect(DomainInfoDataMapperUtils.ytObjectToDomainInfo(desktopYtSample)).toEqual(domainInfoResult);
        });

        test('mobile', () => {
            const domainInfoResult = DomainInfo.createFrom(domainInfoDesktopSample, {
                id: DomainInfoId.fromDomainKey(domainInfoDesktopSample.domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
                payload: addPrefix(payloadSample, 'mobile-'),
            });
            expect(DomainInfoDataMapperUtils.ytObjectToDomainInfo(mobileYtSample)).toEqual(domainInfoResult);
        });

        test('cobined', () => {
            const domainInfoResult = DomainInfo.createFrom(domainInfoDesktopSample, {
                id: DomainInfoId.fromDomainKey(domainInfoDesktopSample.domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
                payload: {
                    ...addPrefix(payloadSample, 'mobile-'),
                    ...payloadSample,
                },
            });
            expect(DomainInfoDataMapperUtils.ytObjectToDomainInfo({
                ...desktopYtSample,
                payload: {
                    ...desktopYtSample.payload,
                    ...mobileYtSample.payload,
                },
            })).toEqual(domainInfoResult);
        });
    });

    describe('domainInfoToYtObject', () => {
        test('desktop', () => {
            expect(DomainInfoDataMapperUtils.domainInfoToYtObject(domainInfoDesktopSample)).toEqual(desktopYtSample);
        });

        test('mobile', () => {
            expect(DomainInfoDataMapperUtils.domainInfoToYtObject(domainInfoMobileSample)).toEqual(mobileYtSample);
        });
    });
});
