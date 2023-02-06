#!/usr/bin/perl

# Юнит-тест на get_vcards и get_vcard
#  $Id$

use strict;
use warnings;

use Test::More;
use Test::Deep;
use Test::Exception;

use Settings;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use VCards;
use CommonMaps qw();

use utf8;

$Yandex::DBTools::DONT_SEND_LETTERS = 1;

# точка по-умолчанию
my $dp = { mid => 1 };
($dp->{x}, $dp->{y}) = split ',', $CommonMaps::DEFAULT_AUTO_POINT;
($dp->{x1}, $dp->{y1}, $dp->{x2}, $dp->{y2}) = split ',', $CommonMaps::DEFAULT_AUTO_BOUNDS;

# схема имен переменных vcard_id_cid_uid


# визитка 1
my %vcard1 = (
    # точки на карте
    auto_bounds     => $CommonMaps::DEFAULT_AUTO_BOUNDS,
    auto_point      => $CommonMaps::DEFAULT_AUTO_POINT,
    auto_precision  => 'near',
    manual_bounds   => '30.415779,59.886203,30.423989,59.888957',
    manual_point    => '30.419884,59.887580',
    # адрес
    country         => 'Россия',
    country_geo_id  => 225,
    geo_id          => 2,
    city            => 'Санкт-Петербург',
    street          => 'улица Крупской',
    house           => 32,
    build           => '1',
    apart           => 2,
    metro           => 20330,
    metro_name      => 'Выборгская',
    # телефон
    country_code    => '+7',
    city_code       => 812,
    phone           => '337-20-67',
    compiled_phone  => '+7#812#337-20-67#',
    ext             => '',
    # прочее
    name            => "HANSE",
    contact_email   => 'some@mail.local',
    contactperson   => 'somebody',
    extra_message   => 'Оригинальные запчасти для иномарок. Оптовые цены в розницу!',
    im_client       => 'icq',
    im_login        => '111111111',
    # огрн
    org_details_id  => 11,
    ogrn            => 304501820500097,
    # время работы
    worktime        => '0#4#10#00#18#00',
    worktimes       => get_worktime('0#4#10#00#18#00'),
    map_id          => 2,
    map_id_auto     => 1,
    is_auto_point   => 0,
);
# она же, но то, что лежит в базе
my %vcard1_db = (
    address_id => 11,
    geo_id => 2,
    phone => '+7#812#337-20-67#',
    country => 'Россия',
    city => 'Санкт-Петербург',
    street => 'улица Крупской',
    house => 32,
    build => 1,
    apart => 2,
    metro => 20330,
    name => 'HANSE',
    contactperson => 'somebody',
    contact_email => 'some@mail.local',
    worktime => '0#4#10#00#18#00',
    extra_message => 'Оригинальные запчасти для иномарок. Оптовые цены в розницу!',
    im_client => 'icq',
    im_login => '111111111',
    org_details_id => 11,
);
my $vcard_1_11_1 = {
    vcard_id    => 1,
    cid         => 11,
    uid         => 1,
    %vcard1,
};

my %vcard2 = (
    auto_bounds     => "37.585889,55.732616,37.589985,55.734927",
    auto_point      => "37.587937,55.733771",
    auto_precision  => "exact",
    city            => "Москва",
    city_code       => 111,
    country         => "Россия",
    country_geo_id  => 225,
    country_code    => '+7',
    ext             => "",
    geo_id          => 213,
    house           => 16,
    manual_bounds   => "37.585889,55.732616,37.589985,55.734927",
    manual_point    => "37.587937,55.733771",
    metro           => 20490,
    metro_name      => "Парк культуры",
    name            => "кто-то",
    phone           => "123-12-12",
    compiled_phone  => '+7#111#123-12-12#',
    street          => "Льва Толстого",
    worktime        => "0#4#0#00#00#00",
    worktimes       => get_worktime('0#4#0#00#00#00'),
    apart           => undef,
    build           => undef,
    contactperson   => undef,
    contact_email   => undef,
    extra_message   => undef,
    im_login        => undef,
    im_client       => undef,
    org_details_id  => undef,
    map_id          => 4,
    map_id_auto     => 4,
    is_auto_point   => 1,
);
my %vcard2_db = (
    address_id => 12,
    geo_id => 213,
    phone => '+7#111#123-12-12#',
    country => 'Россия',
    city => 'Москва',
    street => 'Льва Толстого',
    house => 16,
    metro => 20490,
    name => 'кто-то',
    worktime => '0#4#0#00#00#00',
);
my $vcard_2_12_2 = {
    vcard_id    => 2,
    cid         => 12,
    uid         => 2,
    %vcard2,
};

