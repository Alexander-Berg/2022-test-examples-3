import React from 'react';
import { number } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { TextCut } from '..';

createPlatformStories('Tests/TextCut', TextCut, stories => {
    stories
        .add('plain', TextCut => {
            return (
                <TextCut
                    maxLength={number('maxLength', 200)}
                    maxLineBreaks={number('maxLineBreaks', 6)}
                >
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit,
                    sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
                    Ut enim ad minim veniam, quis nostrud exercitation ullamco
                    laboris nisi ut aliquip ex ea commodo consequat.
                    Duis aute irure dolor in reprehenderit in voluptate velit
                    esse cillum dolore eu fugiat nulla pariatur.
                    Excepteur sint occaecat cupidatat non proident,
                    sunt in culpa qui officia deserunt mollit anim id est laborum.
                </TextCut>
            );
        })
        .add('enableHideText', TextCut => {
            return (
                <TextCut
                    maxLength={number('maxLength', 200)}
                    maxLineBreaks={number('maxLineBreaks', 6)}
                    enableHideText
                >
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit,
                    sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
                    Ut enim ad minim veniam, quis nostrud exercitation ullamco
                    laboris nisi ut aliquip ex ea commodo consequat.
                    Duis aute irure dolor in reprehenderit in voluptate velit
                    esse cillum dolore eu fugiat nulla pariatur.
                    Excepteur sint occaecat cupidatat non proident,
                    sunt in culpa qui officia deserunt mollit anim id est laborum.
                </TextCut>
            );
        });
});
