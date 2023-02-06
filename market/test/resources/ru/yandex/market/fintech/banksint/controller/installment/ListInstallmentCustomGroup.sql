truncate public.custom_group_to_installment,
    public.installment_custom_group,
    public.installment,
    public.installment_files CASCADE;
INSERT INTO public.installment (id, name, description, duration, percentage, enabled, category_ids, brand_ids,
                                min_price, max_price, supplier_type)
VALUES ('MONTH_AND_HALF', '1.5 месяца', 'Не на все категории', 45, 3.00000, true, null, null, 100, 200, 'PARTNER'),
       ('HALF_YEAR', '6 месяцев', null, 180, 5.00000, true, null, null, 1000, 2000, 'PARTNER'),
       ('DISABLED_TEST', '24 месяца', null, 720, 20.00000, false, null, null, 10, 20, 'YANDEX'),
       ('CATEGORY_TEST', '24 месяца', null, 720, 20.00000, true, '{90945}', '{133}', 10000, 20000, 'PARTNER'),
       ('BRAND_TEST', '24 месяца', null, 720, 20.00000, true, '{90867}', '{142}', 100000, 200000, 'PARTNER');

INSERT INTO public.installment_custom_group (id, name, shop_id, category_ids, brand_ids, enabled, start_date, end_date,
                                             source)
VALUES (1, 'first', 42, '{1}', '{1}', true, '2021-10-19 15:18:50.000000', '2028-10-23 15:18:52.000000',
        'DYNAMIC_GROUPS'),
       (2, 'second', 42, '{2}', '{2}', true, '2021-10-19 15:18:50.000000', '2028-10-23 15:18:52.000000',
        'DYNAMIC_GROUPS'),
       (3, 'third', 42, '{3}', '{3}', true, '2021-10-19 15:18:50.000000', '2028-10-23 15:18:52.000000',
        'DYNAMIC_GROUPS'),
       (4, 'fourth', 42, '{4}', '{4}', true, '2021-10-19 15:18:50.000000', '2028-10-23 15:18:52.000000',
        'DYNAMIC_GROUPS'),
       (5, 'fifth', 42, '{5}', '{5}', true, '2021-10-19 15:18:50.000000', '2028-10-23 15:18:52.000000',
        'DYNAMIC_GROUPS'),
       (6, 'sixth', 42, '{6}', '{6}', true, '2021-10-19 15:18:50.000000', '2028-10-23 15:18:52.000000',
        'DYNAMIC_GROUPS'),
       (7, 'seventh', 42, '{7}', '{7}', true, '2021-10-19 15:18:50.000000', '2028-10-23 15:18:52.000000',
        'DYNAMIC_GROUPS'),
       (8, 'eighth', 42, '{8}', '{8}', true, '2021-10-19 15:18:50.000000', '2028-10-23 15:18:52.000000',
        'DYNAMIC_GROUPS');

INSERT INTO public.custom_group_to_installment (custom_group_id, installment_id)
VALUES (1, 'MONTH_AND_HALF'),
       (2, 'HALF_YEAR'),
       (3, 'CATEGORY_TEST'),
       (4, 'BRAND_TEST'),
       (5, 'MONTH_AND_HALF'),
       (6, 'HALF_YEAR'),
       (7, 'CATEGORY_TEST'),
       (8, 'BRAND_TEST');

INSERT INTO public.installment_files(resource_id, shop_id, business_id, name, correct_selected_offers,
                                     invalid_offers, status, total_offers, url_to_download, file_type)
VALUES ('de7cd822-d5f9-47cb-8fee-36546d7c9ab1', 42, 2, 'file.xlsx', 0, 0, 'PENDING', 10, 'url', 'INSTALLMENT'),
       ('319021fd-a7d7-45d0-837d-82a8e4c4bce0', 42, 2, 'file2.xlsx', 0, 0, 'DONE', 10, 'url', 'INSTALLMENT');
