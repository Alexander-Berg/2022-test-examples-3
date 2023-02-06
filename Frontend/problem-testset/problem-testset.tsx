import { boundMethod } from 'autobind-decorator';
import isUndefined from 'lodash/isUndefined';
import React, { ChangeEvent, Component, FocusEvent, ContextType } from 'react';

import Accordion from 'client/components/accordion';
import Button from 'client/components/button';
import ConfirmationModal from 'client/components/confirmation-modal';
import ControlNoAccessHint from 'client/components/control-no-access-hint';
import Fieldset from 'client/components/fieldset';
import Field from 'client/components/field';
import Icon from 'client/components/icon';
import IconControl from 'client/components/icon-control';
import Table from 'client/components/table';
import Textinput from 'client/components/textinput';

import {
    INPUT_FILE_PATTERN_NAME,
    OUTPUT_FILE_PATTERN_NAME,
} from 'client/components/problem-testsets/problem-testset/constants';
import { UserProfileContext } from 'client/contexts';
import { Props, State } from 'client/components/problem-testsets/problem-testset/types';
import { IProblemTest } from 'common/types/problem-test';

import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

import 'client/components/problem-testsets/problem-testset/problem-testset.css';

const b = block('problem-testset');
const fieldset = block('fieldset');

class ProblemTestset extends Component<Props, State> {
    public static contextType = UserProfileContext;
    public context!: ContextType<typeof UserProfileContext>;

    private static columns = [
        {
            className: b('sequence-number'),
            render: (_item: IProblemTest, itemIdx?: number) => {
                if (typeof itemIdx !== 'number') {
                    return '';
                }

                return itemIdx + 1;
            },
        },
        {
            className: b('validity'),
            render: ({ isValid }: IProblemTest) => {
                if (isUndefined(isValid)) {
                    return null;
                }

                return isValid
                    ? ProblemTestset.renderValidTestIcon()
                    : ProblemTestset.renderInvalidTestIcon();
            },
        },
        {
            title: i18n.text({ keyset: 'problem-tests', key: 'test__input' }),
            render: ({ inputPath }: IProblemTest) => inputPath,
        },
        {
            title: i18n.text({ keyset: 'problem-tests', key: 'test__output' }),
            render: ({ outputPath }: IProblemTest) => outputPath,
        },
    ];

    private static renderValidTestIcon() {
        return (
            <span title={i18n.text({ keyset: 'problem-tests', key: 'test-validity__valid' })}>
                <Icon glyph="type-check" size="s" className={b('validity-icon', { valid: true })} />
            </span>
        );
    }

    private static renderInvalidTestIcon() {
        return (
            <span title={i18n.text({ keyset: 'problem-tests', key: 'test-validity__invalid' })}>
                <Icon type="red-circle-10" size="xs" />
            </span>
        );
    }

    constructor(props: Props) {
        super(props);

        const { data } = props;

        if (isUndefined(data)) {
            return;
        }

        const { inputFilePattern, outputFilePattern } = data;
        this.state = {
            inputPattern: inputFilePattern,
            outputPattern: outputFilePattern,
        };
    }

    public render() {
        const { data, updateTests, fetchTestsStarted, updateTestsStarted, readonly } = this.props;

        if (isUndefined(data)) {
            return null;
        }

        const { name, tests } = data;
        const { inputPattern, outputPattern } = this.state;

        const isDisabled = readonly || !this.testsetTemplateModificationAllowed;

        return (
            <Accordion title={name} controls={this.controls}>
                <Fieldset className={fieldset({}, [b('fieldset')])}>
                    <Field>
                        <label className={fieldset('label')}>
                            {i18n.text({
                                keyset: 'problem-tests',
                                key: 'testsets__test__input-pattern',
                            })}
                        </label>
                        <div>
                            <ControlNoAccessHint
                                noAccess={!this.testsetTemplateModificationAllowed}
                                message={i18n.text({
                                    keyset: 'common',
                                    key: 'control-no-access-hint',
                                })}
                            >
                                <Textinput
                                    theme="normal"
                                    size="m"
                                    value={inputPattern}
                                    disabled={isDisabled}
                                    onBlur={this.handleBlurInputFilePattern}
                                    onChange={this.handleChangeInputFilePattern}
                                />
                            </ControlNoAccessHint>
                        </div>
                    </Field>
                    <Field>
                        <label className={fieldset('label')}>
                            {i18n.text({
                                keyset: 'problem-tests',
                                key: 'testsets__test__output-pattern',
                            })}
                        </label>
                        <div>
                            <ControlNoAccessHint
                                noAccess={!this.testsetTemplateModificationAllowed}
                                message={i18n.text({
                                    keyset: 'common',
                                    key: 'control-no-access-hint',
                                })}
                            >
                                <Textinput
                                    theme="normal"
                                    size="m"
                                    value={outputPattern}
                                    disabled={isDisabled}
                                    onBlur={this.handleBlurOutputFilePattern}
                                    onChange={this.handleChangeOutputFilePattern}
                                />
                            </ControlNoAccessHint>
                        </div>
                    </Field>
                </Fieldset>
                {!isDisabled && (
                    <Button
                        theme="normal"
                        size="m"
                        className={b('update-tests')}
                        onClick={updateTests}
                    >
                        {i18n.text({ keyset: 'problem-tests', key: 'testsets__update-tests' })}
                    </Button>
                )}
                <Table
                    className={b('table')}
                    columns={ProblemTestset.columns}
                    data={tests}
                    isLoading={fetchTestsStarted || updateTestsStarted}
                    hasFixedHeader
                    isLimitedByHeight
                />
            </Accordion>
        );
    }

    private get controls() {
        const { onRemove, readonly, data } = this.props;

        if (readonly) {
            return null;
        }

        return (
            <ConfirmationModal
                title={i18n.text({
                    keyset: 'common',
                    key: 'remove-this',
                    params: { name: data?.name },
                })}
                confirmText={i18n.text({ keyset: 'common', key: 'delete' })}
                onConfirm={onRemove}
            >
                {(openModal) => <IconControl type="close-16" size="m" onClick={openModal} />}
            </ConfirmationModal>
        );
    }

    @boundMethod
    private handleChangeInputFilePattern(event: ChangeEvent<HTMLInputElement>) {
        this.setState({ inputPattern: event.target.value });
    }

    @boundMethod
    private handleBlurInputFilePattern(_event: FocusEvent<HTMLElement>) {
        const { data, onChange } = this.props;

        if (isUndefined(data)) {
            return;
        }

        const { inputFilePattern } = data;
        const { inputPattern } = this.state;

        if (inputFilePattern === inputPattern) {
            return;
        }

        onChange(INPUT_FILE_PATTERN_NAME, inputPattern);
    }

    @boundMethod
    private handleChangeOutputFilePattern(event: ChangeEvent<HTMLInputElement>) {
        this.setState({ outputPattern: event.target.value });
    }

    @boundMethod
    private handleBlurOutputFilePattern(_event: FocusEvent<HTMLElement>) {
        const { data, onChange } = this.props;

        if (isUndefined(data)) {
            return;
        }

        const { outputFilePattern } = data;
        const { outputPattern } = this.state;

        if (outputFilePattern === outputPattern) {
            return;
        }

        onChange(OUTPUT_FILE_PATTERN_NAME, outputPattern);
    }

    private get testsetTemplateModificationAllowed() {
        const { accessLevel } = this.context;

        return !accessLevel || accessLevel.testsetTemplateModificationAllowed;
    }
}

export default ProblemTestset;
