import linesToConfigOptions from './linesToConfigOptions';

test('linesToConfigOptions', () => {
    const lines = [
        {
            cron: 100503,
            system: 'fyu4nv5i86wkdie7ypma5d7zvjdct8bf',
            filter: 'default',
            metric: '_404-first-image-10',
        },
        {
            cron: 100503,
            system: 'fyu4nv5i86wkdie7ypma5d7zvjdct8bf',
            filter: 'default',
            metric: '_404-first-image-5',
        },
        {
            cron: 100540,
            system: 'r2pakdgf7v6pz3gkt3dnj4nhyyncd9sn',
            filter: 'RU DESKTOP',
            metric: 'diff-2-serps-newdoc-5',
        },
        {
            cron: 100540,
            system: 'r2pakdgf7v6pz3gkt3dnj4nhyyncd9sn',
            filter: 'RU DESKTOP',
            metric: 'diff-2-serps-newdoc-10',
        },
        {
            cron: 100540,
            system: 'r2pakdgf7v6pz3gkt3dnj4nhyyncd9sn',
            filter: 'RU DESKTOP',
            metric: 'diff-2-serps-query',
        },
        {
            cron: 100540,
            system: 'r2pakdgf7v6pz3gkt3dnj4nhyyncd9sn',
            filter: undefined,
            metric: 'diff-2-serps-query',
        },
    ];
    const configOptions = {
        100503: {
            system: ['fyu4nv5i86wkdie7ypma5d7zvjdct8bf'],
            filter: ['default'],
            metric: ['_404-first-image-10', '_404-first-image-5'],
        },
        100540: {
            system: ['r2pakdgf7v6pz3gkt3dnj4nhyyncd9sn'],
            filter: ['RU DESKTOP'],
            metric: [
                'diff-2-serps-newdoc-5',
                'diff-2-serps-newdoc-10',
                'diff-2-serps-query',
            ],
        },
    };
    expect(linesToConfigOptions(lines)).toEqual(configOptions);
});
