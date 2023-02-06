import React from 'react';

import {TControlRenderFunc} from 'components/FormField/components/Field/Field';
import FieldLabel from 'components/FieldLabel/FieldLabel';
import Hint from 'components/Hint/Hint';
import Input from 'components/Input/Input';

export const renderInput: TControlRenderFunc = ({
    input,
    label,
    message,
    inputType,
}): React.ReactNode => {
    return (
        <Hint message={message}>
            <FieldLabel label={label}>
                <Input
                    size="l"
                    id={input.name}
                    name={input.name}
                    value={input.value}
                    onChange={input.onChange}
                    type={inputType}
                />
            </FieldLabel>
        </Hint>
    );
};
