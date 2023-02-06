import getRevisionsDiff from './getRevisionsDiff';

describe('Basic', () => {
    test('Change name', () => {
        const unchanged = {
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            valid: true,
            parameters: [],
        };
        const prev = {
            name: 'My cron',
            ...unchanged,
        };
        const current = {
            name: 'My perfect cron',
            ...unchanged,
        };
        const res = {
            name: 'My perfect cron',
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    test('Change owner', () => {
        const unchanged = {
            name: 'My cron',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            valid: true,
            parameters: [],
        };
        const prev = {
            owner: 'alinaosv',
            ...unchanged,
        };
        const current = {
            owner: 'v-nemcev',
            ...unchanged,
        };
        const res = {
            name: null,
            owner: 'v-nemcev',
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
});

describe('Change responsible', () => {
    test('Add new', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            valid: true,
            parameters: [],
        };
        const prev = {
            responsibleUsers: ['desur'],
            ...unchanged,
        };
        const current = {
            responsibleUsers: ['desur', 'v-nemcev'],
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: ['v-nemcev'],
                removed: [],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    test('Clear all', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            valid: true,
            parameters: [],
        };
        const prev = {
            responsibleUsers: ['desur'],
            ...unchanged,
        };
        const current = {
            responsibleUsers: [],
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: ['desur'],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    test('Replace some responsible', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            valid: true,
            parameters: [],
        };
        const prev = {
            responsibleUsers: ['desur'],
            ...unchanged,
        };
        const current = {
            responsibleUsers: ['v-nemcev'],
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: ['v-nemcev'],
                removed: ['desur'],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
});

describe('Change cron', () => {
    // 1 1 1 0 = 'Disabled'
    test('Disable valid cron', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            valid: true,
            parameters: [],
        };
        const prev = {
            enabled: true,
            ...unchanged,
        };
        const current = {
            enabled: false,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'Disabled',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 0 1 0 0 = 'Disabled'
    test('Disable invalid cron', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            valid: false,
            parameters: [],
        };
        const prev = {
            enabled: true,
            ...unchanged,
        };
        const current = {
            enabled: false,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'Disabled',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 1 0 1 1 = 'At 8:00'
    test('Enable valid cron', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            valid: true,
            parameters: [],
        };
        const prev = {
            enabled: false,
            ...unchanged,
        };
        const current = {
            enabled: true,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'At 8:00',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 0 0 0 1 = 'Invalid'
    test('Enable invalid cron', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            valid: false,
            parameters: [],
        };
        const prev = {
            enabled: false,
            ...unchanged,
        };
        const current = {
            enabled: true,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'Invalid',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });

    // 0 0 1 0 = null
    test('Make valid disabled cron', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: false,
            parameters: [],
        };
        const prev = {
            valid: false,
            ...unchanged,
        };
        const current = {
            valid: true,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 0 1 1 1 = 'At 8:00'
    test('Make valid enabled cron', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            parameters: [],
        };
        const prev = {
            valid: false,
            ...unchanged,
        };
        const current = {
            valid: true,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'At 8:00',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 1 1 0 1 = 'Invalid'
    test('Make invalid enabled cron', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            parameters: [],
        };
        const prev = {
            valid: true,
            ...unchanged,
        };
        const current = {
            valid: false,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'Invalid',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 1 0 0 0 = null
    test('Make invalid disabled cron', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: false,
            parameters: [],
        };
        const prev = {
            valid: true,
            ...unchanged,
        };
        const current = {
            valid: false,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });

    // 0 1 1 0 = 'Disabled'
    test('Disable && make valid', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            parameters: [],
        };

        const prev = {
            valid: false,
            enabled: true,
            ...unchanged,
        };
        const current = {
            valid: true,
            enabled: false,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'Disabled',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 1 1 0 0 = 'Disabled'
    test('Disable && make invalid', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            parameters: [],
        };

        const prev = {
            valid: true,
            enabled: true,
            ...unchanged,
        };
        const current = {
            valid: false,
            enabled: false,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'Disabled',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 0 0 1 1 = 'At 8:00'
    test('Enable && make valid', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            parameters: [],
        };

        const prev = {
            valid: false,
            enabled: false,
            ...unchanged,
        };
        const current = {
            valid: true,
            enabled: true,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'At 8:00',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 1 0 0 1 = 'Invalid'
    test('Enable && make invalid', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            parameters: [],
        };
        const prev = {
            valid: true,
            enabled: false,
            ...unchanged,
        };
        const current = {
            valid: false,
            enabled: true,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'Invalid',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });

    // 0 1 0 1 = null
    test('Enabled cron && invalid && changed cron expr', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            valid: false,
            enabled: true,
            parameters: [],
        };
        const prev = {
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            ...unchanged,
        };
        const current = {
            expression: '0 00 12 * * ?',
            expressionDescription: 'At 12:00',
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 1 0 1 0 = null
    test('Disabled cron && valid && changed cron expr', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            valid: true,
            enabled: false,
            parameters: [],
        };
        const prev = {
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            ...unchanged,
        };
        const current = {
            expression: '0 00 12 * * ?',
            expressionDescription: 'At 12:00',
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 0 0 0 0 = null
    test('Disabled cron && invalid && changed cron expr', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            valid: false,
            enabled: false,
            parameters: [],
        };
        const prev = {
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            ...unchanged,
        };
        const current = {
            expression: '0 00 12 * * ?',
            expressionDescription: 'At 12:00',
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
    // 1 1 1 1 = 'At 12:00'
    test('Enabled cron && valid && changed cron expr', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            responsibleUsers: [],
            valid: true,
            enabled: true,
            parameters: [],
        };

        const prev = {
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            ...unchanged,
        };
        const current = {
            expression: '0 00 12 * * ?',
            expressionDescription: 'At 12:00',
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: 'At 12:00',
            blocks: [],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
});

describe('Change block parameters', () => {
    test('First revision block params', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            valid: true,
            responsibleUsers: ['desur'],
        };
        const prev = {
            parameters: [],
            ...unchanged,
        };
        const current = {
            parameters: [
                {
                    id: 'metrics-experiments-import-serps',
                    name: 'import serps parameters',
                    type: 'MultiDownloadRequest',
                },
                {
                    id: 'metrics-experiments-graph-config',
                    name: 'graph config parameters',
                    type: 'GraphConfig',
                },
            ],
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: ['Import serps parameters', 'Graph config parameters'],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });

    test('Remove params block', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            valid: true,
            responsibleUsers: ['desur'],
        };
        const prev = {
            parameters: [
                {
                    id: 'metrics-experiments-import-serps',
                    name: 'import serps parameters',
                    type: 'MultiDownloadRequest',
                },
                {
                    id: 'metrics-experiments-graph-config',
                    name: 'graph config parameters',
                    type: 'GraphConfig',
                },
            ],
            ...unchanged,
        };
        const current = {
            parameters: [
                {
                    id: 'metrics-experiments-import-serps',
                    name: 'import serps parameters',
                    type: 'MultiDownloadRequest',
                },
            ],
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: ['Graph config parameters'],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });

    test('Change other params', () => {
        const unchanged = {
            name: 'My cron',
            owner: 'alinaosv',
            expression: '0 00 8 * * ?',
            expressionDescription: 'At 8:00',
            enabled: true,
            valid: true,
            responsibleUsers: ['desur'],
            parameters: [],
        };
        const prev = {
            specificationRevision: 1,
            ...unchanged,
        };
        const current = {
            specificationRevision: 8,
            ...unchanged,
        };
        const res = {
            name: null,
            owner: null,
            responsible: {
                added: [],
                removed: [],
            },
            schedule: null,
            blocks: ['Cron parameters'],
        };

        expect(getRevisionsDiff(prev, current)).toEqual(res);
    });
});
