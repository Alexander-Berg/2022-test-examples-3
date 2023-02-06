import { IApplicationCtx } from 'neo/types/contexts/IApplicationCtx';

export const applicationCtxStub: IApplicationCtx = {
  neo: {
    isFontLoaded: true,
    generateId: (): string => {
      return 'u-1590573235493-0';
    },
    flags: {},
  },
};
