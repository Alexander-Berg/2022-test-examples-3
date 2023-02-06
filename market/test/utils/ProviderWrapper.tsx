import { Provider as ReatomProvider } from '@reatom/react';
import React, { ReactNode } from 'react';
import { createStore, Store as ReatomStore } from '@reatom/core';

interface ProviderWrapperProps {
  reatomStore?: ReatomStore;
}

export const getProviderWrapper = ({ reatomStore }: ProviderWrapperProps) => {
  const defaultReatomStore = createStore();

  return ({ children }: { children: ReactNode }) => (
    <ReatomProvider value={reatomStore || defaultReatomStore}>{children}</ReatomProvider>
  );
};
