#!/usr/bin/perl

#    $Id:$ 

use warnings;
use strict;
use Test::More tests => 6;

use BannerImages;
use HashingTools;

use utf8;
use open ':std' => ':utf8';

*od = \&BannerImages::_im_open_data;
*gd = \&BannerImages::_im_get_data;

my $im_data =
'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAABmJLR0QAAAAAAAD5Q7t_AAAACXBIWXMAAABIAAAASABGyWs-AAAACXZwQWcAAAABAAAAAQDHlV_tAAAADUlEQVQIHWP4__8_AwAI_AL-5gz_qwAAAABJRU5ErkJggg==' ; # 1x1 png


$im_data = decode_64ya($im_data);

my $im = od($im_data);
isa_ok($im, 'Image::Magick', 'created IM object');
ok($im->Get('width') == 1, 'image loaded');
my $new_data = gd($im);

my $im2 = od($new_data);
isa_ok($im2, 'Image::Magick', 'created IM object 2');
ok($im2->Get('magick') eq 'PNG', 'loaded new data');

is($im2->Get('columns'), 1, "image width");
is($im2->Get('rows'), 1, "image height");


