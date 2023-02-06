import DomainInfoDataMapperUtils from '../../../src/infrastructure/data-mappers/DomainInfoDataMapperUtils';
import DomainInfoGatewayUtils from '../../../src/infrastructure/gateways/DomainInfoGatewayUtils';
import DomainInfo from '../../../src/domain/models/domain-info/DomainInfo';
import CollectionUtils from '../../../src/utils/CollectionUtils';
import {
    getUpdatedSlice,
    mergedOzonWithUpdatedPayload,
    oximixWithChangedRules,
    ozonWithUpdatedPayload,
    pgPlainDomainInfos,
    ytPlainObjects,
} from './sync-yt.spec-sample';


describe('sync-yt mapping test', () => {
    const testCases = [
        {
            title: 'nothing to update',
            input: {
                yt: Object.assign([], ytPlainObjects),
                pg: Object.assign([], pgPlainDomainInfos),
            },
            output: {
                toSave: [],
                toDelete: [],
            },
        },
        {
            title: 'update oxymix.ru | rule changed',
            input: {
                yt: Object.assign([], ytPlainObjects),
                pg: getUpdatedSlice(oximixWithChangedRules),
            },
            output: {
                toSave: [oximixWithChangedRules],
                toDelete: [],
            },
        },
        {
            title: 'update ozon.ru | mobile changed',
            input: {
                yt: Object.assign([], ytPlainObjects),
                pg: getUpdatedSlice(ozonWithUpdatedPayload),
            },
            output: {
                toSave: [mergedOzonWithUpdatedPayload],
                toDelete: [],
            },
        },
    ];

    testCases.forEach(testCase => {
        test(testCase.title, () => {
            const ytDomainInfos = testCase.input.yt.map(obj => DomainInfoDataMapperUtils.ytObjectToDomainInfo(obj));
            const mergedDomainInfos = DomainInfoGatewayUtils.mergeDomainInfos(testCase.input.pg);

            expect(mergedDomainInfos.length).toEqual(3);

            const toSave: DomainInfo[] = [];
            const toDelete: DomainInfo[] = [];

            CollectionUtils.merge(
                ytDomainInfos,
                mergedDomainInfos,
                d => d.domainKey,
                d => {
                    toSave.push(d);
                    return d;
                },
                d => {
                    toDelete.push(d);
                    return d;
                },
                (oldDomainInfo, newDomainInfo) => {
                    if (!DomainInfo.equalsByPayload(oldDomainInfo, newDomainInfo)
                        || !CollectionUtils.equals(oldDomainInfo.rules, newDomainInfo.rules)) {
                        toSave.push(newDomainInfo);
                    }
                    return newDomainInfo;
                },
            );
            expect(toSave).toEqual(testCase.output.toSave);
            expect(toDelete).toEqual(testCase.output.toDelete);
        });
    });
});
