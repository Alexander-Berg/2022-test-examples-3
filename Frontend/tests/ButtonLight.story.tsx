import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopButton } from '../Button@desktop';
import { TouchButton } from '../Button@touch';
import { TButton } from '../Button.types';

const datas: TButton[] = [
    {
        label: 'Получить доступ',
        theme: 'light',
        type: 'primary',
    },
    {
        label: 'Получить доступ',
        theme: 'light',
        type: 'primary',
        disabled: true,
    },
    {
        label: 'Попробовать демо',
        theme: 'light',
    },
    {
        label: 'Попробовать демо',
        theme: 'light',
        disabled: true,
    },
    {
        label: 'Получить доступ',
        theme: 'light',
        type: 'link',
    },
    {
        label: 'Получить доступ',
        theme: 'light',
        type: 'link',
        disabled: true,
    },
    {
        label: 'Скачать',
        theme: 'light',
        type: 'download',
    },
    {
        label: 'Скачать',
        theme: 'light',
        type: 'download',
        disabled: true,
    },
    {
        label: 'Аналитика доходности',
        theme: 'light',
        type: 'tabs',
    },
    {
        label: '',
        theme: 'light',
        type: 'back',
    },
    {
        label: '',
        theme: 'light',
        type: 'back',
        disabled: true,
    },
    {
        label: '',
        theme: 'light',
        type: 'up',
    },
    {
        label: '',
        theme: 'light',
        type: 'up',
        disabled: true,
    },
];
const customStyle = {
    display: 'grid',
    gridTemplateColumns: ' 200rem 450rem',
    alignItems: 'center',
    gridGap: '20rem',
    background: '#fff',
    padding: '20rem',
};

new ComponentStories(module, 'Tests|HelpComponent', { desktop: DesktopButton })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('ButtonLight', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={`${data.theme}-${data.type}-${data.disabled}`}>
                        <div>
                            <p> <b>Тема:</b> {data.theme}</p>
                            <p> <b>Тип кнопки:</b> {data.type}</p>
                            <p> <b>Disabled:</b> {data.disabled ? 'Да' : 'Нет'}</p>
                        </div>
                        <Component {...data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));

new ComponentStories(module, 'Tests|HelpComponent', { 'touch-phone': TouchButton })
    .addDecorator(withPlatform(Platform.Touch))
    .add('ButtonLight', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={`${data.theme}-${data.type}-${data.disabled}`}>
                        <div>
                            <p> <b>Тема:</b> {data.theme}</p>
                            <p> <b>Тип кнопки:</b> {data.type}</p>
                            <p> <b>Disabled:</b> {data.disabled ? 'Да' : 'Нет'}</p>
                        </div>
                        <Component {...data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));
