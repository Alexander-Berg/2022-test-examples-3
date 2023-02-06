import Step, {StepData} from './step';

export type HookType = 'afterAll' | 'beforeAll';

export type HookData = {
    type: HookType;
    steps: StepData[];
};

export default class Test {
    type: HookType;

    steps: Step[] = [];

    constructor(type: HookType) {
        this.type = type;
    }

    getData(): HookData {
        return {
            type: this.type,
            steps: this.steps.map(step => step.getData()),
        };
    }
}
