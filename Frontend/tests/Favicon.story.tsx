import React from 'react';
import { withKnobs } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Favicon } from '..';

const Wrapper: React.FC = props => (
    <div style={{ maxWidth: 250, display: 'flex' }}>
        {props.children}
    </div>
);

createPlatformStories('Tests/Favicon', Favicon, stories => {
    stories
        .addDecorator(withKnobs)
        .add('favicon-set', Component => {
            return (
                <div className="favicon-set">
                    <Wrapper>
                        <Component hostname="1pad.ru" />&nbsp;&nbsp;default
                    </Wrapper>
                    <Wrapper>
                        <Component />&nbsp;&nbsp;fallback
                    </Wrapper>
                    <Wrapper>
                        <Component hostname={'not-valid-shop-without-favicon'} />&nbsp;&nbsp;broken
                    </Wrapper>
                    <Wrapper>
                        <Component hostname={'ololo.ru'} />&nbsp;&nbsp;unknown domain
                    </Wrapper>
                </div>
            );
        });
});
