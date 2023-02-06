#!/usr/bin/perl

#    $Id:$ 

use warnings;
use strict;
use Test::More;

use Yandex::DBUnitTest qw/copy_table/;

use Settings;
use BannerImages;
use HashingTools;

use utf8;
use open ':std' => ':utf8';

copy_table(PPCDICT, 'media_formats', with_data=>1);

*od = \&BannerImages::_im_open_data;
*gd = \&BannerImages::_im_get_data;

my $im_data =
'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAABmJLR0QAAAAAAAD5Q7t_AAAACXBIWXMAAABIAAAASABGyWs-AAAACXZwQWcAAAABAAAAAQDHlV_tAAAADUlEQVQIHWP4__8_AwAI_AL-5gz_qwAAAABJRU5ErkJggg=='; # 1x1 png


$im_data = decode_64ya($im_data);

my $img = banner_check_image($im_data);


ok($img->{contentType} eq 'image/png', "correct image type");
ok($img->{width} == 1 && $img->{height} == 1, "image size");

my $im = od($img->{img});
$im->Resize(geometry => '100x100!');
my $img_final = banner_check_image(gd($im));
$im->Resize(geometry => $BannerImages::BANNER_IMAGE_MIN_SIZE*2 .'x'. $BannerImages::BANNER_IMAGE_MIN_SIZE*2);
my $img_raw = banner_check_image(gd($im));

ok($img_final->{contentType} eq 'image/png', "correct image type");

ok(banner_image_check_size(@$img_raw{qw/width height/}), 'banner_image_check_size');


done_testing();
