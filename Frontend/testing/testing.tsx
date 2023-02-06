import { boundMethod } from 'autobind-decorator';
import React, { ChangeEvent, Component } from 'react';

import { IContestSettings } from 'common/types/contestSettings';

import Fieldset from 'client/components/fieldset';
import Field from 'client/components/field';
import Hint from 'client/components/hint';
import RadioButton from 'client/components/radio-button';
import Tumbler from 'client/components/tumbler';
import { OnChangeFunction } from 'client/decorators/with-form/types';
import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

interface Props {
    isDisabled?: boolean;
    reportSettings?: IContestSettings['reportSettings'];
    onChange: OnChangeFunction<boolean>;
}

const fieldset = block('fieldset');

class Testing extends Component<Props> {
    public shouldComponentUpdate(nextProps: Props) {
        const { reportSettings = {} } = this.props;
        const { reportSettings: nextReportSettings = {} } = nextProps;
        const {
            stopOnSampleFail,
            useAcNotOk,
            stopOnFirstFail,
            stopOnFirstFailInTestSet,
        } = reportSettings;
        const {
            stopOnSampleFail: nextStopOnSampleFail,
            useAcNotOk: nextUseAcNotOk,
            stopOnFirstFail: nextStopOnFirstFail,
            stopOnFirstFailInTestSet: nextStopOnFirstFailInTestSet,
        } = nextReportSettings;

        return (
            stopOnSampleFail !== nextStopOnSampleFail ||
            useAcNotOk !== nextUseAcNotOk ||
            stopOnFirstFail !== nextStopOnFirstFail ||
            stopOnFirstFailInTestSet !== nextStopOnFirstFailInTestSet
        );
    }

    public render() {
        const { isDisabled, reportSettings = {} } = this.props;
        const {
            stopOnSampleFail,
            useAcNotOk,
            stopOnFirstFail,
            stopOnFirstFailInTestSet,
        } = reportSettings;
        const radioValue = stopOnSampleFail ? '1' : '';

        return (
            <Fieldset
                isDisabled={isDisabled}
                legend={i18n.text({ keyset: 'contest-settings', key: 'testing' })}
                legendSize="s"
                name="testing"
            >
                <Field name="useAcNotOk">
                    <label className={fieldset('label')}>
                        <div className={fieldset('hint-wrapper')}>
                            {i18n.text({
                                keyset: 'contest-settings',
                                key: 'testing__use-ac-not-ok',
                            })}
                            <Hint
                                className={fieldset('hint')}
                                text={i18n.text({
                                    keyset: 'contest-settings-tips',
                                    key: 'use-ac-instead-ok',
                                })}
                            />
                        </div>
                    </label>
                    <Tumbler
                        view="default"
                        size="l"
                        onChange={this.onAcNotOkChange}
                        checked={Boolean(useAcNotOk)}
                    />
                </Field>
                <Field name="stopOnFirstFail">
                    <label className={fieldset('label')}>
                        {i18n.text({
                            keyset: 'contest-settings',
                            key: 'testing__stop-on-first-fail',
                        })}
                    </label>
                    <Tumbler
                        view="default"
                        size="l"
                        onChange={this.onStopOnFirstFailChange}
                        checked={Boolean(stopOnFirstFail)}
                    />
                </Field>
                <Field name="stopOnFirstFailInTestSet">
                    <label className={fieldset('label')}>
                        <div className={fieldset('hint-wrapper')}>
                            {i18n.text({
                                keyset: 'contest-settings',
                                key: 'testing__stop-on-first-fail-in-test-set',
                            })}
                            <Hint
                                className={fieldset('hint')}
                                text={i18n.text({
                                    keyset: 'contest-settings-tips',
                                    key: 'stop-on-first-fail-in-testset',
                                })}
                            />
                        </div>
                    </label>
                    <Tumbler
                        view="default"
                        size="l"
                        onChange={this.onStopOnFirstFailInTestSetChange}
                        checked={Boolean(stopOnFirstFailInTestSet)}
                    />
                </Field>
                <Field name="stopOnSampleFail">
                    <label className={fieldset('label')}>
                        <div className={fieldset('hint-wrapper')}>
                            {i18n.text({
                                keyset: 'contest-settings',
                                key: 'testing__on-failed-tests',
                            })}
                            <Hint
                                className={fieldset('hint')}
                                text={i18n.text({
                                    keyset: 'contest-settings-tips',
                                    key: 'failed-tests-of-the-conditions',
                                })}
                            />
                        </div>
                    </label>
                    <RadioButton
                        size="m"
                        view="default"
                        onChange={this.onFailedTestsChange}
                        value={radioValue}
                        options={[
                            {
                                value: '',
                                children: i18n.text({
                                    keyset: 'contest-settings',
                                    key: 'testing__continue',
                                }),
                            },
                            {
                                value: '1',
                                children: i18n.text({
                                    keyset: 'contest-settings',
                                    key: 'testing__stop',
                                }),
                            },
                        ]}
                    />
                </Field>
            </Fieldset>
        );
    }

    @boundMethod
    private onAcNotOkChange() {
        const { reportSettings = {}, onChange } = this.props;

        onChange('reportSettings.useAcNotOk')(!reportSettings.useAcNotOk);
    }

    @boundMethod
    private onStopOnFirstFailChange() {
        const { reportSettings = {}, onChange } = this.props;

        onChange('reportSettings.stopOnFirstFail')(!reportSettings.stopOnFirstFail);
    }

    @boundMethod
    private onStopOnFirstFailInTestSetChange() {
        const { reportSettings = {}, onChange } = this.props;

        onChange('reportSettings.stopOnFirstFailInTestSet')(
            !reportSettings.stopOnFirstFailInTestSet,
        );
    }

    @boundMethod
    private onFailedTestsChange(event: ChangeEvent<HTMLInputElement>) {
        const { onChange } = this.props;
        const { value } = event.target;

        onChange('reportSettings.stopOnSampleFail')(Boolean(value));
    }
}

export default Testing;
