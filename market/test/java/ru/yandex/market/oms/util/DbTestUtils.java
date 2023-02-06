package ru.yandex.market.oms.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;


@Component
public final class DbTestUtils {

    private final JdbcTemplate jdbcTemplate;

    public DbTestUtils(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // В будущем заменится удобным механизмом вставки позиций заказа
    public void insertOrder(Long orderId, Long userId, Long deliveryId, OrderStatus status) {
        jdbcTemplate.update("INSERT INTO public.orders\n" +
                "(id, user_id, shop_id, fake, currency, buyer_currency, items_total, buyer_items_total, total, " +
                "buyer_total, fee_total, payment_type, payment_method, delivery_id, buyer_id, shop_order_id, status, " +
                "substatus, notes, created_at, updated_at, status_updated_at, status_expiry_at, last_history_id, " +
                "exchange_rate, payment_id, balance_order_id, refund_planned, refund_actual, user_group, no_auth, " +
                "accept_method, muid, context, is_archived, real_total, subsidy_id, subsidy_balance_order_id, " +
                "subsidy_refund_planned, subsidy_refund_actual, \"global\", is_booked, shop_tax_system, " +
                "subsidy_total, fulfilment, rgb, compensation_payment_id, cancellation_request_substatus, " +
                "cancellation_request_notes, preorder, external_certificate_id, ignore_stocks, substatus_updated_at, " +
                "partition_index, bnpl, business_id, payment_submethod, cancel_expired_at)\n" +
                "VALUES(?, ?, 431782, false, 'RUR', 'RUR', 737.00, 737.00, 836.00, 836.00, 0.74, 1, 3, " +
                "?, 4015265, '4032417', ?, 29, NULL, '2019-08-29 15:32:09.023', '2022-04-27 13:29:21.867', " +
                "'2019-08-29 15:32:18.674', '2019-09-05 15:32:18.674', 281340797, 1.00000000, NULL, NULL, NULL, NULL," +
                " 0, false, 1, NULL, 0, false, NULL, NULL, NULL, NULL, NULL, false, false, 0, 0.00, true, 1, NULL, " +
                "NULL, NULL, false, NULL, false, '2019-08-29 15:32:09.023', NULL, NULL, -1, NULL, NULL);", orderId,
                userId, deliveryId, status.getId());
    }

    public void deleteOrder(Long orderId) {
        jdbcTemplate.update("DELETE FROM public.orders WHERE id = ?", orderId);
    }

    public void insertOrderItem(Long orderId, Long itemId) {
        jdbcTemplate.update("INSERT INTO public.order_item\n" +
                "(order_id, feed_id, offer_id, category_id, feed_category_id, model_id, title, description, pic_url, " +
                "price, buyer_price, count, fee, fee_sum, show_uid, ware_md5, real_show_uid, fee_int, shop_url, " +
                "kind2_params, vat, id, balance_order_id, subsidy, sku, shop_sku, fulfilment_shop_id, " +
                "ff_delivery_balance_order_id, classifier_magic_id, prepay_enabled, is_recommended_by_vendor, " +
                "buyer_discount, weight, width, height, \"depth\", ff_subsidy_balance_order_id, has_snapshot, " +
                "agency_commission, supplier_type, msku, preorder, english_name, hs_code, category_full_name, " +
                "feed_price, warehouse_id, feed_group_id_hash, fit_freezed, freeze_updated_at, " +
                "item_description_english, link, supplier_description, supplier_work_schedule, " +
                "manufacturer_countries, vendor_id, cargo_types, partner_price, partner_price_markups, " +
                "supplier_currency, at_supplier_warehouse, bundle_id, count_in_bundle, external_feed_id, " +
                "fulfilment_warehouse_id, primary_in_bundle, instances, cart_show_uid, pp, loyalty_program_partner, " +
                "digital, seller_inn, bnpl, medical_specs_internal, prescription_guids, quantity, quant_price, " +
                "unit_info, count_step, surcharge_balance_order_id)\n" +
                "VALUES(?, 475690, '200344841.10122', 91491, '', 14112311, 'Смартфон ASUS ZenFone 3 ZE520KL " +
                "32GB золотистый', '', '//avatars.mds.yandex.net/get-mpic/199079/img_id1331218200463056471/50x50', " +
                "737.00, 737.00, 1, 0.0300000000, 0.74, '156708192525126386905', '-08kO7GPpSOppv5rr3Oouw', " +
                "'15670819252512638690506001', 300, 'https://beru" +
                ".ru/product/100131945205?offerid=-08kO7GPpSOppv5rr3Oouw', '[{\"type\":\"enum\"," +
                "\"subType\":\"image_picker\",\"name\":\"Цвет товара\",\"value\":\"золотистый\",\"code\":\"#FFD700\"," +
                "\"specifiedForOffer\":true}]', 2, ?, NULL, NULL, NULL, '10122', 10264538, NULL, " +
                "'8e79e6642cad5847e18620df0d8be91e', false, false, NULL, 1000, 11, 11, 11, NULL, false, NULL, 3, " +
                "100131945205, false, NULL, NULL, 'Мобильные телефоны', NULL, 145, NULL, 1, '2019-08-29 15:32:09" +
                ".565', '', NULL, NULL, 'Пн-Пт: 10:00-19:00, Сб-Вс: 10:00-18:00', '[]', 152863, '{40,200}', NULL, " +
                "'null', 'RUR', false, NULL, 1, NULL, 145, NULL, '[{\"sn\": null, \"UIT\": null, \"cis\": null, " +
                "\"uit\": null, \"imei\": \"through-processing\", \"cisFull\": null, \"balanceOrderId\": null}, " +
                "{\"sn\": \"also\", \"UIT\": null, \"cis\": null, \"uit\": null, \"imei\": \"321-2\", \"cisFull\": " +
                "null, \"balanceOrderId\": null}]'::jsonb::jsonb, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, " +
                "NULL, NULL, NULL, NULL, NULL);", orderId, itemId);
    }

    public void deleteAllOrderItems(Long orderId) {
        jdbcTemplate.update("DELETE FROM public.order_item WHERE order_id = ?", orderId);
    }

    public void insertOrderDelivery(Long orderId, Long deliveryId, Long addressId, Integer[] features) {
        insertDeliveryAddress(deliveryId, addressId);
        jdbcTemplate.update("INSERT INTO public.order_delivery\n" +
                "(id, order_id, \"type\", service_name, price, buyer_price, from_date, to_date, from_time, to_time, " +
                "region_id, outlet_id, shop_int_id, reserved_until, validated_from_date, validated_to_date, " +
                "validated_reserved_until, buyer_address_id, delivery_service_id, order_shipment_id, " +
                "delivery_partner_type, vat, outlet_code, balance_order_id, user_received, shop_address_id, " +
                "commission_percentage, buyer_discount, post_outlet_id, post_address_id, free_delivery_threshold, " +
                "free_delivery_remaining, ignore_sla_check, tariff_data, price_for_shop, tariff_id, " +
                "outlet_storage_period, outlet_storage_limit_date, outlet_purpose, subsidy_balance_order_id, " +
                "features, market_branded, on_demand_outlet_id, verification_code, lift_type, lift_price, " +
                "leave_at_the_door, market_partner, market_post_term, estimated)\n" +
                "VALUES(?, ?, 1, 'Самовывоз', 99.00, 99.00, '2019-08-30', '2019-08-30', NULL, NULL, 213," +
                " 76973275, NULL, NULL, NULL, NULL, NULL, NULL, 200, NULL, 1, 7, '1042614', NULL, false, ?, 200" +
                ".00, NULL, NULL, NULL, NULL, NULL, false, '{\"tariffCode\": \"x5-test-pp-buyer\", " +
                "\"customsLanguage\": null, \"customsLanguages\": null, \"needPersonalData\": false, " +
                "\"customsTranslation\": false}'::jsonb::jsonb, NULL, 4238, NULL, NULL, NULL, NULL, ?, NULL, NULL," +
                " NULL, NULL, NULL, NULL, NULL, NULL, NULL);\n", deliveryId, orderId, addressId, features);
    }

    public void deleteOrderDelivery(Long deliveryId, Long addressId) {
        jdbcTemplate.update("DELETE FROM public.order_delivery WHERE id = ?", deliveryId);
        deleteDeliveryAddress(addressId);
    }

    public void insertDeliveryAddress(Long deliveryId, Long addressId) {
        jdbcTemplate.update("INSERT INTO public.delivery_address\n" +
                "(id, order_id, country, postcode, city, subway, street, km, house, block, building, estate, " +
                "entrance, entryphone, floor, apartment, gps, notes, recipient, phone, \"language\", schedule_string," +
                " precise_region_id, recipient_last_name, recipient_first_name, recipient_middle_name, outlet_name, " +
                "outlet_phones, district, recipient_email, yandex_map_permalink, business_recipient, " +
                "personal_phone_id, calendar_holidays_string)\n" +
                "VALUES(?, ?, 'Россия', '142918', 'Москва', NULL, 'Банная ул', NULL, '25', '1', 'Б', " +
                "NULL, NULL, NULL, NULL, NULL, '36.484687,54.738937', 'Дополнительная информация', 'Пункт выдачи X5'," +
                " '7;800;2009555;;\n" +
                "', 0, '<WorkingTime><WorkingDaysFrom>7</WorkingDaysFrom><WorkingDaysTill>7</WorkingDaysTill" +
                "><WorkingHoursFrom>08:00</WorkingHoursFrom><WorkingHoursTill>22:00</WorkingHoursTill></WorkingTime" +
                "><WorkingTime><WorkingDaysFrom>1</WorkingDaysFrom><WorkingDaysTill>1</WorkingDaysTill" +
                "><WorkingHoursFrom>08:00</WorkingHoursFrom><WorkingHoursTill>22:00</WorkingHoursTill></WorkingTime" +
                "><WorkingTime><WorkingDaysFrom>2</WorkingDaysFrom><WorkingDaysTill>2</WorkingDaysTill" +
                "><WorkingHoursFrom>08:00</WorkingHoursFrom><WorkingHoursTill>22:00</WorkingHoursTill></WorkingTime" +
                "><WorkingTime><WorkingDaysFrom>3</WorkingDaysFrom><WorkingDaysTill>3</WorkingDaysTill" +
                "><WorkingHoursFrom>08:00</WorkingHoursFrom><WorkingHoursTill>22:00</WorkingHoursTill></WorkingTime" +
                "><WorkingTime><WorkingDaysFrom>4</WorkingDaysFrom><WorkingDaysTill>4</WorkingDaysTill" +
                "><WorkingHoursFrom>08:00</WorkingHoursFrom><WorkingHoursTill>22:00</WorkingHoursTill></WorkingTime" +
                "><WorkingTime><WorkingDaysFrom>5</WorkingDaysFrom><WorkingDaysTill>5</WorkingDaysTill" +
                "><WorkingHoursFrom>08:00</WorkingHoursFrom><WorkingHoursTill>22:00</WorkingHoursTill></WorkingTime" +
                "><WorkingTime><WorkingDaysFrom>6</WorkingDaysFrom><WorkingDaysTill>6</WorkingDaysTill" +
                "><WorkingHoursFrom>08:00</WorkingHoursFrom><WorkingHoursTill>22:00</WorkingHoursTill></WorkingTime" +
                ">', NULL, 'Пупкин', 'Василий', 'Петрович', 'Пункт выдачи X5', '{\"7;800;2009555;;\"}', NULL, NULL, " +
                "NULL, NULL, 'dbb98fe1a1424b2890365d6a80dcdd44', NULL);\n", addressId, deliveryId);
    }

    public void deleteDeliveryAddress(Long addressId) {
        jdbcTemplate.update("DELETE FROM public.delivery_address WHERE id = ?", addressId);
    }

    public void insertOrderProperties(Long orderId) {
        jdbcTemplate.update("INSERT INTO public.order_property\n" +
                        "(order_id, \"name\", text_value)\n" +
                        "VALUES(?, 'ignoreOrderInMdb', '1');\n" +
                        "INSERT INTO public.order_property\n" +
                        "(order_id, \"name\", text_value)\n" +
                        "VALUES(?, 'isCrossborder', 'false');\n" +
                        "INSERT INTO public.order_property\n" +
                        "(order_id, \"name\", text_value)\n" +
                        "VALUES(?, 'isWebIntegrationTest', '1');\n" +
                        "INSERT INTO public.order_property\n" +
                        "(order_id, \"name\", text_value)\n" +
                        "VALUES(?, 'mrid', 'autotest/1');\n" +
                        "INSERT INTO public.order_property\n" +
                        "(order_id, \"name\", text_value)\n" +
                        "VALUES(?, 'isEda', 'true');\n",
                orderId, orderId, orderId, orderId, orderId);
    }

    public void deleteOrderProperties(Long orderId) {
        jdbcTemplate.update("DELETE FROM public.order_property WHERE order_id = ?", orderId);
    }
}