my %vcard3 = (
    auto_bounds     => "30.509482,50.467077,30.517693,50.470575",
    auto_point      => "30.513587,50.468826",
    auto_precision  => "exact",
    city            => "Киев",
    city_code       => 44,
    country         => "Украина",
    country_geo_id  => 187,
    country_code    => '+380',
    ext             => "",
    geo_id          => 143,
    house           => 26,
    manual_bounds   => "30.509482,50.467077,30.517693,50.470575",
    manual_point    => "30.513587,50.468826",
    metro           => 101905,
    metro_name      => "Контрактовая площадь",
    name            => "Нотаріус",
    compiled_phone  => '+380#44#425-97-17#',
    phone           => "425-97-17",
    street          => "Ярославская",
    worktime        => "0#5#9#00#17#00",
    worktimes       => get_worktime('0#5#9#00#17#00'),
    apart           => undef,
    build           => undef,
    contactperson   => 'Віктория',
    contact_email   => undef,
    extra_message   => undef,
    im_login        => undef,
    im_client       => undef,
    org_details_id  => 21,
    ogrn            => 1069658007000,
    map_id          => 3,
    map_id_auto     => 3,
    is_auto_point   => 1,    
);
my %vcard3_db = (
    address_id => 21,
    geo_id => 143,
    phone => '+380#44#425-97-17#',
    name => 'Нотаріус',
    city => 'Киев',
    contactperson => 'Віктория',
    country => 'Украина',
    street => 'Ярославская',
    house => 26,
    metro => 101905,
    worktime => '0#5#9#00#17#00',
    org_details_id  => 21,
);
my $vcard_3_21_11 = {
    vcard_id    => 3,
    cid         => 21,
    uid         => 11,
    %vcard3,
};
my $vcard_4_22_11 = {
    vcard_id    => 4,
    cid         => 22,
    uid         => 11,
    %vcard3,
};
my $vcard_5_22_11 = {
    vcard_id    => 5,
    cid         => 22,
    uid         => 11,
    %vcard3,
};

my $vcard_6_23_12 = {
    vcard_id    => 6,
    cid         => 23,
    uid         => 12,
    %vcard3,
};


