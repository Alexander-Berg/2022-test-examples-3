import React from 'react';
import type { CSSProperties } from 'react';
import { number, text } from '@storybook/addon-knobs';

import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import type { IInternalState } from '@src/store/services/internal/types';
import { getInternalInitialState } from '@src/store/services/internal/reducer';
import { StubReduxProvider } from '@src/storybook/stubs/StubReduxProvider';
import { RatingLine } from '../index';

function Wrapper({ children, smallWidth }: React.PropsWithChildren<{ smallWidth?: boolean | string }>) {
    const styles: CSSProperties = { display: 'inline-block' };

    if (smallWidth) {
        styles.width = typeof smallWidth === 'string' ? smallWidth : '100px';
    }

    return (
        <div style={styles} className="story-wrapper">
            {React.Children.map(children, item => (
                <div style={{ margin: '2px 0' }}>
                    {item}
                </div>
            ))}
        </div>
    );
}

const internal: IInternalState = {
    ...getInternalInitialState(),
    expFlags: {
        enable_rating: 1,
        PRODUCTS_enable_reviews: 1,
    },
};

createPlatformStories('Tests/RatingLine', RatingLine, stories => {
    stories
        .addDecorator(withStaticRouter())
        .add('showcase_without_reviews', Component => (
            <StubReduxProvider stub={{ internal }}>
                <Wrapper>
                    <Component value={number('rating', 4)} />
                    <Component value={3} />
                    <Component value={1} />
                    <Component value={0.5} />
                </Wrapper>
            </StubReduxProvider>
        ))
        .add('showcase_with_reviews', Component => (
            <StubReduxProvider stub={{ internal }}>
                <Wrapper>
                    <Component
                        value={number('rating', 4)} reviews={1}
                        productId={text('example_product_id', '845909010')} />
                    <Component value={3} reviews={3} productId={text('example_product_id', '845909010')} />
                    <Component value={1} reviews={4} productId={text('example_product_id', '845909010')} />
                    <Component value={0.5} reviews={5} productId={text('example_product_id', '845909010')} />
                </Wrapper>
            </StubReduxProvider>
        ))
        .add('small_container', Component => (
            <StubReduxProvider stub={{ internal }}>
                <Wrapper smallWidth>
                    <Component
                        value={number('rating', 4)} reviews={5} productId={text('example_product_id', '845909010')} />
                    <Component
                        value={number('rating', 4)} reviews={52478}
                        productId={text('example_product_id', '845909010')} />
                </Wrapper>
            </StubReduxProvider>
        ));
});
