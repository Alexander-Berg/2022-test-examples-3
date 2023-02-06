import React, { useContext, useCallback } from 'react';

import startCheckoutDetailsPreset from '../../../../presets/start-checkout-details';
import { Button } from '../../../Button';
import { CheckoutDetailsFormContext } from '../../CheckoutDetailsFormProvider';

const StartPresetButton: React.FC = () => {
    const { changeState } = useContext(CheckoutDetailsFormContext);

    const onStartCheckoutDetailsSelect = useCallback(() => {
        changeState(startCheckoutDetailsPreset);
    }, [changeState]);

    return (
        <Button size="l" view="default" onClick={onStartCheckoutDetailsSelect}>
            Стандартный CheckoutDetails
        </Button>
    );
};

export default StartPresetButton;
