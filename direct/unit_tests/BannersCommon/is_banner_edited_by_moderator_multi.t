#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Deep;

use Yandex::TimeCommon;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset/;

BEGIN { use_ok('BannersCommon'); }

sub t {
    BannersCommon::is_banner_edited_by_moderator_multi(@_);
}

my %db = (
    mod_edit => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {type => 'banner', id => 1, createtime => today()},
                {type => 'banner', id => 3, createtime => today()},
                {type => 'banner', id => 4, createtime => unix2mysql(time - 30*24*60*60)},
                ],
            2 => [
                {type => 'banner', id => 2, createtime => today()},
                ],
        }
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 2},
            ]
    },
    shard_inc_bid => {
        original_db => PPCDICT,
        rows => [
            {bid => 1, ClientID => 1},
            {bid => 2, ClientID => 2},
            {bid => 3, ClientID => 1},
            {bid => 4, ClientID => 1},
            {bid => 5, ClientID => 1},
            ]
    },
);
init_test_dataset(\%db);

cmp_deeply(t([1]), {1=>1});
cmp_deeply(t([1,2]), {1=>1,2=>1});
cmp_deeply(t([1..8]), {1=>1, 2 => 1, 3=> 1});

done_testing();
