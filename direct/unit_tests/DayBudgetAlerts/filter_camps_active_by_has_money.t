#!/usr/bin/perl

use Direct::Modern;

use Yandex::Test::UTF8Builder;
use Test::More;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/init_test_dataset/;

use DayBudgetAlerts;
use Settings;

my @cases = (
    {sum1 => 0,  sum2 => 0,  sum3 => 0, spent2 => 0,  spent3 => 0,  wallet_cid => 0, a2 => 0, a3 => 0},
    {sum1 => 0,  sum2 => 0,  sum3 => 0, spent2 => 0,  spent3 => 0,  wallet_cid => 1, a2 => 0, a3 => 0},
    {sum1 => 20, sum2 => 10, sum3 => 0, spent2 => 10, spent3 => 20, wallet_cid => 1, a2 => 0, a3 => 0},
    {sum1 => 20, sum2 => 10, sum3 => 0, spent2 => 0,  spent3 => 20, wallet_cid => 1, a2 => 1, a3 => 0},
    {sum1 => 20, sum2 => 10, sum3 => 0, spent2 => 10, spent3 => 0,  wallet_cid => 1, a2 => 1, a3 => 1},
    {sum1 => 20, sum2 => 10, sum3 => 0, spent2 => 15, spent3 => 0,  wallet_cid => 1, a2 => 1, a3 => 1},
    {sum1 => 20, sum2 => 10, sum3 => 0, spent2 => 30, spent3 => 0,  wallet_cid => 1, a2 => 0, a3 => 0},
    {sum1 => 20, sum2 => 10, sum3 => 0, spent2 => 15, spent3 => 15, wallet_cid => 1, a2 => 0, a3 => 0},
    {sum1 => 20, sum2 => 0,  sum3 => 0, spent2 => 14, spent3 => 5,  wallet_cid => 1, a2 => 1, a3 => 1},

    {sum1 => 0,  sum2 => 10, sum3 => 0, spent2 => 0,  spent3 => 0,  wallet_cid => 0, a2 => 1, a3 => 0},
    {sum1 => 0,  sum2 => 10, sum3 => 0, spent2 => 10, spent3 => 0,  wallet_cid => 0, a2 => 0, a3 => 0},
);

my $i = 0;
for my $case (@cases) {
    init_test_dataset({
        campaigns => {
            original_db => PPC(shard => 1),
            rows => {
                1 => [
                    { sum => $case->{sum1}, sum_spent => 0,               wallet_cid => 0,                   cid => 1, type => 'wallet', statusEmpty => 'No', },
                    { sum => $case->{sum2}, sum_spent => $case->{spent2}, wallet_cid => $case->{wallet_cid}, cid => 2, type => 'text',   statusEmpty => 'No', },
                    { sum => $case->{sum3}, sum_spent => $case->{spent3}, wallet_cid => $case->{wallet_cid}, cid => 3, type => 'text',   statusEmpty => 'No', },
                ],
            },
        },
        shard_inc_cid => {
            original_db => PPCDICT,
            rows => [
                map { +{ cid => $_, ClientID => 1 } } qw/1 2 3/
            ],
        },
        shard_client_id => {
            original_db => PPCDICT,
            rows => [
                { ClientID => 1, shard => 1 },
            ],
        },
    });

    subtest "testcase " . ++$i, sub {
        my $camps = get_all_sql(PPC(shard => 1), 'SELECT cid, sum, sum_spent, wallet_cid FROM campaigns');
        my $filtered = DayBudgetAlerts::filter_camps_active_by_has_money($camps);
        ok( $case->{a2} == (scalar grep {$_->{cid} == 2} @$filtered ) , "cid: 2 is " . ($case->{a2} ? "active" : "non-active"));
        ok( $case->{a3} == (scalar grep {$_->{cid} == 3} @$filtered ) , "cid: 3 is " . ($case->{a2} ? "active" : "non-active"));
    };
}


done_testing();
