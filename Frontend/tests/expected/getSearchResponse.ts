import { getAvatarsHost } from 'neo/lib/getStaticsHost';

export const expectedEmpty = {
  isEmpty: true,
  query: '',
  originalQuery: '',
  stories: [],
};

export const expectedNoNewsSearchResponse = {
  isEmpty: false,
  query: 'search_query',
  originalQuery: '',
  typo: {
    isFixed: true,
    linkText: 'mtext',
    linkUrl: 'https://yandex.ru/news',
  },
  stories: [],
};

export const expectedTypeStory = {
  isEmpty: false,
  query: 'search_query',
  originalQuery: '',
  typo: {
    isFixed: true,
    linkText: 'mtext',
    linkUrl: 'https://yandex.ru/news',
  },
  stories: [{
    docs: [{
      image: 'https://favicon.yandex.net/favicon/khersonline.net?stub=1&size=32',
      isFavicon: true,
      isTurbo: false,
      sourceName: 'ХЕРСОН Онлайн',
      text: [],
      time: '14.02.2009 в 02:31',
      title: [{
        isSearched: false,
        text: 'title',
      }],
      url: 'https://yandex.ru/news/story_url',
    }],
    moreUrl: 'https://yandex.ru/more?utm_source=yxnews&utm_medium=mobile',
    sourceName: 'ХЕРСОН Онлайн',
    time: '14.02.2009 в 02:31',
    title: 'title',
    titleTarget: '_blank',
    totalDocsCount: 3,
    url: 'https://yandex.ru/story-url?utm_source=yxnews&utm_medium=mobile',
  }],
};

export const expectedTypeDoc = {
  isEmpty: false,
  query: 'search_query',
  originalQuery: '',
  typo: {
    isFixed: true,
    linkText: 'mtext',
    linkUrl: 'https://yandex.ru/news',
  },
  stories: [{
    docs: [
      {
        image: `https://${getAvatarsHost()}/get-ynews-logo/50744/1013-1496416510291-square/logo-square`,
        isFavicon: false,
        isTurbo: false,
        sourceName: 'Известия',
        text: [],
        time: '14.02.2009 в 02:31',
        title: [{
          isSearched: false,
          text: 'title',
        }],
        url: 'https://yandex.ru/news/story_url',
      },
    ],
  }],
};

export const expectedAggregated = {
  isEmpty: false,
  query: 'search_query',
  originalQuery: '',
  typo: {
    isFixed: true,
    linkText: 'mtext',
    linkUrl: 'https://yandex.ru/news',
  },
  stories: [{
    docs: [
      {
        image: `https://${getAvatarsHost()}/get-ynews-logo/50744/1013-1496416510291-square/logo-square`,
        isFavicon: false,
        isTurbo: false,
        sourceName: 'Известия',
        text: [],
        time: '14.02.2009 в 02:31',
        title: [{
          isSearched: false,
          text: 'title',
        }],
        url: 'https://yandex.ru/news/story_url',
      },
    ],
  }, {
    docs: [{
      image: 'https://favicon.yandex.net/favicon/khersonline.net?stub=1&size=32',
      isFavicon: true,
      isTurbo: false,
      sourceName: 'ХЕРСОН Онлайн',
      text: [],
      time: '14.02.2009 в 02:31',
      title: [{
        isSearched: false,
        text: 'title',
      }],
      url: 'https://yandex.ru/news/story_url',
    }],
    moreUrl: 'https://yandex.ru/more?utm_source=yxnews&utm_medium=mobile',
    sourceName: 'ХЕРСОН Онлайн',
    time: '14.02.2009 в 02:31',
    title: 'title',
    titleTarget: '_blank',
    totalDocsCount: 3,
    url: 'https://yandex.ru/story-url?utm_source=yxnews&utm_medium=mobile',
  }, {
    docs: [
      {
        image: `https://${getAvatarsHost()}/get-ynews-logo/50744/254117695-1478693791391-square/logo-square`,
        isFavicon: false,
        isTurbo: false,
        sourceName: 'Префектура Зеленоградского АО',
        text: [{
          isSearched: false,
          text: 'Новоселье отпраздновали ',
        }, {
          isSearched: true,
          text: 'котики',
        }, {
          isSearched: false,
          text: ' Марсель и Вендал.',
        }],
        time: '01.06.2020 в 13:04',
        title: [{
          isSearched: false,
          text: 'Еще шесть питомцев из приютов «Зеленоград» и «Зоорассвет» стали домашними',
        }],
        url: 'https://zelao.mos.ru/presscenter/news/detail/8934877.html?utm_source=yxnews&utm_medium=mobile',
      },
    ],
  }],
};
