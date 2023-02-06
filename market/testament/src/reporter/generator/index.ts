import fs from 'fs';
import path from 'path';
import os from 'os';
import crypto from 'crypto';

import {isPromise} from 'allure2-js-commons';

import Group from './group';
import Hook, {HookType} from './hook';
import Test from './test';
import Step from './step';
import Root, {RootData} from './root';

const NO_ACTIVE_TEST_MESSAGE = 'No active test';
const NO_ACTIVE_SUITE_MESSAGE = 'No active suite';
const NO_ACTIVE_HOOK_MESSAGE = 'No active hook';
const NO_ACTIVE_STEP_MESSAGE = 'No active step';

export type {RootData} from './root';
export type {GroupData} from './group';
export type {StepData} from './step';
export type {HookType, HookData} from './hook';
export type {TestData} from './test';

export default class ReportGenerator {
    private testFilePath: string;

    private root: Root;

    private groupStack: Group[] = [new Group('root')];

    private stepStack: Step[] = [];

    private currentHook: Hook | null = null;

    private currentTest: Test | null = null;

    constructor(testFilePath: string) {
        this.testFilePath = testFilePath;
        this.root = new Root(testFilePath);
        // this.root.groups.push(this.groupStack[0]);
    }

    reset(): void {
        this.root = new Root(this.testFilePath);
        this.groupStack = [];
        this.stepStack = [];
        this.currentHook = null;
        this.currentTest = null;
    }

    getData(): RootData {
        return this.root.getData();
    }

    getCurrentGroup(): Group | null {
        if (this.groupStack.length === 0) {
            return null;
        }

        return this.groupStack[this.groupStack.length - 1];
    }

    getCurrentStep(): Step | null {
        if (this.stepStack.length === 0) {
            return null;
        }

        return this.stepStack[this.stepStack.length - 1];
    }

    startGroup(name: string): void {
        const currentGroup = this.getCurrentGroup();
        const group = new Group(name);

        if (currentGroup) {
            currentGroup.groups.push(group);
        } else {
            this.root.groups.push(group);
        }

        this.groupStack.push(group);
    }

    endGroup(): void {
        const currentGroup = this.getCurrentGroup();

        if (!currentGroup) {
            throw new Error(NO_ACTIVE_SUITE_MESSAGE);
        }

        this.groupStack.pop();
    }

    startHook(type: HookType): void {
        const currentGroup = this.getCurrentGroup();

        if (!currentGroup) {
            throw new Error(NO_ACTIVE_SUITE_MESSAGE);
        }

        const hook = new Hook(type);

        if (type === 'afterAll') {
            currentGroup.afterAll.push(hook);
        } else {
            currentGroup.beforeAll.push(hook);
        }

        this.currentHook = hook;
    }

    endHook(): void {
        if (!this.currentHook) {
            throw new Error(NO_ACTIVE_HOOK_MESSAGE);
        }

        this.currentHook = null;
    }

    startTest(name: string): void {
        const currentGroup = this.getCurrentGroup();

        if (!currentGroup) {
            throw new Error(NO_ACTIVE_SUITE_MESSAGE);
        }

        const test = new Test(name);
        currentGroup.tests.push(test);
        this.currentTest = test;
    }

    endTest(): void {
        if (!this.currentTest) {
            throw new Error(NO_ACTIVE_TEST_MESSAGE);
        }

        this.currentTest = null;
    }

    startStep(name: string, body: () => any): any {
        const currentGroup = this.getCurrentGroup();

        if (!currentGroup) {
            throw new Error(NO_ACTIVE_SUITE_MESSAGE);
        }

        const currentStep = this.getCurrentStep();
        const step = new Step(name);

        if (currentStep) {
            currentStep.steps.push(step);
        } else if (this.currentHook) {
            this.currentHook.steps.push(step);
        } else if (this.currentTest) {
            this.currentTest.steps.push(step);
        } else {
            throw new Error(NO_ACTIVE_TEST_MESSAGE);
        }

        this.stepStack.push(step);

        let result;

        try {
            result = body();
        } catch (err) {
            step.status = 'failed';
            this.endStep();
            throw err;
        }

        if (isPromise(result)) {
            result
                .then((r: any) => {
                    step.status = 'passed';
                    this.endStep();
                    return r;
                })
                .catch((err: Error) => {
                    step.status = 'failed';
                    this.endStep();
                    throw err;
                });
        } else {
            step.status = 'passed';
            this.endStep();
        }

        return result;
    }

    endStep(): void {
        const currentStep = this.getCurrentStep();

        if (!currentStep) {
            throw new Error(NO_ACTIVE_STEP_MESSAGE);
        }

        this.stepStack.pop();
    }

    static getDataFilePath(testFilePath: string): string {
        const savePath = path.join(os.tmpdir(), 'testament-report-data');
        const fileNameHash = crypto
            .createHash('md5')
            .update(testFilePath)
            .digest('hex');

        if (!fs.existsSync(savePath)) {
            fs.mkdirSync(savePath, {
                recursive: true,
            });
        }

        return path.join(savePath, `${fileNameHash}.json`);
    }

    writeResultData(): void {
        const data = JSON.stringify(this.root.getData());
        fs.writeFileSync(
            ReportGenerator.getDataFilePath(this.root.testFilePath),
            data,
        );
    }

    static readResultData(testFilePath: string): RootData | null {
        try {
            const data = fs.readFileSync(
                ReportGenerator.getDataFilePath(testFilePath),
                'utf8',
            );

            return JSON.parse(data);
        } catch (e) {
            return null;
        }
    }
}

const reportMap: Map<string, ReportGenerator> = new Map();

export function get(testPath: string): ReportGenerator {
    let generator = reportMap.get(testPath);

    if (!generator) {
        generator = new ReportGenerator(testPath);
        reportMap.set(testPath, generator);
    }

    return generator;
}
