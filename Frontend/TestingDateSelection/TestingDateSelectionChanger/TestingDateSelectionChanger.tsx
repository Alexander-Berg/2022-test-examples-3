import React, { useState, useCallback } from 'react';
import { Button } from 'lego-on-react';
import block from 'propmods';
import * as keyset from 'i18n/DraftControlPanel';
import i18n from '@yandex-int/i18n';
import { TestingDateSelectionForm } from '../TestingDateSelectionForm/TestingDateSelectionForm';
import { DevicesTestingFormSettings } from '../../../model/devicesTestingForm';

import './TestingDateSelectionChanger.scss';

const b = block('TestingDateSelectionChanger');
const t = i18n(keyset);

interface Props {
    beforeButtonElement?: React.ReactNode;
    afterButtonElement?: React.ReactNode;
    formSettings: DevicesTestingFormSettings;
    afterFormSave: () => Promise<void>;
}

export const TestingDateSelectionChanger: React.FC<Props> = ({
    afterButtonElement,
    beforeButtonElement,
    formSettings,
    afterFormSave,
}) => {
    const [displayForm, setDisplayForm] = useState(false);

    const hideForm = useCallback(() => {
        setDisplayForm(false);
    }, []);

    const showForm = useCallback(() => {
        setDisplayForm(true);
    }, []);

    const afterSave = useCallback(async() => {
        await afterFormSave();
        hideForm();
    }, [hideForm, afterFormSave]);

    return (
        <div {...b()}>
            {displayForm ? (
                <TestingDateSelectionForm onCancel={hideForm} initialSettings={formSettings} afterSave={afterSave} />
            ) : (
                <>
                    {beforeButtonElement && <div {...b('before')}>{beforeButtonElement}</div>}
                    <div {...b('button')}>
                        <Button size="s" theme="normal" onClick={showForm}>
                            {t('button-change-date')}
                        </Button>
                    </div>
                    {afterButtonElement && <div {...b('after')}>{afterButtonElement}</div>}
                </>
            )}
        </div>
    );
};
