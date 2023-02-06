import * as React from 'react';
import { ActionsMap } from '@storybook/addon-actions';

import { BaseStory } from 'tests/storybook';

export interface StorybookContainerProps {
    storybookClassName?: string;
    storybookWidth?: string;
    storybookHeight?: string;
    storybookStyles?: React.CSSProperties;
    children?: React.ReactNode;
}

const StorybookContainer: React.FC<StorybookContainerProps> = function StorybookContainer({
    storybookClassName,
    storybookWidth,
    storybookHeight,
    storybookStyles,
    children,
}) {
    return (
        <div
            className={storybookClassName}
            style={{ width: storybookWidth, height: storybookHeight, ...storybookStyles }}
        >
            {children}
        </div>
    );
};

type ReactComponentType<T = any> =
    | React.FC<T>
    | React.ComponentClass<T>
    | React.ForwardRefExoticComponent<React.PropsWithoutRef<T> & React.RefAttributes<any>>;

export function createStorybookTemplate<T, S extends ReactComponentType<T> = React.FC<T>>(
    StoryComponent: S,
    events?: ActionsMap,
): BaseStory<T & StorybookContainerProps> & { bind: (...args: any) => BaseStory<T & StorybookContainerProps> } {
    return function storyTemplate({
        storybookClassName,
        storybookWidth,
        storybookHeight,
        storybookStyles,
        ...otherProps
    }: T & StorybookContainerProps) {
        return (
            <StorybookContainer
                storybookClassName={storybookClassName}
                storybookWidth={storybookWidth}
                storybookHeight={storybookHeight}
                storybookStyles={storybookStyles}
            >
                <StoryComponent
                    {...(otherProps as any)}
                    {...events}
                />
            </StorybookContainer>
        );
    };
}
