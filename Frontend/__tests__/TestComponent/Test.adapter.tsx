import * as React from 'react';
import { Adapter } from '@yandex-turbo/core/adapter';
import { ISlotable } from '@yandex-turbo/components/withSlots/withSlots';

import { TestLayoutWithSlots, IProps, ISlots } from './_Layout/Test_Layout_WithSlots';

export interface IScheme {
    block: 'test';
    props: IProps;
}

export default class TestAdapter extends Adapter<IProps, IScheme> {
    public transform(scheme: IScheme): IProps {
        return scheme.props || {};
    }

    public element(props: IProps & ISlotable<ISlots>) {
        return <TestLayoutWithSlots {...props} />;
    }
}
