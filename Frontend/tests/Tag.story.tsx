import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopTag } from '../Tag@desktop';
import { TouchTag } from '../Tag@touch';
import { TTag } from '../Tag.types';

const datas: TTag[] = [
    {
        label: 'Customer Stories',
        theme: 'blue',
    },
    {
        label: 'Попробовать демо',
        theme: 'pink',
    },
    {
        label: 'Попробовать демо',
        theme: 'purple',
    },
    {
        label: 'Logs API',
        theme: 'dark',
    },
];
const customStyle = {
    display: 'grid',
    gridTemplateColumns: ' 200rem 450rem',
    alignItems: 'center',
    gridGap: '20rem',
    background: '#1F2533',
    padding: '20rem',
    color: '#fff',
};

new ComponentStories(module, 'Tests|HelpComponent', { desktop: DesktopTag })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('Tag', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={data.label}>
                        <div>
                            <p> <b>Тема:</b> {data.theme}</p>
                        </div>
                        <Component {...data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));

new ComponentStories(module, 'Tests|HelpComponent', { 'touch-phone': TouchTag })
    .addDecorator(withPlatform(Platform.Touch))
    .add('Tag', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={data.label}>
                        <div>
                            <p> <b>Тема:</b> {data.theme}</p>
                        </div>
                        <Component {...data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));
