import url, { UrlWithParsedQuery } from 'url';
import _ from 'lodash';
import { namedTypes } from 'ast-types';
import { JSCodeshift } from 'jscodeshift/src/core';
import { UrlPlain } from './types';
import { HermioneAstParser } from '../../types';

/* Конвертирует js объекты в AST. Поддерживается только Literal и ObjectExpression. */
export function toAst(value: any, parser: HermioneAstParser): namedTypes.Node {
    const j: JSCodeshift = parser.parser;

    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
        return j.literal(value);
    }

    return j(`(${JSON.stringify(value)})`)
        .find(j.ObjectExpression)
        .get().value;
}

/* Парсит объекты AST в сущности js. Поддерживает только Literal и ObjectExpression, в остальных случаях возвращается null. */
export function fromAst(node: namedTypes.Node, parser: HermioneAstParser): any {
    const j: JSCodeshift = parser.parser;
    if (j.Literal.check(node)) {
        return node.value;
    }
    if (j.ObjectExpression.check(node)) {
        return parser.compile(j(node).toSource(), undefined, true);
    }
    return null;
}

function toPlainUrl(urlObject: UrlWithParsedQuery): UrlPlain {
    const result = _.omit(urlObject, [
        'query',
        'slashes',
        'host',
        'path',
        'href',
        'search',
    ]) as UrlPlain;
    result.query = [];
    const entries = _.entries(urlObject.query);
    for (const [key, value] of entries) {
        if (Array.isArray(value)) {
            result.query.push(...value.map((v) => [key, v] as [string, string]));
        } else {
            result.query.push([key, value]);
        }
    }
    return result;
}

export function parseUrlPlain(str: string): UrlPlain {
    let urlObject: UrlPlain;
    try {
        urlObject = JSON.parse(str);
    } catch {
        urlObject = toPlainUrl(url.parse(str, true));
    }
    const result = {} as UrlPlain;
    const keys = _.keys(url.parse(''));
    for (const key of keys) {
        result[key] = urlObject[key] ?? null;
    }

    return result;
}

/* Выполняет группировку пар строк в пару из строки и массива:
 * [['key', '1'], ['key', '2']] => [['key', ['1', '2']]] */
function group(
    arr: [string | undefined, string | undefined][],
): [string | undefined, string | string[] | undefined][] {
    const result: Record<string, [string, string | string[] | undefined]> = {};
    for (const [key, value] of arr) {
        if (!key) {
            continue;
        }
        if (!result[key]) {
            result[key] = value ? [key, [value]] : [key, value];
        } else {
            (result[key][1] as string[]).push(value as string);
        }
    }
    return _.values(result).map((item) =>
        Array.isArray(item[1]) && item[1].length === 1 ? [item[0], item[1][0]] : item,
    );
}

function match(subject: string | undefined, pattern: string | undefined): boolean {
    let regexpMatches;
    try {
        regexpMatches =
            subject !== undefined && pattern !== undefined && new RegExp(pattern).test(subject);
    } catch {}
    return subject === pattern || regexpMatches;
}

function replace(
    subject: string | undefined,
    pattern: string | undefined,
    replacement: string | undefined,
): string | undefined {
    if (subject === undefined || pattern === undefined || replacement === undefined) {
        return match(subject, pattern) ? replacement : subject;
    }

    subject = subject.replace(pattern, replacement);
    try {
        subject = subject.replace(new RegExp(pattern), replacement);
    } catch {}
    return subject;
}

export function matchUrl(subject: UrlWithParsedQuery, pattern: Partial<UrlPlain>): boolean {
    for (const key of _.keys(_.omit(pattern, 'query'))) {
        if (_.isNil(subject[key])) {
            continue;
        }
        if (!match(subject[key], pattern[key] ?? '')) {
            return false;
        }
    }

    return true;
}

