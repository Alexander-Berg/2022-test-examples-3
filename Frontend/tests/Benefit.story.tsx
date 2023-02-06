import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopSimpleCard } from '../SimpleCard@desktop';
import { TouchSimpleCard } from '../SimpleCard@touch';
import { TSimpleCardPropsData } from '../SimpleCard.types';

const dataList: TSimpleCardPropsData[] = [
    {
        size: 'medium',
        type: 'benefit',
        theme: 'dark',
        title: '120 млрд',
        description: 'событий в сутки',
    },
    {
        size: 'medium',
        type: 'benefit',
        theme: 'dark',
        description: 'Всегда онлайн',
    },
    {
        size: 'medium',
        type: 'benefit',
        theme: 'dark',
        title: '40 000',
        description: 'приложений',
    },
    {
        size: 'small',
        type: 'benefit',
        theme: 'dark',
        title: 'Events',
        description: '125 634 658 (+10%)',
    },
];

const customStyle = {
    display: 'grid',
    alignItems: 'center',
    gridGap: '20rem',
    background: '#1B202B',
    color: '#fff',
    padding: '20rem',
};

new ComponentStories(module, 'SimpleCard', { desktop: DesktopSimpleCard })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('Benefit', Component => (
        <div style={customStyle} className="test">
            {
                dataList.map(data => (
                    <div key={`${data.size}-${data.title}`}>
                        <p> <b>size:</b> {data.size}</p>
                        <React.Fragment>
                            <Component data={data} />
                        </React.Fragment>
                    </div>
                ),
                )
            }
        </div>
    ));

new ComponentStories(module, 'SimpleCard', { 'touch-phone': TouchSimpleCard })
    .addDecorator(withPlatform(Platform.Touch))
    .add('Benefit', Component => (
        <div style={customStyle} className="test">
            {
                dataList.map(data => (
                    <div key={`${data.size}-${data.title}`}>
                        <p> <b>size:</b> {data.size}</p>
                        <React.Fragment key={`${data.theme}-${data.type}`}>
                            <Component data={data} />
                        </React.Fragment>
                    </div>
                ),
                )
            }
        </div>
    ));
