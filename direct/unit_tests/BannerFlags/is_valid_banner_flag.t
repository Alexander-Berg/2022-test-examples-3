#!/usr/bin/perl

use warnings;
use strict;

use Test::More;
use BannerFlags;

*iv = \&BannerFlags::is_valid_banner_flag_name;

ok iv("unfamily");
ok iv("UnFamily");
ok iv("age:18");
ok iv("good_bad");
ok iv("good_bad:43");

ok !iv("ARRAY(0xTOIUGO)");
ok !iv("ARR AY");
ok !iv("ARR ");

done_testing;
