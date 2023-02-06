/* eslint-disable @typescript-eslint/no-var-requires,global-require */

import {readdirSync, readFileSync} from 'fs';
import {resolve as pathResolve, join as pathJoin} from 'path';

import {MetricGenConfig} from '../generate/metrics';
import {buildMetrics} from '../scripts/lib/build-metrics';
import {ClickphiteConfig, CPConfig} from '../src/lib/clickphite/ClickphiteConfig';
import {generateClickphitePreview} from '../src/lib/clickphite/generateClickphitePreview';
import {AnyTable} from '../src/lib/table';

const DIST_ROOT = pathResolve(__dirname, '..', 'generated');
const CLICKPHITE_ROOT = pathResolve(DIST_ROOT, 'clickphite');
const METRICS_ROOT = pathResolve(DIST_ROOT, 'metrics');
const TABLES_ROOT = pathResolve(DIST_ROOT, 'tables');

function readDirTree(dir: string): string[] {
    const paths = readdirSync(dir);
    const result = [];

    for (const path of paths) {
        if (path.includes('.')) {
            result.push(path);
        } else {
            const subPaths = readDirTree(pathJoin(dir, path));
            result.push(...subPaths.map(subPath => pathJoin(path, subPath)));
        }
    }

    return result;
}

describe('Проверка конфигов кликфита', () => {
    let configs: Record<string, ClickphiteConfig | CPConfig>;
    const configsList = readDirTree(CLICKPHITE_ROOT).sort().filter(name => !/legacy|\.json/.test(name));

    beforeAll(() => {
        configs = require('../generate/clickphite').default;
    });

    it('Список документации в dist актуален', () => {
        expect(configsList).toEqual(
            Object.keys(configs).filter(config => !/legacy|market_front_speed_kpi$/.test(config)).map(config => `${config}.md`).sort(),
        );
    });

    test.each(configsList)('Документация %s актуальна', name => {
        const generated = readFileSync(pathResolve(CLICKPHITE_ROOT, name), 'utf8');

        // eslint-disable-next-line prefer-destructuring
        name = name.split('.')[0];
        const config = configs[name];
        const actual = generateClickphitePreview(name, config as ClickphiteConfig);
        expect(generated).toEqual(actual);
    });
});

describe('Проверка сгенерированных метрик', () => {
    const metrics: Record<string, string> = {};
    const metricsList = readdirSync(METRICS_ROOT).sort();

    beforeAll(() => {
        const configs: Record<string, MetricGenConfig> = require('../generate/metrics').default;
        Object.entries(configs).forEach(([name, config]) => { metrics[name] = buildMetrics(name, config); });
    });

    it('Список метрик в dist актуален', () => {
        expect(metricsList).toEqual(Object.keys(metrics).map(sensor => `${sensor}.ts`).sort());
    });

    test.each(metricsList)('Набор метрик %s актуален', name => {
        const generated = readFileSync(pathResolve(METRICS_ROOT, name), 'utf8');
        const actual = metrics[name.split('.')[0]];
        expect(generated).toEqual(actual);
    });
});

describe('Проверка таблиц', () => {
    const tablesList: string[] = readDirTree(TABLES_ROOT).sort();

    let rawTables: Record<string, Record<string, AnyTable>>;
    beforeAll(() => {
        rawTables = require('../generate/tables').default;
    });

    it('Список таблиц в generated актуален', () => {
        expect(tablesList).toEqual(
            Object.entries(rawTables).flatMap(([tableGroup, tables]) =>
                Object.keys(tables).map(tableName => `${tableGroup}/${tableName}.md`),
            ).sort(),
        );
    });

    test.each(tablesList)('Таблица %s актуальна', name => {
        const generated = readFileSync(pathResolve(TABLES_ROOT, name)).toString();

        const combinedPath = name.split('/');
        const dirName = combinedPath[0];
        const fileName = combinedPath[1].split('.')[0];
        const rawTable = rawTables[dirName][fileName];

        const actual = rawTable.toMarkdown();

        expect(generated).toEqual(actual);
    });
});
