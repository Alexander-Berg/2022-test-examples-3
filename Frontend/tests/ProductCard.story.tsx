import React from 'react';
import { number, select, text, withKnobs } from '@storybook/addon-knobs';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { StubReduxProvider } from '@src/storybook/stubs/StubReduxProvider';
import { Platform } from '@src/typings/platform';
import type { IInternalState } from '@src/store/services/internal/types';
import type { IFavoritesState } from '@src/store/services/favorites/types';
import { ProductCard } from '../index';
import type { IProductCardProps } from '../ProductCard.typings';
import { ECardType } from '../ProductCard.typings';

const internal: IInternalState = {
    nonce: '',
    baseUrl: '',
    canonicalUrl: '',
    isYandexNet: true,
    isYandexApp: false,
    isYandexAppWebVerticalsExp: false,
    isYandexAppUAExp: false,
    expFlags: {},
    origin: '',
    project: 'products',
    currentYear: 2022,
};

const favorites: IFavoritesState = {
    requestStatus: 'failed',
};

const defaultProps: Omit<IProductCardProps, 'price'> = {
    id: '1',
    images: [{
        src: '//avatars.mds.yandex.net/get-mpic/4754204/img_id4085559111223247146.png/100x100',
        srcHd: '//avatars.mds.yandex.net/get-mpic/4754204/img_id4085559111223247146.png/200x200',
    }],
    title: 'Смартфон Apple iPhone 12 64GB, черный Смартфон Apple iPhone 12 64GB, черный Смартфон Apple iPhone 12 64GB, черный Смартфон Apple iPhone 12 64GB, черный ',
    sourceName: 'Яндекс.Маркет',
    type: ECardType.offer,
    url: 'https://market.yandex.ru/',
};

