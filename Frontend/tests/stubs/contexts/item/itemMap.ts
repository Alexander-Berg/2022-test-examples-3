import { mainMenuRubrics } from 'news/tests/stubs/contexts/item/mainMenuRubrics';
import { tzInfo } from 'news/tests/stubs/contexts/item/tzInfo';
import { nextPageNull, nextPage, nextPageSearch } from 'news/tests/stubs/contexts/item/nextPage';
import { textInfoEmpty, textInfo } from 'news/tests/stubs/contexts/item/textInfo';
import { agenciesInfo } from 'news/tests/stubs/contexts/item/agenciesInfo';
import {
  newsSearchContent,
  newsSearchContentDoc,
  newsSearchAggregated,
} from 'news/tests/stubs/contexts/item/newsSearchContent';
import { rubricsInfo } from 'news/tests/stubs/contexts/item/rubricsInfo';
import { saasBanfilterFull, saasBanfilterAggregated } from 'news/tests/stubs/contexts/item/saasBanfilter';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const itemMap: Record<string, any> = {
  main_menu_rubrics: mainMenuRubrics,
  news_search_content: newsSearchContent,
  news_search_content_doc: newsSearchContentDoc,
  news_search_aggregated: newsSearchAggregated,
  next_page: nextPage,
  next_page_null: nextPageNull,
  next_page_search: nextPageSearch,
  text_info: textInfo,
  agencies_info: agenciesInfo,
  text_info_empty: textInfoEmpty,
  tz_info: tzInfo,
  rubrics_info: rubricsInfo,
  saas_banfilter_full: saasBanfilterFull,
  saas_banfilter_aggregated: saasBanfilterAggregated,
};
