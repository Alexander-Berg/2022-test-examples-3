import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopListCard } from '../ListCard@desktop';
import { TouchListCard } from '../ListCard@touch';
import { TListCardPropsData } from '../ListCard.types';

const customStyleDesktop = {
    display: 'grid',
    backgroundColor: '#252A36',
    padding: '80rem',
    height: '1000rem',
};

const data: TListCardPropsData = {
    items: [
        {
            title: 'ARPPU',
            color: '#498BFF',
        },
        {
            title: 'AOV',
            color: '#494AC2',
        },
        {
            title: 'ARPU',
            color: '#6E30E8',
        },
        {
            title: '1',
            color: '#6E30E8',
        },
        {
            title: '2',
            color: '#6E30E8',
        },
        {
            title: '3',
            color: '#6E30E8',
        },
    ],
};

new ComponentStories(module, 'ListCard', { desktop: DesktopListCard })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('ListCard', Component => (
        <div style={customStyleDesktop} className="test">
            <Component data={data} />
        </div>
    ));

const customStyleTouch = {
    width: '375rem',
    backgroundColor: '#252A36',
    height: '1000rem',
    padding: '20rem',
};

new ComponentStories(module, 'ListCard', { 'touch-phone': TouchListCard })
    .addDecorator(withPlatform(Platform.Touch))
    .add('ListCard', Component => (
        <div style={customStyleTouch} className="test">
            <Component data={data} />
        </div>
    ));
