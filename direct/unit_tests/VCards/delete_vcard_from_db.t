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

use utf8;

my %db = (
    # uid владельцев визитки определяется через кампанию
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { cid => 11, uid => 11 },
                { cid => 12, uid => 11 },
                { cid => 13, uid => 12 },
                { cid => 14, uid => 12 },
                { cid => 15, uid => 13 },
            ],
            2 => [
                { cid => 21, uid => 21 },
                { cid => 22, uid => 21 },
                { cid => 23, uid => 22 },
                { cid => 24, uid => 22 },
                { cid => 25, uid => 23 },
            ],
        },
    },
    banners => {
        original_db => PPC(shard => 'all'),
    },
    mediaplan_banners => {
        original_db => PPC(shard => 'all'),
    },
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                # две визитки одного пользователя - обе используют адрес
                { vcard_id => 11, cid => 11, uid => 11, address_id => 11, org_details_id => 11 },
                { vcard_id => 12, cid => 12, uid => 11, address_id => 11, org_details_id => undef },
                # две визитки одного пользователя, одна из них с адресом
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                #
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => [
                { vcard_id => 21, cid => 21, uid => 21, address_id => 21, org_details_id => 21 },
                { vcard_id => 22, cid => 22, uid => 21, address_id => 21, org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
    },
    addresses => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => [
                { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
    },
    org_details => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => [
                { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
    },
    shard_inc_vcard_id => {
        original_db => PPCDICT,
        rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
    },
    shard_inc_org_details_id => {
        original_db => PPCDICT,
        rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 11, ClientID => 11 },
            { uid => 12, ClientID => 12 },
            { uid => 13, ClientID => 13 },
            { uid => 21, ClientID => 21 },
            { uid => 22, ClientID => 22 },
            { uid => 23, ClientID => 23 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 11, shard => 1 },
            { ClientID => 12, shard => 1 },
            { ClientID => 13, shard => 1 },
            { ClientID => 21, shard => 2 },
            { ClientID => 22, shard => 2 },
            { ClientID => 23, shard => 2 },
        ],
    },
);

