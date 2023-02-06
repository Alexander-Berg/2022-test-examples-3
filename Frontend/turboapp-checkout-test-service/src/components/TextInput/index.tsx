import React from 'react';
import { compose } from '@bem-react/core';
import {
    Textinput as LegoTextinput,
    withViewDefault,
    withSizeM,
    withHasClear,
} from '@yandex-lego/components/Textinput/desktop';

export const Textinput = compose(withViewDefault, withSizeM, withHasClear)(LegoTextinput);
export type TextInputProps = Parameters<typeof Textinput>[0];

export default (props: TextInputProps) => <Textinput view="default" size="m" {...props} />;
