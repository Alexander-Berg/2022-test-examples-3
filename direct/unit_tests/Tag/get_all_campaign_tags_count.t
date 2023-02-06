#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;

use Settings;
use Yandex::DBUnitTest qw/:all/;

BEGIN { use_ok( 'Tag' ); }

use utf8;
use open ':std' => ':utf8';

*c = *Tag::get_all_campaign_tags_count;

my %db = (
    tag_campaign_list => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { tag_id => 1, cid => 11, tag_name => 'одна метка на кампании 11' },
                { tag_id => 2, cid => 12, tag_name => '1 из 2 меток на кампании 12' },
                { tag_id => 3, cid => 12, tag_name => '2 из 2 меток на кампании 12' },
            ],
            2 => [
                { tag_id => 4, cid => 13, tag_name => '1 из 3 меток на кампании 13' },
                { tag_id => 5, cid => 13, tag_name => '2 из 3 меток на кампании 13' },
                { tag_id => 6, cid => 13, tag_name => '3 из 3 меток на кампании 13' },
            ],
        },
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 11, ClientID => 1 },
            { cid => 12, ClientID => 2 },
            { cid => 13, ClientID => 3 },
            { cid => 14, ClientID => 1 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 1},
            {ClientID => 3, shard => 2},
        ],
    },
);
init_test_dataset(\%db);

is(c(11), 1, '1 tag on camp from 1st shard');
is(c(12), 2, '2 tags on camp from 1st shard');
is(c(13), 3, '3 tags on camp from 2nd shard');
is(c(14), 0, '0 tags on camp from 1st shard');
is(c(15), 0, '0 tags on not existing camp');

done_testing();
