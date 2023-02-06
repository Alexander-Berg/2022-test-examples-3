import React from 'react';
import { MemoryRouter, MemoryRouterProps } from 'react-router-dom';

export interface WithStorybookRouterOptions {
    router?: MemoryRouterProps;
}

export function withStorybookRouter(options: WithStorybookRouterOptions = {}) {
    return (Story) => {
        return (
            <MemoryRouter {...options.router}>
                <Story />
            </MemoryRouter>
        );
    };
}
