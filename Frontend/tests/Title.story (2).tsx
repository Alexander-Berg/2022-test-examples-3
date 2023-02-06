import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';
import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { Title as TitleDesktop } from '../Title@desktop';
import { Title as TitleTouch } from '../Title@touch';

new ComponentStories('Tests|Title', { desktop: TitleDesktop })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('plain', Component => (
        <Component>Hello storybook</Component>
    ));

new ComponentStories('Tests|Title', { 'touch-phone': TitleTouch })
    .addDecorator(withPlatform(Platform.Touch))
    .add('plain', Component => (
        <Component>Hello storybook</Component>
    ));
