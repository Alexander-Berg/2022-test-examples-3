import React from 'react';
import { withKnobs } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Kebab, KebabItem } from '..';

createPlatformStories('Tests/Kebab', Kebab, stories => {
    stories
        .addDecorator(withKnobs)
        .add('plain', Component => {
            const items = ['menu item 1', 'menu item 2', 'menu item with some text'];
            return (
                <div>
                    <Component>
                        {items.map(text => <KebabItem key={text}>{text}</KebabItem>)}
                    </Component>
                </div>
            );
        })
        .add('withSingleMenuItem', Component => {
            return (
                <div>
                    <Component>
                        <KebabItem>single menu item</KebabItem>
                    </Component>
                </div>
            );
        });
});
