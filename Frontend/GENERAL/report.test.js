const { createReport, stringifyReport, parseReport } = require('./report');

const reselectiveMock = {
    directlyAffected: [
        {
            entity: {
                block: 'Attach',
            },
            tech: 'css',
            layer: 'common',
        },
        {
            entity: {
                block: 'Attach',
            },
            tech: 'css',
            layer: 'common',
        },
    ],
    affectedDependents: [
        {
            entity: {
                block: 'Button',
            },
            tech: 'css',
            layer: 'common',
        },
        {
            entity: {
                block: 'Attach',
            },
            tech: 'js',
            layer: 'common',
        },
        {
            entity: {
                block: 'Button',
                mod: { name: 'theme', val: 'clear' },
            },
            tech: 'css',
            layer: 'common',
        },
        {
            entity: {
                block: 'Button',
                mod: { name: 'theme', val: 'clear' },
            },
            tech: 'js',
            layer: 'common',
        },
    ],
    plainBlocksList: [
        'Attach',
        'Button',
    ],
};

describe('report', () => {
    it('create report without lib name', () => {
        const report = createReport(reselectiveMock);

        expect(stringifyReport(report)).toMatchSnapshot();
    });

    it('create report with lib name', () => {
        const report = createReport(reselectiveMock, { lib: '@yandex-lego/components' });

        expect(stringifyReport(report)).toMatchSnapshot();
    });

    it('create report with story points and date', () => {
        const report = createReport(reselectiveMock, { lib: '@yandex-lego/components', issue: { storyPoints: 2 }, date: '2020-01-01', categories: [{ name: 'test', value: 'test' }] });

        expect(stringifyReport(report)).toMatchSnapshot();
    });

    it('create report with spent story points', () => {
        const report = createReport(reselectiveMock, { lib: '@yandex-lego/components', issue: { spentSp: 3 } });

        expect(stringifyReport(report)).toMatchSnapshot();
    });

    it('report parse and normalize', () => {
        const report = createReport(reselectiveMock, { lib: '@yandex-lego/components', issue: { spentSp: 3 } });

        const str = stringifyReport(report);
        expect(report).toEqual(parseReport(str));
    });
});
