insert into partner(
    id, type, name
)
values (
    223462, 'DELIVERY','Иствард'
),(
    223463, 'DELIVERY','Альтика М'
);

insert into routing_transport_type(
    id, name, capacity, routing_priority
) values
(1, 'Газель', 4.5, 10),
(2, 'Грузовик', 10, 20);

insert into routing_order_tag (
    id, name, description
) values (
    100, 'dropship', 'Забор'
);

insert into routing_transport_type_routing_order_tag(
    routing_transport_type_id, routing_order_tag_id
)
values (1, 100),
    (2, 100);

insert into routing_transport_schedule_rule(
    id,
    name,
    transport_type_id,
    vehicle_type,
    shift_start_time,
    shift_end_time,
    end_day_offset,
    depot_id,
    delivery_service_id,
    count
)
values
(
       1,
       '3 газельки от Истварда',
       1,
       'CAR',
       '0:00:00',
       '0:00:00',
       1,
       172,
       223462,
       3
),
(
        2,
        '2 грузовика от Исварда',
        2,
        'CAR',
        '0:00:00',
        '0:00:00',
        1,
        172,
        223462,
        2
);

select setval(pg_get_serial_sequence('routing_transport_schedule_rule', 'id'), 3);