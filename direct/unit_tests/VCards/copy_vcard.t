#!/usr/bin/perl

#  $Id$

use strict;
use warnings;

use Test::More;
use Test::Deep;
use Test::Exception;

use Settings;
use Yandex::DBShards;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;
use VCards;

use Test::JavaIntapiMocks::GenerateObjectIds;

use utf8;

local $Yandex::DBShards::IDS_LOG_FILE = undef;

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
    org_details_id => undef,
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
    org_details_id  => undef,
);

my @tests = (
    {
        old_vcard_id => 1,
        overrides => {},
    }, {
        old_vcard_id => 2,
        overrides => {},
    }, {
        old_vcard_id => 3,
        overrides => {},
    }, {
        old_vcard_id => 1,
        overrides => { cid => 12, uid => 2 },
        new_inc_vcard_id_row => { vcard_id => 100, ClientID => 2 },
        new_vcard_row_1 => { vcard_id => 100, cid => 12, uid => 2, %vcard1_db },
        new_vcard_row_2 => undef,
        text => 'на кампанию другого пользователя в том же шарде #1'
    }, {
        old_vcard_id => 3,
        overrides => { cid => 22, uid => 12 },
        new_inc_vcard_id_row => { vcard_id => 100, ClientID => 12 },
        new_vcard_row_1 => undef,
        new_vcard_row_2 => { vcard_id => 100, cid => 22, uid => 12, %vcard3_db },
        text => 'на кампанию другого пользователя в том же шарде #2'
    }, {
        old_vcard_id => 3,
        overrides => { cid => 12, uid => 2 },
        new_inc_vcard_id_row => { vcard_id => 100, ClientID => 2 },
        new_vcard_row_1 => { vcard_id => 100, cid => 12, uid => 2, %vcard3_db },
        new_vcard_row_2 => undef,
        text => 'на кампанию другого пользователя из #2 в #1 шард'
    }, {
        old_vcard_id => 2,
        overrides => { cid => 21, uid => 11 },
        new_inc_vcard_id_row => { vcard_id => 100, ClientID => 11 },
        new_vcard_row_1 => undef,
        new_vcard_row_2 => { vcard_id => 100, cid => 21, uid => 11, %vcard2_db },
        text => 'на кампанию другого пользователя из #1 в #2 шард'
    },
);

my %db = (
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { vcard_id => 1, cid => 11, uid => 1, %vcard1_db },
                { vcard_id => 2, cid => 12, uid => 2, %vcard2_db },
            ],
            2 => [
                { vcard_id => 3, cid => 21, uid => 11, %vcard3_db },
            ],
        },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ClientID => 1, uid => 1 },
                { ClientID => 2, uid => 2 },
            ],
            2 => [
                { ClientID => 11, uid => 11 },
                { ClientID => 12, uid => 12 },
            ],
        },
    },
    addresses => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
    maps => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
    org_details => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
    geo_regions => {
        original_db => PPCDICT,
        rows => [],
    },
    shard_inc_vcard_id => {
        original_db => PPCDICT,
        rows => [
            { vcard_id => 1, ClientID => 1 },
            { vcard_id => 2, ClientID => 2 },
            { vcard_id => 3, ClientID => 11 },
            # initial auto-increment
            { vcard_id => 99, ClientID => 0 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 1 },
            { ClientID => 11, shard => 2 },
            { ClientID => 12, shard => 2 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 11, ClientID => 1 },
            { cid => 12, ClientID => 2 },
            { cid => 21, ClientID => 11 },
            { cid => 22, ClientID => 12 },
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 1, ClientID => 1 },
            { uid => 2, ClientID => 2 },
            { uid => 11, ClientID => 11 },
            { uid => 12, ClientID => 12 },
        ],
    }
);

sub _get_vcard {
    my ($id, $exclude) = @_;
    $exclude ||= [];

    my $vcard = get_one_vcard($id);
    # всегда отличается - не сравниваем
    delete $vcard->{vcard_id};
    # часть полей могла быть переопределена
    delete $vcard->{$_} foreach @$exclude;

    return $vcard;
}


foreach my $test (@tests) {
    my $test_name = "копируем визитку $test->{old_vcard_id}";
    $test_name .= " ($test->{text})" if $test->{text};

    init_test_dataset(\%db);
    
    my $new_vcard_id;
    lives_ok { $new_vcard_id = copy_vcard($test->{old_vcard_id}, $test->{overrides}); } $test_name;

    is($new_vcard_id, 100, 'новая визитка имеет другое значение vcard_id') if $test->{overrides}{cid};
    
    check_test_dataset({
        shard_inc_vcard_id => {
            original_db => PPCDICT,
            rows => [
                @{ $db{shard_inc_vcard_id}->{rows} },
                ($test->{new_inc_vcard_id_row} // ()),
            ],
        },
        vcards => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { vcard_id => 1, cid => 11, uid => 1, %vcard1_db },
                    { vcard_id => 2, cid => 12, uid => 2, %vcard2_db },
                    ($test->{new_vcard_row_1} ? $test->{new_vcard_row_1} : ()),
                ],
                2 => [
                    { vcard_id => 3, cid => 21, uid => 11, %vcard3_db },
                    ($test->{new_vcard_row_2} ? $test->{new_vcard_row_2} : ()),
                ],
            },
        },
    }, 'проверяем данные в базе');

    cmp_deeply(
        _get_vcard($new_vcard_id, [keys $test->{overrides}]),
        _get_vcard($test->{old_vcard_id}, [keys $test->{overrides}]),
        'сравниваем визитки через get_vcards'
    );

    # ! опять я на эти грабли наступил :(
    Yandex::DBShards::clear_cache();
}

done_testing;
