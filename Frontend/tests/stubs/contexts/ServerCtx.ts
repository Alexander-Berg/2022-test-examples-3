import { IServerCtx } from 'neo/types/contexts';
import { EPlatform } from 'neo/types/EPlatform';

export const serverCtxStub: IServerCtx = {
  neo: {
    ctxRR: {
      findLastItem: (): {} => {
        return {};
      },
    },
    flags: {},
    platform: EPlatform.PHONE,
    isYandex: true,
    browserInfo: {
      os: {},
      browser: {},
    },
  },
} as unknown as IServerCtx;
