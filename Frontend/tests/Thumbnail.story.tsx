import { number, boolean } from '@storybook/addon-knobs';
import React from 'react';

import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { withStubReduxProvider } from '@src/storybook/decorators/withStubReduxProvider';
import { Platform } from '@src/typings/platform';
import { Thumbnail } from '../index';
import type { IThumbnailProps } from '../Thumbnail.typings';

const defaultProps: IThumbnailProps = {
    src: '//avatars.mds.yandex.net/get-mpic/4754204/img_id4085559111223247146.png/100x100',
    srcHd: '//avatars.mds.yandex.net/get-mpic/4754204/img_id4085559111223247146.png/200x200',
};

interface IWrapperProps {
    width: number;
    height: number;
}

const Wrapper: React.FC<IWrapperProps> = ({ children, width, height }) => {
    return (
        <div style={{ width, height }}>
            {children}
        </div>
    );
};

createPlatformStories('Tests/Thumbnail', Thumbnail, (stories, platform) => {
    const defaultWrapperProps: IWrapperProps = platform === Platform.Desktop ?
        { width: 255, height: 255 } :
        { width: 187, height: 168 };

    const defaultCustomSize: IWrapperProps = platform === Platform.Desktop ?
        { width: 168, height: 168 } :
        { width: 144, height: 128 };

    stories
        .addDecorator(withStubReduxProvider())
        .add('plain', Thumbnail => {
            const width = number('width', defaultWrapperProps.width);
            const height = number('height', defaultWrapperProps.height);
            const disabled = boolean('disabled', false);

            return (
                <Wrapper width={width} height={height}>
                    <Thumbnail
                        {...defaultProps}
                        disabled={disabled}
                    />
                </Wrapper>
            );
        })
        .add('fallback', Thumbnail => {
            return (
                <Wrapper {...defaultWrapperProps}>
                    <Thumbnail />
                </Wrapper>
            );
        })
        .add('customSize', Thumbnail => {
            const width = number('width', defaultCustomSize.width);
            const height = number('height', defaultCustomSize.height);

            return (
                <Thumbnail {...defaultProps} width={width} height={height} />
            );
        })
        .add('customSizeFallback', Thumbnail => {
            return (
                <Thumbnail width={defaultCustomSize.width} height={defaultCustomSize.height} />
            );
        });
});
