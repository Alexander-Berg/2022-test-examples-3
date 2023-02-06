import { AnalyticsAliases } from '../LcAnalytics.types';

export const bannerid = '1234567890';

export const analyticsMocks = {
    metrika: {
        metrika: {
            ids: ['base-metrika'],
            params: { base: 'param' },
        },
    },
    metrikaWithGA: {
        googleAnalytics: {
            id: 'GA-123',
        },
        metrika: {
            ids: ['metrika'],
            params: { visitParam: 'param' },
        },
    },
    ga: {
        googleAnalytics: {
            id: 'GA-123',
        },
    },
    full: {
        googleAnalytics: {
            id: 'GA-123',
        },
        metrika: {
            ids: ['metrika'],
            params: { visitParam: 'param' },
        },
        facebookPixel: {
            id: 'facebook',
        },
        myTarget: {
            id: 'target',
        },
        vkPixel: {
            id: 'pixel',
        },
        tiktokPixel: {
            id: 'tiktok',
        },
    },
    empty: {},
};

export const presetMocks = {
    metrikaWithGA: [
        {
            id: '38637415',
            type: AnalyticsAliases.metrika,
            goals: {
                switch: 'switch',
            },
            settings: {
                clickmap: false,
            },
        },
        {
            id: 'ga',
            type: AnalyticsAliases.googleAnalytics,
        },
    ],
    metrika: [
        {
            id: '38637415',
            type: AnalyticsAliases.metrika,
            goals: {
                switch: 'switch',
            },
            settings: {
                clickmap: false,
            },
        },
    ],
    metrikaWithDifferentGoals: [
        {
            id: '38637415',
            type: AnalyticsAliases.metrika,
            goals: {
                switch: 'switch',
            },
            settings: {
                clickmap: false,
            },
        },
        {
            id: '38637416',
            type: AnalyticsAliases.metrika,
            goals: {
                switch: 'another_switch',
            },
            settings: {
                clickmap: false,
            },
        },
    ],
};

export const trackGAParamsMock = {
    base: {
        eventAction: 'action',
        hitType: 'event',
        url: 'url',
    },
    full: {
        hitType: 'event',
        eventAction: 'action',
        eventCategory: 'cat',
        eventLabel: 'label',
        url: 'url',
    },
};

export const visitParamsMock = {
    base: {
        key: 'value',
    },
};
