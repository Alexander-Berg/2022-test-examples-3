import { EContentType } from 'news/types/IRubricStory';
import { getAvatarsHost } from 'neo/lib/getStaticsHost';

export const story = {
  content_type: EContentType.PREVIEW,
  annot: {
    no_banner: false,
    stid: 'stid',
    title: {
      text: 'Определена эффективность вакцины «КовиВак»',
      doc: {
        agency_id: 1047,
        logo_square: `//${getAvatarsHost()}/get-ynews-logo/26056/1047-1478692902215-square`,
        title: 'Определена эффективность вакцины «КовиВак»',
        cl4url: 'cl4url',
        transliterated_title: 'translit_title',
        source_name: 'Lenta.ru (title)',
        pub_date: 0,
        weights: '0 0 0 1 0.612894',
        url: 'https://lenta.ru/news/2021/06/02/third/',
      },
    },
    best_rubric: 99974,
    story_url: 'https://yandex.ru/news/story/Opredelena_ehffektivnost_vakciny_KoviVak--0083b5d7d36ce7cc53f8f9d8f1c85cee?lang=ru&rubric=index&fan=1&persistent_id=145432497',
    brand_safety_categories: [16384, 512],
    is_favourite: true,
    persistent_id: '1234',
    snippets: [
      {
        plain_text: 'Эффективность третьей российской вакцины от коронавируса «КовиВак», по предварительным данным, составляет более 80 процентов, сообщает «Интерфакс» со ссылкой на данные центра имени Чумакова, где разработали препарат.',
        doc: {
          agency_id: 1047,
          logo_square: `//${getAvatarsHost()}/get-ynews-logo/26056/1047-1478692902215-square`,
          title: 'Определена эффективность вакцины «КовиВак»',
          cl4url: 'cl4url',
          transliterated_title: 'translit_title_snippet',
          source_name: 'Lenta.ru (snippets)',
          pub_date: 0,
          weights: '0 0 0 1 0.612894',
          url: 'https://lenta.ru/news/2021/06/02/third/',
        },
      },
    ],
    rubrics: [39, 99974],
  },
  counts: {
    photo: 0,
    video: 0,
    by_genre: {
      total: {
        uniqs: 0,
      },
    },
  },
};

export const storyNoSnippetsDoc = {
  content_type: EContentType.PREVIEW,
  annot: {
    no_banner: false,
    stid: 'stid',
    title: {
      text: 'Определена эффективность вакцины «КовиВак»',
      doc: {
        agency_id: 1047,
        logo_square: `//${getAvatarsHost()}/get-ynews-logo/26056/1047-1478692902215-square`,
        title: 'Определена эффективность вакцины «КовиВак»',
        cl4url: 'cl4url',
        transliterated_title: 'Opredelena_ehffektivnost_vakciny_KoviVak',
        source_name: 'Lenta.ru (title)',
        pub_date: 0,
        weights: '0 0 0 1 0.612894',
        url: 'https://lenta.ru/news/2021/06/02/third/',
      },
    },
    best_rubric: 99974,
    story_url: 'https://yandex.ru/news/story/Opredelena_ehffektivnost_vakciny_KoviVak--0083b5d7d36ce7cc53f8f9d8f1c85cee?lang=ru&rubric=index&fan=1&persistent_id=145432497',
    brand_safety_categories: [16384, 512],
    is_favourite: true,
    persistent_id: '145432497',
    snippets: [
      {
        plain_text: 'Эффективность третьей российской вакцины от коронавируса «КовиВак», по предварительным данным, составляет более 80 процентов, сообщает «Интерфакс» со ссылкой на данные центра имени Чумакова, где разработали препарат.',
      },
    ],
    rubrics: [39, 99974],
  },
  counts: {
    photo: 0,
    video: 0,
    by_genre: {
      total: {
        uniqs: 0,
      },
    },
  },
};

export const storyWithoutSnippets = {
  content_type: EContentType.PREVIEW,
  annot: {
    no_banner: false,
    stid: 'stid',
    title: {
      text: 'Определена эффективность вакцины «КовиВак»',
      doc: {
        agency_id: 1047,
        logo_square: `//${getAvatarsHost()}/get-ynews-logo/26056/1047-1478692902215-square`,
        title: 'Определена эффективность вакцины «КовиВак»',
        cl4url: 'cl4url',
        transliterated_title: 'Opredelena_ehffektivnost_vakciny_KoviVak',
        source_name: 'Lenta.ru (title)',
        pub_date: 0,
        weights: '0 0 0 1 0.612894',
        url: 'https://lenta.ru/news/2021/06/02/third/',
      },
    },
    best_rubric: 99974,
    story_url: 'https://yandex.ru/news/story/Opredelena_ehffektivnost_vakciny_KoviVak--0083b5d7d36ce7cc53f8f9d8f1c85cee?lang=ru&rubric=index&fan=1&persistent_id=145432497',
    brand_safety_categories: [16384, 512],
    is_favourite: true,
    persistent_id: '145432497',
    snippets: [],
    rubrics: [39, 99974],
  },
  counts: {
    photo: 0,
    video: 0,
    by_genre: {
      total: {
        uniqs: 0,
      },
    },
  },
};
