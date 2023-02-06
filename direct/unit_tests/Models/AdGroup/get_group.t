#!/usr/bin/perl

use my_inc "../../..";
use Direct::Modern;

use Test::More;


use utf8;
use open ':std' => ':utf8';
binmode STDERR, ":utf8";
binmode STDOUT, ":utf8";
use Yandex::Test::UTF8Builder;
use Yandex::DBUnitTest qw/:all/;
use Direct::Test::DBObjects;

use Models::AdGroup;
use Settings;

use List::Util qw/sum/;
use JSON;

=head2 COMMENT

    Функция еще не адаптирована к шардингу, но использует шардированную Primitives::get_bid2pid_hash
    Поэтому ссылаемся везде на 1ый шард в качестве временного решения

=cut

my %db = (
    campaigns => {
        original_db => PPC(shard => 1),
        like => 'campaigns',
        rows => {1 => [
            {cid => 456, statusEmpty => 'No', uid => 1},
            {cid => 891, statusEmpty => 'No', uid => 1},
            {cid => 777, statusEmpty => 'No', uid => 1},
            {cid => 789, statusEmpty => 'No', uid => 1},
        ]},
    }, 
    camp_options => {
        original_db => PPC(shard => 1),
        like => 'camp_options',
        rows => {1 => [
            {cid => 456},
            {cid => 891},
            {cid => 777},
            {cid => 789},
        ]},
    }, 
    phrases => {
        original_db => PPC(shard => 1),
        rows => {1 => [
            {pid => 781, cid => 456, geo => '10174,-969,-10867', group_name => 'Test group'},
            {pid => 612, cid => 456, geo => 225},
            {pid => 908, cid => 456, geo => 225},
            {pid => 987, cid => 789, geo => 225},
            {pid => 988, cid => 777, geo => 225},
            {pid => 896, cid => 891, geo => '225,166'},
        ]},
    },
    banners => {
        original_db => PPC(shard => 1),
        rows => {1 => [
            {bid => 562, pid => 781},
            {bid => 563, pid => 781},
            {bid => 564, pid => 781},
            {bid => 981, pid => 908},
            {bid => 101, pid => 896},
            {bid => 102, pid => 896},
            {bid => 123, pid => 987},
            {bid => 124, pid => 987, statusArch => 'Yes'},
        ]},
    }, 
    bids => {
        original_db => PPC(shard => 1),
        rows => {1 => [
            {pid => 781, id => 1},
            {pid => 781, id => 2},
            {pid => 781, id => 3},
            {pid => 612, id => 4},
            {pid => 612, id => 5},
            {pid => 908, id => 6},
            {pid => 896, id => 7},
            {pid => 987, id => 8},
        ]},
    },
    bids_retargeting => {
        original_db => PPC(shard => 1),
        like => 'bids_retargeting',
        rows => {1 => [
            {pid => 781, ret_id => 1, ret_cond_id => 1},
            {pid => 781, ret_id => 2, ret_cond_id => 2},
            {pid => 781, ret_id => 3, ret_cond_id => 3},
            {pid => 612, ret_id => 4, ret_cond_id => 4},
        ]}
    },
    retargeting_conditions => {
        original_db => PPC(shard => 1),
        like => 'retargeting_conditions',
        rows => {1 => [
            {ret_cond_id => 1},
            {ret_cond_id => 2},
            {ret_cond_id => 3},
            {ret_cond_id => 4},
        ]}
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            {pid => 781, ClientID => 1},
            {pid => 612, ClientID => 1},
            {pid => 908, ClientID => 1},
            {pid => 896, ClientID => 2},
            {pid => 987, ClientID => 2},
            {pid => 988, ClientID => 2},
        ],
    },
    shard_inc_bid => {
        original_db => PPCDICT,
        rows => [
            {bid => 562, ClientID => 1},
            {bid => 563, ClientID => 1},
            {bid => 564, ClientID => 1},
            {bid => 981, ClientID => 1},
            {bid => 101, ClientID => 2},
            {bid => 102, ClientID => 2},
            {bid => 123, ClientID => 2},
        ],
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [],
    },
    targeting_categories => {
        original_db => PPCDICT,
        rows => [],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 456,  ClientID => 1 },
            { cid => 891,  ClientID => 2 },
            { cid => 777,  ClientID => 2 },
            { cid => 789,  ClientID => 2 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 1 },
        ],
    },
    users => {
        original_db => PPC(shard => 1),
        rows => [
            { uid => 1, ClientID => 1 },
        ],
    },
    (
        map {
            $_ => { original_db => PPC(shard => 1),
                    ($Direct::Test::DBObjects::TABLE_ENGINE{$_} 
                             ? (engine => $Direct::Test::DBObjects::TABLE_ENGINE{$_}) 
                             : ()) 
            }
        } qw/
            adgroups_dynamic
            bids_base
            bids_arc
            bids_dynamic
            bids_href_params
            bids_phraseid_history
            bids_performance
            clients
            domains
            group_params
            hierarchical_multipliers
            minus_words
            banners_additions
            additions_item_callouts
            additions_item_disclaimers
            additions_item_experiments
            images banner_images_formats banner_images_pool
            banners_performance perf_creatives banner_resources
            banners_minus_geo
            catalogia_banners_rubrics
            banner_prices
            adgroups_minus_words
            banners_content_promotion_video
            content_promotion_video
            banner_permalinks
            organizations
            banner_measurers
            banners_content_promotion
            content_promotion
            adgroup_priority
        /
    ),
);
init_test_dataset(\%db);

