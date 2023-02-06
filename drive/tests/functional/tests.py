from suites import basic_operations_tests, basic, minutes_offer_tests, hourly_offer_tests, radar_tests, block_user_tests \
    , fix_point_tests, future_car_tests, minimal_price_tests, switch_offers_tests

SUITES = [
    basic.ValidateSchemas,
    basic_operations_tests.BasicOperationsSuite,
    minutes_offer_tests.MinutesOfferSuite,
    hourly_offer_tests.HourlyOfferSuite,
    radar_tests.RadarSuite,
    block_user_tests.BlockUserSuite,
    fix_point_tests.FixPointOfferSuite,
    future_car_tests.FutureCarSuite,
    minimal_price_tests.MinimalPriceSuite,
    switch_offers_tests.SwitchOfferSuite,
]
