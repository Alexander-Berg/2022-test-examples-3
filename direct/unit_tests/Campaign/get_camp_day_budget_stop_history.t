#!/usr/bin/perl
use Direct::Modern;

use Test::More;

use Settings;
use Yandex::DateTime qw/now/;
use Yandex::DBTools;

use Test::CreateDBObjects;

BEGIN {
    use_ok 'Campaign';
}

create_tables();

my $cid = create('campaign');

my $minus0 = now()->ymd();
my $today = $minus0;
my $minus1 = now()->subtract(days => 1)->ymd();
my $minus2 = now()->subtract(days => 2)->ymd();
my $minus13 = now()->subtract(days => $Campaign::DAILY_BUDGET_STOP_STATS_DAYS - 1)->ymd();
my $minus14 = now()->subtract(days => ($Campaign::DAILY_BUDGET_STOP_STATS_DAYS))->ymd();

my @tests = (
    # [
    #     "<test name>",
    #     [<db stats>],
    #     [<expected stats>]
    # ],
    [
        "no stats",
        [],
        [],
    ],
    [
        "out of two stats in the same day pick the latest",
        [
            "$minus1 12:34:56",
            "$minus1 23:45:12",
            "$minus2 13:22:02",
        ],
        [
            "$minus1 23:45:12",
            "$minus2 13:22:02",
        ],
    ],
    [
        "border stats",
        [
            "$minus13 18:11:43",
            "$minus14 06:12:36",
        ],
        [
            "$minus13 18:11:43",
        ],
    ],
    [
        "unsorted stats",
        [
            "$minus1 23:45:12",
            "$minus2 13:22:02",
            "$minus0 01:33:16",
        ],
        [
            "$minus0 01:33:16",
            "$minus1 23:45:12",
            "$minus2 13:22:02",
        ],
    ],
    [
        "only old stats",
        [
            "$minus14 06:12:36",
        ],
        [],
    ],
);


for my $t (@tests) {
    my ($test_name, $db_stats, $expected_stats) = @$t;

    fill_camp_stats($db_stats);
    my $actual_stats = Campaign::get_day_budget_stop_history($cid, fake_current_date => $today);

    is_deeply($actual_stats, $expected_stats, $test_name);
}

done_testing();

sub fill_camp_stats {
    my ($stats) = @_;

    do_delete_from_table(PPC(cid => $cid), 'camp_day_budget_stop_history', where => {cid => $cid});
    do_mass_insert_sql(
       PPC(cid => $cid),
       "INSERT INTO camp_day_budget_stop_history(cid, stop_time) VALUES %s",
       [map { [$cid, $_] } @$stats]
    );
}
