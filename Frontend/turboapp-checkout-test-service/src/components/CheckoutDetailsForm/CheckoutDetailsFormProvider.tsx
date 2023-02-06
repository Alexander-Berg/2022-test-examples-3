import React, { createContext, useCallback, useMemo } from 'react';
import clone from 'clone-deep';

import { CheckoutDetailsUpdate } from '../../types/checkout-api';

type CheckoutDetailsFormProviderValue = {
    defaultState: CheckoutDetailsUpdate;
    state: CheckoutDetailsUpdate;
    changeState: (newState: CheckoutDetailsUpdate) => void;
};

export const CheckoutDetailsFormContext = createContext<CheckoutDetailsFormProviderValue>({
    defaultState: {},
    state: {},
    changeState: () => ({}),
});

type Props = {
    defaultState: CheckoutDetailsUpdate;
    state: CheckoutDetailsUpdate;
    setState: (state: CheckoutDetailsUpdate) => void;
};

export const CheckoutDetailsFormProvider: React.FC<Props> = ({ defaultState, state, setState, children }) => {
    const changeState = useCallback(
        (newState: CheckoutDetailsUpdate) => {
            setState(clone(newState));
        },
        [setState]
    );

    const checkoutDetailsFormProviderValue = useMemo<CheckoutDetailsFormProviderValue>(
        () => ({
            defaultState,
            state,
            changeState,
        }),
        [defaultState, state, changeState]
    );

    return (
        <CheckoutDetailsFormContext.Provider value={checkoutDetailsFormProviderValue}>
            {children}
        </CheckoutDetailsFormContext.Provider>
    );
};
