#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 5;
use Test::Deep;
use Test::Exception;


use Settings;
use Yandex::DBUnitTest qw/:all/;

BEGIN { use_ok( 'OrgDetails' ); }

use utf8;
use open ':std' => ':utf8';
*cod = sub {OrgDetails::clean_org_details(@_)};

my %db = (
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { vcard_id => 1, uid => 1, org_details_id => 1 },
                { vcard_id => 2, uid => 1, org_details_id => 2 },
            ],
            2 => [
                { vcard_id => 3, uid => 2, org_details_id => 5 },
                { vcard_id => 4, uid => 2, org_details_id => 6 },
            ],
        },
    },
    org_details => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { uid => 1, org_details_id => 1 },
                { uid => 1, org_details_id => 2 },
                { uid => 1, org_details_id => 3 },
            ],
            2 => [
                { uid => 2, org_details_id => 4 },
                { uid => 2, org_details_id => 5 },
                { uid => 2, org_details_id => 6 },
            ],
        },
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 1, ClientID => 1 },
            { uid => 2, ClientID => 2 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 2 },
        ],
    },
    shard_inc_org_details_id => {
        original_db => PPCDICT,
        rows => [
            { org_details_id => 1, ClientID => 1 },
            { org_details_id => 2, ClientID => 1 },
            { org_details_id => 3, ClientID => 1 },
            { org_details_id => 4, ClientID => 2 },
            { org_details_id => 5, ClientID => 2 },
            { org_details_id => 6, ClientID => 2 },
        ],
    },
);


init_test_dataset(\%db);
lives_ok { cod(uid => 1) };
check_test_dataset({
    org_details => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { uid => 1, org_details_id => 1 },
                { uid => 1, org_details_id => 2 },
            ],
            2 => $db{org_details}->{rows}->{2},
        },
    },
    shard_inc_org_details_id => {
        original_db => PPCDICT,
        rows => [
            { org_details_id => 1, ClientID => 1 },
            { org_details_id => 2, ClientID => 1 },
            { org_details_id => 4, ClientID => 2 },
            { org_details_id => 5, ClientID => 2 },
            { org_details_id => 6, ClientID => 2 },
        ],
    },
});

init_test_dataset(\%db);
lives_ok { cod(uid => 2) };
check_test_dataset({
    org_details => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => $db{org_details}->{rows}->{1},
            2 => [
                { uid => 2, org_details_id => 5 },
                { uid => 2, org_details_id => 6 },
            ],
        },
    },
    shard_inc_org_details_id => {
        original_db => PPCDICT,
        rows => [
            { org_details_id => 1, ClientID => 1 },
            { org_details_id => 2, ClientID => 1 },
            { org_details_id => 3, ClientID => 1 },
            { org_details_id => 5, ClientID => 2 },
            { org_details_id => 6, ClientID => 2 },
        ],
    },
});
