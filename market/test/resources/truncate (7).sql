truncate table
    sc_ds_partner_mapping,
    stock_log,
    order_scan_log,
    user_pwd_scan_log,
    operation_log,
    user_auth_log,

    postponed_operation,
    postponed_place_operation,

    sortable_barcode,
    route_task_cell_finish,
    route_finish_place,
    place_partner_code,
    place_history,
    place_item,
    place,

    user_password,

    pool,
    cell_pool,

    order_logistic_point,
    target_logistic_point,
    order_ff_status_history,
    order_history_text,
    order_item,
    order_ticket,
    orders,
    order_update_history,
    order_sender_verification,

    route_cell,
    in_advance_reserve,
    route_finish_order,
    route_finish,
    route_history_text,
    route,

    warehouse_property,
    warehouse_schedule,
    warehouse,

    courier,
    courier_shift,

    client_return,

    delivery_service_intake_schedule,
    delivery_service_property,
    delivery_service,

    location,
    measurements,
    print_task_field,
    print_task,

    sorting_center_property,
    user_property,

    users,
    cell,
    lot_history,
    lot,
    zone,
    partner_mapping,
    partner_mapping_group,
    partner_mapping_group_item,
    cross_dock_mapping,
    sorting_center,
    sorting_center_partner,

    measurements_so,
    sortable_history,
    sortable,
    site,

    route_so_finish_sortable,
    route_so_finish,
    route_so_site,
    route_so,

    queue_task,
    queue_log,
    inbound_info,
    inbound,
    registry,
    inbound_courier,
    registry_order,
    inbound_status_history,
    registry_sortable,

    outbound,
    outbound_status_history,
    outbound_registry,
    outbound_documents,
    outbound_trn_docs,

    sort_error_log,

    process_flow,
    flow_operation,
    flow_stage,
    flow_operation_context,
    operation,
    process,
    zone_process,
    flow,

    configuration,
    schrodinger_box.archive,
    c2c_box_size_class;

alter sequence registry_id_seq restart with 1;
alter sequence lot_id_seq restart with 1;
alter sequence route_id_seq restart with 1000;
alter sequence route_so_id_seq restart with 2000;
alter sequence orders_id_seq restart with 3000;
alter sequence place_id_seq restart with 4000;
alter sequence if exists sortable_barcode_seq restart with 100000;
alter sequence if exists transfer_material_values_for_storage_act_seq restart with 1;
alter sequence if exists order_ff_status_history_id_seq restart with 1;