export function replaceUrl(
    subject: UrlWithParsedQuery,
    pattern: Partial<UrlPlain>,
    replacement: Partial<UrlPlain>,
): UrlWithParsedQuery {
    const result = _.cloneDeep(subject);
    // Проверка нужна для того, чтобы часть url не заменилась при совпадении частей, но не всего url.
    if (!matchUrl(subject, pattern)) {
        return result;
    }

    for (const key of _.keys(_.omit(pattern, 'query'))) {
        if (!subject[key] || !pattern[key] || !replacement[key]) {
            continue;
        }
        result[key] = replace(subject[key], pattern[key], replacement[key]);
    }

    if (pattern.query && subject.query && replacement.query) {
        const resultQuery: [string | undefined, string | undefined][] = [];
        // const preparedPattern = flatten(pattern.query);
        const subjectQuery = toPlainUrl(subject).query ?? [];
        // const preparedReplacement = flatten(replacement.query);

        for (let i = 0; i < pattern.query.length; i++) {
            const [patternKey, patternValue] = pattern.query[i];
            const [replaceKey, replaceValue] = replacement.query[i];
            if (!patternKey) {
                resultQuery.push([replaceKey, replaceValue]);
            }
            for (let j = 0; j < subjectQuery.length; j++) {
                const [subjectKey, subjectValue] = subjectQuery[j];
                if (match(subjectKey, patternKey) && match(subjectValue, patternValue)) {
                    subjectQuery[j][0] = replace(subjectKey, patternKey, replaceKey);
                    subjectQuery[j][1] = replace(subjectValue, patternValue, replaceValue);
                }
            }
        }

        resultQuery.push(...subjectQuery);

        result.query = _.fromPairs(
            group(resultQuery)
                .map((item) =>
                    item[0] && !Array.isArray(item[1]) && Array.isArray(subject.query[item[0]])
                        ? [item[0], [item[1]]]
                        : item,
                )
                .filter(([key]) => key),
        );

        result.search = null;
    }
    return result;
}

export function replaceUrlString(
    urlString: string,
    search: Partial<UrlPlain>,
    replacement: Partial<UrlPlain>,
    isQueryString = false,
): string {
    const urlObject = url.parse((isQueryString ? '?' : '') + urlString, true);
    const replacedUrl = replaceUrl(urlObject, search, replacement);
    let result = url.format(replacedUrl);
    if (isQueryString) {
        result = _.trimStart(result, '?');
    }
    return result;
}

/* Функция рекурсивно выполняет обход каждого узла объекта, вызывая callback.
 * Если callback вернул false, обход вглубь останавливается. */
export function walk(
    root: Record<string, any>,
    cb: (object: Record<string, any>, property: string, path: string[]) => boolean,
): void {
    const rootId = Symbol();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    type QueueItem = { object: Record<string, any>; property: string; path: string[] };
    const queue: QueueItem[] = [
        { object: { [rootId]: root }, property: rootId as unknown as string, path: [] },
    ];

    while (queue.length) {
        const { object, property, path } = queue.pop() as QueueItem;

        let keepGoing = true;
        // Это условие необходимо для того, чтобы не вызывать callback от корня, когда property - это symbol rootId
        if (typeof property === 'string') {
            keepGoing = cb(object, property, path);
        }

        if (typeof object[property] !== 'object' || !keepGoing) {
            continue;
        }

        _.forOwn(object[property], (_value, key): void => {
            queue.push({
                object: object[property],
                property: key,
                path: typeof key === 'string' ? [...path, key] : [...path],
            });
        });
    }
}

/* Функция аналогична walk, но при обходе пытается развернуть JSON-строки в объекты и пройти по ним. */
export function walkAndUnwrapJson(
    root: Record<string, any>,
    cb: (object: Record<string, any>, property: string, path: string[]) => boolean,
    prefixPath: string[] = [],
): void {
    walk(root, (object, property, path): boolean => {
        const currentPath = [...prefixPath, ...path];
        const currentProperty = _.last(currentPath) ?? property;
        if (typeof object[currentProperty] === 'string') {
            try {
                object[currentProperty] = JSON.parse(object[currentProperty]);
                const keepGoing = cb(object, currentProperty, currentPath);
                if (keepGoing) {
                    walkAndUnwrapJson(object[currentProperty], cb, currentPath);
                }
                object[currentProperty] = JSON.stringify(object[currentProperty]);
                return false;
            } catch {}
        }
        return cb(object, currentProperty, currentPath);
    });
}
