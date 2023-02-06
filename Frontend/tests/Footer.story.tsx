import React from 'react';
import type { CSSProperties } from 'react';
import type { StaticRouterProps } from 'react-router-dom/server';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { withStubReduxProvider } from '@src/storybook/decorators/withStubReduxProvider';
import { Platform } from '@src/typings/platform';

import { Footer } from '../index';

const routerProps: Partial<StaticRouterProps> = {
    location: {
        search: '?text=iphone',
    },
};

createPlatformStories('Tests/Footer', Footer, (stories, platform) => {
    const containerStyle: CSSProperties = {
        padding: 2,
        // На тачах добавляем фон, чтобы было видно секции-островки.
        backgroundColor: platform === Platform.Touch ? '#eee' : '',
    };

    stories
        .addDecorator(withStaticRouter(routerProps))
        .addDecorator(withStubReduxProvider())
        .add('plain', Footer => (
            <div style={containerStyle}>
                <Footer />
            </div>
        ))
        .add('empty-search', Footer => (
            <div style={containerStyle}>
                <Footer />
            </div>
        ));
});
