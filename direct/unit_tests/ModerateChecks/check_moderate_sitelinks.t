#!/usr/bin/perl

# $Id$

use strict;
use warnings;

use Test::More;
use Yandex::HashUtils;

BEGIN { use_ok('ModerateChecks'); };

use utf8;
use open ':std' => ':utf8';

# баннер без сайтлинков
my $empty_banner = {
    'bid' => 1234567,
    'body' => 'Лучшие африканские слоны со скидой 42% до лета 2012! Торопитесь!',
    'domain' => 'yandex.ru',
    'domain_ascii' => 'yandex.ru',
    'href' => 'yandex.ru',
    'statusModerate' => 'Yes',
    'title' => 'Холодильник у вас в велике',
};

# 1-й баннер с сайтлинками
my $nonempty_banner_1 = hash_merge( {}, $empty_banner, {
    'sitelinks_set_id' => 11111,
    'sitelinks' => [
        {
            'hash' => '7529657739425191226',
            'href' => 'yandex.ru/yandsearch?text=elefants',
            'sl_id' => 12345,
            'title' => 'слоны на яндексе',
            'description' => undef,
        },
        {
            'hash' => '1066870078425068397',
            'href' => 'yandex.ru/yandsearch?text=more+elefants',
            'sl_id' => 54321,
            'title' => 'и снова слоны на яндексе',
            'description' => undef,
        },
    ],
} );

# 2-й баннер с другими сайтлинками
my $nonempty_banner_2 = hash_merge( {}, $empty_banner, {
    'sitelinks_set_id' => 22222,
    'sitelinks' => [
        {
            'hash' => '6176940359150747256',
            'href' => 'yandex.ru/yandsearch?text=dogs',
            'sl_id' => 9000,
            'title' => 'собаки',
            'description' => undef,
        },
    ],
} ); 

# 3-й баннер с другими сайтлинками и без sitelinks_set_id, и уже промодерированный
my $nonempty_banner_3 = hash_merge( {}, $empty_banner, {
    'statusSitelinksModerate' => 'Yes',
    'sitelinks_set_id' => undef,
    'sitelinks' => [
        {
            'hash' => '6176940359150742251',
            'href' => 'yandex.ru/yandsearch?text=cats',
            'sl_id' => 45600,
            'title' => 'кошки',
            'description' => undef,
        },
    ],
} ); 

# и старый и новый баннер без сайтлинков => модерация не требуется (нечего модерировать)
ok( !check_moderate_sitelinks( $empty_banner, $empty_banner ), 'old and new banner have no sitelinks' );

# новый с сайтлинками, старый без них => модерация требуется
ok( check_moderate_sitelinks( $nonempty_banner_1, $empty_banner ), 'new banner with sitelinks, old without' );

# новый без сайтлинков, старый с ними => модерация не требуется (нечего модерировать)
ok( !check_moderate_sitelinks( $empty_banner, $nonempty_banner_1 ), 'new banner without sitelinks, old with' );

# оба баннера с различными сайтлинками, оба баннера имеют sitelinks_set_id => модерация требуется
ok( check_moderate_sitelinks( $nonempty_banner_2, $nonempty_banner_1 ), 'both banners have different sitelinks and sitelinks_set_id');

# оба баннера с одинаковыми сайтлинками => модерация не требуется
ok( !check_moderate_sitelinks( $nonempty_banner_1, $nonempty_banner_1 ), 'both banners have same sitelinks');

# оба баннера с различными сайтлинками. один баннер не имеет sitelinks_set_id, и "новый" баннер уже был промодерирован => модерация требуется
ok( check_moderate_sitelinks( $nonempty_banner_3, $nonempty_banner_1 ), 'both banners have same sitelinks, one banner without sitelinks_set_id and "new" banner is already moderated');

done_testing();
