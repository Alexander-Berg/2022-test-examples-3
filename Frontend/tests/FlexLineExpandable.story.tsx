import React, { useMemo, useCallback } from 'react';
import {
    select,
    number,
} from '@storybook/addon-knobs';
import { action } from '@storybook/addon-actions';

import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';

import { FlexLineExpandable as FlexLineExpandableCommon } from '../FlexLineExpandable';

import './FlexLineExpandable.story.scss';

function createItem(text: string, options?: { hidden?: boolean; isMoreButton?: boolean }): JSX.Element {
    return (
        <div
            className={[
                options?.hidden ? 'FlexStory-Hidden' : '',
                options?.isMoreButton ? 'FlexStory-More' : 'FlexStory-Item',
            ].filter(Boolean).join(' ')}
        >
            {text}
        </div>
    );
}

const allItems = ['элемент 1', 'элемент 2', 'элемент 3', 'элемент 4', 'элемент 5'];
const long = allItems.join(' ');
const itemsMap = {
    '0': [],
    '1': allItems.slice(0, 1),
    '2': allItems.slice(0, 2),
    '3': allItems.slice(0, 3),
    '4': allItems.slice(0, 4),
    '5': allItems,
    many: [...allItems, ...allItems, ...allItems, ...allItems],
    'long 1': [long],
    'long 2': [long, long],
    'long 3': [long, long, long],
};

createPlatformStories('Tests/FlexLineExpandable', FlexLineExpandableCommon, stories => {
    stories
        .add('plain', FlexLineExpandable => {
            const items = select('items', itemsMap, allItems);
            const margin = number('margin', 6);
            const itemMinWidth = number('itemMinWidth', 30);
            const lines = number('lines', 1);

            const renderItems = useMemo(() => items.map(item => {
                return (hidden?: boolean) => createItem(item, { hidden });
            }), [items]);
            const renderMore = useCallback((hiddenItemsCount: number, hidden?: boolean) => {
                return createItem(`+ ${hiddenItemsCount}`, { hidden, isMoreButton: true });
            }, []);

            return (
                <div className="FlexStory" style={{ width: '300px' }}>
                    <FlexLineExpandable
                        items={renderItems}
                        renderMore={renderMore}
                        margin={margin}
                        itemMinWidth={itemMinWidth}
                        lines={lines}

                        onExpand={action('onExpand')}
                    />
                </div>
            );
        });
});
