import * as React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Icon, withArrow, withVerified, withCheckOutline } from '@src/components/Icon';
import { ListItem } from '../index';

const IconArrow = withArrow(Icon);
const IconVerified = withVerified(Icon);
const IconCheck = withCheckOutline(Icon);

createPlatformStories('Tests/ListItem', ListItem, stories => {
    const size: React.CSSProperties = { width: 380 };

    stories
        .add('showcase', ListItem => {
            return (
                <div style={size}>
                    <ListItem rightIcon={IconArrow} leftIcon={IconVerified}>
                        Магазин проверен Яндексом
                    </ListItem>
                    <ListItem rightIcon={IconArrow} leftIcon={IconCheck}>
                        Более 1000 человек знают магазин <br />
                        Более 10 покупок за март
                    </ListItem>
                </div>
            );
        });
});
