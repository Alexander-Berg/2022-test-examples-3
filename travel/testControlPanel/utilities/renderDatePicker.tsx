import React from 'react';

import {TControlRenderFunc} from 'components/FormField/components/Field/Field';
import FieldLabel from 'components/FieldLabel/FieldLabel';
import DatePicker from 'projects/testControlPanel/components/DatePicker/DatePicker';

export const renderDatePicker: TControlRenderFunc = ({
    input,
    label,
}): React.ReactNode => {
    return (
        <FieldLabel label={label}>
            <DatePicker
                name={input.name}
                value={input.value}
                onChange={input.onChange}
            />
        </FieldLabel>
    );
};
