import { compose } from '@yandex-turbo/core/hoc';
import { withDisplayName } from '@yandex-turbo/components/withDisplayName/withDisplayName';
import { withSlots, ISlotable } from '@yandex-turbo/components/withSlots/withSlots';

import { Test, IProps as IFullProps } from '../Test';

export type IProps = Pick<IFullProps, 'text'>;
export type ISlots = Pick<IFullProps, 'bottomElem'>;

export const TestLayoutWithSlots = compose<IProps & ISlotable<ISlots>>(
    withDisplayName('Test'),
    withSlots<IProps, ISlots>()
)(Test);
