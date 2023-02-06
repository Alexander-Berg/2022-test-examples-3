import { URL } from 'url';
import { EService } from 'neo/types/EService';
import { serverCtxStub as serverCtxStubNeo } from 'neo/tests/stubs/contexts';
import { IServerCtx, IServerCtxInner as ISportServerCtxInner } from 'sport/types/contexts/IServerCtx';
import { itemMap as ITEM_MAP } from 'sport/tests/stubs/contexts/item/itemMap';
import { IRequest } from 'mg/types/apphost';
import { INewsRequest } from 'mg/types/apphost/news_request';
import { IServerCtxInner } from 'neo/types/contexts';
import { ICtxRR } from 'neo/types/report-renderer';
import { EPage } from 'sport/types/EPage';
import { TFlagsContainer } from 'sport/experiments/flags';

type TNeo = Omit<Partial<IServerCtx['neo']>, 'flags'> & {
  flags?: Partial<TFlagsContainer>
};

export function getServerCtxStub(
  args?: {
    findLastItemArgs?: Record<string, string>,
    specialArgs?: {
      neo?: TNeo;
      sport?: Partial<IServerCtx['sport']>;
    },
    additionalItemMap?: Record<string, object>,
  },
): IServerCtx {
  const itemMap = args?.additionalItemMap
    ? { ...ITEM_MAP, ...args.additionalItemMap }
    : ITEM_MAP;

  return {
    neo: {
      ...serverCtxStubNeo.neo,
      page: EPage.MAIN,
      ctxRR: {
        findLastItem: (type: string) => itemMap[args?.findLastItemArgs?.[type] || type],
        getItems: (type: string) => itemMap[args?.findLastItemArgs?.[type] || type] || [],
      } as ICtxRR,
      service: EService.SPORT,
      url: new URL('https://yandex.ru/sport'),
      reqid: '1590572947960507-788120642695967590200098-priemka-stable-sport-app-host-2-SPORT-SPORT_INDEX',
      flags: {} as IServerCtxInner['flags'],
      timestamp: 1647888754000,
      ...(args?.specialArgs?.neo as Partial<IServerCtx['neo']> || {}),
    },
    sport: {
      rubricsInfo: {} as ISportServerCtxInner['rubricsInfo'],
      isWOg: false,
      is404: false,
      isAppSearchHeader: false,
      isAjax: false,
      header: {
        retpath: '__test__',
      },
      isRobot: false,
      isFontLoaded: true,
      isTests: false,
      cmntCounts: {},
      request: {
        scheme: 'https',
        hostname: 'yandex.ru',
      } as IRequest,
      newsRequest: {} as INewsRequest,
      utmReferrer: 'from_sport',
      linkConfig: {
        utmReferrer: 'from_sport',
        utmSource: 'yxsport',
        serviceRoute: '/sport',
      },
      currentTimestamp: 1647888754000,
      timezone: 3,
      ...(args?.specialArgs?.sport || {}),
    },
  };
}
