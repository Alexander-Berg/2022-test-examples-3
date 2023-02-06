import React from 'react';
import { withKnobs } from '@storybook/addon-knobs';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { StubReduxProvider } from '@src/storybook/stubs/StubReduxProvider';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { getUserInitialState } from '@src/store/services/user/reducer';
import { getEntitiesInitialState } from '@src/store/services/entities/reducer';
import { getSubscriptionInitialState } from '@src/store/services/subscription/reducer';

import type { IUserState } from '@src/store/services/user/types';
import type { IEntitiesState } from '@src/store/services/entities/types';
import type { ISubscriptionState } from '@src/store/services/subscription/types';
import type { ISubscription } from '@src/typings/subscription';

import type { IPriceSubscriptionProps } from '../PriceSubscription.types';
import { PriceSubscription } from '..';

const skuId = 'sku_id_0';

const skuSubscription: ISubscription = {
    id: 'id_0',
    skuId,
    startTracking: 0,
    email: '',
    price: 666,
};

const user: IUserState = {
    ...getUserInitialState(),
    blackboxUser: {
        email: 'email@yandex.ru',
        hasPlus: true,
        name: {},
        uid: '',
    },
};

const subscription: ISubscriptionState = {
    ...getSubscriptionInitialState(),
    requestStatus: 'success',
};

const entities: IEntitiesState = {
    ...getEntitiesInitialState(),
    subscription: { [skuSubscription.id]: skuSubscription },
};

const defaultProps: IPriceSubscriptionProps = {
    className: 'PriceSubscriptionStory',
    skuId: skuId,
    price: 666,
};

createPlatformStories('Tests/PriceSubscription', PriceSubscription, stories => {
    stories
        .addDecorator(withKnobs)
        .addDecorator(withStaticRouter())
        .add('plain', Component => {
            const props = { ...defaultProps, isInitiallyVisible: true };

            return (
                <StubReduxProvider stub={{ user, subscription }}>
                    <Component {...props} />
                </StubReduxProvider>
            );
        })
        .add('unsubscribed', Component => {
            const props = { ...defaultProps };

            return (
                <StubReduxProvider stub={{ user, subscription }}>
                    <Component {...props} />
                </StubReduxProvider>
            );
        })
        .add('subscribed', Component => {
            const props = { ...defaultProps };

            return (
                <StubReduxProvider stub={{ user, entities, subscription }}>
                    <Component {...props} />
                </StubReduxProvider>
            );
        });
});
