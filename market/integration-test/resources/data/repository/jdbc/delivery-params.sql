TRUNCATE delivery_distributor_params;

INSERT INTO delivery_distributor_params(id,
                                        delivery_cost,
                                        delivery_duration,
                                        flag_id,
                                        location_id,
                                        min_weight,
                                        max_weight,
                                        strict_bounds_type)
VALUES (1, 1, 10, 1, 2, 0, 10, 'full');
