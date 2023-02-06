import React, { ReactNode, createContext } from 'react';
import { renderHook } from '@testing-library/react-hooks';

import { useComponentContext } from '../useComponentContext';

// @ts-ignore
const ParentComponentContext = createContext(null as { test: boolean });

describe('useComponentContext', () => {
    it("should get passed component's context", () => {
        const wrapper = ({ children }: { children?: ReactNode }) => (
            <ParentComponentContext.Provider value={{ test: true }}>{children}</ParentComponentContext.Provider>
        );

        const { result } = renderHook(
            () => useComponentContext(ParentComponentContext, '.ParentComponent', '.ChildComponent'),
            { wrapper },
        );

        expect(result.current.test).toEqual(true);
    });

    it('should give an error when no context were provided', () => {
        const wrapper = ({ children }: { children?: ReactNode }) => (
            <>{children}</>
        );
        const { result } = renderHook(
            () => useComponentContext(ParentComponentContext, '.ParentComponent', '.ChildComponent'),
            { wrapper },
        );

        expect(result.error).toBeDefined();
    });
});
