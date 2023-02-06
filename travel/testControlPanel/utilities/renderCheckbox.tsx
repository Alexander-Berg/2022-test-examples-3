import React from 'react';

import {TControlRenderFunc} from 'components/FormField/components/Field/Field';
import Checkbox from 'components/Checkbox/Checkbox';

export const renderCheckbox: TControlRenderFunc = ({
    input,
    label,
}): React.ReactNode => {
    return (
        <Checkbox
            id={input.name}
            name={input.name}
            checked={Boolean(input.value)}
            onChange={input.onChange}
            size="m"
            label={label}
        />
    );
};
