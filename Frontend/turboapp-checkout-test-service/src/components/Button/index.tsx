import { compose, composeU } from '@bem-react/core';
import {
    Button as LegoButton,
    withSizeS,
    withSizeM,
    withSizeL,
    withViewDefault,
    withViewAction,
    withViewClear,
    withViewPseudo,
    withTypeLink,
    withWidthMax,
    withWidthAuto,
} from '@yandex-lego/components/Button/desktop';

export const Button = compose(
    composeU(withSizeS, withSizeM, withSizeL),
    composeU(withViewDefault, withViewAction, withViewClear, withViewPseudo),
    withTypeLink,
    composeU(withWidthMax, withWidthAuto)
)(LegoButton);

export type ButtonProps = Parameters<typeof Button>[0];
