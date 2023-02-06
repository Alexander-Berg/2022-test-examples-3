#!/usr/bin/perl

#    $Id:$ 

use warnings;
use strict;
use Test::More;

use Settings;
use BannerImages;

use utf8;
use open ':std' => ':utf8';

BEGIN {
    use_ok('Direct::Model::ImageFormat');
}

{
    no warnings 'redefine';
    *BannerImages::are_logos_enabled = sub {return 1};
}

*b = \&banner_image_check_size;

ok(!b(79, 79), '< min size');

is(b(80, 80), 'logo');

ok(!b(80, 81), 'logo, not squared');

is(b(150, 150), 'small', '1:1 min');
is(b(151, 151), 'small', '1:1 min+1');
is(b(170, 170), 'small', '1:1');
is(b(150, 200), 'small', '3:4 min');
is(b(200, 150), 'small', '4:3 min');
is(b(2000, 1500), 'regular', 'big 4:3');

ok(!b(150, 201), '> 4:3');
ok(!b(201, 150), '< 4:3');

is(b(1600, 900), 'wide', 'wide 1600x900');
is(b(1080, 607), 'wide', 'wide 1080x607');
is(b(1080, 608), 'wide', 'wide 1080x608');
ok(!b(1080, 609), '1080x609');
ok(!b(1072, 603), '16x9 <min');
ok(!b(607, 1080), '9:16');

ok(!b(6070, 10800), 'extra big wide');
ok(!b(5001, 5000), 'extra big regular');


done_testing();