my @tests = (
    [[{cid => 456}, {only_pid => 1, only_creatives => 1}],
        {banners => 4, phrases => 6, groups => 3, retargetings => 4}],
    [[{cid => 456, geo => '10174,-969,-10867'}, {only_pid => 1, only_creatives => 1}],
        {banners => 3, phrases => 3, groups => 1, retargetings => 3}],        
    [[{bid => 101}, {only_pid => 1, only_creatives => 1}],
        {banners => 1, phrases => 1, groups => 1, retargetings => 0}],
    [[{bid => [563, 564]}, {only_pid => 1, only_creatives => 1}],
        {banners => 2, phrases => 3, groups => 1, retargetings => 3}],
    [[{pid => [781, 896, 908, 7777777]}, {only_pid => 1, only_creatives => 1}],
        {banners => 6, phrases => 5, groups => 3, retargetings => 3}],
        
    [[{pid => [781, 896, 908, 7777777], geo => 225, bid => [563, 101, 981]}, {only_pid => 1, only_creatives => 1}],
        {banners => 1, phrases => 1, groups => 1, retargetings => 0}],

    # DIRECT-27451: Test group_name filtering: case-insensetive + %name%
    [[{cid => 456, group_name => 'st GR'}, {only_pid => 1, only_creatives => 1}],
        {banners => 3, phrases => 3, groups => 1, retargetings => 3}],

    [[{cid => 789, arch_banners => 0}, {only_pid => 1, only_creatives => 1}],
        {banners => 1, phrases => 1, groups => 1, retargetings => 0}],
    [[{cid => 777}, {pass_empty_groups => 1, only_creatives => 1}],
        {banners => 0, phrases => 0, groups => 1, retargetings => 0}],

);

foreach my $t (@tests) {
    my $groups = get_groups(@{$t->[0]});
    
    foreach my $f (qw/banners phrases/) {
        is(sum(map {scalar @{$_->{$f}}} @$groups), $t->[1]->{$f}, $f.": ".to_json($t));
    }

    is(sum(map {scalar @{$_->{retargetings} || []}} @$groups), $t->[1]->{retargetings}, "retargetings: ".to_json($t));
    is(scalar(@$groups), $t->[1]->{groups}, "groups: ".to_json($t));
}

done_testing;
