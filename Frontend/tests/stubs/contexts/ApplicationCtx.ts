import { IApplicationCtx } from 'neo/types/contexts/IApplicationCtx';

export const applicationCtxStub: IApplicationCtx = {
  neo: {
    generateId: (): string => {
      return 'u-1590573235493-0';
    },
    flags: {},
  },
};
