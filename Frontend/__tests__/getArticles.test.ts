import { getArticles } from 'news/lib/story/getArticles';
import { ILegacyArticle } from 'news/types/apphost/news_legacy_articles';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import { EArticleTag, articleTagsMap } from 'news/components/Story/__Article/Story__Article.types';
import { turboUrls } from 'news/tests/stubs/contexts/item/turboUrls';
import { getAvatarsHost } from 'neo/lib/getStaticsHost';

function getArticleMock(): ILegacyArticle[] {
  return [
    {
      Title: 'Test title',
      URL: 'https://vz.ru',
      Agency: {
        ID: 10,
        Name: 'AIF',
        LogoURL: `https://${getAvatarsHost()}/get-ynews-logo/135513/1040-1478692902361-square/`,
      },
      FirstParagraph: 'Big annotation for article',
      Tags: [1, 2],
      publishDate: {
        Seconds: 10000,
      },
    },
  ];
}

function getServerCtx(mock?: ILegacyArticle[]) {
  return getServerCtxStub({
    findLastItemArgs: { news_legacy_articles: 'news_legacy_articles' },
    additionalItemMap: { news_legacy_articles: { articles: mock ?? getArticleMock() } },
    specialArgs: {
      neo: {
        flags: {
          'yxneo_news_phone_story_turbo-articles': '1',
        },
      },
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
      },
    },
  });
}

describe('getArticles', () => {
  let serverCtx = getServerCtx();

  afterEach(() => {
    serverCtx = getServerCtx();
  });

  it('добавляет параметр utm_source к ссылке на статью', () => {
    const articles = getArticles(serverCtx, turboUrls);
    const url = new URL(articles![0].url);

    expect(url.searchParams.get('utm_source') === 'yandex_article').toBeTruthy();
  });

  it('берет только первый элемент из массива tags', () => {
    const articles = getArticles(serverCtx, turboUrls);
    expect(articles![0].tag === articleTagsMap[EArticleTag.ANALYTICS]).toBeTruthy();
  });

  it('оставляет поле tag пустым в случае, если теги не пришли', () => {
    const mock = getArticleMock();
    mock[0].Tags = undefined;
    serverCtx = getServerCtx(mock);
    const articles = getArticles(serverCtx, turboUrls);
    expect(articles![0].tag).toBeUndefined();
  });
});
