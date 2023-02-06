import React, { useMemo, useState } from 'react';

import { CheckoutDetailsUpdate } from '../../types/checkout-api';

import { TabsMenu } from '../TabsMenu';

import ResultJson from './components/ResultJson';
import { CheckoutDetailsFormProvider } from './CheckoutDetailsFormProvider';

import Total from './sections/Total';
import PayerDetails from './sections/PayerDetails';
import PaymentOptions from './sections/PaymentOptions';
import Comment from './sections/Comment';
import Orders from './sections/Orders';
import ValidationErrors from './sections/ValidationErrors';

import Presets from './presets';

import styles from './CheckoutDetailsForm.module.css';

type CheckoutDetailsFormProps = {
    defaultState?: CheckoutDetailsUpdate;
    state: CheckoutDetailsUpdate;
    setState: (state: CheckoutDetailsUpdate) => void;
};

enum Tabs {
    Form = 'form',
    Json = 'json',
}

const emptyState: CheckoutDetailsUpdate = {};

const CheckoutDetailsForm: React.FC<CheckoutDetailsFormProps> = ({ defaultState, state, setState }) => {
    const [activeTab, setActiveTab] = useState(Tabs.Form);

    const tabs = useMemo(() => {
        return [
            {
                id: Tabs.Form,
                onClick: () => setActiveTab(Tabs.Form),
                content: 'Форма',
            },
            {
                id: Tabs.Json,
                onClick: () => setActiveTab(Tabs.Json),
                content: 'JSON',
            },
        ];
    }, []);

    return (
        <CheckoutDetailsFormProvider defaultState={defaultState || emptyState} state={state} setState={setState}>
            <div className={styles.form}>
                <TabsMenu
                    size="m"
                    view="default"
                    layout="horiz"
                    className={styles.tabs}
                    activeTab={activeTab}
                    tabs={tabs}
                />

                {activeTab === Tabs.Form && (
                    <>
                        <Orders />
                        <PayerDetails />
                        <Comment />
                        <PaymentOptions />
                        <Total />
                        <ValidationErrors />
                    </>
                )}

                {activeTab === Tabs.Json && <ResultJson />}
            </div>

            <div className={styles.controls}>
                <Presets />
            </div>
        </CheckoutDetailsFormProvider>
    );
};

export default CheckoutDetailsForm;
