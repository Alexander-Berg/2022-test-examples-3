import DomainInfoGatewayUtils from '../../../src/infrastructure/gateways/DomainInfoGatewayUtils';
import DomainInfo from '../../../src/domain/models/domain-info/DomainInfo';
import DomainInfoId from '../../../src/domain/models/domain-info/DomainInfoId';
import UUID from '../../../src/utils/UUID';
import DomainInfoStatus from '../../../src/domain/models/domain-info/DomainInfoStatus';
import DomainInfoType from '../../../src/domain/models/domain-info/DomainInfoType';
import DeviceType from '../../../src/domain/models/DeviceType';
import { addPrefix } from './DomainInfoDataMapperUtils.spec';

const DEFAULT_UUID = 'bd6080aa-7f23-4b7a-83de-6a2ec06e312b';
describe('DomainInfoGatewayUtils', () => {
    const domain = `test${new Date().getTime()}.ru`;
    const deviceType = DeviceType.MOBILE;
    const type = DomainInfoType.SHOP;
    const baseDomainInfo = new DomainInfo({
        id: DomainInfoId.fromDomainKey(new UUID(DEFAULT_UUID), deviceType, type),
        domain,
        deviceType,
        status: DomainInfoStatus.UNKNOWN,
        type: DomainInfoType.SHOP,
        payload: { cart: { titles: 'title' } },
        restricted: false,
        comments: ['comment'],
        rules: [],
    });

    const baseAnswer = {
        id: `${DEFAULT_UUID}:robot-sovetnik:${deviceType}:${type}`,
        type: 'SHOP',
        meta: {
            status: 'UNKNOWN',
            restricted: false,
            domain,
            comments: ['comment'],
            deviceType,
        },
        attributes: {
            category: '',
            currency: '',
            isbn: '',
            name: '',
            pictures: '',
            price: '',
            vendor: '',
        },
        cart: {
            currency: '',
            multiplyItemsPrice: false,
            prices: '',
            quantities: '',
            titles: 'title',
            totalPrices: '',
            urlTemplate: '',
        },
        checkout: {
            placeOrderBtn: '',
            urlTemplate: '',
        },
        mainPageTemplates: [],
        productPageSelector: '',
        urlTemplates: [],
    };

    describe('serializeDomainInfo', () => {
        it('should serializeDomainInfo with UUID', async () => {
            const domainInfo = new DomainInfo({
                id: baseDomainInfo.id,
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });
            domainInfo.complement();

            const data = JSON.stringify(DomainInfoGatewayUtils.serializeDomainInfo(domainInfo), null, 2);

            expect(data).toEqual(JSON.stringify(baseAnswer, null, 2));
        });

        it('should serializeDomainInfo with domain', async () => {
            const domainInfo = new DomainInfo({
                id: DomainInfoId.fromDomainKey(domain, deviceType, type),
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });
            domainInfo.complement();

            const data = JSON.stringify(DomainInfoGatewayUtils.serializeDomainInfo(domainInfo), null, 2);

            const answer = Object.assign({}, baseAnswer, { id: DomainInfoId.fromDomainKey(domain, deviceType, type).toString() });
            expect(data).toEqual(JSON.stringify(answer, null, 2));
        });
        //
        it('should serializeDomainInfo with UUID and selectorman', async () => {
            const domainInfo = new DomainInfo({
                id: DomainInfoId.fromString(`${DEFAULT_UUID}:shtruk:${deviceType}:${type}`),
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });

            domainInfo.complement();

            const data = JSON.stringify(DomainInfoGatewayUtils.serializeDomainInfo(domainInfo), null, 2);

            const answer = Object.assign({}, baseAnswer, { id: DomainInfoId.fromString(`${DEFAULT_UUID}:shtruk:${deviceType}:${type}`).toString() });
            expect(data).toEqual(JSON.stringify(answer, null, 2));
        });

        it('should serializeDomainInfo with domain and selectorman', async () => {
            const domainInfo = new DomainInfo({
                id: DomainInfoId.fromString(`${domain}:shtruk:${deviceType}:${type}`),
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });
            domainInfo.complement();

            const data = JSON.stringify(DomainInfoGatewayUtils.serializeDomainInfo(domainInfo), null, 2);

            const answer = Object.assign({}, baseAnswer, { id: DomainInfoId.fromString(`${domain}:shtruk:${deviceType}:${type}`).toString() });
            expect(data).toEqual(JSON.stringify(answer, null, 2));
        });
    });

    describe('deserializeDomainInfo', () => {
        it('should deserializeDomainInfo with UUID', () => {
            const data = Object.assign({}, baseAnswer);
            const domainInfo = new DomainInfo({
                id: baseDomainInfo.id,
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });

            const deserialize = DomainInfoGatewayUtils.deserializeDomainInfo(domainInfo.domain, data, domainInfo.id.value);
            deserialize.clean(true);

            expect(deserialize).toEqual(domainInfo);
        });

        it('should deserializeDomainInfo with domain', () => {
            const data = Object.assign({}, baseAnswer);
            data.id = DomainInfoId.fromDomainKey(domain, deviceType, type).toString();

            const domainInfo = new DomainInfo({
                id: DomainInfoId.fromDomainKey(domain, deviceType, type),
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });

            const deserialize = DomainInfoGatewayUtils.deserializeDomainInfo(domainInfo.domain, data, domainInfo.id.value);
            deserialize.clean(true);

            expect(deserialize).toEqual(domainInfo);
        });

        it('should deserializeDomainInfo with UUID and staff', () => {
            const data = Object.assign({}, baseAnswer);
            data.id = DomainInfoId.fromString(`${DEFAULT_UUID}:shtruk:${deviceType}:${type}`).toString();
            const domainInfo = new DomainInfo({
                id: DomainInfoId.fromString(`${DEFAULT_UUID}:shtruk:${deviceType}:${type}`),
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });

            const deserialize = DomainInfoGatewayUtils.deserializeDomainInfo(domainInfo.domain, data, domainInfo.id.value);
            deserialize.clean(true);

            expect(deserialize).toEqual(domainInfo);
        });

        it('should deserializeDomainInfo with UUID and staff', () => {
            const data = Object.assign({}, baseAnswer);
            data.id = DomainInfoId.fromString(`${domain}:shtruk`).toString();
            const domainInfo = new DomainInfo({
                id: DomainInfoId.fromString(`${domain}:shtruk`),
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });

            const deserialize = DomainInfoGatewayUtils.deserializeDomainInfo(domainInfo.domain, data, domainInfo.id.value);
            deserialize.clean(true);

            expect(deserialize).toEqual(domainInfo);
        });
    });

    describe('backward capability tests', () => {
        it('should deserializeDomainInfo without UUID and domain', () => {
            const data = Object.assign({}, baseAnswer);
            delete data.id;
            delete data.meta.domain;

            data.meta.status = 'failed';

            const domainInfo = new DomainInfo({
                id: baseDomainInfo.id,
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });

            const deserialize = DomainInfoGatewayUtils.deserializeDomainInfo(domain, data, domainInfo.id.value);
            deserialize.clean(true);

            expect(deserialize.domain).toEqual(domainInfo.domain);
        });

        it('should deserializeDomainInfo without UUID and domain', () => {
            const data = Object.assign({}, baseAnswer);
            delete data.id;
            delete data.meta.domain;

            data.meta.status = 'failed';

            const domainInfo = new DomainInfo({
                id: DomainInfoId.fromString(`${DEFAULT_UUID}:shtruk:${deviceType}:${type}`),
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });

            const deserialize = DomainInfoGatewayUtils.deserializeDomainInfo(domain, data, undefined, 'shtruk');
            deserialize.clean(true);

            expect(deserialize.domain).toEqual(domainInfo.domain);
        });

        it('should deserializeDomainInfo without UUID and domain and staff', () => {
            const data = Object.assign({}, baseAnswer);
            delete data.id;
            delete data.meta.domain;

            data.meta.status = 'failed';

            const domainInfo = new DomainInfo({
                id: DomainInfoId.fromDomainKey(domain, deviceType, type),
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });

            const deserialize = DomainInfoGatewayUtils.deserializeDomainInfo(domain, data, undefined);

            expect(deserialize.domain).toEqual(domainInfo.domain);
        });

        it('should deserializeDomainInfo with UUID and staff', () => {
            const data = Object.assign({}, baseAnswer);
            delete data.id;
            delete data.meta.domain;

            data.meta.status = 'failed';

            const domainInfo = new DomainInfo({
                id: DomainInfoId.fromString(`${domain}:shtruk:${deviceType}:${type}`),
                domain: baseDomainInfo.domain,
                deviceType: baseDomainInfo.deviceType,
                status: baseDomainInfo.status,
                type: baseDomainInfo.type,
                payload: baseDomainInfo.payload,
                restricted: baseDomainInfo.restricted,
                comments: baseDomainInfo.comments,
                rules: baseDomainInfo.rules,
            });

            const deserialize = DomainInfoGatewayUtils.deserializeDomainInfo(domain, data, undefined, 'shtruk');

            expect(deserialize.domain).toEqual(domainInfo.domain);
        });
    });

    describe('mergeDomainInfos', () => {
        const desktopInfo = new DomainInfo({
            id: DomainInfoId.fromDomainKey(new UUID(), DeviceType.DESKTOP, DomainInfoType.SHOP),
            domain: 'sample.ru',
            deviceType: DeviceType.DESKTOP,
            status: baseDomainInfo.status,
            type: baseDomainInfo.type,
            payload: baseDomainInfo.payload,
            restricted: baseDomainInfo.restricted,
            comments: baseDomainInfo.comments,
            rules: baseDomainInfo.rules,
        });

        // @ts-ignore
        const mobileInfo = DomainInfo.createFrom(desktopInfo, {
            id: DomainInfoId.fromDomainKey(new UUID(), DeviceType.MOBILE, DomainInfoType.SHOP),
            deviceType: DeviceType.MOBILE,
        });

        // @ts-ignore
        const tabletInfo = DomainInfo.createFrom(desktopInfo, {
            id: DomainInfoId.fromDomainKey(new UUID(), DeviceType.TABLET, DomainInfoType.SHOP),
            deviceType: DeviceType.TABLET,
        });

        test('one (desktop) info', () => {
            const result = DomainInfoGatewayUtils.mergeDomainInfos([desktopInfo]);

            expect(result).toEqual([
                DomainInfo.createFrom(desktopInfo, {
                    id: DomainInfoId.fromDomainKey(desktopInfo.domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
                    deviceType: DeviceType.DESKTOP,
                }),
            ]);
        });

        test('one (mobile) info', () => {
            const result = DomainInfoGatewayUtils.mergeDomainInfos([mobileInfo]);

            expect(result).toEqual([
                DomainInfo.createFrom(mobileInfo, {
                    id: DomainInfoId.fromDomainKey(desktopInfo.domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
                    deviceType: DeviceType.DESKTOP,
                    payload: addPrefix(mobileInfo.payload, 'mobile-'),
                    // @ts-ignore
                    rules: addPrefix(mobileInfo.rules, 'mobile-'),
                }),
            ]);
        });

        test('two info', () => {
            const result = DomainInfoGatewayUtils.mergeDomainInfos([desktopInfo, mobileInfo]);

            expect(result).toEqual([
                DomainInfo.createFrom(desktopInfo, {
                    id: DomainInfoId.fromDomainKey(desktopInfo.domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
                    deviceType: DeviceType.DESKTOP,
                    payload: {
                        ...desktopInfo.payload,
                        ...addPrefix(mobileInfo.payload, 'mobile-'),
                    },
                    rules: [
                        ...desktopInfo.rules,
                        // @ts-ignore
                        ...addPrefix(mobileInfo.rules, 'mobile-'),
                    ],
                }),
            ]);
        });

        test('three info', () => {
            const result = DomainInfoGatewayUtils.mergeDomainInfos([desktopInfo, mobileInfo, tabletInfo]);

            expect(result).toEqual([
                DomainInfo.createFrom(desktopInfo, {
                    id: DomainInfoId.fromDomainKey(mobileInfo.domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
                    deviceType: DeviceType.DESKTOP,
                    payload: {
                        ...desktopInfo.payload,
                        ...addPrefix(mobileInfo.payload, 'mobile-'),
                        ...addPrefix(tabletInfo.payload, 'tablet-'),
                    },
                    rules: [
                        ...desktopInfo.rules,
                        ...addPrefix(mobileInfo.rules, 'mobile-'),
                        ...addPrefix(tabletInfo.rules, 'tablet-'),
                    ],
                }),
            ]);
        });

        test('two infos for merge, one alone', () => {
            const notForMergeInfo = DomainInfo.createFrom(desktopInfo, {
                id: DomainInfoId.fromDomainKey('sample2.ru', DeviceType.DESKTOP, DomainInfoType.SHOP),
                domain: 'sample2.ru',
            });
            const result = DomainInfoGatewayUtils.mergeDomainInfos([desktopInfo, mobileInfo, notForMergeInfo]);

            expect(result).toEqual([
                DomainInfo.createFrom(desktopInfo, {
                    id: DomainInfoId.fromDomainKey(desktopInfo.domain, DeviceType.DESKTOP, DomainInfoType.SHOP),
                    deviceType: DeviceType.DESKTOP,
                    payload: {
                        ...desktopInfo.payload,
                        ...addPrefix(mobileInfo.payload, 'mobile-'),
                    },
                    rules: [
                        ...desktopInfo.rules,
                        // @ts-ignore
                        ...addPrefix(mobileInfo.rules, 'mobile-'),
                    ],
                }),
                notForMergeInfo,
            ]);
        });
    });
});
