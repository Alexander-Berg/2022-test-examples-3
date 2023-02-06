import React from 'react';
import { number, text, withKnobs } from '@storybook/addon-knobs';

import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { ListCut } from '../index';
import type { IListCutProps, TListCutItemRender } from '../ListCut.typings';

const items = Array.from(Array(5)).map((_, i) => `Элемент ${i + 1}`);

const renderItem: TListCutItemRender<string> = (({ item }) => <div>{item}</div>);

createPlatformStories<IListCutProps<string>>('Tests/ListCut', ListCut, stories => {
    stories
        .addDecorator(withKnobs)
        .add('plain', ListCut => (
            <ListCut
                items={items}
                renderItem={renderItem}
                initialCount={number('initialCount', 2)}
                step={number('step', 0)}
                moreText={text('showMoreText', 'Показать ещё')}
                hideText={text('hideText', '')}
            />
        ));
});
