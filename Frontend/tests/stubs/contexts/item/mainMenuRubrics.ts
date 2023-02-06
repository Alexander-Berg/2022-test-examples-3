export const mainMenuRubrics = {
  type: 'main_menu_rubrics',
  data: [{
    name: 'Главное',
    url: '/news',
    is_region: false,
    id: 0,
    alias: 'index',
    is_active: true,
  },
  {
    name: 'Москва',
    url: '/news/region/moscow',
    is_region: true,
    id: 213,
    alias: 'Moscow',
  },
  {
    name: 'Интересное',
    url: '/news/rubric/personal_feed',
    is_region: false,
    id: 404043,
    alias: 'personal_feed',
    subrubrics: [{
      name: 'Очень интересное',
      url: '/news/rubric/personal_feed/special',
      id: 2136,
      alias: 'Special',
    }],
  },
  {
    name: 'Спорт',
    url: '/news/rubric/sport',
    is_region: false,
    id: 90,
    alias: 'sport',
  }],
};
