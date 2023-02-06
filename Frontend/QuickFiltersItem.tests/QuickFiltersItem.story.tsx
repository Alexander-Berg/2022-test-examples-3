import * as React from 'react';
import { withKnobs, text, boolean } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { QuickFiltersItem } from '../index';

createPlatformStories('Tests/QuickFilters/Item', QuickFiltersItem, stories => {
    stories
        .addDecorator(withKnobs)
        .add('showcase', QuickFiltersItem => {
            return (
                <QuickFiltersItem
                    id="example"
                    text={text('text', 'Название фильтра')}
                    active={boolean('active', false)}
                    type="number"
                    component="div"
                />
            );
        });
});
