export type StepStatus = 'failed' | 'passed';

export type StepData = {
    name: string;
    status?: StepStatus;
    steps: StepData[];
};

export default class Step {
    name: string;

    status?: StepStatus;

    steps: Step[] = [];

    constructor(name: string) {
        this.name = name;
    }

    getData(): StepData {
        return {
            name: this.name,
            status: this.status,
            steps: this.steps.map(step => step.getData()),
        };
    }
}
