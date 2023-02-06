#!/usr/bin/perl

# $Id$ 

=head1
 
    Тест проверяет, что одна и та же картинка при сохранении 
    будет получать один и тот же хеш. 
    Варианты, почему это может быть не так: 
    в комменатрий в картинке может добавляться время создания/модификации.

    Test::MockTime::set_fixed_time на ImageMagick не действует, 
    поэтому сравниваем хеш с эталонным, посчитанным один раз. 

    Со сменой версии ImageMagick тест может сломаться 
    (будут генерироваться другие бинарные данные), 
    тогда эталонный хеш можно поменять.
    Но важно, чтобы в неизменном окружении от запуска к запуску хеш считался бы одинаковым.

=cut

use warnings;
use strict;
use Test::More;

use Yandex::DBUnitTest qw/copy_table/;

use Settings;
use BannerImages;

use FindBin qw/$Bin/;

use utf8;

copy_table(PPCDICT, 'media_formats', with_data=>1);
open(my $fh, "<", "$Bin/hash_repeatability.data.png") or die "can't open test image";

my $png = join "", <$fh>;

my $im = BannerImages::_im_open_data($png);
is($im->Get('width'),  150, 'image loaded');
my $new_png = BannerImages::_im_get_data($im);
my $md5 = banner_check_image($new_png)->{md5};

is($md5, "JSu_pJid6gzO466JCP19PA", "hash of image shouldn't depend on time image was created");

done_testing();

