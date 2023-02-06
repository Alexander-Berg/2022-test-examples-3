delete
from common_property
where name = 'disable_upload_installments';

truncate custom_group_to_installment,
    installment_custom_group,
    installment_group,
    installment_files,
    installment CASCADE;

INSERT INTO installment (id, name, description, duration, percentage, enabled, category_ids, brand_ids,
                         supplier_type, min_price, max_price)
VALUES ('MONTH_AND_HALF', '1.5 месяца', 'Не на все категории', 45, 3.00000, false, '{90947, 90946}', null, 'YANDEX',
        1000, 60000),
       ('HALF_YEAR', '6 месяцев', null, 180, 5.00000, true, null, null, 'PARTNER', 3000, 200000),
       ('YEAR', '12 месяцев', null, 360, 10.00000, true, null, null, 'PARTNER', 3000, 200000),
       ('TWO_YEARS', '24 месяца', null, 720, 20.00000, true, null, null, 'PARTNER', 3000, 200000);

-- shops 101,102,103
-- general
insert into installment_group
values (101, 'HALF_YEAR'),
       (101, 'YEAR'),

       (-99, 'MONTH_AND_HALF'), -- have no enabled installments

       (102, 'MONTH_AND_HALF'), --disabled
       (102, 'HALF_YEAR'),

       (103, 'MONTH_AND_HALF'), -- disabled
       (103, 'HALF_YEAR'),
       (103, 'YEAR'),
       (103, 'TWO_YEARS');

insert into installment_files (resource_id, shop_id, business_id, name, status, file_type)
values ('one-two-three-id-201', 201, 201201, 'filename.file', 'DONE', 'INSTALLMENT');

--custom
insert into installment_custom_group
values (1011, 'group-101-1', 101, '{90867}', '{2,3,4}', true, '2020-10-10', '2020-10-10', 'DYNAMIC_GROUPS', null),
       (1012, 'group-101-2', 101, '{90945}', '{2,3,4}', true, '2020-10-10', '2020-10-10', 'DYNAMIC_GROUPS', null),

       (-981, 'group--99-1', -98, '{90867}', '{2,3,4}', false, '2020-10-10', '2020-10-10', 'DYNAMIC_GROUPS', null), -- disabled

       (1021, 'group-102-1', 102, '{90867, 90947, 90946}', '{2,3,4}', true, '2020-10-10', '2020-10-10', 'DYNAMIC_GROUPS', null),
       (1022, 'group-102-2', 102, '{90949, 90948, 90951}', '{2,3,4}', true, '2020-10-10', '2020-10-10', 'DYNAMIC_GROUPS', null),

       (1031, 'group-103-1', 103, '{90867, 90947}', '{2,3,4}', true, '2020-10-10', '2020-10-10', 'DYNAMIC_GROUPS', null),
       (1032, 'group-103-2', 103, '{90946, 90949}', '{2,3,4}', true, '2020-10-10', '2020-10-10', 'DYNAMIC_GROUPS', null),
       (1033, 'group-103-3', 103, '{90948, 90951}', '{2,3,4}', false, '2020-10-10', '2020-10-10',
        'DYNAMIC_GROUPS', null),                                                                                    --disabled group
       (1034, 'group-103-2', 103, '{90946, 90949}', '{2,3,4}', true, '2020-10-10', '2020-10-10',
        'DYNAMIC_GROUPS', null),                                                                                    -- duplicate 1032
       (1035, 'group-103-2', 103, '{1}', '{2,3,4}', true, '2020-10-10', '2020-10-10', 'DYNAMIC_GROUPS', null), --invalid category

       (2011, 'group-201-1', 201, '{}', '{}', true, '2020-10-10', '2020-10-10', 'FILE', 'one-two-three-id-201');


insert into custom_group_to_installment
values (1011, 'HALF_YEAR'),
       (1011, 'YEAR'),
       (1012, 'HALF_YEAR'),
       (1012, 'YEAR'),

       (-981, 'YEAR'),

       (1021, 'MONTH_AND_HALF'), --disabled
       (1021, 'HALF_YEAR'),
       (1022, 'MONTH_AND_HALF'), --disabled
       (1022, 'HALF_YEAR'),

       (1031, 'MONTH_AND_HALF'), -- disabled
       (1031, 'HALF_YEAR'),
       (1031, 'YEAR'),
       (1031, 'TWO_YEARS'),
       (1032, 'MONTH_AND_HALF'), -- disabled
       (1032, 'HALF_YEAR'),
       (1033, 'YEAR'),
       (1033, 'TWO_YEARS'),
       (1034, 'MONTH_AND_HALF'), -- duplicate 1032
       (1034, 'HALF_YEAR'),      -- duplicate 1032
       (1035, 'MONTH_AND_HALF'), --invalid category
       (1035, 'HALF_YEAR'); --invalid category


insert into installment_skus (resource_id, sku, msku)
values ('one-two-three-id-201', 'dvcxz', 2), -- 6, 12
       ('one-two-three-id-201', 'dnhcg', 3), -- 6,24
       ('one-two-three-id-201', 'dmfvid', 4), -- 6,24
       ('one-two-three-id-201', 'dmvhjnhcg', 5); -- 6,12,24

insert into sku_to_installments (msku, resource_id, installment_id, sku)
values (2, 'one-two-three-id-201', 'HALF_YEAR', 'dvcxz'),
       (2, 'one-two-three-id-201', 'YEAR', 'dvcxz'),
       (3, 'one-two-three-id-201', 'HALF_YEAR', 'dnhcg'),
       (3, 'one-two-three-id-201', 'TWO_YEARS', 'dnhcg'),
       (4, 'one-two-three-id-201', 'HALF_YEAR', 'dmfvid'),
       (4, 'one-two-three-id-201', 'TWO_YEARS', 'dmfvid'),
       (5, 'one-two-three-id-201', 'HALF_YEAR', 'dmvhjnhcg'),
       (5, 'one-two-three-id-201', 'YEAR', 'dmvhjnhcg'),
       (5, 'one-two-three-id-201', 'TWO_YEARS', 'dmvhjnhcg');
