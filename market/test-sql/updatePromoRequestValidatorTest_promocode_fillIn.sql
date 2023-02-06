INSERT INTO public.promos (id, promo_id, promo_key, source, name, status, mechanics_type, created_at,
                           start_at, end_at) VALUES (1, 'cf_0000000073', 'rQLhbHJ-h11lrdRaH3iepQ', 'CATEGORYIFACE', '111', 'NEW', 'cheapest_as_gift', '1646338282', '1668103200', '1670781599');
insert into mechanics_promocode (id, promo_id, code_type, "value", code, min_cart_price, max_cart_price,
                                 apply_multiple_times, budget, additional_conditions)
values (1, 1, 'PERCENTAGE', 10, 'CODE', 10, 200000, false, 999999, 'Additional conditions');
