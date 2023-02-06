import { URL } from 'url';
import { IServerCtx } from 'news/types/contexts/IServerCtx';
import { EService } from 'neo/types/EService';
import { serverCtxStub as serverCtxStubNeo } from 'neo/tests/stubs/contexts';
import { itemMap as ITEM_MAP } from 'news/tests/stubs/contexts/item/itemMap';

export function getServerCtxStub(
  args?: {
    findLastItemArgs?: Record<string, string>,
    specialArgs?: Record<string, object>,
    additionalItemMap?: Record<string, object>,
  },
): IServerCtx {
  const itemMap = args?.additionalItemMap
    ? { ...ITEM_MAP, ...args.additionalItemMap }
    : ITEM_MAP;

  return {
    neo: {
      ...serverCtxStubNeo.neo,
      ctxRR: {
        findLastItem: (type: string) => itemMap[args?.findLastItemArgs?.[type] || type],
        getItems: (type: string) => itemMap[args?.findLastItemArgs?.[type] || type] || [],
      },
      service: EService.NEWS,
      url: new URL('https://yandex.ru/news/story/Sobyanin_obyavil'),
      reqid: '1590572947960507-788120642695967590200098-priemka-stable-news-app-host-2-NEWS-NEWS_RUBRIC',
      flags: {},
      ...(args?.specialArgs?.neo || {}),
    },
    news: {
      request: {
        scheme: 'https',
        hostname: 'yandex.ru',
      },
      linkConfig: {
        utmReferrer: 'from_news',
        utmSource: 'yxnews',
        serviceRoute: '/news',
        preservedParams: ['family'],
      },
      ...(args?.specialArgs?.news || {}),
    },
  } as IServerCtx;
}
