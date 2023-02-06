import * as React from 'react';
import type { StaticRouterProps } from 'react-router-dom/server';
import { withStubReduxProvider } from '@src/storybook/decorators/withStubReduxProvider';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { EShopStat } from '@src/schema/shop/types';
import { ShopCard } from '..';
import type { IShopCardProps } from '../ShopCard.typings';

const routerProps: Partial<StaticRouterProps> = {
    location: {
        search: '?text=iphone',
    },
};

const style: React.CSSProperties = {
    width: 500,
    background: '#eeeff2',
    padding: '15px 0',
};

createPlatformStories('Tests/ShopCard', ShopCard, stories => {
    const props: IShopCardProps = {
        rating: 4.5,
        name: 'Техномаркет',
        host: 'technoma.net',
        gradesHist: [1103, 300, 22, 0, 203],
        reviewsCount: 1921,
        isValidated: true,
        audienceSize: EShopStat.tier0,
        purchases: 10,
    };
    stories
        .addDecorator(withStaticRouter(routerProps))
        .addDecorator(withStubReduxProvider())
        .add('plain', ShopCard => (
            <div style={style}>
                <ShopCard {...props} />
            </div>
        ));
});
