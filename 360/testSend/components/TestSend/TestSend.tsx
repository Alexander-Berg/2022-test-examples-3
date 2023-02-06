import React, { useCallback, useEffect, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import {
    testSendInvalidAddress,
    testSendMaximumAddresses,
    testSendNoEmails,
} from '@/locales/i18n/mail-liza/fan-errors';
import { cancel, send } from '@/locales/i18n/mail-liza/fan-common';
import {
    fieldNameTestSendRecipients,
    fieldDescriptionTestSendRecipients,
    fieldPlaceholderTestSendRecipients,
    fieldInputLabelTestSendRecipients,
    testSendTemplateVariablesTooLongError,
} from '@/locales/i18n/mail-liza/fan-campaign-form';
import { Button } from '@/components/Button';
import { LegoModal } from '@/components/Modal';
import { TextField } from '@/components/Field';
import { debounce } from '@/utils/debounce';
import { useMetrika } from '@/hooks/useMetrika';
import { MAIL_LIST_VARIABLE_VALUE_LENGTH_LIMIT } from '@/features/mailLists/constants';

import { IFieldConfig, useField, useForm, ValidatorFactories } from '@/utils/form';

import { BoldText, JustText } from '@/components/Typography';
import { MAX_TEST_SENDS_EMAIL } from '@/../common/constants';

import { getLetterUserTemplatesVariables, getTestSendState } from '../../selectors';
import {
    testSendStart as testSendStartAction,
    testSendAbort as testSendAbortAction,
    ITestSendStartPayload,
} from '../../actions';
import { getErrorText } from '../../utils/getErrorText';

import { LetterTemplatesVariables } from '../LetterTemplatesVariables';
import { EMAIL_RE } from '../../constants';

import classes from './TestSend.css';

export interface ITestSendProps {
    campaignSlug: string;
}

const splitEmails = (emails: string) => emails.split(',').map(email => email.trim()).filter(Boolean);
const emailsExist = (emails: string) => splitEmails(emails).length !== 0;
const countEmailsAtStringCorrect =
    (emails: string) => emailsExist(emails) && splitEmails(emails).length <= MAX_TEST_SENDS_EMAIL;
const hasNoInvalidEmails =
    (emails: string) => countEmailsAtStringCorrect(emails) && !splitEmails(emails).find(email => !EMAIL_RE.test(email));

const checkVariablesLength = (variables: Record<string, string>) =>
    !Object.values(variables).find(v => v.length > MAIL_LIST_VARIABLE_VALUE_LENGTH_LIMIT);

const getErrorAtVariablesLength = (variables: Record<string, string>) => {
    const errors: Record<string, string> = {};

    Object.entries(variables).forEach(([key, val]) => {
        if (val.length > MAIL_LIST_VARIABLE_VALUE_LENGTH_LIMIT) {
            errors[key] = testSendTemplateVariablesTooLongError({ count: MAIL_LIST_VARIABLE_VALUE_LENGTH_LIMIT });
        }
    });

    return errors;
};

const useTestSendForm = (props: ITestSendProps & { isLoading: boolean; userTemplateVars: string[] }) => {
    const { userTemplateVars, campaignSlug, isLoading } = props;

    const emailFieldValidator = useMemo(
        (): IFieldConfig<string> => ({
            initialValue: '',
            validators: [
                ValidatorFactories.required(() => getErrorText('empty')),
                ValidatorFactories.custom(emailsExist, () => testSendNoEmails),
                ValidatorFactories.custom(countEmailsAtStringCorrect, () => testSendMaximumAddresses),
                ValidatorFactories.custom(hasNoInvalidEmails, (v: string) => `${testSendInvalidAddress} ${v}`),
            ],
        }),
        [],
    );

    const dispatch = useDispatch();
    const testSendStart = useCallback(
        (payload: ITestSendStartPayload) =>
            dispatch(testSendStartAction(payload)), [dispatch],
    );

    const emailField = useField(emailFieldValidator);

    const letterTemplatesVars = useMemo(() => userTemplateVars.reduce((acc, templateVar) => {
        acc[templateVar] = '';

        return acc;
    }, {} as Record<string, string>), [userTemplateVars]);

    const templateVarialbesValidator = useMemo(
        (): IFieldConfig<Record<string, string>> => ({
            initialValue: letterTemplatesVars,
            validators: [
                ValidatorFactories.custom(checkVariablesLength, getErrorAtVariablesLength),
            ],
        }),
        [letterTemplatesVars],
    );
    const letterTemplatesVarsFields = useField(templateVarialbesValidator);

    const submit = useCallback(
        (formValues: { email: string; letterTemplatesVars: Record<string, string> }) => {
            if (isLoading) {
                return;
            }

            testSendStart({
                recipients: splitEmails(formValues.email),
                campaign_slug: campaignSlug,
                user_template_variables: formValues.letterTemplatesVars,
            });
        },
        [isLoading, campaignSlug, testSendStart],
    );

    return useForm({
        fields: {
            email: emailField,
            letterTemplatesVars: letterTemplatesVarsFields,
        },
        onSubmit: submit,
    });
};

export function TestSend(props: ITestSendProps) {
    const { isActive, isLoading, error } = useSelector(getTestSendState);
    const { sendMetrika } = useMetrika('Test mail popup');
    const debauncedMetrika = useMemo(() => debounce(sendMetrika, 600), [sendMetrika]);

    const dispatch = useDispatch();
    const testSendAbort = useCallback(() => dispatch(testSendAbortAction()), [dispatch]);

    const letterUserTemplatesVariables = useSelector(getLetterUserTemplatesVariables) ?? [];
    const testSendForm = useTestSendForm({
        campaignSlug: props.campaignSlug,
        isLoading,
        userTemplateVars: letterUserTemplatesVariables,
    });

    const handleCloseButtonClick = useCallback(
        () => {
            sendMetrika('Cancel click');

            return testSendAbort();
        },
        [testSendAbort, sendMetrika],
    );
    const handleInputChange = useCallback(
        (value: string) => {
            debauncedMetrika('enter email');
            testSendForm.fields.email.setValue(value);
        },
        [testSendForm, debauncedMetrika],
    );
    const handleSubmit = useCallback(
        (evt: React.FormEvent) => {
            evt.preventDefault();

            const subbmited = testSendForm.handleSubmit(evt);

            if (subbmited) {
                testSendForm.reset();
            }
        },
        [testSendForm],
    );

    const displayableError =
        error || !testSendForm.isValid && !testSendForm.isPristine && testSendForm.fields.email.error || undefined;

    useEffect(() => {
        if (isActive) {
            sendMetrika('show');
        }
    }, [sendMetrika, isActive]);

    return (
        <LegoModal
            visible={ isActive }
            onClose={ handleCloseButtonClick }
            className={ classes.modal }
        >
            <form className={ classes.wrapper } onSubmit={ handleSubmit }>
                <section className={ classes.emailBlock }>
                    <BoldText typography="subheader-l" className={ classes.subheader }>
                        { fieldNameTestSendRecipients }
                    </BoldText>
                    <JustText typography="body-short-l" className={ classes.bodyShort }>
                        { fieldDescriptionTestSendRecipients }
                    </JustText>
                    <TextField
                        inputLabel={ fieldInputLabelTestSendRecipients }
                        value={ testSendForm.fields.email.value }
                        error={ displayableError }
                        onChange={ handleInputChange }
                        placeholder={ fieldPlaceholderTestSendRecipients }
                        disabled={ isLoading }
                        autoFocus
                        invalid={ Boolean(displayableError) }
                        autoComplete={ 'none' }
                    />
                </section>
                <LetterTemplatesVariables
                    onChange={ testSendForm.fields.letterTemplatesVars.setValue }
                    variables={ letterUserTemplatesVariables }
                    error={ testSendForm.fields.letterTemplatesVars.error as Record<string, string> }
                    values={ testSendForm.fields.letterTemplatesVars.value }
                />
                <footer className={ classes.buttons }>
                    <Button
                        view="default"
                        size="l"
                        className={ classes.modalButton }
                        onClick={ handleCloseButtonClick }
                    >
                        { cancel }
                    </Button>
                    <Button
                        type="submit"
                        view="action"
                        size="l"
                        className={ classes.modalButton }
                        progress={ isLoading }
                        disabled={ Boolean(displayableError) }
                    >
                        { send }
                    </Button>
                </footer>
            </form>
        </LegoModal>
    );
}
