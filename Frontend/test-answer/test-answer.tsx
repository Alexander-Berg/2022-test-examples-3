import { boundMethod } from 'autobind-decorator';
import React, { ChangeEvent, Component } from 'react';

import { AnswerType } from 'common/types/problem';

import Fieldset from 'client/components/fieldset';
import Field from 'client/components/field';
import Hint from 'client/components/hint';
import { Props } from 'client/components/problem-settings/test-answer/types';
import ProblemTest, { ITestUpdate } from 'client/components/problem-test';
import { SectionNavHashItem } from 'client/components/section-nav';
import Select from 'client/components/select';
import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

import 'client/components/problem-settings/test-answer/test-answer.css';

const b = block('test-answer');
const fieldset = block('fieldset');

class TestAnswer extends Component<Props> {
    private get multi() {
        return this.props.answerType === AnswerType.CHECKBOX;
    }

    public render() {
        const { isDisabled } = this.props;

        return (
            <Fieldset className={b()} raw isDisabled={isDisabled} group>
                <legend className={fieldset('legend', { size: 'm' })}>
                    <SectionNavHashItem hash="test-answer" after="common">
                        {i18n.text({ keyset: 'problem-settings', key: 'answer-title' })}
                    </SectionNavHashItem>
                </legend>
                <div className={fieldset('controls')}>{this.renderSelect()}</div>
            </Fieldset>
        );
    }

    public renderAnswerTypeSelect() {
        const { answerType, isDisabled } = this.props;
        return (
            <Field>
                <label className={fieldset('label')}>
                    <div className={fieldset('hint-wrapper')}>
                        {i18n.text({ keyset: 'problem-settings', key: 'answer-type-label' })}
                        <Hint
                            className={fieldset('hint')}
                            text={i18n.text({
                                keyset: 'problem-settings-tips',
                                key: 'test-answer-type',
                            })}
                        />
                    </div>
                </label>
                <Select
                    theme="normal"
                    size="m"
                    onChange={this.onAnswerTypeChange}
                    value={answerType}
                    disabled={isDisabled}
                    className={b('answer-type-select')}
                    options={Object.keys(AnswerType).map((type) => ({
                        value: type,
                        content: i18n.text({
                            keyset: 'problem-settings',
                            key: `answer-type__${type}`,
                        }),
                    }))}
                />
            </Field>
        );
    }

    @boundMethod
    private onAnswerTypeChange(event: ChangeEvent<HTMLSelectElement>) {
        const answerTypeValue = event.target.value as AnswerType;

        const { answers, options, answerType: prevAnswerType } = this.props;

        const isChangeToRadioButton =
            answerTypeValue === AnswerType.RADIOBUTTON && prevAnswerType !== answerTypeValue;

        this.props.onChange('details')({
            answerType: answerTypeValue,
            answers: isChangeToRadioButton ? [] : answers,
            options,
        });
    }

    private renderSelect() {
        const { options, answers, getUniqId, isDisabled } = this.props;
        const id = getUniqId();

        return (
            <>
                {this.renderAnswerTypeSelect()}
                <Field>
                    <label className={fieldset('label')} htmlFor={id}>
                        <div className={fieldset('hint-wrapper')}>
                            {i18n.text({ keyset: 'problem-settings', key: 'variants-label' })}
                            <Hint
                                className={fieldset('hint')}
                                text={i18n.text({
                                    keyset: 'problem-settings-tips',
                                    key: 'test-answer-variants',
                                })}
                            />
                        </div>
                    </label>
                    <ProblemTest
                        multi={this.multi}
                        variants={options}
                        answer={answers}
                        disabled={isDisabled}
                        onUpdate={this.onUpdate}
                    />
                </Field>
            </>
        );
    }

    @boundMethod
    private onUpdate({ variants, answer }: ITestUpdate) {
        this.props.onChange('details')({
            answerType: this.props.answerType,
            answers: answer,
            options: variants,
        });
    }
}

export default TestAnswer;
