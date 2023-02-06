export const newsSearchContent = [{
  type: 'news_search_content',
  data: [{
    docs: [{
      pub_date: 1234567890,
      url: 'https://yandex.ru/news/story_url',
      title: 'title',
      transliterated_title: 'tr_title',
      cl4url: 'cl4url',
      agency_id: 254060379,
    }],
    story_url: '/story-url',
    title: {
      doc: {
        source_name: 'ХЕРСОН Онлайн',
        pub_date: 1234567890,
      },
      text: 'title',
    },
    more: 'more',
    num_of_sources: 3,
  }],
}];

export const newsSearchContentDoc = [{
  type: 'news_search_content',
  data: [{
    type: 'doc',
    doc: {
      pub_date: 1234567890,
      url: 'https://yandex.ru/news/story_url',
      title: 'title',
      transliterated_title: 'tr_title',
      cl4url: 'cl4url',
      agency_id: 1013,
    },
  }],
}];

export const newsSearchAggregated = [{
  type: 'news_search_content',
  data: [{
    type: 'doc',
    doc: {
      pub_date: 1234567890,
      url: 'https://yandex.ru/news/story_url',
      title: 'title',
      transliterated_title: 'tr_title',
      cl4url: 'cl4url',
      agency_id: 1013,
    },
  }, {
    docs: [{
      pub_date: 1234567890,
      url: 'https://yandex.ru/news/story_url',
      title: 'title',
      transliterated_title: 'tr_title',
      cl4url: 'cl4url',
      agency_id: 254060379,
    }],
    story_url: '/story-url',
    title: {
      doc: {
        source_name: 'ХЕРСОН Онлайн',
        pub_date: 1234567890,
      },
      text: 'title',
    },
    more: 'more',
    num_of_sources: 3,
  }],
},
{
  type: 'news_search_content',
  next_page: '/next_page2',
  data: [{
    type: 'doc',
    doc: {
      source_name: 'Окружная электронная газета Зеленограда',
      pub_date: 1591005880,
      snippet: 'Новоселье отпраздновали [котики] Марсель и Вендал.',
      title: 'Еще шесть питомцев из приютов «Зеленоград» и «Зоорассвет» стали домашними',
      url: 'https://zelao.mos.ru/presscenter/news/detail/8934877.html',
      agency_id: 254117695,
    },
  }],
}];
