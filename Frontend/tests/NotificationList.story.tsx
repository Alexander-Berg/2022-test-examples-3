import React from 'react';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { StubReduxProvider } from '@src/storybook/stubs/StubReduxProvider';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { NotificationList } from '../index';

const notifications = {
    list: [
        {
            id: 1,
            type: 'offline',
            data: {},
        },
        {
            id: 2,
            type: 'error',
            data: {},
        },
        {
            id: 3,
            type: 'subscribeToPriceChangesError',
            data: {},
        },
        {
            id: 4,
            type: 'unsubscribeFromPriceChangesError',
            data: {},
        },
        {
            id: 5,
            type: 'unsubscribeFromPriceChangesSuccess',
            data: {},
        },
        {
            id: 6,
            type: 'priceHasChanged',
            data: {},
        },
    ],
};

createPlatformStories('Tests/NotificationList', NotificationList, stories => {
    stories
        .addDecorator(withStaticRouter())
        .add('plain', NotificationList => (
            <StubReduxProvider stub={{ notifications }}>
                <NotificationList />
            </StubReduxProvider>
        ));
});
