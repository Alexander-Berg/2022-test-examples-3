import * as React from 'react';
import { text, select, boolean } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Icon, withStarts } from '@src/components/Icon';
import type { IPromotionIncutProps } from '../PromotionIncut.typings';
import {
    PromotionIncut,
    PromotionIncutTitle,
    PromotionIncutText,
    useButtonClose,
} from '../index';

const IconStars = withStarts(Icon);
const wrapperStyle = {
    maxWidth: 344,
};

const THEMES: Record<string, IPromotionIncutProps['theme']> = {
    default: 'default',
    purple: 'purple',
    red: 'red',
};

createPlatformStories('Tests/PromotionIncut', PromotionIncut, stories => {
    stories
        .add('showcase', PromotionIncut => {
            const [visible, handleClose] = useButtonClose();

            if (!visible) {
                return null;
            }

            const title = text('title', 'Раздел «Товары» в Яндексе');
            const textIncut = text('text', 'Сравнивайте цены в разных магазинах и покупайте выгодно');

            return (
                <div style={wrapperStyle}>
                    <PromotionIncut
                        onClose={boolean('withClose', true) ? handleClose : undefined}
                        icon={boolean('withIcon', true) ? IconStars : undefined}
                        theme={select('theme', THEMES, 'default')}
                    >
                        { title ? (<PromotionIncutTitle>{title}</PromotionIncutTitle>) : null }
                        { textIncut ? (<PromotionIncutText>{textIncut}</PromotionIncutText>) : null }
                    </PromotionIncut>
                </div>
            );
        });
});
