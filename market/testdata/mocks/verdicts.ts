import type {GetVerdictsResponse} from 'shared/bcm/indexator/datacamp/types';

export const VERDICTS_MOCK: GetVerdictsResponse = {
    messages: [
        {
            explanation: {
                namespace: 'shared.indexer.error.codes',
                code: '390',
                params: [
                    {name: 'code', value: '390'},
                    {name: 'creditTemlateId', value: '11111'},
                ],
                level: 2,
            },
            isRelevant: true,
        },
        {
            explanation: {
                namespace: 'shared.indexer.error.codes',
                code: '342',
                params: [{name: 'code', value: '342'}],
                level: 2,
            },
            isRelevant: true,
        },
        {
            identifiers: [{shopId: 10738537}],
            explanation: {
                namespace: 'mboc.ci.error',
                code: 'mboc.error.md-miss-life-time-value',
                params: [],
                level: 3,
            },
            isRelevant: false,
        },
        {
            identifiers: [{shopId: 10441453}],
            explanation: {
                namespace: 'mboc.ci.error',
                code: 'mboc.error.md-miss-life-time-value',
                params: [],
                level: 3,
            },
            isRelevant: false,
        },
        {
            identifiers: [{shopId: 10441453}],
            explanation: {
                namespace: 'shared.indexer.error.codes',
                code: '49S',
                params: [{name: 'code', value: '49S'}],
                level: 3,
            },
            isRelevant: true,
        },
        {
            identifiers: [{shopId: 10441453}, {shopId: 10738537}],
            explanation: {
                namespace: 'shared.indexer.error.codes',
                code: '45l',
                params: [
                    {name: 'code', value: '45l'},
                    {name: 'SSKU', value: '123'},
                ],
                level: 3,
            },
            isRelevant: true,
        },
        {
            identifiers: [{shopId: 10441453}],
            explanation: {
                namespace: 'shared.indexer.error.codes',
                code: '35j',
                params: [{name: 'code', value: '35j'}],
                level: 2,
            },
            isRelevant: true,
        },
        {
            identifiers: [{shopId: 10441453}],
            explanation: {
                namespace: 'shared.indexer.error.codes',
                code: '36F',
                params: [{name: 'code', value: '36F'}],
                level: 2,
            },
            isRelevant: true,
        },
        {
            identifiers: [{shopId: 10441453}],
            explanation: {
                namespace: 'shared.indexer.error.codes',
                code: '392',
                params: [{name: 'code', value: '392'}],
                level: 2,
            },
            isRelevant: true,
        },
        {
            identifiers: [{shopId: 10441453}],
            explanation: {
                namespace: 'shared.indexer.error.codes',
                code: '38w',
                params: [
                    {name: 'code', value: '38w'},
                    {name: 'tagName', value: 'weight'},
                ],
                level: 2,
            },
            isRelevant: true,
        },
    ],
};

export const VERDICTS_MOCK_WITH_MIGRATION_CONFLICT: GetVerdictsResponse = {
    messages: [
        {
            explanation: {
                code: 'MIGRATION_CONFLICT',
                level: 3,
                namespace: 'shared.hidden-offers.subreasons.codes',
                params: [
                    {
                        name: 'sourceBarcodes',
                        value: '123123',
                    },
                    {
                        name: 'targetBarcodes',
                        value: '4607004650642',
                    },
                    {
                        name: 'sourceName',
                        value: 'nokia',
                    },
                    {
                        name: 'targetName',
                        value: 'Nokia 3510 зеленая',
                    },
                ],
                text: 'Товар с таким SKU уже существует и отличается по описанию',
            },
            identifiers: [
                {
                    businessId: 11218444,
                    offerId: '100300402',
                    shopId: 11218443,
                },
                {
                    businessId: 11218444,
                    offerId: '100300402',
                    shopId: 11340738,
                    warehouseId: 0,
                },
            ],
            isRelevant: true,
        },
    ],
};
