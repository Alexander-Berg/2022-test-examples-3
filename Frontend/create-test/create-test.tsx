import { boundMethod } from 'autobind-decorator';
import React, { ChangeEvent, Component, Fragment } from 'react';

import { CommonError } from 'common/schemas/common';
import { createProblemTestSchema } from 'common/schemas/problem-test';
import { IProblemTest } from 'common/types/problem';

import Button from 'client/components/button';
import {
    AMOUNT_OF_DATA_ROWS,
    EMPTY_STATE,
    FIELD_NAMES,
    RESET_STATE,
} from 'client/components/create-test/constants';
import { LABELS } from 'client/components/create-test/texts';
import { Props, State } from 'client/components/create-test/types';
import Fieldset from 'client/components/fieldset';
import Field from 'client/components/field';
import IconControl from 'client/components/icon-control';
import Modal from 'client/components/modal';
import {
    IValidationFormComposedComponentProps,
    TextareaField,
    TextinputField,
    WithValidationForm,
} from 'client/decorators/with-validation';
import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

import 'client/components/create-test/create-test.css';

const b = block('create-test');
const fieldset = block('fieldset');

class CreateTest extends Component<Props, State> {
    public readonly state: State = RESET_STATE;
    private readonly id = this.props.getUniqId();

    public componentDidUpdate(prevProps: Props) {
        if (this.props.isPopupJustOpened()) {
            this.setState(EMPTY_STATE);
        }
        if (
            prevProps.createTestStarted &&
            !this.props.createTestStarted &&
            !this.props.createTestError
        ) {
            this.props.closePopup();
            this.resetForm();
        }
    }

    public render() {
        const { children, isPopupVisible, openPopup } = this.props;
        const { data } = this.state;

        return (
            <Fragment>
                {children(openPopup)}
                <Modal
                    theme="normal"
                    shadow
                    visible={isPopupVisible}
                    onClose={this.handleCloseModal}
                >
                    <div className={b('modal')}>
                        <div className={b('modal-close')} onClick={this.handleCloseModal}>
                            <IconControl size="l" type="close-24" />
                        </div>
                        <WithValidationForm<IProblemTest>
                            className={b()}
                            schema={createProblemTestSchema}
                            onSubmit={this.handleSubmit}
                            initialValues={{ ...data }}
                        >
                            {this.renderForm()}
                        </WithValidationForm>
                    </div>
                </Modal>
            </Fragment>
        );
    }

    private renderForm() {
        const { createTestStarted } = this.props;

        return ({ invalid }: IValidationFormComposedComponentProps) => (
            <Fragment>
                <h1 className={b('title')}>
                    {i18n.text({ keyset: 'problem-tests', key: 'create-test' })}
                </h1>
                <Fieldset className={b('controls')}>
                    {this.renderInputPath()}
                    {this.renderInputContent()}
                    {this.renderOutputPath()}
                    {this.renderOutputContent()}
                </Fieldset>
                <Button
                    className={b('submit-button')}
                    size="l"
                    theme="action"
                    type="submit"
                    disabled={invalid}
                    progress={createTestStarted}
                >
                    {i18n.text({
                        keyset: 'common',
                        key: 'add',
                    })}
                </Button>
            </Fragment>
        );
    }

    private renderInputPath() {
        const {
            data: { inputPath },
        } = this.state;

        return this.renderPath(
            `${this.id}__${FIELD_NAMES.INPUT_PATH}`,
            inputPath,
            LABELS.inputPath,
            FIELD_NAMES.INPUT_PATH,
        );
    }

    private renderInputContent() {
        const {
            data: { inputContent },
        } = this.state;

        return this.renderContent(
            `${this.id}__${FIELD_NAMES.INPUT_CONTENT}`,
            inputContent,
            LABELS.inputContent,
            FIELD_NAMES.INPUT_CONTENT,
        );
    }

    private renderOutputPath() {
        const {
            data: { outputPath },
        } = this.state;

        return this.renderPath(
            `${this.id}__${FIELD_NAMES.OUTPUT_PATH}`,
            outputPath,
            LABELS.outputPath,
            FIELD_NAMES.OUTPUT_PATH,
        );
    }

    private renderOutputContent() {
        const {
            data: { outputContent },
        } = this.state;

        return this.renderContent(
            `${this.id}__${FIELD_NAMES.OUTPUT_CONTENT}`,
            outputContent,
            LABELS.outputContent,
            FIELD_NAMES.OUTPUT_CONTENT,
        );
    }

    private renderPath(fieldId: string, value: string, labelText: string, field: string) {
        const onChange = (event: ChangeEvent<HTMLInputElement>) =>
            this.handleChangeValue(event.target.value, field);

        return (
            <Field>
                <label htmlFor={fieldId} className={b('label', [fieldset('label')])}>
                    {labelText}
                </label>
                <TextinputField
                    name={field}
                    errorMessages={{
                        [CommonError.REQUIRED_VALUE]: i18n.text({
                            keyset: 'common',
                            key: 'required-field',
                        }),
                    }}
                    controlProps={{
                        id: fieldId,
                        theme: 'normal',
                        size: 'm',
                        value,
                        autoComplete: 'off',
                        onChange,
                    }}
                />
            </Field>
        );
    }

    private renderContent(fieldId: string, value: string, labelText: string, field: string) {
        const onChange = (event: ChangeEvent<HTMLTextAreaElement>) =>
            this.handleChangeValue(event.target.value, field);

        return (
            <Field>
                <label htmlFor={fieldId} className={b('label', [fieldset('label')])}>
                    {labelText}
                </label>
                <TextareaField
                    name={field}
                    errorMessages={{
                        [CommonError.REQUIRED_VALUE]: i18n.text({
                            keyset: 'common',
                            key: 'required-field',
                        }),
                    }}
                    controlProps={{
                        id: fieldId,
                        view: 'default',
                        size: 'm',
                        rows: AMOUNT_OF_DATA_ROWS,
                        value,
                        onChange,
                    }}
                />
            </Field>
        );
    }

    @boundMethod
    private handleChangeValue(value: string, field: string) {
        this.setState((state) => ({
            data: {
                ...state.data,
                [field]: value,
            },
        }));
    }

    @boundMethod
    private handleSubmit() {
        const { problemId, createTest } = this.props;
        const { data } = this.state;

        createTest({ problemId, data });
    }

    @boundMethod
    private handleCloseModal() {
        const { closePopup } = this.props;

        closePopup();
        this.resetForm();
    }

    private resetForm() {
        this.setState(RESET_STATE);
    }
}

export default CreateTest;
