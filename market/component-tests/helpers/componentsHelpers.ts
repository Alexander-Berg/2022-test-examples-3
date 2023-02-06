import os from 'os';
import fs from 'fs';
import path from 'path';
import open from 'open';
import {Dispatch} from 'redux';
import {prettyDOM} from '@testing-library/dom';

/**
 * Тайпинги и описание для хелперов находятся в cats.d.ts
 * При добавлении нового хелпера или правки текущего
 * нужно не забыть поправить типы в тайпингах и экпортировать его
 * в ./componentsGlobal
 */

type ShallowMerge<A, B> = Omit<A, keyof B> & B;

const getTestManagerPattern = (id: string) => `https://testpalm.yandex-team.ru/testcase/${id}`;

type DescribeNameScope = 'Page' | 'Pidget' | 'Component' | 'Widget' | 'Examples';
// eslint-disable-next-line prettier/prettier
type DescribeName<Scope extends DescribeNameScope> = `${Scope}. ${string}`;

type CurrentTestData = {
    id: string | null;
    name: string | null;
};

jest.mock('@yandex-market/b2b-core/app', () => {
    const _Resource = jest.requireActual('@yandex-market/b2b-core/app/resource').default;
    return {
        Resource: _Resource,
    };
});
const isSingleRun = process.argv.filter(x => /^marketmbi-\d+$/.test(x)).length === 1;

export const currentTestData: CurrentTestData = {
    id: null,
    name: 'null',
};

// !!! не забывайте await для async шагов !!! //
export function step<T>(name: string, fn: () => T): T {
    // eslint-disable-next-line prefer-destructuring
    const reporter = global.reporter;

    if (!reporter) {
        if (isSingleRun) {
            process.stderr.write(`- ${name}\n`);
        }
        return fn();
    }

    return reporter.runStep(name, fn);
}

export function queryI18nElementDeep(elem: Element | null, key?: string): Element | null {
    if (!elem) {
        return null;
    }

    const currentKey = elem.getAttribute('data-e2e-i18n-key');

    if (key && currentKey === key) {
        return elem;
    }
    if (currentKey) {
        return elem;
    }

    return key ? elem.querySelector(`[data-e2e-i18n-key="${key}"]`) : elem.querySelector(`[data-e2e-i18n-key]`);
}

export function queryByI18nKeyDeep(key: string, elem: Element | null = document.body): Element | null {
    return queryI18nElementDeep(elem, key);
}

type AssertI18nKeyDeepParams = {
    key: string;
    params?: Record<string, any>;
    visible?: boolean;
};

class AssertI18nKeyDeepError extends Error {}

/**
 * ВАЖНО: если element = <I18n id="foo:bar" />, других I18n внутри нет, key = "foo:bazz",
 * то assert отработает без ошибок - это нужно иметь в виду!!!
 */
export function assertI18nKeyDeep(
    expectedKeyOrParams: string | AssertI18nKeyDeepParams,
    element: Element | null = document.body,
) {
    const assertParams: AssertI18nKeyDeepParams =
        typeof expectedKeyOrParams === 'string' ? {key: expectedKeyOrParams} : expectedKeyOrParams;
    const {key, params, visible} = assertParams;

    const i18nContainer = queryByI18nKeyDeep(key, element);
    const checkDoesNotExist = visible === false;
    const checkParams = params && !checkDoesNotExist;
    const stepName = [
        checkDoesNotExist ? 'Отсутствие' : 'Наличие',
        `ключа ${key}`,
        checkParams ? `с параметрами ${JSON.stringify(params)}` : null,
    ]
        .filter(Boolean)
        .join(' ');

    return step(stepName, () => {
        if (checkDoesNotExist) {
            expect(i18nContainer).not.toBeInTheDocument();
        } else {
            try {
                expect(i18nContainer).toBeInTheDocument();
            } catch (e) {
                // Такая ошибка лучше читается, чем
                // "received value must be an HTMLElement or an SVGElement. / Received has value: null"
                throw new AssertI18nKeyDeepError(`I18n key "${key}" not found!`);
            }
        }

        if (checkParams) {
            assertI18nParams(params, i18nContainer!);
        }
    });
}

export function assertI18nToBeContained(expected: string, element: Element) {
    return step(`Наличие ключа - ${expected}`, () => {
        expect(element).toHaveTextContent(expected);
    });
}

