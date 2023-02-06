#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use HashingTools;

BEGIN {
    use_ok('Direct::Model::ImageFormat');
    no warnings 'redefine';
    *{BannerImages::are_logos_enabled} = sub {return 1};
}

{
my $img_data = 'fake image data';
my $fmt = Direct::Model::ImageFormat->new(image => $img_data);
ok($fmt->hash);
ok($fmt->info->{error});
}

{
my $img_data = 
'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAABmJLR0QAAAAAAAD5Q7t_AAAACXBIWXMAAABIAAAASABGyWs-AAAACXZwQWcAAAABAAAAAQDHlV_tAAAADUlEQVQIHWP4__8_AwAI_AL-5gz_qwAAAABJRU5ErkJggg=='; # 1x1 png

$img_data = decode_64ya($img_data);
my $fmt = Direct::Model::ImageFormat->new(image => $img_data);
ok($fmt->hash);
ok(!$fmt->info->{error});
ok($fmt->width > 0 && $fmt->height > 0);
}

done_testing;

