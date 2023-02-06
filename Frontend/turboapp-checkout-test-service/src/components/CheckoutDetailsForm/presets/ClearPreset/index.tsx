import React, { useContext, useCallback } from 'react';

import { Button } from '../../../Button';
import { CheckoutDetailsFormContext } from '../../CheckoutDetailsFormProvider';

const ClearPresetButton: React.FC = () => {
    const { changeState } = useContext(CheckoutDetailsFormContext);

    const onStartCheckoutDetailsSelect = useCallback(() => {
        changeState({});
    }, [changeState]);

    return (
        <Button size="l" view="default" onClick={onStartCheckoutDetailsSelect}>
            Очистить форму
        </Button>
    );
};

export default ClearPresetButton;