export function getI18nDataDeep(element: Element | null): {key: string | null; params: Record<string, unknown>} {
    return {
        key: getI18nKeyDeep(element),
        params: getI18nParamsDeep(element),
    };
}

export function getI18nKeyDeep(element: Element | null): null | string {
    if (!element) return null;

    const textEl = queryI18nElementDeep(element);
    if (!textEl) return null;

    return textEl.getAttribute('data-e2e-i18n-key');
}

const matchI18nParams = (element: Element): Record<string, unknown> | null => {
    const matched = element.innerHTML.match(/\|({.+})$/);

    return matched && matched[1] ? JSON.parse(matched[1]) : null;
};

export function getI18nParamsDeep(element: Element | null): Record<string, unknown> {
    if (!element) return {};

    const rootElParams = matchI18nParams(element);
    if (rootElParams) return rootElParams;

    const textEl = queryI18nElementDeep(element);
    if (!textEl) return {};

    return matchI18nParams(textEl) ?? {};
}

export function assertI18nParams(expected: Record<string, unknown>, element: Element): void {
    const parsedParams = getI18nParamsDeep(element);
    expect(parsedParams).toEqual(expected);
}

export function assertLink(expectedKey: string | null, expectedUrl: string, element: Element | null): void {
    // есть случаи, когда нужно проверить ссылку без танкерного ключа
    if (expectedKey) {
        assertI18nKeyDeep(expectedKey, element);
    }

    step(`Верная ссылка - ${expectedUrl}`, () => {
        expect(element).toHaveAttribute('href', expectedUrl);
    });
}

export function queryBySelectorAll<T extends Element = Element>(
    query: string,
    container: Element = document.body,
): T[] {
    return Array.from(container.querySelectorAll(query)) as T[];
}

export function getElementsTextsBySelector(query: string, container: Element = document.body): Array<string | null> {
    return (Array.from(container.querySelectorAll(query)) as Element[]).map(item => item.textContent);
}

const PRETTY_DOM_HTML_MAX_LENGTH = 1000000;

export function printHTML(target?: Element | null) {
    if (!target) {
        // eslint-disable-next-line no-console
        console.log('[printHTML] target element is null or undefined');
        return;
    }

    const domString = prettyDOM(target, PRETTY_DOM_HTML_MAX_LENGTH);
    if (!domString) {
        // eslint-disable-next-line no-console
        console.log('[printHTML] no DOM string found');
        return;
    }

    // eslint-disable-next-line no-console
    console.log(domString);
}

export async function openHTML(target?: Element) {
    const hrTime = process.hrtime();
    // в наносекундах для точности
    const timeStamp = hrTime[0] * 1000000000 + hrTime[1];
    const outputFilename = path.join(os.tmpdir(), `${currentTestData.id}-${timeStamp}.html`);
    const domString = target
        ? prettyDOM(target, PRETTY_DOM_HTML_MAX_LENGTH, {highlight: false})
        : window.document.body.parentElement?.outerHTML;
    fs.writeFileSync(outputFilename, domString || 'no DOM string');
    return open(outputFilename);
}

export function queryBySelector<T extends Element = Element>(
    query: string,
    container: Element | null = document.body,
): T | null {
    // Типизация для случаев cat.queryBySelector(selector, cat.queryBySelector(rootSelector));
    if (container === null) {
        throw new Error('container is null!');
    }
    return container.querySelector(query) as T;
}

export function getBySelector<T extends Element = Element>(query: string, container: Element = document.body): T {
    const el = container.querySelector(query);

    if (!el) {
        throw new Error(`Unable to find an element by: ${query}`);
    }

    return el as T;
}

export function assertCssRule(rule: string, value: string, element: HTMLElement): void {
    expect(element.style.getPropertyValue(rule)).toEqual(value);
}

export function joinSelectors(
    selector: string,
    {parentSelector, additionalSelector}: {parentSelector?: string; additionalSelector?: string},
): string {
    const finalRootSelector = additionalSelector ? `${selector}${additionalSelector}` : selector;

    return parentSelector ? `${parentSelector} ${finalRootSelector}` : finalRootSelector;
}

const TEST_METRIKA_GOAL_ACTION = '_TEST_METRIKA_GOAL_';

type WaitForGoalParams = {
    goal: {type: string; payload?: any};
    task: () => any;
    timeout?: number;
    commonActionType?: string;
};

