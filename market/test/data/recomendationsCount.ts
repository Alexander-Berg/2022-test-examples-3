import { FilterType, RecommendationCountsDTO, WeeksType } from 'src/java/definitions-replenishment';

export const recommendationsCount: RecommendationCountsDTO = {
  filter: [
    { count: 245, processed: false, id: FilterType.ALL, weeks: [] },
    { count: 149, processed: true, id: FilterType.PROCESSED, weeks: [] },
    { count: 96, processed: false, id: FilterType.NEED_MANUAL_REVIEW, weeks: [] },
    { count: 22, processed: false, id: FilterType.NEW, weeks: [] },
    {
      count: 52,
      processed: false,
      id: FilterType.SALES_ZERO,
      weeks: [
        { count: 1, processed: false, id: WeeksType.ZERO_TWO },
        { count: 1, processed: false, id: WeeksType.TWO_THREE },
        { count: 1, processed: false, id: WeeksType.THREE_FOUR },
        { count: 49, processed: false, id: WeeksType.FOUR_EIGHT },
      ],
    },
    {
      count: 49,
      processed: false,
      id: FilterType.SALES_ZERO_AND_POSITIVE_STOCK_OR_TRANSIT,
      weeks: [
        { count: 0, processed: true, id: WeeksType.ZERO_TWO },
        { count: 0, processed: true, id: WeeksType.TWO_THREE },
        { count: 0, processed: true, id: WeeksType.THREE_FOUR },
        { count: 49, processed: false, id: WeeksType.FOUR_EIGHT },
      ],
    },
    {
      count: 0,
      processed: true,
      id: FilterType.SALES_LT_QUANTUM,
      weeks: [
        { count: 0, processed: true, id: WeeksType.ZERO_TWO },
        { count: 0, processed: true, id: WeeksType.TWO_THREE },
        { count: 0, processed: true, id: WeeksType.THREE_FOUR },
        { count: 0, processed: true, id: WeeksType.FOUR_EIGHT },
      ],
    },
    {
      count: 5,
      processed: false,
      id: FilterType.SC,
      weeks: [
        { count: 1, processed: false, id: WeeksType.ZERO_TWO },
        { count: 1, processed: false, id: WeeksType.TWO_THREE },
        { count: 1, processed: false, id: WeeksType.THREE_FOUR },
        { count: 1, processed: false, id: WeeksType.FOUR_EIGHT },
        { count: 1, processed: false, id: WeeksType.EIGHT_INF },
      ],
    },
    { count: 6, processed: false, id: FilterType.SPECIAL_ORDER, weeks: [] },
    { count: 7, processed: false, id: FilterType.TRANSIT_WARNING, weeks: [] },
    { count: 8, processed: false, id: FilterType.ASSORTMENT_GOODS_SUB_SSKU, weeks: [] },
  ],
};
