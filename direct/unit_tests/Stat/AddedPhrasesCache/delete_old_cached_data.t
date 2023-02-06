#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use Test::Exception;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DateTime;
use Stat::AddedPhrasesCache;

use Settings;

sub _date_format {
    my $dt = shift;
    return $dt->ymd('-').' '.$dt->hms(':');
}


my $now = _date_format(now());
my $day_ago = _date_format(now() - duration("1d1H"));
my $two_days_ago = _date_format(now() - duration("2d1H"));
my $three_days_ago = _date_format(now() - duration("3d1H"));


my %db = (
    added_phrases_cache => {
        original_db => PPC(shard => 'all'),
        rows        => {
            1 => [
                { phrase_hash => 501, pid => 0, cid => 501, type => 'minus', add_date => $now },
                { phrase_hash => 502, pid => 1, cid => 501, type => 'plus', add_date => $day_ago },
                { phrase_hash => 503, pid => 2, cid => 501, type => 'minus', add_date => $three_days_ago },
            ],
            2 => [
                { phrase_hash => 504, pid => 0, cid => 502, type => 'minus', add_date => $now },
                { phrase_hash => 505, pid => 6, cid => 502, type => 'plus', add_date => $two_days_ago },
                { phrase_hash => 506, pid => 7, cid => 504, type => 'minus', add_date => $three_days_ago },
            ],
        },
    },
);

init_test_dataset(\%db);

Stat::AddedPhrasesCache::delete_old_phrases_from_cache(shard => 1, max_days_in_storage => 2);

my $cached_data = get_one_column_sql(PPC(shard => 1), "SELECT phrase_hash FROM added_phrases_cache ORDER BY phrase_hash");
is_deeply([501, 502], $cached_data, "other hashes should not be deleted from shard 1");

$cached_data = get_one_column_sql(PPC(shard => 2), "SELECT phrase_hash FROM added_phrases_cache ORDER BY phrase_hash");
is_deeply([504, 505, 506], $cached_data, "nothing should be deleted from shard 2");

Stat::AddedPhrasesCache::delete_old_phrases_from_cache(shard => 2, max_days_in_storage => 1);

$cached_data = get_one_column_sql(PPC(shard => 1), "SELECT phrase_hash FROM added_phrases_cache ORDER BY phrase_hash");
is_deeply([501, 502], $cached_data, "other hashes should not be deleted from shard 1");

$cached_data = get_one_column_sql(PPC(shard => 2), "SELECT phrase_hash FROM added_phrases_cache ORDER BY phrase_hash");
is_deeply([504], $cached_data, "nothing should be deleted from shard 2");

dies_ok { Stat::AddedPhrasesCache::delete_old_phrases_from_cache(shard => 1, max_days_in_storage => '2d') } "Non numeric max_days_in_storage should result in error";

done_testing();

1;
