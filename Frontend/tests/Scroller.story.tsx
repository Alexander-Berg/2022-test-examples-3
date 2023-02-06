import React from 'react';
import type { CSSProperties } from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Scroller } from '../index';

const containerStyle: CSSProperties = {
    maxWidth: 600,
    padding: 2,
    border: '2px solid #eee',
};

const cardStyle: CSSProperties = {
    flexShrink: 0,
    width: 100,
    height: 100,
    display: 'inline-flex',
    justifyContent: 'center',
    alignItems: 'center',
    border: '1px solid grey',
};

const cards = Array.from({ length: 10 }).map((_, i) => {
    return (<div key={i} style={cardStyle}>{i}</div>);
});

createPlatformStories('Tests/Scroller', Scroller, stories => {
    stories
        .add('plain', Component => (
            <div style={containerStyle}>
                <Component scrollStep={1}>
                    {cards}
                </Component>
            </div>
        ));
});
