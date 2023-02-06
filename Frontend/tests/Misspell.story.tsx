import * as React from 'react';
import type { StaticRouterProps } from 'react-router-dom/server';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { withStubReduxProvider } from '@src/storybook/decorators/withStubReduxProvider';
import { Misspell } from '../index';

const routerProps: StaticRouterProps = {
    location: {
        search: '?text=iphone',
    },
};

createPlatformStories('Tests/Misspell', Misspell, stories => {
    stories
        .addDecorator(withStaticRouter(routerProps))
        .addDecorator(withStubReduxProvider())
        .add('plain', Misspell => {
            const texts = {
                original: 'очепятка',
                fixed: 'опечатка',
            };

            return (
                <Misspell texts={texts} />
            );
        });
});
