import React, { useContext, useCallback } from 'react';

import Checkbox from '../../../Checkbox';
import { Section } from '../../components/Section';
import { AddSection } from '../../components/AddSection';
import { CheckoutDetailsFormContext } from '../../CheckoutDetailsFormProvider';

const Comment: React.FC = () => {
    const { defaultState, state, changeState } = useContext(CheckoutDetailsFormContext);

    const needComment = Boolean(state.requestOrderComment);

    const onChange = useCallback(() => {
        const isChecked = !needComment;

        changeState({
            ...state,
            requestOrderComment: isChecked,
        });
    }, [state, changeState, needComment]);

    const onAdd = useCallback(() => {
        changeState({ ...state, requestOrderComment: defaultState.requestOrderComment ?? false });
    }, [defaultState, state, changeState]);

    const onDelete = useCallback(() => {
        const newState = { ...state };
        delete newState.requestOrderComment;

        changeState(newState);
    }, [state, changeState]);

    if (!('requestOrderComment' in state)) {
        return <AddSection onClick={onAdd}>Комментарий к заказу</AddSection>;
    }

    return (
        <Section title="Комментарий к заказу" onDelete={onDelete}>
            <Checkbox checked={needComment} onChange={onChange} label="Показать поле для комментария" />
        </Section>
    );
};

export default Comment;
