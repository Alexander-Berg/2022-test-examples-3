import React from 'react';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { StubReduxProvider } from '@src/storybook/stubs/StubReduxProvider';
import { ShopList } from '../index';
import { props, state } from './stubs';

createPlatformStories('Tests/ShopList', ShopList, stories => {
    stories
        .addDecorator(withStaticRouter())
        .add('default', Component => (
            <StubReduxProvider stub={state}>
                <div style={{ maxWidth: 350 }}>
                    <Component {...props} />
                </div>
            </StubReduxProvider>
        ));
});
