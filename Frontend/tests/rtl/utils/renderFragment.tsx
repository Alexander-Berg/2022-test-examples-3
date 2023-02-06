import { render, RenderResult } from '@testing-library/react';
import { createMemoryHistory, MemoryHistory, MemoryHistoryOptions } from 'history';
import React from 'react';
import { act } from 'react-dom/test-utils';
import { Router } from 'react-router-dom';
import { PageFragment } from './fragments/PageFragment';

export interface RenderParams {
    initialEntries?: MemoryHistoryOptions['initialEntries']
}

export interface IRenderFragmentResult<T extends PageFragment> {
    content: T,
    history: MemoryHistory,
}

export async function renderFragment<T extends PageFragment>(
    Fragment:  new (container: Element) => T,
    component: React.ReactElement,
    params: RenderParams = {}
): Promise<IRenderFragmentResult<T>> {
    const memoryHistory = createMemoryHistory({
        initialEntries: params?.initialEntries || ['/'],
        initialIndex: 0,

    });

    window.IntersectionObserver = jest.fn().mockImplementation(() => ({
        observe: jest.fn(),
        unobserve: jest.fn(),
        disconnect: jest.fn(),
    }));

    let rendered: RenderResult | null = null;

    await act(async () => {
        rendered = await render(
            <Router history={memoryHistory}>
                {component}
            </Router>
        );
    });

    if (!rendered) {
        throw new Error('Container is undefined');
    }

    return {
        content: new Fragment(rendered!.container),
        history: memoryHistory
    };
}
