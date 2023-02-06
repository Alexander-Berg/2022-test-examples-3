import * as React from 'react';
import { number } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Rating } from '../index';

function Wrapper({ children }: React.PropsWithChildren<{}>) {
    return (
        <div style={{ display: 'inline-block' }} className="story-wrapper">
            {React.Children.map(children, item => (
                <div style={{ margin: '2px 0' }}>
                    {item}
                </div>
            ))}
        </div>
    );
}

createPlatformStories('Tests/Rating', Rating, stories => {
    stories
        .add('showcase', Rating => (
            <Wrapper>
                <Rating value={number('rating', 4)} />
                <Rating value={3} />
                <Rating value={1} />
                <Rating value={0.5} />
            </Wrapper>
        ));
});
