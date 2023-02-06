import crypto from 'crypto';
import url, { UrlWithParsedQuery } from 'url';
import _ from 'lodash';
import { Command } from 'commander';
import { TestdataManagerOptions, UrlPlain } from './types';
import { parseUrlPlain, replaceUrl, replaceUrlString } from './utils';
import {
    FULL_URL_REGEXP_STRING,
    PATH_URL_REGEXP_STRING,
    QUERY_URL_REGEXP_STRING,
} from './constants';

export function hermioneReplacer(
    args: any[],
    commandName: string,
    pluginOptions: TestdataManagerOptions,
): any[] {
    if (!pluginOptions.oldUrl || !pluginOptions.newUrl) {
        throw new Error('oldUrl or newUrl option was not provided, but is required to proceed');
    }

    const result: any[] = [];

    for (let arg of args) {
        arg = _.cloneDeep(arg);
        if (typeof arg === 'string') {
            const urlObject = url.parse(arg, true);
            result.push(
                url.format(replaceUrl(urlObject, pluginOptions.oldUrl, pluginOptions.newUrl)),
            );
        } else if (arg) {
            if (arg.pathname) {
                arg.pathname = url.format(
                    replaceUrl(
                        url.parse(arg.pathname, true),
                        pluginOptions.oldUrl,
                        pluginOptions.newUrl,
                    ),
                );
            } else {
                const urlObject = {
                    query: arg,
                };
                arg = replaceUrl(
                    urlObject as UrlWithParsedQuery,
                    pluginOptions.oldUrl,
                    pluginOptions.newUrl,
                ).query;
                _.forOwn(arg, (value, key) => {
                    if (typeof value === 'string') {
                        arg[key] = decodeURIComponent(value);
                    } else if (Array.isArray(value)) {
                        arg[key] = value.map((p) =>
                            typeof p === 'string' ? decodeURIComponent(p) : p,
                        );
                    }
                });
            }
            result.push(arg);
        } else {
            result.push(null);
        }
    }
    return result;
}

export function testpalmReplacer(data: any, pluginOptions: TestdataManagerOptions): any {
    if (!pluginOptions.oldUrl || !pluginOptions.newUrl) {
        throw new Error('oldUrl or newUrl option was not provided, but is required to proceed');
    }

    const urlObject = {
        query: data,
    } as UrlWithParsedQuery;
    const query = replaceUrl(
        urlObject as UrlWithParsedQuery,
        pluginOptions.oldUrl,
        pluginOptions.newUrl,
    ).query;

    _.forOwn(query, (value, key) => {
        if (typeof value === 'string') {
            query[key] = decodeURIComponent(value);
        } else if (Array.isArray(value)) {
            query[key] = value.map((p) => (typeof p === 'string' ? decodeURIComponent(p) : p));
        }
    });

    return query;
}

export function testdataStringReplacer(
    object: string | Record<string, any>,
    path: string[],
    pluginOptions: TestdataManagerOptions,
): string {
    if (!pluginOptions.oldUrl || !pluginOptions.newUrl) {
        throw new Error('oldUrl or newUrl option was not provided, but is required to proceed');
    }

    let input = _.get(object, path);

    // Подготовка строки, замена %%HOST%% на хеш, корректное имя хоста. %%HOST%% без обработки вызывает проблемы при парсинге url
    const dummyHost = crypto.createHash('sha1').update('').digest('hex').slice(0, 16) + '.com';
    input = input.replace(/%%HOST%%/g, dummyHost);

    input = input.replace(
        new RegExp(
            `${FULL_URL_REGEXP_STRING}|${PATH_URL_REGEXP_STRING}|${QUERY_URL_REGEXP_STRING}`,
            'g',
        ),
        (match): string => {
            return replaceUrlString(
                match,
                pluginOptions.oldUrl as Partial<UrlPlain>,
                pluginOptions.newUrl as Partial<UrlPlain>,
                !/^(http|\/|\?)/.test(match),
            );
        },
    );

    // Восстановление строки, возврат %%HOST%%
    return input.replace(new RegExp(`${dummyHost}`, 'g'), '%%HOST%%');
}

export function testdataQueryObjectReplacer(
    data: string | Record<string, any>,
    path: string[],
    pluginOptions: TestdataManagerOptions,
): string | string[] | undefined {
    const subject = {
        query: _.get(data, path.slice(0, -1)),
    } as UrlWithParsedQuery;

    const result = replaceUrl(
        subject,
        pluginOptions.oldUrl as Partial<UrlPlain>,
        pluginOptions.newUrl as Partial<UrlPlain>,
    );

    return result.query[_.last(path) as string];
}

export function testdataSlowReplacer(
    object: string | Record<string, any>,
    path: string[],
    pluginOptions: TestdataManagerOptions,
): string | Record<string, any> | undefined {
    const value = _.get(object, path);
    const propertyName = _.last(path);
    const queryParamNames = pluginOptions.oldUrl?.query?.map(_.property(0));

    if (queryParamNames?.includes(propertyName)) {
        return testdataQueryObjectReplacer(object, path, pluginOptions);
    } else if (typeof value === 'string') {
        return testdataStringReplacer(object, path, pluginOptions);
    }
    return value;
}

interface ReplacerObject {
    readonly pattern: string;
    replace(...args: any[]): string;
}

interface ReplacerFabric {
    (search: Partial<UrlPlain>, replacement: Partial<UrlPlain>): ReplacerObject[];
}

const URL_SYMBOL = '[\\w%.-]';

const withDelimiters = (str: string): string => {
    return `(?<!${URL_SYMBOL})${str}(?!${URL_SYMBOL})`;
};

