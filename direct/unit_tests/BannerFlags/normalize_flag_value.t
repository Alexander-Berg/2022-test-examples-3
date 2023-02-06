#!/usr/bin/perl

use strict;
use Test::More;
use Test::Deep;

BEGIN { use_ok('BannerFlags'); }

use warnings;
use utf8;

my @tests = (
    [age => 18, "18+", "age number without postfix"],
    [age => "18+", "18+", "age number with postfix"],
    [age => 0, "0+", "age number without postfix 2"],
    [age => "0", "0+", "age number without postfix 2"],
    [age => "16p", "16+", "age number with wrong postfix"],
    [baby_food => 5, "5months", "baby_food number without postfix"],
    [baby_food => "5months", "5months", "baby_food number with postfix"],
    [baby_food => "5mnths", "5months", "baby_food number with postfix"],
    [age => "", "99+", "empty value"],
    [age => undef, "99+", "undef value"],
    [age => "+wd", "+wd", "invalid value1"],
    [age => "wd+", "wd+", "invalid value2"],
    [age => "17+", "17+", "number not in varianst set"],
);

foreach my $test (@tests) {
    
    is(BannerFlags::normalize_flag_value($test->[0], $test->[1]), $test->[2], $test->[3]);
}

# Проверяем многократные вызовы над одними и теми же данными
foreach my $test (@tests) {
    # двойной вызов
    is(BannerFlags::normalize_flag_value(
          $test->[0], BannerFlags::normalize_flag_value($test->[0], $test->[1])), $test->[2], $test->[3]);

    # тройной вызов
    is(BannerFlags::normalize_flag_value(
          $test->[0], BannerFlags::normalize_flag_value(
                $test->[0], BannerFlags::normalize_flag_value($test->[0], $test->[1]))), $test->[2], $test->[3]);
}
done_testing;
