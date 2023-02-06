import { compose, composeU } from '@bem-react/core';
import {
    Icon as LegoIcon,
    withTypeArrow,
    withGlyphTypeCross,
    withGlyphTypeCrossWebsearch,
    withGlyphTypeArrow,
} from '@yandex-lego/components/Icon/desktop';

export const Icon = compose(
    withTypeArrow,
    composeU(withGlyphTypeCross, withGlyphTypeCrossWebsearch, withGlyphTypeArrow)
)(LegoIcon);
export type IconProps = Parameters<typeof Icon>[0];
