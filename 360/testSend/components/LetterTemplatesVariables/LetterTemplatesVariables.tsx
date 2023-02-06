import React, { useCallback, useMemo } from 'react';

import { TextField } from '@/components/Field';
import { BoldText, JustText } from '@/components/Typography';
import { testSendTemplateVariablesTitle, testSendTemplateVariablesDesc } from '@/locales/i18n/mail-liza/fan-campaign-form';

import classes from './LetterTemplatesVariables.css';

interface ITestSendVariableFieldProps {
    onChange: (obj: { variable: string; inputValue: string }) => void;
    variable: string;
    value: string;
    error: string | null;
}

function TestSendVariableField(props: ITestSendVariableFieldProps) {
    const { value, variable, onChange, error } = props;

    const onInputChange = useCallback((inputValue: string) => onChange({
        variable,
        inputValue,
    }), [onChange, variable]);
    const inputLabel = useMemo(() => {
        if (variable.length > 55) {
            return `${variable.slice(0, 55)}...`;
        }

        return variable;
    }, [variable]);

    return (
        <TextField
            inputLabel={ inputLabel }
            value={ value }
            error={ error }
            onChange={ onInputChange }
            title={ variable }
        />
    );
}

interface ILetterTemplatesVariablesProps {
    variables: string[];
    onChange: (values: Record<string, string>) => void;
    values: Record<string, string>;
    error: Record<string, string> | null;
}

export function LetterTemplatesVariables(props: ILetterTemplatesVariablesProps) {
    const { variables, values, error, onChange } = props;

    const onInputChange = useCallback(({ variable, inputValue }) => {
        onChange({
            ...values,
            [variable]: inputValue,
        });
    }, [values, onChange]);

    if (variables.length === 0) return null;

    return (
        <section className={ classes.wrapper }>
            <BoldText typography="subheader-l" className={ classes.subheader }>
                { testSendTemplateVariablesTitle }
            </BoldText>
            <JustText typography="body-short-l" className={ classes.bodyShort }>
                { testSendTemplateVariablesDesc }
            </JustText>

            {
                variables.map(variable => {
                    return (
                        <TestSendVariableField
                            key={ variable }
                            variable={ variable }
                            value={ values[variable] }
                            error={ error ? error[variable] : null }
                            onChange={ onInputChange }
                        />
                    );
                })
            }
        </section>
    );
}
