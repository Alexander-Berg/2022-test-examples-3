#!/usr/bin/perl

use Direct::Modern;

use Carp qw/croak/;
use Test::More;

use Settings;
use ModerateChecks;
use BannerFlags;
use geo_regions;

require my_inc;

*cmbfm = sub { return check_moderate_banner_for_regions(@_) };

my $forex_flag = BannerFlags::get_banner_flags_as_hash("forex");
my $empty_flag = BannerFlags::get_banner_flags_as_hash("");

# Смена Белоруссии на Украину С флагом forex
ok(!cmbfm($forex_flag, $geo_regions::UKR, $geo_regions::BY), "No moderation on change UKR to BY");

# Смена Белоруссии на Украину БЕЗ без флага forex 
is(cmbfm($empty_flag, $geo_regions::UKR, $geo_regions::BY), 0, "No flags and no moderation on change UKR to BY");

# Добавление России к Белоруссии БЕЗ без флага forex 
is(cmbfm($empty_flag, "$geo_regions::BY,$geo_regions::RUS", $geo_regions::BY), 0, "No flags");

# Добавление России к Белоруссии С флагом forex 
is(cmbfm($forex_flag, "$geo_regions::BY,$geo_regions::RUS", $geo_regions::BY), 1, "Add Russia with flag");

# Замена Белоруссии на Россию  С флагом forex 
is(cmbfm($forex_flag, $geo_regions::RUS, $geo_regions::BY), 1, "Change BY to Russia with flag");

# Замена Белоруссии на Россию БЕЗ флага forex 
is(cmbfm($empty_flag, $geo_regions::RUS, $geo_regions::BY), 0, "Change BY to Russia without flag");

# Замена России на часть России С флагом forex 
is(cmbfm($forex_flag, $geo_regions::MOSCOW, $geo_regions::RUS), 0, "Change Russia to part of Russia with flag");

# Замена России на часть России БЕЗ флага forex 
is(cmbfm($empty_flag, $geo_regions::MOSCOW, $geo_regions::RUS), 0, "Change Russia to part of Russia without flag");

# Замена части России на Россиию С флагом forex
is(cmbfm($forex_flag, $geo_regions::RUS, $geo_regions::MOSCOW), 0, "Change part of Russia to Russia with flag");

# Замена части России на Россиию БЕЗ флага forex
is(cmbfm($empty_flag, $geo_regions::RUS, $geo_regions::MOSCOW), 0, "Change part of Russia to Russia without flag");

done_testing;