import * as React from 'react';
import { action } from '@storybook/addon-actions';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import type { IQuickFiltersItemProps } from '../components/QuickFiltersItem/QuickFiltersItem.typings';
import { QuickFiltersItem } from '../components/QuickFiltersItem';
import { QuickFilters } from '../index';

const filtersList: IQuickFiltersItemProps[] = [{
    id: '1',
    text: 'Цена',
    type: 'number',
    active: true,
}, {
    id: '2',
    text: 'Бренд',
    type: 'enum',
    active: false,
}, {
    id: '3',
    text: 'Цвет',
    type: 'enum',
    active: true,
}];

createPlatformStories('Tests/QuickFilters', QuickFilters, stories => {
    stories
        .add('showcase', QuickFilters => {
            return (
                <QuickFilters>
                    {
                        filtersList.map(item => (
                            <QuickFiltersItem
                                key={item.id}
                                onClick={action('ClickItem-ToFilter')}
                                onClear={action('ClickItem-Clear')}
                                {...item}
                            />
                        ))
                    }
                </QuickFilters>
            );
        });
});
