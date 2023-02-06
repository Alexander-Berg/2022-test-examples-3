#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Yandex::DBUnitTest qw/:all/;
use Reports::Offline::Performance;

use Settings;


my %db = (
    adgroups_performance => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {pid => 1, feed_id => 1, statusBlGenerated => 'yes'},
                {pid => 2, feed_id => 2, statusBlGenerated => 'yes'},
                {pid => 3, feed_id => 3, statusBlGenerated => 'yes'},
            ],
        },
    },
    perf_feed_categories => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { feed_id => 1, category_id => 2, parent_category_id => 1, name => '1-2' },
                { feed_id => 1, category_id => 1, parent_category_id => 2, name => '1-1' },
                { feed_id => 2, category_id => 135, parent_category_id => 12, name  => '2-135' },
                { feed_id => 2, category_id => 12, parent_category_id => 0, name  => '2-12' },
                { feed_id => 3, category_id => 0, parent_category_id => 0, name => '3-0' },
                { feed_id => 3, category_id => 1, parent_category_id => 2, name => '3-1' },
                { feed_id => 3, category_id => 2, parent_category_id => 0, name => '3-2' },
            ],
        },
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            {pid => 1, ClientID => 1},
            {pid => 2, ClientID => 1},
            {pid => 3, ClientID => 1},
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
        ],
    },
);

init_test_dataset(\%db);


my @data = (
    {
        pid => 1,
        category_id => 2,
        expected => ['1-1', '1-2', undef, undef, undef],
    },
    {
        pid => 2,
        category_id => 135,
        expected => ['2-12', '2-135', undef, undef, undef],
    },
    {
        pid => 2,
        category_id => 12,
        expected => ['2-12', undef, undef, undef, undef],
    },
    {
        pid => 3,
        category_id => 1,
        expected => ['3-0', '3-2', '3-1', undef, undef],
    },
);


Test::More::plan(tests => scalar (@data));

for my $sample (@data) {
    my @categories = Reports::Offline::Performance::_get_category_names({}, $sample->{pid}, $sample->{category_id});
    is_deeply(\@categories, $sample->{expected}, 'Incorrect result');
}

1;
