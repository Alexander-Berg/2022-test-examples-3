/**
 * Метрики без разделения на короткие/длинные клики и без метрик на доли.
 * @see https://nda.ya.ru/t/7waIZKgU4uyUC9
 */
export interface IMetrics {
    // ##### Общее #####
    /**
     * Общее количество запросов на ТВ.
     * Складывается как сумма запросов к: главной, поиску, офферам, SKU и моделям.
     */
    'products.all_requests': number;
    /**
     * Запросы к поисковой выдаче ТВ
     * Дозапросы следующих страниц не считаются новыми запросами!
     */
    'products.requests': number;
    /** Запросы к странице оффера. */
    'products.offer_requests': number;
    /** Запросы к странице SKU. */
    'products.sku_requests': number;
    /** Запросы к странице модели. */
    'products.model_requests': number;

    // ##### Поисковая выдача #####
    /** Все внешние клики. */
    'products.all_external_clicks': number;
    /** Клики на поисковой выдаче. */
    'products.total_clicks': number;
    /** Клики по фильтру цены. */
    'products.price_filter_clicks': number;
    /** Клики по изменению сортировки. */
    'products.sorting_order_clicks': number;
    /** Клики по всем карточкам товаров. */
    'products.product_card_clicks': number;
    /** Клики по карточке-офферу. */
    'products.product_card_offer_clicks': number;
    /** Клики по карточке-SKU. */
    'products.product_card_sku_clicks': number;
    /** Клики по карточке-модели. */
    'products.product_card_product_clicks': number;
    /** Клики по карточкам в ТГ. */
    'products.product_card_adv_clicks': number;

    // ##### Карточка SKU #####
    /** Клики на карточках SKU. */
    'products.sku_total_clicks': number;
    /** Внешние клики на карточках SKU. */
    'products.sku_external_clicks': number;

    // ##### Карточка оффера #####
    /** Клики на офферах. */
    'products.offer_total_clicks': number;
    /** Внешние клики на офферах. */
    'products.offer_external_clicks': number;

    // ##### Карточка модели #####
    /** Клики на карточках моделей. */
    'products.model_total_clicks': number;
    /** Внешние клики на карточках моделей. */
    'products.model_external_clicks': number;
}

/**
 * Возвращает все посчитанные метрики на момент вызова.
 * Счётчики доезжают до логов не сразу, поэтому перед вызовом этого хелпера
 * лучше поставить паузу, чтобы наверняка увидеть все посчитанные метрики.
 * @see https://a.yandex-team.ru/arc_vcs/frontend/projects/infratest/packages/hermione-get-counters#getmetricsmetrics-1
 */
export function yaGetMetrics(
    this: WebdriverIO.Browser,
) {
    const metrics: IMetrics = {
        'products.all_requests': 1,
        'products.requests': 1,
        'products.offer_requests': 1,
        'products.sku_requests': 1,
        'products.model_requests': 1,
        'products.all_external_clicks': 1,
        'products.total_clicks': 1,
        'products.price_filter_clicks': 1,
        'products.sorting_order_clicks': 1,
        'products.product_card_clicks': 1,
        'products.product_card_offer_clicks': 1,
        'products.product_card_sku_clicks': 1,
        'products.product_card_product_clicks': 1,
        'products.product_card_adv_clicks': 1,
        'products.sku_total_clicks': 1,
        'products.sku_external_clicks': 1,
        'products.offer_total_clicks': 1,
        'products.offer_external_clicks': 1,
        'products.model_total_clicks': 1,
        'products.model_external_clicks': 1,
    };
    return this.getMetrics(metrics);
}
