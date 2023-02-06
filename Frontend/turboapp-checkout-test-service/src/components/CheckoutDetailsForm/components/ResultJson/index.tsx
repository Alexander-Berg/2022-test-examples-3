import React, { useCallback, useContext, useEffect, useState } from 'react';

import { Textarea, TextareaProps } from '../../../Textarea';
import { CheckoutDetailsFormContext } from '../../CheckoutDetailsFormProvider';

import styles from './ResultJson.module.css';

const ResultJson: React.FC = () => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const [json, setJson] = useState(JSON.stringify(state, null, 4));
    const [formState, setFormState] = useState<TextareaProps['state']>();
    const [hint, setHint] = useState('');
    const [isFocused, setFocused] = useState(false);

    const onChange = useCallback(
        (e: React.ChangeEvent<HTMLTextAreaElement>) => {
            const newJson = e.target.value;

            setJson(newJson);

            try {
                const newState = JSON.parse(newJson);

                setFormState(undefined);
                setHint('');

                changeState(newState);
            } catch (err) {
                setFormState('error');
                setHint(`Невалидный json: ${err.message}`);
            }
        },
        [changeState]
    );

    const onFocus = useCallback(() => setFocused(true), []);
    const onBlur = useCallback(() => setFocused(false), []);

    useEffect(() => {
        if (!isFocused) {
            setJson(JSON.stringify(state, null, 4));
        }
    }, [state, isFocused]);

    return (
        <Textarea
            className={styles.textarea}
            view="default"
            size="m"
            value={json}
            onChange={onChange}
            state={formState}
            hint={hint}
            onFocus={onFocus}
            onBlur={onBlur}
        />
    );
};

export default ResultJson;
