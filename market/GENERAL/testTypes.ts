import type {TEST_STATUS} from '../constants';

type Values<T extends Record<PropertyKey, unknown>> = T[keyof T];

export type TestStatus = Values<typeof TEST_STATUS>;

/** Параметры теста */
export type TestParams = {
    disabled?: boolean;
    name: string;
    login?: string;
    password?: string;
    relativeUrl?: string;
    errorCodeRegexp?: string;
    expectedText?: string;
    defaultContentType?: string;
};

type TestResultCommon = {
    retries?: number;
};

/** Успешный результат теста */
export type TestResultSuccess = TestResultCommon & {
    status: typeof TEST_STATUS.SUCCESS;
    params: TestParams;
};
/** Скипнутый результат теста */
export type TestResultSkipped = TestResultCommon & {
    status: typeof TEST_STATUS.SKIPPED;
    params: TestParams;
};
/** Провальный результат теста */
export type TestResultFailed = TestResultCommon & {
    status: typeof TEST_STATUS.FAILED;
    params: TestParams;
    error?: unknown;
    reason: string;
};
/** Результат теста */
export type TestResult = TestResultSuccess | TestResultSkipped | TestResultFailed;

type TypeByStatus = {
    [TEST_STATUS.FAILED]: {
        result: TestResultFailed;
    };
    [TEST_STATUS.SUCCESS]: {
        result: TestResultSuccess;
    };
    [TEST_STATUS.SKIPPED]: {
        result: TestResultSkipped;
    };
};

/** Результаты тестов разбитые на группы в зависимости от результата */
export type TestReportGroups = {
    [K in TestStatus]: {
        count: number;
        results: Array<TypeByStatus[K]['result']>;
    };
};

/** Финальный отчет */
export type FinalReport = {
    total: number;
    groups: TestReportGroups;
};

/** Запуск теста */
export type RunTest = (params: TestParams) => Promise<TestResult>;
