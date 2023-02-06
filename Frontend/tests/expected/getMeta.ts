export const expectedNonEmpty = {
  title: 'бобр – Яндекс.Новости',
  canonical: 'https://yandex.ru/news/search?text=%D0%B1%D0%BE%D0%B1%D1%80',
  og: {
    title: 'бобр – Яндекс.Новости',
    url: 'https://yandex.ru/news/search?text=%D0%B1%D0%BE%D0%B1%D1%80',
    siteName: 'Яндекс.Новости',
    image: {
      url: 'test-file-stub',
      width: 1200,
      height: 630,
      type: 'image/png',
    },
  },
  twitter: {
    title: 'бобр – Яндекс.Новости',
    image: {
      url: 'test-file-stub',
      width: 1200,
    },
  },
};

export const expectedEmptyResult = {
  title: 'Новостей по вашему запросу не найдено – Яндекс.Новости',
  canonical: 'https://yandex.ru/news/search?text=abraqadabradabrafoo',
  og: {
    title: 'Новостей по вашему запросу не найдено – Яндекс.Новости',
    url: 'https://yandex.ru/news/search?text=abraqadabradabrafoo',
    siteName: 'Яндекс.Новости',
    image: {
      url: 'test-file-stub',
      width: 1200,
      height: 630,
      type: 'image/png',
    },
  },
  twitter: {
    title: 'Новостей по вашему запросу не найдено – Яндекс.Новости',
    image: {
      url: 'test-file-stub',
      width: 1200,
    },
  },
};

export const expectedEmptyQuery = {
  title: 'Задан пустой поисковый запрос – Яндекс.Новости',
  canonical: 'https://yandex.ru/news/search?text=',
  og: {
    title: 'Задан пустой поисковый запрос – Яндекс.Новости',
    url: 'https://yandex.ru/news/search?text=',
    siteName: 'Яндекс.Новости',
    image: {
      url: 'test-file-stub',
      width: 1200,
      height: 630,
      type: 'image/png',
    },
  },
  twitter: {
    title: 'Задан пустой поисковый запрос – Яндекс.Новости',
    image: {
      url: 'test-file-stub',
      width: 1200,
    },
  },
};
