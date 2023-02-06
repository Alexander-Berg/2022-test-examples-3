'use strict';

const HOST = 'http://laas.yandex.ru';

const ROUTE = /\/region/;

const RESPONSE = {
    region_id: 213,
    precision: 2,
    latitude: 55.753215,
    longitude: 37.622504,
    should_update_cookie: false,
    is_user_choice: false,
    suspected_region_id: 213,
    city_id: 213,
    region_by_ip: 213,
    suspected_region_city: 213,
    location_accuracy: 15000,
    location_unixtime: 1545314822,
    suspected_latitude: 55.753215,
    suspected_longitude: 37.622504,
    suspected_location_accuracy: 15000,
    suspected_location_unixtime: 1545314822,
    suspected_precision: 2,
    probable_regions_reliability: 1.7,
    probable_regions: [{ region_id: 213, weight: 0.99 }],
    country_id_by_ip: 225,
    is_anonymous_vpn: false,
    is_public_proxy: false,
    is_serp_trusted_net: false,
    is_tor: false,
    is_hosting: false,
    is_gdpr: false,
    is_mobile: false,
    is_yandex_net: true,
    is_yandex_staff: false,
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
