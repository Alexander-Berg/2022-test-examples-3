truncate public.sku_to_installments,
    public.installment_skus,
    public.installment,
    public.installment_files CASCADE;
INSERT INTO public.installment (id, name, description, duration, percentage, enabled, category_ids, brand_ids,
                                min_price, max_price, supplier_type)
VALUES ('MONTH_AND_HALF', '1.5 месяца', 'Не на все категории', 45, 3.00000, true, null, null, 100, 200, 'PARTNER'),
       ('HALF_YEAR', '6 месяцев', null, 180, 5.00000, true, null, null, 1000, 2000, 'PARTNER'),
       ('TWO_YEARS', '24 месяца', null, 720, 20.00000, true, null, null, 1000, 2000, 'PARTNER');

INSERT INTO public.installment_files(resource_id, shop_id, business_id, name, correct_selected_offers,
                                     invalid_offers, status, total_offers, url_to_download, file_type,
                                     updated_at, fail_reason)
VALUES ('85b438fc-a482-4fd7-83d1-505c6b5dcc30', 42, 2, 'file4.xlsx', 0, 0, 'PROCESSING', 0, 'url4', 'INSTALLMENT',
        '2021-10-19 15:18:50.000000', null);
