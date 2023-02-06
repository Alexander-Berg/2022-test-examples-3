import React from 'react';
import { number, text, withKnobs } from '@storybook/addon-knobs';

import type { IExactPrice } from '@src/typings/price';
import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { withStubReduxProvider } from '@src/storybook/decorators/withStubReduxProvider';
import { ShopItemBase as ShopItem } from '../index';

const props = {
    hostname: 'coinmarketcap.com',
    shopName: 'Solana',
    linkProps: { url: 'https://coinmarketcap.com/currencies/solana/' },
    price: {
        type: 'exact',
        current: '160',
        currency: 'RUR',
    } as IExactPrice,
    id: '',
    showUid: '',
    reqid: '',
};

createPlatformStories('Tests/ShopItem', ShopItem, stories => {
    stories
        .addDecorator(withKnobs)
        .addDecorator(withStaticRouter())
        .addDecorator(withStubReduxProvider())
        .add('plain', Component => {
            const width = number('width', 250);
            const shopName = text('shopName', props.shopName);
            const price = text('price', props.price.current);
            const oldPrice = text('oldPrice', '');
            const priceLabel = text('priceLabel', '');
            const priceLabelDisclaimer = text('priceLabelDisclaimer', '');
            const hostname = text('hostname', props.hostname);

            return (
                <div style={{ maxWidth: width }}>
                    <Component
                        {...props}
                        shopName={shopName}
                        hostname={hostname}
                        price={{
                            ...props.price,
                            current: price,
                            old: oldPrice,
                        }}
                        priceLabel={priceLabel}
                        priceLabelDisclaimer={priceLabelDisclaimer}
                    />
                </div>
            );
        })
        .add('default', Component => {
            return (
                <div style={{ maxWidth: 250 }}>
                    <Component {...props} />
                </div>
            );
        })
        .add('longName', Component => {
            return (
                <div style={{ maxWidth: 250 }}>
                    <Component
                        {...props}
                        shopName="BlaBlaBlaBlaBlaBlaBlaBlaBlaBlaBlaBla"
                    />
                </div>
            );
        })
        .add('placeholder', Component => {
            return (
                <div style={{ maxWidth: 250 }}>
                    <Component
                        {...props}
                        hostname={undefined}
                    />
                </div>
            );
        })
        .add('oldPrice', Component => {
            return (
                <div style={{ maxWidth: 250 }}>
                    <Component
                        {...props}
                        price={{
                            ...props.price,
                            old: '180',
                        }}
                    />
                </div>
            );
        })
        .add('longNameWithOldPrice', Component => {
            return (
                <div style={{ maxWidth: 250 }}>
                    <Component
                        {...props}
                        shopName="BlaBlaBlaBlaBlaBlaBlaBlaBlaBlaBlaBla"
                        price={{
                            ...props.price,
                            old: '180',
                        }}
                    />
                </div>
            );
        })
        .add('verified', Component => {
            return (
                <div style={{ maxWidth: 250 }}>
                    <Component
                        {...props}
                        verified
                        />
                </div>
            );
        });
});
