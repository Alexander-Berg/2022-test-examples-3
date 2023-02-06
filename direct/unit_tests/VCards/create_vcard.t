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
use Yandex::HashUtils;
use Yandex::Test::UTF8Builder;
use VCards;

use Test::JavaIntapiMocks::GenerateObjectIds;

use utf8;

local $Yandex::DBShards::IDS_LOG_FILE = undef;

my %vcard = (
    cid => 999,
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

my %db = (
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [{ uid => 1, ClientID => 1 }],
            2 => [{ uid => 2, ClientID => 2 }],
            },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 2 },
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 1, ClientID => 1 },
            { uid => 2, ClientID => 2 },
        ],
    },
    shard_inc_vcard_id => {
        original_db => PPCDICT,
        rows => [
            { vcard_id => 1, ClientID => 1 },
            { vcard_id => 2, ClientID => 2 },
            # initial auto-increment
            { vcard_id => 99, ClientID => 0 },
        ],
    },
);

init_test_dataset(\%db);
lives_ok { create_vcards(1, [hash_merge({uid => 1}, \%vcard)]) } 'создаем визитку с получением нового vcard_id по uid в шарде 1';
check_test_dataset({
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [ { vcard_id => 100, %vcard } ],
            2 => [],
        },
    },
    shard_inc_vcard_id => {
        original_db => PPCDICT,
        rows => [
            { vcard_id => 1, ClientID => 1 },
            { vcard_id => 2, ClientID => 2 },
            # initial auto-increment
            { vcard_id => 99, ClientID => 0 },
            { vcard_id => 100, ClientID => 1 },
        ],
    },
}, 'проверяем данные в бд');

Yandex::DBShards::clear_cache();
init_test_dataset(\%db);
lives_ok { create_vcards(2, [hash_merge({uid => 2}, \%vcard)]) } 'создаем визитку с получением нового vcard_id по cid в шарде 2';
check_test_dataset({
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [ { vcard_id => 100, %vcard } ],
        },
    },
    shard_inc_vcard_id => {
        original_db => PPCDICT,
        rows => [
            { vcard_id => 1, ClientID => 1 },
            { vcard_id => 2, ClientID => 2 },
            # initial auto-increment
            { vcard_id => 99, ClientID => 0 },
            { vcard_id => 100, ClientID => 2 },
        ],
    },
}, 'проверяем данные в бд');

done_testing();
