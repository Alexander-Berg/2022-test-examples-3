import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopHtmlContent } from '../HtmlContent@desktop';
import { TouchHtmlContent } from '../HtmlContent@touch';
import { THtmlContentType } from '../HtmlContent.types';

const datas: THtmlContentType[] = [
    'h1', 'h2', 'h3', 'h4', 'button', 'menu-tabs', 'news', 'text1', 'text2', 'text3',
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

new ComponentStories(module, 'Tests|HelpComponent', { desktop: DesktopHtmlContent })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('HtmlContent', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={data}>
                        <Component content={data} type={data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));

new ComponentStories(module, 'Tests|HelpComponent', { 'touch-phone': TouchHtmlContent })
    .addDecorator(withPlatform(Platform.Touch))
    .add('HtmlContent', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={data}>
                        <Component content={data} type={data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));
