import {omit, pick} from 'lodash';

const serpsetIds = [26170347, 26170199];
const serpsetFilter = ['onlySearchResult', 'onlySearchResult'];
const serpsetInfo = [
    {
        leftSerpset: serpsetIds[0],
        rightSerpset: serpsetIds[1],
        leftComponentFilter: serpsetFilter[0],
        rightComponentFilter: serpsetFilter[1],
    },
    {
        leftSerpset: serpsetIds[1],
        rightSerpset: serpsetIds[1],
        leftComponentFilter: serpsetFilter[1],
        rightComponentFilter: serpsetFilter[1],
    },
];

const commonCheckFields = [
    'name',
    'description',
    'deprecated',
    'greaterBetter',
];
const nullDiff = {
    diff: {
        value: 0,
        percent: 0,
        pValue: 1,
        verboseSignification: 'GRAY',
        wins: 0,
        losses: 0,
        signification: 'GRAY',
    },
};
const notCalculatedMetric = {
    leftData: {
        value: -2,
        queryAmount: 10,
        standardDeviation: 0,
    },
    rightData: {
        value: -2,
        queryAmount: 10,
        standardDeviation: 0,
    },
    ...nullDiff,
};

const metricParams = {
    leftFilter: 'onlySearchResult',
    rightFilter: 'onlySearchResult',
    greaterBetter: true,
};
const metricDetails = {
    GROUP1metric1: {
        group: 'GROUP1',
        name: 'metric1',
        description: 'Descr: metric1',
        ...metricParams,
    },
    GROUP1metric2: {
        group: 'GROUP1',
        name: 'metric2',
        description: 'Descr: metric2',
        ...metricParams,
    },
    GROUP2metric2: {
        group: 'GROUP2',
        name: 'metric2',
        description: 'Descr: metric2',
        ...metricParams,
    },
};
const metricCalcParams = {
    leftOverFresh: null,
    rightOverFresh: null,
    absolute: false,
    leftDate: '2020-03-16T19:49:34.368+0300',
    rightDate: '2020-03-16T19:49:34.368+0300',
    leftMetricDate: '2020-03-16T19:49:34.368+0300',
    rightMetricDate: null,
};
const metricCalcParamsOverFresh = {
    ...metricCalcParams,
    leftOverFresh: {
        metricsDate: '2020-03-16T19:49:34.368+0300',
    },
    rightOverFresh: null,
};

const metricsValues = {
    GROUP1metric1: [
        {
            leftData: {
                value: 0.1,
                queryAmount: 10,
                standardDeviation: 0.001,
            },
            rightData: {
                value: 0.2,
                queryAmount: 10,
                standardDeviation: 0.001,
            },
            diff: {
                value: -0.1,
                percent: -0.5,
                pValue: 0.99,
                verboseSignification: 'RED',
                wins: 0.5,
                losses: 0.5,
                signification: 'RED',
            },
        },
        {
            leftData: {
                value: 0.2,
                queryAmount: 10,
                standardDeviation: 0.001,
            },
            rightData: {
                value: 0.2,
                queryAmount: 10,
                standardDeviation: 0.001,
            },
            ...nullDiff,
        },
    ],
    GROUP1metric2: [
        /* empty metric - calculated for 1 serpset, but not avaliable for baseline */
        notCalculatedMetric,
        /* empty metric - not avaliable for baseline */
        notCalculatedMetric,
    ],

    GROUP2metric2: [
        /* empty metric - not calculated for 1 serpset, but avaliable for baseline */
        notCalculatedMetric,
        {
            leftData: {
                value: 3,
                queryAmount: 10,
                standardDeviation: 0.1,
            },
            rightData: {
                value: 3,
                queryAmount: 10,
                standardDeviation: 0.1,
            },
            ...nullDiff,
        },
    ],
};

const basicCheck = {
    name: 'basicCheck',
    description: 'Descr: basicCheck',
    leftData: {value: 0, queryAmount: 10, standardDeviation: 0},
    rightData: {value: 0, queryAmount: 10, standardDeviation: 0},
    leftThreshold: 0,
    rightThreshold: 0,
    leftState: 'PASSED',
    rightState: 'PASSED',
    threshold: 0,
    diff: {},
    deprecated: false,
    greaterBetter: true,
};
const diffCheck = {
    ...basicCheck,
    name: 'diffCheck',
    description: 'Descr: diffCheck',
    leftState: 'FAILED',
    rightState: 'FAILED',
};
const overFreshCheck = {
    name: 'over-fresh-requirements',
    description:
        'Requirements that were updated after mstand metric was calculated. ' +
        '((https://wiki.yandex-team.ru/metrics/compare/#over-fresh-requirements Wiki))',
    leftData: {
        value: '1',
        queryAmount: 1,
        standardDeviation: 0,
    },
    rightData: {
        value: '',
        queryAmount: 1,
        standardDeviation: 0,
    },
    diff: {
        losses: null,
        pValue: 1,
        percent: 0,
        signification: 'GRAY',
        value: 0,
        verboseSignification: 'GRAY',
        wins: null,
    },
    deprecated: false,
    greaterBetter: false,
    leftState: 'FAILED',
    rightState: 'PASSED',
    threshold: 0,
    leftOverFresh: {metricDate: '2020-03-16T19:49:34.368+0300'},
    rightOverFresh: null,
    isOverFresh: true,
};
const checksValues = {
    GROUP1metric1checks: [[diffCheck], [basicCheck]],
    GROUP1metric2checks: [[], []],
    GROUP2metric2checks: [[], []],
};

