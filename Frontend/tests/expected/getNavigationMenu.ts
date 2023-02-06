export const EXPECTED_NAVIGATION_MENU_ITEMS = [
  {
    alias: 'index',
    isActive: true,
    text: 'Главное',
    url: 'https://yandex.ru/news',
  },
  {
    alias: 'Moscow',
    isActive: false,
    text: 'Москва',
    url: 'https://yandex.ru/news/region/moscow',
  },
  {
    alias: 'personal_feed',
    isActive: false,
    text: 'Интересное',
    url: 'https://yandex.ru/news/rubric/personal_feed',
    subrubrics: [
      {
        alias: 'Special',
        isActive: false,
        text: 'Очень интересное',
        url: 'https://yandex.ru/news/rubric/personal_feed/special',
      },
    ],
  },
  {
    alias: 'sport',
    isActive: false,
    text: 'Спорт',
    url: 'https://yandex.ru/sport?utm_source=yxnews&utm_medium=mobile',
  },
];

export const EXPECTED_NAVIGATION_MENU_ITEMS_VERTICAL = [
  {
    alias: 'index',
    isActive: true,
    text: 'Главное',
    url: 'https://yandex.ru/news?vertical=1',
  },
  {
    alias: 'Moscow',
    isActive: false,
    text: 'Москва',
    url: 'https://yandex.ru/news/region/moscow?vertical=1',
  },
  {
    alias: 'personal_feed',
    isActive: false,
    text: 'Интересное',
    url: 'https://yandex.ru/news/rubric/personal_feed?vertical=1',
    subrubrics: [
      {
        alias: 'Special',
        isActive: false,
        text: 'Очень интересное',
        url: 'https://yandex.ru/news/rubric/personal_feed/special?vertical=1',
      },
    ],
  },
  {
    alias: 'sport',
    isActive: false,
    text: 'Спорт',
    url: 'https://yandex.ru/sport?utm_source=yxnews&utm_medium=mobile',
  },
];

export const EXPECTED_NAVIGATION_MENU = {
  isSticky: true,
  items: EXPECTED_NAVIGATION_MENU_ITEMS,
};

export const EXPECTED_NAVIGATION_MENU_VERTICAL_ANDROID = {
  isSticky: false,
  items: EXPECTED_NAVIGATION_MENU_ITEMS_VERTICAL,
};

export const EXPECTED_NAVIGATION_MENU_VERTICAL_APPSEARCH = {
  isSticky: false,
  items: EXPECTED_NAVIGATION_MENU_ITEMS_VERTICAL,
};