export async function waitForGoalReached<S extends {getActions: any}>(
    store: S,
    {goal, task, timeout = 1000, commonActionType}: WaitForGoalParams,
): Promise<void> {
    const INTERVAL = 100;
    let retryTime = timeout;
    let reachedGoal: WaitForGoalParams['goal'];

    const {type, payload} = goal;

    await step(`Совершаем действие, которое должно породить достижение цели ${type}`, async () => {
        await task();
    });

    return new Promise(resolve => {
        const interval = setInterval(() => {
            if (retryTime <= 0 && !reachedGoal) {
                clearInterval(interval);

                throw new Error(`Цель ${type} не была достигнута за ${timeout} мс`);
            }

            reachedGoal = store
                .getActions()
                .find(
                    (action: any) =>
                        (action.type !== commonActionType && action.type === type) ||
                        (action.type === TEST_METRIKA_GOAL_ACTION && action.payload[0] === type) ||
                        (action.type === commonActionType &&
                            action.payload.goal === payload?.goal &&
                            action.payload.params?.action === payload?.params?.action),
                );

            // FIXME вызывать step с await MARKETPARTNER-36922
            if (reachedGoal) {
                if (payload) {
                    step(`Проверяем соответствие параметров цели ${type} ожидаемым`, () => {
                        if (reachedGoal.type === type) {
                            expect(reachedGoal.payload).toEqual(payload);
                        } else {
                            expect(reachedGoal.payload[1]).toEqual(payload);
                        }
                    });
                } else {
                    step(`Цель ${type} была достигнута`, () => {
                        expect(true).toBeTruthy();
                    });
                }

                clearInterval(interval);

                resolve();
            }

            retryTime -= INTERVAL;
        }, INTERVAL);
    });
}

export function extendPo<T extends {rootSelector: string}, N extends Record<string, any>>(
    po: T,
    methodsCreator: (originalPo: T) => N,
): ShallowMerge<T, N> {
    return Object.create(po, Object.getOwnPropertyDescriptors(methodsCreator(po)));
}

type testMetrikaMiddleware<State = any, MetrikaHandler = any> = (config: {
    [key: string]: MetrikaHandler;
}) => (store: State) => (next: Dispatch<any>) => (action: any) => any;

export const testMetrikaMiddleware: testMetrikaMiddleware = config => store => {
    return next => action => {
        if (config[action.type]) {
            next({
                type: TEST_METRIKA_GOAL_ACTION,
                payload: config[action.type](store.getState(), action),
            });
        }
        return next(action);
    };
};

let isInDescribe = false;
let describeParentsNames: string[] = [];

function typedDescribe<Scope extends DescribeNameScope>(name: DescribeName<Scope>, fn: () => void): void {
    let r;

    // Returning a Promise from "describe" is not supported.
    // Tests must be defined synchronously.
    // Returning a value from "describe" will fail the test in a future version of Jest.
    const checkSync = (checked: () => unknown) => () => {
        const result = checked();

        if (result) {
            it('cat.describe body must not return value', () => {
                expect(result).toBeUndefined();
            });
        }
    };

    if (isInDescribe) {
        r = describe(name, checkSync(fn));
    } else {
        isInDescribe = true;
        r = describe(name, checkSync(fn));
        isInDescribe = false;
        describeParentsNames = [];
    }

    return r;
}

export function group(name: string, fn: () => void): void {
    if (isInDescribe) {
        describeParentsNames.push(name);
        fn();
        describeParentsNames.pop();
    } else {
        throw new Error('Группа должна быть вложена в describe');
    }
}

export type TestParams = {
    name: string;
    id: string;
    isMobile?: boolean;
    skip?: boolean;
};

export function test({name, id, isMobile, skip}: TestParams, fn: () => any): void {
    const testIt = skip ? it.skip : it;
    const mobileNamePostfix = isMobile ? ' (mobile)' : '';
    const parentsDescribePrefix = describeParentsNames.length ? `${describeParentsNames.join(' ')} ` : '';
    const testName = `[${id}] ${parentsDescribePrefix}${name}${mobileNamePostfix}`;

    testIt(testName, async () => {
        if (global.reporter) {
            global.reporter.testId(getTestManagerPattern(id));
        }

        currentTestData.id = id;
        currentTestData.name = testName;

        await fn();
    });
}

export {typedDescribe as describe};