init_test_dataset({
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { cid => 11, uid => 1 },
            ],
            2 => [
                { cid => 21, uid => 11 },
                { cid => 23, uid => 11 },
            ],
        },
    },    
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { bid => 1, cid => 11, vcard_id => 1 },
                { bid => 5, cid => 12,}
            ],
            2 => [
                { bid => 2, cid => 21, vcard_id => 3 },
                { bid => 3, cid => 23, vcard_id => 6 },
                { bid => 4, cid => 23, },
            ],
        },
    },
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { vcard_id => 1, cid => 11, uid => 1, %vcard1_db },
                { vcard_id => 2, cid => 12, uid => 2, %vcard2_db },
            ],
            2 => [
                { vcard_id => 3, cid => 21, uid => 11, %vcard3_db },
                { vcard_id => 4, cid => 22, uid => 11, %vcard3_db },
                { vcard_id => 5, cid => 22, uid => 11, %vcard3_db },
                { vcard_id => 6, cid => 23, uid => 12, %vcard3_db },
            ],
        },
    },
    addresses => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { aid => 11, ClientID => 1, map_id => 2, map_id_auto => 1, precision => 'near' },
                { aid => 12, ClientID => 2, map_id => 4, map_id_auto => 4, precision => 'exact' },
            ],
            2 => [
                { aid => 21, ClientID => 11, map_id => 3, map_id_auto => 3, precision => 'exact' },
            ],
        },
    },
    maps => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                $dp,
                { mid => 2, x => 30.419884, y => 59.887580, x1 => 30.415779, y1 => 59.886203, x2 => 30.423989, y2 => 59.888957 },
                { mid => 4, x => 37.587937, y => 55.733771, x1 => 37.585889, y1 => 55.732616, x2 => 37.589985, y2 => 55.734927 },
            ],
            2 => [
                $dp,
                { mid => 3, x => 30.513587, y => 50.468826, x1 => 30.509482, y1 => 50.467077, x2 => 30.517693, y2 => 50.470575 },
            ],
        },
    },
    org_details => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { org_details_id => 11, uid => 1, ogrn => 304501820500097 },
            ],
            2 => [
                { org_details_id => 21, uid => 11, ogrn => 1069658007000 },
            ],
        },
    },
    geo_regions => {
        original_db => PPCDICT,
        rows => [
            { region_id => 2, name => 'Санкт-Петербург' },
            { region_id => 213, name => 'Киев' },
            { region_id => 143, name => 'Москва' },
            { region_id => 101905, name => 'Контрактовая площадь' },
            { region_id => 20490, name => 'Парк культуры' },
            { region_id => 20330, name => 'Выборгская' },
        ],
    },
    shard_inc_vcard_id => {
        original_db => PPCDICT,
        rows => [
            { vcard_id => 1, ClientID => 1 },
            { vcard_id => 2, ClientID => 2 },
            { vcard_id => 3, ClientID => 11 },
            { vcard_id => 4, ClientID => 11 },
            { vcard_id => 5, ClientID => 11 },
            { vcard_id => 6, ClientID => 11 },
            # не существует
            { vcard_id => 100, ClientID => 11 },
        ],
    },
    shard_inc_org_details_id => {
        original_db => PPCDICT,
        rows => [
            { org_details_id => 11, ClientID => 1 },
            { org_details_id => 21, ClientID => 11 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 1 },
            { ClientID => 11, shard => 2 },
            # не существует
            { ClientID => 100, shard => 2},
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 1, ClientID => 1 },
            { uid => 2, ClientID => 2 },
            { uid => 11, ClientID => 11 },
            { uid => 12, ClientID => 11 },
            # не существует
            { uid => 100, ClientID => 100 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 11, ClientID => 1 },
            { cid => 12, ClientID => 2 },
            { cid => 21, ClientID => 11 },
            { cid => 22, ClientID => 11 },
            { cid => 23, ClientID => 11 },
            # на несуществующую кампани
            { cid => 100, ClientID => 100 },
        ],
    },
});

my $fb = {from_banners_only => 1};

lives_ok { cmp_deeply (get_vcards({}),                  [], 'empty array as result for empty where condition') };
lives_ok { cmp_deeply (get_vcards({vcard_id => 99}),    [], 'empty array as result for unreachable condition') };
lives_ok { cmp_deeply (get_vcards({uid => 99}),         [], 'empty array as result for unreachable condition') };
lives_ok { cmp_deeply (get_vcards({cid => 99}),         [], 'empty array as result for unreachable condition') };
lives_ok { cmp_deeply (get_vcards({vcard_id => 100}),   [], 'empty array as result for unreachable condition') };
lives_ok { cmp_deeply (get_vcards({uid => 100}),        [], 'empty array as result for unreachable condition') };
lives_ok { cmp_deeply (get_vcards({cid => 100}),        [], 'empty array as result for unreachable condition') };

# схема имен переменных vcard_id_cid_uid
cmp_deeply(get_one_vcard(1), $vcard_1_11_1, '1st shard: get one vcard by vcard_id');
cmp_deeply(get_vcards({vcard_id => 1}),      bag($vcard_1_11_1), '1st shard: get vcard by vcard_id');
cmp_deeply(get_vcards({cid => 11}),          bag($vcard_1_11_1), '1st shard: get vcard by cid');
cmp_deeply(get_vcards({uid => 1}),           bag($vcard_1_11_1), '1st shard: get vcard by uid');
cmp_deeply(get_vcards({vcard_id => 1}, $fb), bag($vcard_1_11_1), '1st shard: get vcard by vcard_id (with from_banners_only flag)');
cmp_deeply(get_vcards({cid => 11},     $fb), bag($vcard_1_11_1), '1st shard: get vcard by cid (with from_banners_only flag)');
cmp_deeply(get_vcards({uid => 1},      $fb), bag($vcard_1_11_1), '1st shard: get vcard by uid (with from_banners_only flag)');

