import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopImage } from '../Image@desktop';
import { TouchImage } from '../Image@touch';
import { TImagePropsData } from '../Image.types';

const dataList: TImagePropsData[] = [
    {
        size: 'large',
        imageLinks: {
            imgLink: 'https://avatars.mds.yandex.net/get-adv/114583/2a000001803c3cdbb0617000d0a05c250b59/orig',
        },

    },
    {
        size: 'medium',
        imageLinks: {
            imgLink: 'https://avatars.mds.yandex.net/get-adv/114583/2a000001803c3cdbb0617000d0a05c250b59/orig',
        },
    },
    {
        size: 'small',
        imageLinks: {
            imgLink: 'https://avatars.mds.yandex.net/get-adv/114583/2a000001803c3cdbb0617000d0a05c250b59/orig',
        },
    },
];

const customStyle = {
    display: 'grid',
    alignItems: 'center',
    gridGap: '20rem',
    background: '#fff',
    color: '#1B202B',
    padding: '20rem',
};

new ComponentStories(module, 'Card', { desktop: DesktopImage })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('Image', Component => (
        <div style={customStyle} className="test">
            {
                dataList.map(data => (
                    <div key={`${data.size}-${data.imageLinks.imgLink}`}>
                        <p> <b>Size:</b> {data.size}</p>
                        <React.Fragment>
                            <Component data={data} />
                        </React.Fragment>
                    </div>
                ),
                )
            }
        </div>
    ));

new ComponentStories(module, 'Card', { 'touch-phone': TouchImage })
    .addDecorator(withPlatform(Platform.Touch))
    .add('Image', Component => (
        <div style={customStyle} className="test">
            {
                dataList.map(data => (
                    <div key={`${data.size}-${data.imageLinks.imgLink}`}>
                        <p> <b>Size:</b> {data.size}</p>
                        <React.Fragment>
                            <Component data={data} />
                        </React.Fragment>
                    </div>
                ),
                )
            }
        </div>
    ));