createPlatformStories('Tests/ProductCard', ProductCard, (stories, platform) => {
    const wrapperStyle: React.CSSProperties = { width: platform === Platform.Desktop ? 300 : 200 };
    stories
        .addDecorator(withKnobs)
        .addDecorator(withStaticRouter())
        .add('currentPrice', Component => {
            const rating = number('rating', 4.5);
            const priceCurrent = number('price', 86790);
            const priceOld = number('oldPrice', 90000);
            const title = text('title', defaultProps.title);
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            const sourceName = text('sourceName', defaultProps.sourceName!);
            return (
                <StubReduxProvider stub={{ favorites, internal }}>
                    <div style={wrapperStyle}>
                        <Component
                            {...defaultProps}
                            rating={rating}
                            title={title}
                            sourceName={sourceName}
                            price={{
                                type: 'exact',
                                currency: 'RUR',
                                current: priceCurrent.toString(),
                                old: priceOld?.toString(),
                            }}
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('rangePrice', Component => {
            const min = number('min', 86790);
            const max = number('max', 90000);
            const title = text('title', defaultProps.title);
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            const sourceName = text('sourceName', defaultProps.sourceName!);
            return (
                <StubReduxProvider stub={{ favorites, internal }}>
                    <div style={wrapperStyle}>
                        <Component
                            {...defaultProps}
                            title={title}
                            sourceName={sourceName}
                            price={{
                                type: 'range',
                                currency: 'RUR',
                                min: min.toString(),
                                max: max.toString(),
                            }}
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('withImageProps', Component => {
            const min = number('min', 86790);
            const max = number('max', 90000);
            const title = text('title', defaultProps.title);
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            const sourceName = text('sourceName', defaultProps.sourceName!);
            const width = number('width', 148);
            const height = number('height', 148);
            return (
                <StubReduxProvider stub={{ favorites, internal }}>
                    <div style={{ width, height }}>
                        <Component
                            {...defaultProps}
                            thumbnailOptions={{
                                width,
                                height,
                            }}
                            title={title}
                            sourceName={sourceName}
                            price={{
                                type: 'range',
                                currency: 'RUR',
                                min: min.toString(),
                                max: max.toString(),
                            }}
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('withoutOffer', ProductCard => {
            return (
                <StubReduxProvider stub={{ favorites, internal }}>
                    <div style={wrapperStyle}>
                        <ProductCard
                            id={defaultProps.id}
                            images={defaultProps.images}
                            title={defaultProps.title}
                            type={ECardType.offer}
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('product', ProductCard => {
            const min = number('min', 86790);
            const max = number('max', 90000);
            const title = text('title', defaultProps.title);
            const offersCount = number('offersCount', 4);
            const rating = number('rating', 4.5);

            return (
                <StubReduxProvider stub={{ favorites, internal }}>
                    <div style={wrapperStyle}>
                        <ProductCard
                            {...defaultProps}
                            offersCount={offersCount}
                            title={title}
                            rating={rating}
                            price={{
                                type: 'range',
                                currency: 'RUR',
                                min: min.toString(),
                                max: max.toString(),
                            }}
                            type={ECardType.product}
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('sku', ProductCard => {
            const min = number('min', 86790);
            const max = number('max', 90000);
            const priceLabel = text('priceLabel', '');
            const title = text('title', defaultProps.title);
            const offersCount = number('offersCount', 4);
            const rating = number('rating', 4.5);

            return (
                <StubReduxProvider stub={{ favorites, internal }}>
                    <div style={wrapperStyle}>
                        <ProductCard
                            {...defaultProps}
                            offersCount={offersCount}
                            title={title}
                            rating={rating}
                            price={{
                                type: 'range',
                                currency: 'RUR',
                                min: min.toString(),
                                max: max.toString(),
                            }}
                            priceLabel={priceLabel}
                            type={ECardType.sku}
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('usedOffer', ProductCard => {
            const min = number('min', 86790);
            const max = number('max', 90000);
            const priceLabel = text('priceLabel', '');
            const title = text('title', defaultProps.title);
            const offersCount = number('offersCount', 4);
            const rating = number('rating', 4.5);

            return (
                <StubReduxProvider stub={{ favorites, internal }}>
                    <div style={wrapperStyle}>
                        <ProductCard
                            {...defaultProps}
                            offersCount={offersCount}
                            title={title}
                            rating={rating}
                            price={{
                                type: 'range',
                                currency: 'RUR',
                                min: min.toString(),
                                max: max.toString(),
                            }}
                            priceLabel={priceLabel}
                            type={ECardType.offer}
                            date="17 марта"
                            sellerName="Иван Иванов"
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('withZeroPrice', ProductCard => {
            const priceLabel = text('priceLabel', '');
            const title = text('title', defaultProps.title);
            const rating = number('rating', 4.5);

            return (
                <StubReduxProvider stub={{ favorites, internal }}>
                    <div style={wrapperStyle}>
                        <ProductCard
                            {...defaultProps}
                            title={title}
                            rating={rating}
                            price={{
                                type: 'range',
                                currency: 'RUR',
                                min: '0',
                                max: '0',
                            }}
                            priceLabel={priceLabel}
                            type={ECardType.sku}
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('expTitleThumb', ProductCard => {
            const priceLabel = text('priceLabel', '');
            const title = text('title', defaultProps.title);
            const rating = number('rating', 4.5);
            const type = select('type', [ECardType.offer, ECardType.sku, ECardType.product, ECardType.RMG], ECardType.offer);

            return (
                <StubReduxProvider
                    stub={{
                        favorites,
                        internal: {
                            ...internal,
                            expFlags: {
                                PRODUCTS_card_thumb_increase: 1,
                                PRODUCTS_card_title_two_rows: 1,
                            },
                        } }}>
                    <div style={wrapperStyle}>
                        <ProductCard
                            {...defaultProps}
                            title={title}
                            rating={rating}
                            price={{
                                type: 'exact',
                                currency: 'RUR',
                                current: '3690',
                            }}
                            priceLabel={priceLabel}
                            type={type}
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('expAloneOfferSku', ProductCard => {
            const priceLabel = text('priceLabel', '');
            const title = text('title', defaultProps.title);
            const rating = number('rating', 4.5);
            const offersCount = number('offersCount', 1);

            return (
                <StubReduxProvider
                    stub={{ favorites, internal: {
                        ...internal,
                        expFlags: {
                            PRODUCTS_sku_aloneoffer_shoptitle: 1,
                        },
                    } }}>
                    <div style={wrapperStyle}>
                        <ProductCard
                            {...defaultProps}
                            title={title}
                            rating={rating}
                            offersCount={offersCount}
                            price={{
                                type: 'exact',
                                currency: 'RUR',
                                current: '3690',
                            }}
                            priceLabel={priceLabel}
                            type={ECardType.sku}
                        />
                    </div>
                </StubReduxProvider>
            );
        })
        .add('expOfferAsSku', ProductCard => {
            const min = number('min', 86790);
            const max = number('max', 90000);
            const priceLabel = text('priceLabel', '');
            const offersCount = number('offersCount', 4);
            const rating = number('rating', 4.5);

            return (
                <StubReduxProvider
                    stub={{ favorites, internal: {
                        ...internal,
                        expFlags: { PRODUCTS_offer_as_sku: 1 }
                    } }}>
                    <div style={wrapperStyle}>
                        <ProductCard
                            {...defaultProps}
                            offersCount={offersCount}
                            rating={rating}
                            price={{
                                type: 'range',
                                currency: 'RUR',
                                min: min.toString(),
                                max: max.toString(),
                            }}
                            priceLabel={priceLabel}
                            type={ECardType.offer}
                            sellerName="wildberries"
                        />
                    </div>
                </StubReduxProvider>
            );
        });
});