cmp_deeply(get_one_vcard(2), $vcard_2_12_2, '1st shard: get one vcard by vcard_id');
cmp_deeply(get_vcards({vcard_id => 2}),      bag($vcard_2_12_2), '1st shard: get vcard by vcard_id');
cmp_deeply(get_vcards({vcard_id => 2}, $fb), [], '1st shard: get vcard by vcard_id (empty array with from_banners_only flag)');
cmp_deeply(get_vcards({cid => 12}),          bag($vcard_2_12_2), '1st shard: get vcard by cid');
cmp_deeply(get_vcards({cid => 12},     $fb), [], '1st shard: get vcard by cid (empty array with from_banners_only flag)');
cmp_deeply(get_vcards({uid => 2}),           bag($vcard_2_12_2), '1st shard: get vcard by uid');
cmp_deeply(get_vcards({uid => 2},      $fb), [], '1st shard: get vcard by uid (empty array with from_banners_only flag)');

cmp_deeply(get_one_vcard(3), $vcard_3_21_11, '2nd shard: get one vcard by vcard_id');

cmp_deeply(get_one_vcard(4), $vcard_4_22_11, '2nd shard: get one vcard by vcard_id');
cmp_deeply(get_one_vcard(5), $vcard_5_22_11, '2nd shard: get one vcard by vcard_id');
cmp_deeply(get_vcards({vcard_id => 3}),      bag($vcard_3_21_11), '2nd shard: get vcard by vcard_id');
cmp_deeply(get_vcards({vcard_id => 4}),      bag($vcard_4_22_11), '2nd shard: get vcard by vcard_id');
cmp_deeply(get_vcards({vcard_id => 5}),      bag($vcard_5_22_11), '2nd shard: get vcard by vcard_id');
cmp_deeply(get_vcards({vcard_id => 3}, $fb), bag($vcard_3_21_11), '2nd shard: get vcard by vcard_id (with from_banners_only flag)');
cmp_deeply(get_vcards({vcard_id => 4}, $fb), [], '2nd shard: get vcard by vcard_id (empty array with from_banners_only flag)');
cmp_deeply(get_vcards({vcard_id => 5}, $fb), [], '2nd shard: get vcard by vcard_id (empty array with from_banners_only flag)');
cmp_deeply(get_vcards({cid => 21}),          bag($vcard_3_21_11), '2nd shard: get vcard by cid');
cmp_deeply(get_vcards({cid => 21},     $fb), bag($vcard_3_21_11), '2nd shard: get vcard by cid (with from_banners_only flag)');
cmp_deeply(get_vcards({cid => 22}),          bag($vcard_4_22_11, $vcard_5_22_11), '2nd shard: get vcard by cid');
cmp_deeply(get_vcards({cid => 22},     $fb), [], '2nd shard: get vcard by cid (empty array with from_banners_only flag)');
cmp_deeply(get_vcards({uid => 11}),          bag($vcard_3_21_11, $vcard_4_22_11, $vcard_5_22_11), '2nd shard: get vcard by uid');
cmp_deeply(get_vcards({uid => 11},     $fb), bag($vcard_3_21_11), '2nd shard: get vcard by uid (with from_banners_only flag)');

cmp_deeply(get_vcards({vcard_id => [1, 5]}),      bag($vcard_1_11_1, $vcard_5_22_11), 'both shards: get vcard by vcard_id');
cmp_deeply(get_vcards({vcard_id => [1, 5]}, $fb), bag($vcard_1_11_1), 'both shards: get vcard by vcard_id  (with from_banners_only flag)');
cmp_deeply(get_vcards({cid => [12,21]}),          bag($vcard_2_12_2, $vcard_3_21_11), 'both shards: get vcard by cid');
cmp_deeply(get_vcards({cid => [12,21]},     $fb), bag($vcard_3_21_11), 'both shards: get vcard by cid (with from_banners_only flag)');
cmp_deeply(get_vcards({uid => [1,11]}),           bag($vcard_1_11_1, $vcard_3_21_11, $vcard_4_22_11, $vcard_5_22_11), 'both shards: get vcard by uid');
cmp_deeply(get_vcards({uid => [1,11]},      $fb), bag($vcard_1_11_1, $vcard_3_21_11), 'both shards: get vcard by uid (with from_banners_only flag)');


cmp_deeply(get_vcards({cid => 23}, {from_banners_only => 1, only_if_common => 1}), undef, 'get common vcard by cid - undef (1 banner with ci, 1 without)');

done_testing();
