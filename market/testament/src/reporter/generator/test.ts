import Step, {StepData} from './step';

export type TestData = {
    name: string;
    steps: StepData[];
};

export default class Test {
    name: string;

    steps: Step[] = [];

    constructor(name: string) {
        this.name = name;
    }

    getData(): TestData {
        return {
            name: this.name,
            steps: this.steps.map(step => step.getData()),
        };
    }
}
