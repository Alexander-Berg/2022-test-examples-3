#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use Test::MockTime ();

use Settings;
use Yandex::DBUnitTest qw/:all/;

use Holidays qw/
    is_great_holiday
    is_weekend
    is_workday
    is_holiday
    is_holiday_everywhere
    is_weekend_workday
    regions_with_holidays
    regions_with_weekend_workday
    /;

my @tests = (
    # [$input, $expected_out_ru, $eo_ua, $eo_weekend, $eo_workday, $eo_weekend_workday]
    ['2009-05-01', 1, 0, 0, 0, 0],
    ['2009-05-02', 1, 0, 1, 0, 0],
    ['2009-05-03', 0, 1, 1, 0, 0],
    ['2009-01-01', 0, 0, 0, 1, 0],
    ['2009-12-31', 0, 0, 0, 1, 0],
    ['2010-05-01', 1, 0, 1, 0, 0],
    ['2010-05-02', 0, 0, 1, 1, 1],
    ['2010-05-03', 1, 1, 0, 0, 0],
    ['2000-05-02', 0, 0, 0, 1, 0],
    ['20090501',   1, 0, 0, 0, 0],
    ['20000502',   0, 0, 0, 1, 0],
    ['2016-08-05', 1, 1, 0, 0, 0],
    [undef,        0, 1, 1, 0, 0], # 2009-05-03
);

Test::More::plan(tests => scalar(@tests) * 6 + 6);

Test::MockTime::set_fixed_time('2009-05-03T07:32:19Z');

my $dataset = {
    great_holidays => {
        original_db => PPCDICT,
        rows => [
            {holiday_date => '2009-05-01', region_id => 225, type => 'holiday'},
            {holiday_date => '2009-05-02', region_id => 225, type => 'holiday'},
            {holiday_date => '2009-05-03', region_id => 187, type => 'holiday'},
            {holiday_date => '2010-05-01', region_id => 225, type => 'holiday'},
            {holiday_date => '2010-05-03', region_id => 225, type => 'holiday'},
            {holiday_date => '2010-05-02', region_id => 225, type => 'workday'},
            {holiday_date => '2010-05-03', region_id => 187, type => 'holiday'},
            {holiday_date => '2016-08-07', region_id => 225, type => 'workday'},
            (map {{holiday_date => '2016-08-05', region_id => $_, type => 'holiday'}} @Holidays::LOCAL_HOLIDAYS_SUPPORTED_REGIONS),
        ]
    }
};
init_test_dataset($dataset);

for my $test (@tests) {
    my ($input, $expected_out, $expected_out_ua, $expected_out_weekend, $expected_out_workday, $eo_weekend_workday) = @$test;
    my $test_name = 'is_great_holiday(' . ($input // 'undef') . ") = $expected_out";
    is(is_great_holiday($input) ? 1 : 0, $expected_out, $test_name);

    $test_name = 'is_great_holiday(' . ($input // 'undef') . ", 187) = $expected_out_ua";
    is(is_great_holiday($input, 187) ? 1 : 0, $expected_out_ua, $test_name);

    $test_name = 'is_weekend(' . ($input // 'undef') . ") = $expected_out_weekend";
    is(is_weekend($input) ? 1 : 0, $expected_out_weekend, $test_name);

    $test_name = 'is_workday(' . ($input // 'undef') . ") = $expected_out_workday";
    is(is_workday($input) ? 1 : 0, $expected_out_workday, $test_name);

    $test_name = 'is_holiday(' . ($input // 'undef') . ") ? 0 : 1 = $expected_out_workday";
    is(is_holiday($input) ? 0 : 1, $expected_out_workday, $test_name);

    $test_name = 'is_weekend_workday(' . ($input // 'undef') . ") = $eo_weekend_workday";
    is(is_weekend_workday($input) ? 1 : 0, $eo_weekend_workday, $test_name);
}

is(is_holiday_everywhere('2016-08-05') ? 1 : 0, 0);
is(is_holiday_everywhere('2016-08-06') ? 1 : 0, 1);
is(is_holiday_everywhere('2016-08-07') ? 1 : 0, 0);

is_deeply(regions_with_holidays('2010-05-03'), [225, 187]);
is_deeply(regions_with_holidays('2016-08-05'), \@Holidays::LOCAL_HOLIDAYS_SUPPORTED_REGIONS);
is_deeply(regions_with_weekend_workday('2016-08-07'), [225]);