my @tests = (
    {
        # какие визитки удаляем
        to_delete => 0,
        # на самом деле нет такой визитки, все должно остаться на своих местах
        # проверочные данные
        vcards_rows => $db{vcards}->{rows},
        addresses_rows => $db{addresses}->{rows},
        org_details_rows => $db{org_details}->{rows},
        inc_vcard_id_rows => $db{shard_inc_vcard_id}->{rows},
        inc_org_details_id_rows => $db{shard_inc_org_details_id}->{rows},
    }, {
        # какие визитки удаляем
        to_delete => 11,
        # проверочные данные
        vcards_rows => {
            1 => [
                # { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => $db{vcards}->{rows}->{2},
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => $db{addresses}->{rows}->{2},
        },
        org_details_rows => {
            1 => [
                # { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => $db{org_details}->{rows}->{2},
        },
        inc_vcard_id_rows => [
            # { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            # { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => 12,
        vcards_rows => {
            1 => [
                { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                # { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => $db{vcards}->{rows}->{2},
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => $db{addresses}->{rows}->{2},
        },
        org_details_rows => {
            1 => [
                { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => $db{org_details}->{rows}->{2},
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            # { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [11, 12],
        vcards_rows => {
            1 => [
                # { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                # { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => $db{vcards}->{rows}->{2},
        },
        addresses_rows => {
            1 => [
                # { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => $db{addresses}->{rows}->{2},
        },
        org_details_rows => {
            1 => [
                # { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => $db{org_details}->{rows}->{2},
        },
        inc_vcard_id_rows => [
            # { vcard_id => 11, ClientID => 11 },
            # { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            # { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => 13,
        vcards_rows => {
            1 => [
                { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                # { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => $db{vcards}->{rows}->{2},
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                # { aid => 12, ClientID => 12 },
            ],
            2 => $db{addresses}->{rows}->{2},
        },
        org_details_rows => {
            1 => [
                { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => $db{org_details}->{rows}->{2},
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            # { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => 14,
        vcards_rows => {
            1 => [
                { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                # { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => $db{vcards}->{rows}->{2},
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => $db{addresses}->{rows}->{2},
        },
        org_details_rows => {
            1 => [
                { org_details_id => 11, uid => 11 },
                # { org_details_id => 12, uid => 12 },
            ],
            2 => $db{org_details}->{rows}->{2},
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            # { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            # { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [13, 14],
        vcards_rows => {
            1 => [
                { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                # { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                # { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => $db{vcards}->{rows}->{2},
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                # { aid => 12, ClientID => 12 },
            ],
            2 => $db{addresses}->{rows}->{2},
        },
        org_details_rows => {
            1 => [
                { org_details_id => 11, uid => 11 },
                # { org_details_id => 12, uid => 12 },
            ],
            2 => $db{org_details}->{rows}->{2},
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            # { vcard_id => 13, ClientID => 12 },
            # { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            # { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => 15,
        vcards_rows => {
            1 => [
                { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                # { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => $db{vcards}->{rows}->{2},
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => $db{addresses}->{rows}->{2},
        },
        org_details_rows => {
            1 => [
                { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => $db{org_details}->{rows}->{2},
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            # { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [11..15],
        vcards_rows => {
            1 => [],
            2 => $db{vcards}->{rows}->{2},
        },
        addresses_rows => {
            1 => [],
            2 => $db{addresses}->{rows}->{2},
        },
        org_details_rows => {
            1 => [],
            2 => $db{org_details}->{rows}->{2},
        },
        inc_vcard_id_rows => [
            # { vcard_id => 11, ClientID => 11 },
            # { vcard_id => 12, ClientID => 11 },
            # { vcard_id => 13, ClientID => 12 },
            # { vcard_id => 14, ClientID => 12 },
            # { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            # { org_details_id => 11, ClientID => 11 },
            # { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    },
    # зеркальная copy-paste с заменой 1 <-> 2, все то же самое, но в другом шарде
    {
        to_delete => 21,
        vcards_rows => {
            1 => $db{vcards}->{rows}->{1},
            2 => [
                # { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => $db{addresses}->{rows}->{1},
            2 => [
                { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => $db{org_details}->{rows}->{1},
            2 => [
                # { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            # { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            # { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => 22,
        vcards_rows => {
            1 => $db{vcards}->{rows}->{1},
            2 => [
                { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                # { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => $db{addresses}->{rows}->{1},
            2 => [
                { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => $db{org_details}->{rows}->{1},
            2 => [
                { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            # { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [21, 22],
        vcards_rows => {
            1 => $db{vcards}->{rows}->{1},
            2 => [
                # { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                # { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => $db{addresses}->{rows}->{1},
            2 => [
                # { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => $db{org_details}->{rows}->{1},
            2 => [
                # { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            # { vcard_id => 21, ClientID => 21 },
            # { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            # { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => 23,
        vcards_rows => {
            1 => $db{vcards}->{rows}->{1},
            2 => [
                { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                # { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => $db{addresses}->{rows}->{1},
            2 => [
                { aid => 21, ClientID => 21 },
                # { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => $db{org_details}->{rows}->{1},
            2 => [
                { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            # { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => 24,
        vcards_rows => {
            1 => $db{vcards}->{rows}->{1},
            2 => [
                { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                # { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => $db{addresses}->{rows}->{1},
            2 => [
                { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => $db{org_details}->{rows}->{1},
            2 => [
                { org_details_id => 21, uid => 21 },
                # { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            # { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            # { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [23, 24],
        vcards_rows => {
            1 => $db{vcards}->{rows}->{1},
            2 => [
                { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                # { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                # { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => $db{addresses}->{rows}->{1},
            2 => [
                { aid => 21, ClientID => 21 },
                # { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => $db{org_details}->{rows}->{1},
            2 => [
                { org_details_id => 21, uid => 21 },
                # { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            # { vcard_id => 23, ClientID => 22 },
            # { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            # { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => 25,
        vcards_rows => {
            1 => $db{vcards}->{rows}->{1},
            2 => [
                { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                # { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => $db{addresses}->{rows}->{1},
            2 => [
                { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => $db{org_details}->{rows}->{1},
            2 => [
                { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            # { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [21..25],
        vcards_rows => {
            1 => $db{vcards}->{rows}->{1},
            2 => [],
        },
        addresses_rows => {
            1 => $db{addresses}->{rows}->{1},
            2 => [],
        },
        org_details_rows => {
            1 => $db{org_details}->{rows}->{1},
            2 => [],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            # { vcard_id => 21, ClientID => 21 },
            # { vcard_id => 22, ClientID => 21 },
            # { vcard_id => 23, ClientID => 22 },
            # { vcard_id => 24, ClientID => 22 },
            # { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            # { org_details_id => 21, ClientID => 21 },
            # { org_details_id => 22, ClientID => 22 },
        ],
    },
    # комбинированно удаляем визитки из обоих шардов сразу
    {
        to_delete => [11, 22],
        vcards_rows => {
            1 => [
                # { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => [
                { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                # { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => [
                { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => [
                # { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => [
                { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            # { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            # { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            # { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [12, 21],
        vcards_rows => {
            1 => [
                { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                # { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => [
                # { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => [
                { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => [
                { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => [
                # { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            # { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            # { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            # { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [11, 12, 21, 22],
        vcards_rows => {
            1 => [
                # { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                # { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => [
                # { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                # { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => [
                # { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => [
                # { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => [
                # { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => [
                # { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            # { vcard_id => 11, ClientID => 11 },
            # { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            # { vcard_id => 21, ClientID => 21 },
            # { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            # { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            # { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [13,24,25],
        vcards_rows => {
            1 => [
                { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                # { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => [
                { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                # { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                # { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                # { aid => 12, ClientID => 12 },
            ],
            2 => [
                { aid => 21, ClientID => 21 },
                { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => [
                { org_details_id => 11, uid => 11 },
                { org_details_id => 12, uid => 12 },
            ],
            2 => [
                { org_details_id => 21, uid => 21 },
                # { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            # { vcard_id => 13, ClientID => 12 },
            { vcard_id => 14, ClientID => 12 },
            { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            { vcard_id => 23, ClientID => 22 },
            # { vcard_id => 24, ClientID => 22 },
            # { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            # { org_details_id => 22, ClientID => 22 },
        ],
    }, {
        to_delete => [14,15,23],
        vcards_rows => {
            1 => [
                { vcard_id => 11, cid => 11, uid => 11, address_id => 11,    org_details_id => 11 },
                { vcard_id => 12, cid => 12, uid => 11, address_id => 11,    org_details_id => undef },
                { vcard_id => 13, cid => 13, uid => 12, address_id => 12,    org_details_id => undef },
                # { vcard_id => 14, cid => 13, uid => 12, address_id => undef, org_details_id => 12 },
                # { vcard_id => 15, cid => 14, uid => 13 },
            ],
            2 => [
                { vcard_id => 21, cid => 21, uid => 21, address_id => 21,    org_details_id => 21 },
                { vcard_id => 22, cid => 22, uid => 21, address_id => 21,    org_details_id => undef },
                # { vcard_id => 23, cid => 23, uid => 22, address_id => 22,    org_details_id => undef },
                { vcard_id => 24, cid => 23, uid => 22, address_id => undef, org_details_id => 22 },
                { vcard_id => 25, cid => 24, uid => 23 },
            ],
        },
        addresses_rows => {
            1 => [
                { aid => 11, ClientID => 11 },
                { aid => 12, ClientID => 12 },
            ],
            2 => [
                { aid => 21, ClientID => 21 },
                # { aid => 22, ClientID => 22 },
            ],
        },
        org_details_rows => {
            1 => [
                { org_details_id => 11, uid => 11 },
                # { org_details_id => 12, uid => 12 },
            ],
            2 => [
                { org_details_id => 21, uid => 21 },
                { org_details_id => 22, uid => 22 },
            ],
        },
        inc_vcard_id_rows => [
            { vcard_id => 11, ClientID => 11 },
            { vcard_id => 12, ClientID => 11 },
            { vcard_id => 13, ClientID => 12 },
            # { vcard_id => 14, ClientID => 12 },
            # { vcard_id => 15, ClientID => 13 },
            { vcard_id => 21, ClientID => 21 },
            { vcard_id => 22, ClientID => 21 },
            # { vcard_id => 23, ClientID => 22 },
            { vcard_id => 24, ClientID => 22 },
            { vcard_id => 25, ClientID => 23 },
        ],
        inc_org_details_id_rows => [
            { org_details_id => 11, ClientID => 11 },
            # { org_details_id => 12, ClientID => 12 },
            { org_details_id => 21, ClientID => 21 },
            { org_details_id => 22, ClientID => 22 },
        ],
    },
    # удаляем всё
    {
        to_delete => [11..15, 21..25],
        vcards_rows => {
            1 => [],
            2 => [],
        },
        addresses_rows => {
            1 => [],
            2 => [],
        },
        org_details_rows => {
            1 => [],
            2 => [],
        },
        inc_vcard_id_rows => [],
        inc_org_details_id_rows => [],
    },
);


Test::More::plan (tests => 2 * @tests);

foreach my $test (@tests) {
    my $test_name = "удаляем визитки, vcard_id: " . (ref $test->{to_delete} ? join(', ', @{ $test->{to_delete} }) : $test->{to_delete});

    init_test_dataset(\%db);
    
    lives_ok { delete_vcard_from_db($test->{to_delete}); } $test_name;

    check_test_dataset({
        vcards => {
            original_db => PPC(shard => 'all'),
            rows => $test->{vcards_rows},
        },
        addresses => {
            original_db => PPC(shard => 'all'),
            rows => $test->{addresses_rows},
        },
        org_details => {
            original_db => PPC(shard => 'all'),
            rows => $test->{org_details_rows},
        },
        shard_inc_vcard_id => {
            original_db => PPCDICT,
            rows => $test->{inc_vcard_id_rows},
        },
        shard_inc_org_details_id => {
            original_db => PPCDICT,
            rows => $test->{inc_org_details_id_rows},
        },
    }, 'проверяем данные в базе');

    # на всякий случай
    Yandex::DBShards::clear_cache();
}