const urlFullEdit: ReplacerFabric = (
    search: Partial<UrlPlain>,
    replacement: Partial<UrlPlain>,
): ReplacerObject[] => {
    const potentialPattern = url.format(_.omit(search, 'query'));
    if (_.isEmpty(potentialPattern)) {
        return [];
    }
    return [
        {
            pattern: potentialPattern,
            replace(): string {
                return url.format(_.omit(replacement, 'query'));
            },
        },
    ];
};

const urlPathnameEdit: ReplacerFabric = (
    search: Partial<UrlPlain>,
    replacement: Partial<UrlPlain>,
): ReplacerObject[] => {
    if (!search.pathname) {
        return [];
    }
    return [
        {
            pattern: withDelimiters(search.pathname),
            replace(): string {
                return replacement.pathname ?? '';
            },
        },
    ];
};

const queryStringEdit: ReplacerFabric = (
    search: Partial<UrlPlain>,
    replacement: Partial<UrlPlain>,
): ReplacerObject[] => {
    if (!search.query || !replacement.query) {
        return [];
    }
    return search.query.flatMap((from, index): ReplacerObject[] => {
        const to = replacement.query?.[index] as [string | undefined, string | undefined];
        if (!from[0]) {
            return [];
        }
        const pair = from[0] + '=' + (from[1] ?? '');
        return [
            {
                pattern: `\\?${pair}&|\\?${pair}(?!${URL_SYMBOL})|&${pair}(?!${URL_SYMBOL})|(?<!${URL_SYMBOL})${pair}&`,
                replace(match): string {
                    if (to[0]) {
                        let replacement = to[0] + '=' + to[1] ?? '';
                        if (match[0] === '?' || match[0] === '&') {
                            replacement = match[0] + replacement;
                        }
                        if (match.endsWith('&')) {
                            replacement = replacement + match.slice(-1);
                        }
                        return replacement;
                    }
                    return match.startsWith('?') && match.endsWith('&') ? '?' : '';
                },
            },
        ];
    });
};

const queryStringAdd: ReplacerFabric = (
    search: Partial<UrlPlain>,
    replacement: Partial<UrlPlain>,
): ReplacerObject[] => {
    if (!search.query || !replacement.query) {
        return [];
    }
    return search.query.flatMap((from, index): ReplacerObject[] => {
        const to = replacement.query?.[index] as [string | undefined, string | undefined];
        if (from[0]) {
            return [];
        }

        return [
            {
                pattern: `[?&]${URL_SYMBOL}+=${URL_SYMBOL}*(?![\\w&%,-/])`,
                replace(match): string {
                    return match + '&' + to[0] + '=' + to[1];
                },
            },
        ];
    });
};

export function testdataReplacer(
    search: Partial<UrlPlain>,
    replacement: Partial<UrlPlain>,
    contents: string,
): string {
    if (_.get(search, 'query.length', 0) !== _.get(replacement, 'query.length', 0)) {
        throw new Error(`Search and replacement queries must have equal lengths`);
    }

    const replacers: ReplacerObject[] = [
        urlFullEdit,
        urlPathnameEdit,
        queryStringEdit,
        queryStringAdd,
    ].flatMap((factory) => factory(search, replacement));

    for (const replacer of replacers) {
        contents = contents.replace(new RegExp(replacer.pattern, 'g'), replacer.replace);
    }

    return contents;
}

const defaultOptions: TestdataManagerOptions = {
    enabled: true,
    searchPath: '.',
    useSlowAlgorithm: false,
    hermioneReplacers: {
        yaOpenSerp: hermioneReplacer,
        yaOpenAds: hermioneReplacer,
    },
    testpalmReplacers: {
        params: testpalmReplacer,
    },
    testdataReplacers: {
        default: testdataReplacer,
    },
} as TestdataManagerOptions;

export function parseConfig(options: Partial<TestdataManagerOptions>): TestdataManagerOptions {
    const result = _.cloneDeep(_.defaultsDeep(options, defaultOptions));

    return result;
}

export function addOptionsFromInput(
    options: TestdataManagerOptions,
    command: Command,
    commandOptions: Record<string, any>,
): void {
    const prepare = (url: string): string => {
        return !url.includes('?') && url.includes('=') ? '?' + url : url;
    };

    const encode = (param: [string | undefined, string | undefined]): [string, string] => {
        return [encodeURIComponent(param[0] ?? ''), encodeURIComponent(param[1] ?? '')];
    };

    options.searchPath = command.args[0] ?? options.searchPath;
    options.oldUrl = parseUrlPlain(prepare(commandOptions.oldUrl));
    options.newUrl = parseUrlPlain(prepare(commandOptions.newUrl));
    options.useSlowAlgorithm = Boolean(commandOptions.useSlowAlgorithm);

    if (options.oldUrl.query && options.newUrl.query) {
        const [oldQuery, newQuery] = [options.oldUrl.query, options.newUrl.query];
        const times = Math.abs(oldQuery.length - newQuery.length);
        const isNewQuerySmaller = oldQuery.length > newQuery.length;
        for (let i = 0; i < times; i++) {
            isNewQuerySmaller ? newQuery.push(['', undefined]) : oldQuery.push(['', undefined]);
        }

        const queryLength = _.get(options, 'oldUrl.query.length', 0);
        for (let i = 0; i < queryLength; i++) {
            const from = oldQuery[i];
            const to = newQuery[i];
            if (from[0]) {
                oldQuery.push(encode(from));
                newQuery.push(encode(to));
            } else {
                [to[0], to[1]] = encode(to);
            }
        }
    }
}
