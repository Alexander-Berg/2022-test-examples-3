import path from 'path';
import {fromPairs, toPairs, isEmpty, complement} from 'ramda';

import glob from 'glob';

const nonEmpty = complement(isEmpty);

const mockShop = {
    campaignId: 1000573719,
    shopId: 10270808,
    contacts: {
        owner: 'user',
        admin: 'admin',
        developer: 'developer',
        operator: 'operator',
    },
};

const mockUser = {
    uid: 862832600,
    login: 'autotests-dropship-02',
    password: 'yandexteam',
};

const mockBusiness = {
    businessId: 123495,
    contacts: {
        owner: 'user',
        admin: 'admin',
    },
};

jest.doMock('spec/utils', () => ({
    makeShotCase: <T>(x: T): T => x,
    makeShotSuite: <T>(x: T): T => x,
    makeKadavrCase: <T>(x: T): T => x,
    makeKadavrSuite: <T>(x: T): T => x,
    makePO: jest.fn(() => ({
        xpathRow: () => '',
        listOutletItemNth: () => '',
        row: () => '',
        businessCardDetailsByNth: () => '',
    })),
    resolve: () => jest.fn(() => ({})),
    select: Object.assign(jest.fn(), {xpath: jest.fn()}),
    getTestingShop: jest.fn(() => mockShop),
    getProductionShop: jest.fn(() => mockShop),
    getTestingSupplier: jest.fn(() => mockShop),
    getTestingYellowPartner: jest.fn(() => mockShop),
    getTestingBusiness: jest.fn(() => mockBusiness),
    getUser: jest.fn(() => mockUser),
    byI18nKey: jest.fn(() => ''),
}));

jest.doMock('ginny', () => ({
    PageObject: jest.fn(),
    mergeSuites: jest.fn((...args) => Object.assign({}, ...args)),
}));
jest.doMock('ginny-helpers', () => ({
    describe: jest.fn(),
    it: jest.fn(),
    makePO: jest.fn(() => ({xpathRow: () => ''})),
    resolve: () => jest.fn(() => ({})),
}));

jest.doMock('reselector', () => ({
    resolve: jest.fn(() => ({})),
    resolveBy: () => jest.fn(() => ({})),
    select: Object.assign(jest.fn(), {xpath: jest.fn()}),
}));

jest.doMock(path.resolve('./lib/app/resource'), () => jest.fn(), {virtual: true});
jest.doMock(path.resolve('./lib/spec/utils/openPage'), () => jest.fn(), {virtual: true});

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const isInternalMember = key => ['beforeEach', 'afterEach', 'before', 'after'].includes(key);

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const excludeInternalMembers = obj =>
    fromPairs(
        // @ts-expect-error(TS2769) найдено в рамках MARKETPARTNER-16237
        toPairs(obj)
            .filter(([key]) => !isInternalMember(key))
            .map(([key, val]) =>
                Object.prototype.toString.call(val) === '[object Object]'
                    ? [key, excludeInternalMembers(val)]
                    : [key, val],
            )
            .filter(nonEmpty),
    );

const pathToPages = 'client.next/pages/**/spec/e2e/shots/index.js';

// Собираем страничные сьюты в один мега-объект для каждого файла
const geminiFiles = glob.sync(pathToPages);
const geminiFileSuites = geminiFiles.map(file => ({
    file,
    suite: excludeInternalMembers({story: require(file)}), // eslint-disable-line global-require
}));

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const getSuiteFullName = suite => (suite.id ? `${suite.suiteName}: ${suite.id}` : suite.suiteName);

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const checkSuiteChildren = suite => {
    // @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
    const children = (suite.childSuites || []).map(child => getSuiteFullName(child));
    // @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
    const duplicates = children.filter((name, index) => children.indexOf(name) !== index);

    expect(duplicates).toHaveLength(0);
};

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const getSuiteName = suite => {
    checkSuiteChildren(suite);
    const fullName = getSuiteFullName(suite);
    if (!suite.childSuites) return [fullName];

    const result = [];

    for (let i = 0; i < suite.childSuites.length; i++) {
        // @ts-expect-error(TS7022) найдено в рамках MARKETPARTNER-16237
        const subpaths = getSuiteName(suite.childSuites[i]);
        for (let j = 0; j < subpaths.length; j++) {
            result.push([fullName, subpaths[j]]);
        }
    }
    return result;
};

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const flatten = (arr, result = []) => {
    for (let i = 0, length = arr.length; i < length; i++) {
        const value = arr[i];
        if (Array.isArray(value)) {
            flatten(value, result);
        } else {
            // @ts-expect-error(TS2345) найдено в рамках MARKETPARTNER-16237
            result.push(value);
        }
    }

    return result;
};

/*
 *   Получаем для каждого сьюта цепочку названий такого вида:
 *   'Product Upload > Catalog uploaded > Upload Description: marketmbi-3204'
 *   'Product Upload > Catalog uploaded > Upload button: marketmbi-3204'
 *   (полный путь - поскольку дубликатов не должно быть только внутри одного уровня)
 */

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const getSuiteNameChains = suites => {
    const data = suites;
    // @ts-expect-error(TS7034) найдено в рамках MARKETPARTNER-16237
    const names = [];

    // @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
    data.map(x => x.suite.story).forEach(suite => {
        // в процессе проверяем дубликаты имен дочерних сьютов
        // на каждом уровне дерева
        checkSuiteChildren(suite);

        const suiteNames = getSuiteName(suite);
        // @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
        names.push(suiteNames.map(x => flatten(x)).map(x => x.join(' > ')));
    });

    // @ts-expect-error(TS7005) найдено в рамках MARKETPARTNER-16237
    return flatten(names);
};

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const checkForDuplicateNames = suites => {
    const suiteNameChains = getSuiteNameChains(suites);

    const duplicates = suiteNameChains.filter((name, index) => suiteNameChains.indexOf(name) !== index);

    expect(duplicates).toHaveLength(0);
};

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const containsUnicode = string => /[^\u0000-\u00ff]/.test(string); // eslint-disable-line no-control-regex

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const checkForUnicodeSuiteNames = suites => {
    const suiteNameChains = getSuiteNameChains(suites);

    expect(suiteNameChains.filter(suiteName => containsUnicode(suiteName))).toHaveLength(0);
};

describe('Валидация параметров в gemini-тестах', () => {
    it('Проверяем на отсутствие дубликатов suiteNames', () => checkForDuplicateNames(geminiFileSuites));
    it('Проверяем на отсутствие юникода в названиях сьютов', () => checkForUnicodeSuiteNames(geminiFileSuites));
});
