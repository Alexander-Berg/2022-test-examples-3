import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export class TestTrainsOrderSteps extends Component {
    searchStep: Component;
    orderSteps: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'orderSteps') {
        super(browser, qa);

        this.searchStep = new Component(browser, {
            parent: this.qa,
            current: 'search',
            key: 'oneWayForward',
        });

        this.orderSteps = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'orderStep',
            },
            Component,
        );
    }

    async getActiveStepText(): Promise<string | undefined> {
        const activeStep = await this.orderSteps.find(async step => {
            return (await step.getAttribute('data-active')) === 'true';
        });

        return activeStep?.getText();
    }

    async getDisabledStepsTexts(): Promise<string[]> {
        return this.orderSteps.reduce(async (accSteps, step) => {
            const stepIsDisabled =
                (await step.getAttribute('disabled')) &&
                (await step.getAttribute('data-active')) !== 'true';

            if (stepIsDisabled) {
                accSteps.push(await step.getText());
            }

            return accSteps;
        }, [] as string[]);
    }

    async getStepByText(text: string): Promise<Component | undefined> {
        return this.orderSteps.find(async step => {
            return (await step.getText()) === text;
        });
    }
}
