/* pgmigrate-encoding: utf-8 */
select copy_products('%0_inapp_apple_for_disk', '_inapp_apple_for_disk', '_inapp_apple_for_disk_test');

-- добавляем новую скидочную линейку и добавляем в нее созданные продукты
with promo_line_id as
         (
             insert into product_lines (product_set_id, created_at, order_num, selector_bean_el)
                 values ((select id from product_sets where key = 'inapp_ios_disk'), now(), 1001,
                         'productLineSelectorFactory.availableSelector()')
                 returning id
         ),
     order_num as
         (
             select code, order_num
             from (
                      values ('%standard100%', 1),
                             ('%premium1000%', 2),
                             ('%premium3000%', 3),
                             ('%premium5000%', 4)
                  ) s(code, order_num)
         )
insert
into user_products_to_product_lines (product_line_id, user_product_id, order_num)
    (
        select promo_line_id.id, up.id, order_num
        from promo_line_id,
             user_products up
                 join order_num o on up.code like o.code
        where up.code like '%_inapp_apple_for_disk_test'
    );

-- создадим акцию для тестирования
    insert
into promo_templates
(code, description, promo_name_tanker_key_id, from_date, to_date, application_area, application_type, duration,
 duration_measurement, activation_email_template_key, mobile_background_url, created_at)
values ('ios_introductory_test', 'тестируем интро',
        (select id from tanker_keys where project = 'disk-ps-billing' and key_set = 'for_tests' and key = 'test_key'),
        now(), null,
        'per_user'::promo_application_area, 'multiple_time'::promo_application_type, null, null, null,
        'https://picture.jpg',
        now());

-- добавим в акцию созданную линейку
insert into promo_product_lines(promo_template_id, product_line_id)
VALUES ((select id from promo_templates where code = 'ios_introductory_test'),
        (select l.id
         from product_lines l
                  join product_sets ps on l.product_set_id = ps.id
         where ps.key = 'inapp_ios_disk'
           and l.order_num = 1001));
