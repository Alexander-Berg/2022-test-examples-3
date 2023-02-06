import { compose, composeU } from '@bem-react/core';
import { Spin as LegoSpin, withSizeM, withSizeL, withViewDefault } from '@yandex-lego/components/Spin/desktop';

export const Spin = compose(composeU(withSizeM, withSizeL), withViewDefault)(LegoSpin);
export type SpinProps = Parameters<typeof Spin>[0];
