import React from 'react';
import noop from 'lodash/noop';

import {TControlRenderFunc} from 'components/FormField/components/Field/Field';
import FieldLabel from 'components/FieldLabel/FieldLabel';
import Select from 'components/Select/Select';
import Hint from 'components/Hint/Hint';

export const renderSelect: TControlRenderFunc = (
    {input, options, label, getOptionDescription = noop},
    title,
): React.ReactNode => {
    return (
        <Hint message={getOptionDescription(input.value)}>
            <FieldLabel label={label}>
                <Select
                    size="l"
                    theme="secondary"
                    menuWidth={'auto'}
                    width={'max'}
                    options={options}
                    id={input.name}
                    name={input.name}
                    value={input.value}
                    onChange={input.onChange}
                    placeholder={title}
                />
            </FieldLabel>
        </Hint>
    );
};
