import { compose } from '@bem-react/core';

import {
    Textarea as LegoTextarea,
    withViewDefault,
    withSizeM,
    withAutoResize,
} from '@yandex-lego/components/Textarea/desktop';

export const Textarea = compose(withViewDefault, withSizeM)(LegoTextarea);
export type TextareaProps = Parameters<typeof Textarea>[0];

export const ResizableTextarea = compose(withAutoResize)(Textarea);