// NB: metrics order for 1 and 2 pair is different, but they should handle properly
// лишний вложенный массив из-за того, что данные запрашиваются через multiple
const rawData = [
    [
        {
            metricObservations:
                // SB pair metrics
                // [26170347, 26170199]
                [
                    {
                        metric: metricDetails.GROUP1metric1,

                        ...metricCalcParamsOverFresh,
                        ...metricsValues.GROUP1metric1[0],

                        checks: checksValues.GROUP1metric1checks[0],
                    },
                    {
                        metric: metricDetails.GROUP1metric2,

                        ...metricCalcParams,
                        ...metricsValues.GROUP1metric2[0],

                        checks: checksValues.GROUP1metric2checks[0],
                    },
                    {
                        metric: metricDetails.GROUP2metric2,

                        ...metricCalcParams,
                        ...metricsValues.GROUP2metric2[0],

                        checks: checksValues.GROUP2metric2checks[0],
                    },
                ],
        },
        {
            metricObservations:
                // [26170199, 26170199]
                [
                    {
                        metric: metricDetails.GROUP1metric2,

                        ...metricCalcParams,
                        ...metricsValues.GROUP1metric2[1],

                        checks: checksValues.GROUP1metric2checks[1],
                    },
                    {
                        metric: metricDetails.GROUP2metric2,

                        ...metricCalcParams,
                        ...metricsValues.GROUP2metric2[1],

                        checks: checksValues.GROUP2metric2checks[1],
                    },
                    {
                        metric: metricDetails.GROUP1metric1,

                        ...metricCalcParams,
                        ...metricsValues.GROUP1metric1[1],

                        checks: checksValues.GROUP1metric1checks[1],
                    },
                ],
        },
    ],
];

const parsedData = [
    {
        name: 'GROUP1',
        metrics: [
            {
                ...pick(metricDetails.GROUP1metric1, [
                    'group',
                    'name',
                    'description',
                    'greaterBetter',
                ]),

                failedChecks: [overFreshCheck.name, diffCheck.name],
                values: [
                    {
                        ...serpsetInfo[0],
                        ...metricCalcParamsOverFresh,
                        ...metricsValues.GROUP1metric1[0],
                    },
                    {
                        ...serpsetInfo[1],
                        ...metricCalcParams,
                        ...metricsValues.GROUP1metric1[1],
                    },
                ],

                checks: [
                    {
                        ...pick(overFreshCheck, commonCheckFields),
                        hasFailed: true,
                        values: [
                            {
                                ...serpsetInfo[0],
                                ...omit(overFreshCheck, [
                                    ...commonCheckFields,
                                    'isOverFresh',
                                    'leftOverFresh',
                                    'rightOverFresh',
                                ]),
                            },
                            {
                                ...serpsetInfo[1],
                            },
                        ],
                    },
                    {
                        ...pick(diffCheck, commonCheckFields),
                        hasFailed: true,
                        values: [
                            {
                                ...serpsetInfo[0],
                                ...omit(diffCheck, commonCheckFields),
                            },
                            {
                                ...serpsetInfo[1],
                            },
                        ],
                    },
                    {
                        ...pick(basicCheck, commonCheckFields),
                        hasFailed: false,
                        values: [
                            {
                                ...serpsetInfo[0],
                            },
                            {
                                ...serpsetInfo[1],
                                ...omit(basicCheck, commonCheckFields),
                            },
                        ],
                    },
                ],
            },
            {
                ...pick(metricDetails.GROUP1metric2, [
                    'group',
                    'name',
                    'description',
                    'greaterBetter',
                ]),

                failedChecks: [],
                values: [
                    {
                        ...serpsetInfo[0],
                        ...metricCalcParams,
                        ...metricsValues.GROUP1metric2[0],
                    },
                    {
                        ...serpsetInfo[1],
                        ...metricCalcParams,
                        ...metricsValues.GROUP1metric2[1],
                    },
                ],

                checks: [],
            },
        ],
    },
    {
        name: 'GROUP2',
        metrics: [
            {
                ...pick(metricDetails.GROUP2metric2, [
                    'group',
                    'name',
                    'description',
                    'greaterBetter',
                ]),

                failedChecks: [],
                values: [
                    {
                        ...serpsetInfo[0],
                        ...metricCalcParams,
                        ...metricsValues.GROUP2metric2[0],
                    },
                    {
                        ...serpsetInfo[1],
                        ...metricCalcParams,
                        ...metricsValues.GROUP2metric2[1],
                    },
                ],

                checks: [],
            },
        ],
    },
];

export {serpsetIds, serpsetFilter, rawData, parsedData};
