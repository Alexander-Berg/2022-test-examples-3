import React from 'react';
import { DataSourceCtx } from 'neo/contexts/DataSourceCtx';
import { ApplicationCtx } from 'neo/contexts/ApplicationCtx';
import { dataSourceCtxStub, applicationCtxStub } from 'neo/tests/stubs/contexts';

export const Context = (children: () => JSX.Element) => {
  return (
    <ApplicationCtx.Provider value={applicationCtxStub}>
      <DataSourceCtx.Provider value={dataSourceCtxStub}>
        {children()}
      </DataSourceCtx.Provider>
    </ApplicationCtx.Provider>
  );
};
