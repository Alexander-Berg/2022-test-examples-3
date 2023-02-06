import EnvironmentJsdom from 'jest-environment-jsdom';
import {Circus, Config} from '@jest/types';
import {EnvironmentContext} from '@jest/environment';

import ReportGenerator from '../reporter/generator';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const {init: initResolver} = require('../resolver');

class TestamentEnvironment extends EnvironmentJsdom {
    reportGenerator: ReportGenerator;

    config: Config.ProjectConfig;

    constructor(config: Config.ProjectConfig, options: EnvironmentContext) {
        super(config);

        this.config = config;
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        this.reportGenerator = new ReportGenerator(options.testPath!);
    }

    async setup(): Promise<void> {
        await super.setup();
        await initResolver();
        this.global.step = this.reportGenerator.startStep.bind(
            this.reportGenerator,
        );
        this.global.jestEnvironmentOptions = this.config.testEnvironmentOptions;
    }

    async teardown(): Promise<void> {
        await super.teardown();

        this.reportGenerator.writeResultData();
    }

    // eslint-disable-next-line class-methods-use-this
    handleTestEvent(event: Circus.AsyncEvent): void {
        switch (event.name) {
            case 'hook_start': {
                const {type} = event.hook;
                if (type === 'afterAll' || type === 'beforeAll') {
                    this.reportGenerator.startHook(type);
                }
                break;
            }

            case 'hook_failure':
            case 'hook_success': {
                const {type} = event.hook;
                if (type === 'afterAll' || type === 'beforeAll') {
                    this.reportGenerator.endHook();
                }
                break;
            }

            case 'run_describe_start': {
                if (event.describeBlock.parent) {
                    this.reportGenerator.startGroup(event.describeBlock.name);
                }
                break;
            }

            case 'run_describe_finish': {
                if (event.describeBlock.parent) {
                    this.reportGenerator.endGroup();
                }
                break;
            }

            case 'test_start': {
                this.reportGenerator.startTest(event.test.name);
                break;
            }

            case 'test_skip':
            case 'test_todo':
            case 'test_done': {
                this.reportGenerator.endTest();
                break;
            }

            default:
                break;
        }
    }
}

export default TestamentEnvironment;
