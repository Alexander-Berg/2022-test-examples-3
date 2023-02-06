import React from 'react';
import { compose } from '@bem-react/core';
import { Checkbox as LegoCheckbox, withSizeM, withViewDefault } from '@yandex-lego/components/Checkbox/desktop';

export const Checkbox = compose(withSizeM, withViewDefault)(LegoCheckbox);
export type CheckboxProps = Parameters<typeof Checkbox>[0];

export default (props: CheckboxProps) => <Checkbox view="default" size="m" {...props} />;
