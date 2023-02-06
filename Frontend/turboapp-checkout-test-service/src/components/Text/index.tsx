import { compose, composeU } from '@bem-react/core';
import {
    Text as LegoText,
    withTypographyHeadlineL,
    withTypographyHeadlineM,
    withTypographyHeadlineS,
    withTypographyHeadlineXS,
    withTypographyControlM,
    withWeightLight,
} from '@yandex-lego/components/Text/desktop';

export const Text = compose(
    composeU(
        withTypographyHeadlineXS,
        withTypographyHeadlineS,
        withTypographyHeadlineM,
        withTypographyHeadlineL,
        withTypographyControlM
    ),
    withWeightLight
)(LegoText);

export type TextProps = Parameters<typeof Text>[0];
