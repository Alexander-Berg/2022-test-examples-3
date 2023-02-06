type TSortOrder = 'ASC' | 'DESC';
type TExpressionType = 'EQ' | 'IN';
type TExpressionJoinType = 'AND' | 'OR';

export interface ITestPalmExpression {
    type: TExpressionType;
    key: string;
    value: string | string[];
}

export interface ITestPalmExpressionJoin {
    type: TExpressionJoinType;
    left: TTestPalmFilterExpression;
    right: TTestPalmFilterExpression;
}

export type TTestPalmFilterExpression =
    | ITestPalmExpression
    | ITestPalmExpressionJoin;

export interface ITestSuite {
    tags: string[];
    locked: boolean;
    groups: [];
    ignoreSuiteOrder: boolean;
    excluded: [];
    title: string;
    description: string;
    descriptionFormatted: string;
    createdTime: number;
    lastModifiedTime: number;
    modifiedBy: string;
    createdBy: string;
    filter: {
        sorting: {id: TSortOrder};
        excludedFields: string[];
        includedFields: string[];
        skip: number;
        limit: number;
        expression: TTestPalmFilterExpression;
    };
    properties: [];
    orders: [];
    id: string;
}

export interface ITestCase {
    removed: boolean;
    descriptionFormatted: string;
    automationFormatted: string;
    preconditionsFormatted: string;
    isAutotest: boolean;
    flags: [];
    attributes: Record<string, string[]>;
    meta: object;
    description: string;
    preconditions: string;
    createdBy: string;
    createdTime: number;
    modifiedBy: string;
    stepsExpects: ITestCaseStep[];
    bugs: {
        trackerId: string;
        description: string;
        isResolved: boolean;
        isBug: boolean;
        assignee: [];
        priority: string;
        idDisplay: string;
        foundInTestRun: string;
        type: object[];
        status: object[];
        createdTime: number;
        title: string;
        url: string;
        id: string;
    }[];
    tasks: [];
    automationTasks: [];
    stats: {
        commentsCount: number;
        linksCount: number;
        latestRunTime: number;
        latestRunDuration: number;
        avgRunDuration: number;
        maxRunDuration: number;
        minRunDuration: number;
        totalRunCount: number;
        _id: string;
        latestRunStatus: string;
    };
    status: string;
    properties: [];
    estimatedTime: number;
    name: string;
    lastModifiedTime: number;
    attachments: [];
    id: number;
}

export interface ITestCaseStep {
    step: string;
    stepFormatted: string;
    expect: string;
    expectFormatted: string;
}

export interface ITestDefinition {
    removed: boolean;
    hidden: boolean;
    values: string[];
    order: number;
    title: string;
    restricted: boolean;
    createdTime: number;
    lastModifiedTime: number;
    description: string;
    id: string;
}

export type TPreparedTestCaseStep = {do: string} | {assert: string};
