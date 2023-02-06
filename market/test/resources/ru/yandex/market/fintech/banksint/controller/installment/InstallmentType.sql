truncate public.custom_group_to_installment,
    public.installment_custom_group,
    public.installment CASCADE;
INSERT INTO public.installment (id, name, description, duration, percentage, enabled, category_ids, brand_ids,
                                min_price, max_price, supplier_type)
VALUES ('MONTH_AND_HALF', '1.5 месяца', 'Не на все категории', 45, 3.00000, true, null, null, 100, 200, 'PARTNER'),
       ('HALF_YEAR', '6 месяцев', null, 180, 5.00000, true, null, null, 1000, 2000, 'PARTNER'),
       ('DISABLED_TEST', '24 месяца', null, 720, 20.00000, false, null, null, 10, 20, 'YANDEX'),
       ('CATEGORY_TEST', '24 месяца', null, 720, 20.00000, true, '{90945}', '{133}', 10000, 20000, 'PARTNER'),
       ('BRAND_TEST', '24 месяца', null, 721, 20.00000, true, '{90867}', '{142}', 100000, 200000, 'PARTNER');
