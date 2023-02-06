import React from 'react';
import type { CSSProperties } from 'react';
import { number, withKnobs } from '@storybook/addon-knobs';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { rmgProcessStrategy, mapRmgTemplateToSearch } from '@src/schema/rmg/utils';
import type { IRMGItem } from '@src/typings/rmg';
import type { IRMGTemplateItem } from '@src/schema/rmg/types';
import { StubReduxProvider } from '@src/storybook/stubs/StubReduxProvider';
import type { IEntitiesState } from '@src/store/services/entities/types';
import { getEntitiesInitialState } from '@src/store/services/entities/reducer';

import type { RichMediaGalleryProps } from '../RichMediaGallery.typings';
import { RichMediaGallery } from '..';

import './RichMediaGallery.story.scss';

const { smart_center: mock } = require('./yabsMock');

const defaultProps: RichMediaGalleryProps = {
    className: 'RichMediaGalleryStory',
    productCardClassName: 'RichMediaGalleryStory-ProductCard',
    items: mock.map(mapRmgTemplateToSearch),
    visibilityCountUrls: [],
};

const reduceRmgTemplateToEntities = (acc: Record<string, IRMGItem> = {}, item: IRMGTemplateItem) => {
    acc[item.object_id] = rmgProcessStrategy(item);
    return acc;
};

const entities: IEntitiesState = {
    ...getEntitiesInitialState(),
    rmg: mock.reduce(reduceRmgTemplateToEntities, {}),
};

const containerStyle: CSSProperties = {
    maxWidth: 600,
    padding: 2,
    border: '2px solid #eee',
};

createPlatformStories('Tests/RichMediaGallery', RichMediaGallery, stories => {
    stories
        .addDecorator(withKnobs)
        .addDecorator(withStaticRouter())
        .add('plain', Component => {
            const thumbnailOptions = {
                width: number('image-width', 148),
                height: number('image-height', 120),
            };

            const props = {
                ...defaultProps,
                thumbnailOptions,
            };

            return (
                <StubReduxProvider stub={{ entities }}>
                    <div style={containerStyle}>
                        <Component {...props} />
                    </div>
                </StubReduxProvider>
            );
        });
});
