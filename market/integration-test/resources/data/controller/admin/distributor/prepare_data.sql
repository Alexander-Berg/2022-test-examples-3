TRUNCATE delivery_distributor_params RESTART IDENTITY CASCADE;

INSERT INTO delivery_distributor_params (flag_id, location_id, min_weight,
                                         max_weight, strict_bounds_type,
                                         delivery_cost, delivery_duration,
                                         created, updated)
VALUES (1, 1, 1, 1, 'none', 1, 1, '2018-10-29T00:00:00.00000', '2018-10-29T00:00:00.00000');
