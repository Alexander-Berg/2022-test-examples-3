const {isatty} = require('tty');
const Allure = require('allure-js-commons');

// eslint-disable-next-line no-control-regex
const reAnsi = /[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-PRZcf-nqry=><]|[\0-\x09\x0B-\x1F]/g;
const now = Date.now.bind(Date); // mockdate :(
const isTerminal = isatty(0) && isatty(1) && isatty(2); // отладка в терминале

function stripAnsi(s) {
    return String(s).replace(reAnsi, '');
}

class KittenAllure extends Allure {
    // jest-allure/dist/Reporter
    addLabel(name, value) {
        this.getCurrentTest().addLabel(name, value);
        return this;
    }

    addParameter(name, value) {
        this.getCurrentTest().addParameter('argument', name, value);
        return this;
    }

    testId(testId) {
        this.addLabel('testId', testId);
        return this;
    }

    startStep(name, ts) {
        super.startStep(name, ts ?? now());
        if (isTerminal) this._printCurrentStep();
    }

    endStep(status, ts) {
        super.endStep(status ?? 'passed', ts ?? now());
    }

    _printCurrentStep() {
        if (this.suites.length !== 1) return; // только для первого
        const suite = this.getCurrentSuite();
        const {currentStep} = suite;
        let level = 0;
        for (let step = currentStep; step.parent; step = step.parent) ++level;
        if (level === 1 && currentStep.parent.steps.length === 1) {
            process.stderr.write(`\n\n${currentStep.parent.name}\n`);
        }
        process.stderr.write(`${' '.repeat(level)}- ${currentStep.name}\n`);
    }

    _stripAnsi = stripAnsi;

    _snapshot(err) {
        if (!this.getCurrentSuite()) return; // тест уже упал
        if (err && !('html' in err)) {
            const html = global.document?.firstElementChild?.outerHTML || '<!-- empty -->';
            Reflect.set(err, 'html', html);
            this.createAttachment('HTML snapshot', `\uFEFF<!DOCTYPE html>${html}`, 'text/html');
        }
    }

    // @yandex-market/hermione-allure-reporter/lib/RuntimeAdapter.js
    runStep(name, fn) {
        try {
            this.startStep(name, now());
            const result = fn ? fn() : null;

            if (result instanceof Promise || typeof result?.then === 'function') {
                return result.then(
                    pass => {
                        this.endStep('passed', now());
                        return pass;
                    },
                    err => {
                        if (err) this._snapshot(err);
                        this.endStep('failed', now());
                        throw err;
                    },
                );
            }

            this.endStep('passed', now());

            return result;
        } catch (e) {
            if (e) this._snapshot(e);
            this.endStep('failed', now());
            throw e;
        }
    }

    createAttachment(title, data, mimeType) {
        const promised = typeof data === 'function' ? data() : data;
        const buf = Buffer.isBuffer(promised) ? promised : Buffer.from(String(data));
        return this.addAttachment(title, buf, mimeType);
    }

    _allure = {
        endStep: status => {
            // HACK runIndependentSteps
            console.error('Deprecated endStep(%s):\n- %s', status, this.getCurrentSuite().currentStep.name);
        },
    };
}

// @see node_modules/jest-allure/src/setup.ts + real timestamps
class JasmineAllureReporter {
    constructor(allure) {
        this.allure = allure;
    }

    suiteStarted(suite) {
        if (suite) {
            this.allure.startSuite(suite.fullName, now());
        } else {
            throw Error('test without suite');
        }
    }

    jasmineDone() {
        if (this.allure.getCurrentSuite()) {
            this.allure.endSuite(now());
        }
    }

    suiteDone() {
        this.allure.endSuite(now());
    }

    specStarted(spec) {
        if (!this.allure.getCurrentSuite()) {
            this.suiteStarted();
        }
        this.allure.startCase(spec.description, now());
    }

    specDone(spec) {
        let error;
        if (spec.status === 'pending') {
            error = {message: spec.pendingReason};
        }
        if (spec.status === 'disabled') {
            error = {message: 'This test was disabled'};
        }
        const failure =
            spec.failedExpectations && spec.failedExpectations.length ? spec.failedExpectations[0] : undefined;
        if (failure) {
            error = {
                message: stripAnsi(failure.message),
                stack: stripAnsi(failure.stack),
            };
        }

        this.allure.endCase(spec.status, error, now());
    }
}

function registerAllureReporter() {
    if (global.reporter) return;
    const allure = new KittenAllure();
    global.reporter = allure;
    global.jasmine.getEnv().addReporter(new JasmineAllureReporter(allure));
}

function getSuitePath(test) {
    const result = [];

    for (let cursor = test.parent; cursor.parent; cursor = cursor.parent) {
        result.push(cursor.name);
    }

    return result;
}

// jest-circus
function handleTestEvent(event) {
    const {reporter} = this;

    switch (event.name) {
        case 'setup':
            if (!reporter) {
                this.reporter = new KittenAllure();
            }
            break;

        case 'test_start':
            if (!reporter.getCurrentSuite()) {
                const title = getSuitePath(event.test).join(' » ');
                reporter.startSuite(title, now());
            }
            reporter.startCase(event.test.name, now());
            break;

        case 'test_fn_failure':
            if (reporter.getCurrentTest()) {
                const failure = event.error;
                const error = {
                    message: stripAnsi(failure.message),
                    stack: stripAnsi(failure.stack || ''),
                };
                error.stack = String(error.stack).replace(error.message, '');
                reporter.endCase('failed', error, now());
                if (!failure?.html) {
                    const html = this.document?.firstElementChild?.outerHTML || '<!-- empty -->';
                    reporter.createAttachment('HTML snapshot', `\uFEFF<!DOCTYPE html>${html}`, 'text/html');
                }
            } else console.error('Unexpected test_fn_failure:', event.error);
            break;

        case 'test_done':
            if (reporter.getCurrentTest()) {
                if (!reporter.getCurrentTest().stop) {
                    const {errors} = event.test;
                    if (errors?.length) {
                        // Array<Exception | [Exception | undefined, Exception]>
                        const firstError = [].concat(...errors).find(Boolean);
                        reporter.endCase('broken', firstError, now());
                    } else {
                        reporter.endCase('passed', null, now());
                    }
                }
            } else console.error('Unexpected test_done', event.test);
            break;

        case 'test_skip':
        case 'test_todo':
            reporter.endCase('skipped', null, now());
            break;

        case 'run_finish':
            if (reporter.getCurrentSuite()) {
                reporter.endSuite(now());
            }
            break;

        case 'error':
            console.error('[JestEventError]', event.error);
            if (reporter && reporter.getCurrentTest()) {
                const {error} = event;
                if (error) {
                    reporter.startStep(`### ${error.message || error}`);
                    reporter.createAttachment('Unhandled error', String(error.stack), 'text/plain');
                    reporter.endStep('broken', now());
                }
            }
            break;

        default:
    }
}

module.exports = {
    KittenAllure,
    registerAllureReporter,
    handleTestEvent,
};
