/* pgmigrate-encoding: utf-8 */

-- создаём тестовую линейку в тестинге/престейбле
insert into product_lines (product_set_id, created_at, order_num, selector_bean_el, description)
    select id, now(), 2000, 'productLineSelectorFactory.availableSelector()', 'web test start discount line'
    from product_sets
    where key = 'mail_pro_b2c'
      and exists(select * from environment
                 where key = 'env'
                   and value in ('test', 'prestable'));

-- переносим тестовые продукты в тестовую линейку в тестинге/престейбле
update user_products_to_product_lines
set product_line_id = (select id from product_lines where description = 'web test start discount line')
where user_product_id in (select id from user_products
                          where code in ('stat_period_test', 'stat_period_test_1_5', 'stat_period_test_4_1'))
  and exists(select * from environment
             where key = 'env'
               and value in ('test', 'prestable'));
