import { getStoryStat } from 'news/lib/top/getStoryStat';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import { ITopStory } from 'news/types/ITopStory';
import { EImpact } from 'mg/types/EImpact';
import { INewsNewsdResponseRubric } from 'news/types/apphost/news_newsd_response';
import { IRubric } from 'news/types/IRubric';

describe('getStoryStat', () => {
  const instoryUrl = '/news/instory/a--6207473e959da6f4c3d19abbc9b650d8';

  it('возвращает региональный интерес в регионе текущей рубрики', () => {
    const serverCtx = getServerCtxRegionStub({
      new_lr: 213,
      rubric: {
        id: 2,
        is_region: true,
      },
    });
    const storyStat = getStoryStat(serverCtx, { story: getTopStoryStub(), instoryUrl });

    expect(storyStat?.regionalInterest).toEqual(EImpact.HIGH);
  });

  it('возвращает региональный интерес в регионе пользователя, если текущая рубрика не регион', () => {
    const serverCtx = getServerCtxRegionStub({
      new_lr: 213,
      rubric: {
        id: 2,
        is_region: false,
      },
    });
    const storyStat = getStoryStat(serverCtx, { story: getTopStoryStub(), instoryUrl });

    expect(storyStat?.regionalInterest).toEqual(EImpact.LOW);
  });

  it('возвращает региональный интерес в регионе пользователя, если текущая рубрика не указана', () => {
    const serverCtx = getServerCtxRegionStub({
      new_lr: 213,
    });
    const storyStat = getStoryStat(serverCtx, { story: getTopStoryStub(), instoryUrl });

    expect(storyStat?.regionalInterest).toEqual(EImpact.LOW);
  });

  it('возвращает список источников с наибольшим количеством визитов, если текущая рубрика не регион', () => {
    const serverCtx = getServerCtxRegionStub({
      new_lr: 213,
      rubric: {
        id: 2,
        is_region: false,
      },
    });

    const storyStat = getStoryStat(serverCtx, { story: getTopStoryStub(), instoryUrl });

    expect(storyStat?.sources?.length).toEqual(5);
    expect(storyStat?.sources?.[4]).toEqual({
      agencyName: 'Остальные источники',
      docUrl: instoryUrl,
    });
  });
});

function getServerCtxRegionStub(
  args: {
    new_lr: number,
    rubric?: IRubric['rubric'],
  },
) {
  const { new_lr, rubric } = args;
  const additionalItemMap = rubric ? {
    news_newsd_response: [
      {
        handler: 'rubric',
        data: {
          rubric: {
            rubric,
          },
        },
      } as INewsNewsdResponseRubric,
    ],
  } : undefined;

  return getServerCtxStub({
    specialArgs: {
      news: {
        newsRequest: {
          new_lr,
          issue: {
            id: 100,
          },
        },
      },
    },
    additionalItemMap,
  });
}

function getTopStoryStub() {
  return {
    related: [],
    story_stat: {
      TopPositions: [],
      GTAdditions: [
        {
          Traffic: 0,
          Impact: EImpact.LOW,
          Top: {
            Issue: 100,
            Region: 213,
          },
        },
        {
          Traffic: 0,
          Impact: EImpact.HIGH,
          Top: {
            Issue: 100,
            Region: 2,
          },
        },
      ],
      IndexRefVisits: [
        {
          AgencyId: 1027,
          DocTitle: 'Решетников предложил провести донастройку налогов «без революций»',
          DocUrl: 'https://www.rbc.ru/economics/07/06/2021/60bca4a89a7947ab21de6385',
          IsRestrictedRussia: true,
          ShareInStory: 0.3267918229,
          VisitCount: 383,
        },
        {
          AgencyId: 102,
          DocTitle: 'Налоги без революций, как единороссы пойдут в Думу. Главное за ночь',
          DocUrl: 'https://www.rbc.ru/ins/society/07/06/2021/60bd9cd99a7947dd23fb73ce',
          IsRestrictedRussia: true,
          ShareInStory: 0.308873713,
          VisitCount: 362,
        },
        {
          AgencyId: 1047,
          DocTitle: 'Власти России назвали неизбежным повышение части налогов',
          DocUrl: 'https://lenta.ru/news/2021/06/07/invest/',
          IsRestrictedRussia: true,
          ShareInStory: 0.2918088734,
          VisitCount: 342,
        },
        {
          AgencyId: 1040,
          DocTitle: 'Решетников назвал неизбежным повышение части налогов',
          DocUrl: 'https://www.gazeta.ru/business/news/2021/06/07/n_16071794.shtml',
          IsRestrictedRussia: true,
          ShareInStory: 0.06996586919,
          VisitCount: 82,
        },
        {
          AgencyId: 1116,
          DocTitle: 'Решетников заявил о возможной «донастройке» налоговой системы',
          DocUrl: 'https://rg.ru/2021/06/07/reshetnikov-zaiavil-o-vozmozhnoj-donastrojke-nalogovoj-sistemy.html',
          IsRestrictedRussia: false,
          ShareInStory: 0.001706484589,
          VisitCount: 2,
        },
      ],
    },
  } as unknown as ITopStory;
}
