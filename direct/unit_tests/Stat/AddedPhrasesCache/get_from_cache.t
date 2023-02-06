#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use Test::Exception;

use Yandex::DBUnitTest qw/:all/;
use Yandex::DateTime;
use Stat::AddedPhrasesCache;

use Settings;

sub _date_format {
    my $dt = shift;
    return $dt->ymd('-').' '.$dt->hms(':');
}


my $now = _date_format(now());
my $four_days_ago = _date_format(now() - duration("4d1H"));

my %db = (
    added_phrases_cache => {
        original_db => PPC(shard => 'all'),
        rows        => {
            2 => [
                {phrase_hash => 1, bids_id => 51, pid => 2, cid => 921, type => 'plus', add_date => $now},
                {phrase_hash => 2, bids_id => 0, pid => 2, cid => 921, type => 'minus', add_date => $now},
                {phrase_hash => 3, bids_id => 53, pid => 2, cid => 921, type => 'plus', add_date => $four_days_ago},
            ],
            3 => [
                {phrase_hash => 4, bids_id => 54, pid => 4, cid => 931, type => 'plus', add_date => $now},
                {phrase_hash => 5, bids_id => 0, pid => 5, cid => 931, type => 'minus', add_date => $four_days_ago},
            ],
        },
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            2 => [{uid => 503, cid => 921, type => 'text', OrderID => 951}],
            3 => [{uid => 504, cid => 931, type => 'text', OrderID => 961}],
        },
    },
    bids => {
        original_db => PPC(shard => 'all'),
        rows => {
            2 => [{id => 51, phrase => 'text', norm_phrase => 'text'},],
            3 => [{id => 54, phrase => 'text', norm_phrase => 'text'},],
        },
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            {cid => 921, ClientID => 12},
            {cid => 931, ClientID => 13},
        ],
    },
    shard_order_id => {
        original_db => PPCDICT,
        rows => [
            {OrderID => 951, ClientID => 12},
            {OrderID => 961, ClientID => 13},
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 12, shard => 2},
            {ClientID => 13, shard => 3},
        ],
    },
);

init_test_dataset(\%db);

my @test_data = (
    ["OrderID", [951], undef,
        {951 => {2 => {plus => {1 => [1], 0 => [3]}, minus => {1 => [2]}}}}],
    ["OrderID", [951, 961], undef,
        {951 => {2 => {plus => {1 => [1], 0 => [3]}, minus => {1 => [2]}}},
            961 => {4 => {plus => {1 => [4]}}, 5 => {minus => {1 => [5]}}}}],
    ["OrderID", [951, 961], {type => 'minus'},
        {951 => {2 => {minus => {1 => [2]}}}, 961 => {5 => {minus => {1 => [5]}}}}],
    ["cid", [921], undef,
        {951 => {2 => {plus => {1 => [1], 0 => [3]}, minus => {1 => [2]}}}}],
    ["cid", [921, 931], undef,
        {951 => {2 => {plus => {1 => [1], 0 => [3]}, minus => {1 => [2]}}},
            961 => {4 => {plus => {1 => [4]}}, 5 => {minus => {1 => [5]}}}}],
    ["cid", [921, 931], {type => 'minus'},
        {951 => {2 => {minus => {1 => [2]}}}, 961 => {5 => {minus => {1 => [5]}}}}],
);

for my $data (@test_data) {
    my ($key, $ids, $opts, $expected) = @$data;

    my $real = Stat::AddedPhrasesCache::get_phrases_from_cache_by_key($key => $ids, ($opts && %$opts ? %$opts : ()));
    is_deeply($real, $expected, "Real result equals to expected");
}

dies_ok { Stat::AddedPhrasesCache::get_phrases_from_cache_by_key(pid => [123]) } "Cannot get phrases by key different from OrderID";
dies_ok { Stat::AddedPhrasesCache::get_phrases_from_cache_by_key(OrderID => [123], type => 'test') } "Cannot get phrases of invalid type";

done_testing();

1;
