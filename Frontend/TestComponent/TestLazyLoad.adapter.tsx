import * as React from 'react';
import { Adapter } from '@yandex-turbo/core/adapter';
import { IAdapterContext } from '@yandex-turbo/types/AdapterContext';

import { IProps, TestLazyLoadAdvert, TestLazyLoadEmbed } from './TestLazyLoad';

interface IScheme {
    type?: string;
}

export default class TestLazyLoadAdapter extends Adapter<IProps, IScheme, IAdapterContext> {
    public transform(data: IScheme): IProps {
        return {
            type: data.type || 'embed',
        };
    }

    public element(props: IProps): JSX.Element {
        if (props.type === 'embed') {
            return <TestLazyLoadEmbed {...props} />;
        }

        if (props.type === 'advert') {
            return <TestLazyLoadAdvert {...props} />;
        }

        return <></>;
    }
}
