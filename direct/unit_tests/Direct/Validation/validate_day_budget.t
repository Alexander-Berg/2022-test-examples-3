#!/usr/bin/perl

# $Id: validate_day_budget.t 123368 2016-08-22 13:15:05Z liosha $

use strict;
use warnings;
use utf8;

use Yandex::Test::UTF8Builder;
use Test::More;
use Yandex::Test::ValidationResult;

use Settings;
use Yandex::DBUnitTest qw/:all/;

use Yandex::HashUtils;
use Storable qw/dclone/;

BEGIN { use_ok('Direct::Validation::DayBudget', 'validate_camp_day_budget', 'validate_wallet_day_budget'); }

my %db = (
    camp_options => {
        original_db => PPC(shard => 'all'),
        like => 'camp_options',
        rows => {
            1 => [{cid => 300000001, day_budget_daily_change_count => 2},
                  {cid => 300000002, day_budget_daily_change_count => 3}],
            2 => [{cid => 300000003, day_budget_daily_change_count => 3}]
            },
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            {cid => 300000001, ClientID => 1},
            {cid => 300000002, ClientID => 1},
            {cid => 300000003, ClientID => 2},
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 2},
        ],
    },
);

init_test_dataset(\%db);

my %normal_day_budget = (
    cid => 300000001,
    strategy => 'default', 
    new_day_budget_data => {
        day_budget => 123, 
        day_budget_show_mode => 'stretched', 
        },
    old_day_budget_data => {
        day_budget => 456, 
        day_budget_daily_change_count => 2,
        },
    currency => 'YND_FIXED'
);

my $day_budget_without_change_count = dclone(\%normal_day_budget);
delete $day_budget_without_change_count->{old_day_budget_data}{day_budget_daily_change_count};

my $day_budget_with_max_change_count = dclone(\%normal_day_budget);
$day_budget_with_max_change_count->{old_day_budget_data}->{day_budget_daily_change_count} = 3;

my $vr_1 = validate_camp_day_budget(%normal_day_budget);
ok_validation_result($vr_1, 'корректные данные дневного бюджета');

# валидация должна ругаться на автобюджетные стратегии с установленным дневным бюджетом
for my $strategy(qw/autobudget autobudget_avg_click autobudget_week_bundle/) {
    my $vr_2 = validate_camp_day_budget(%{hash_merge({}, \%normal_day_budget, {strategy => $strategy})});
    cmp_validation_result($vr_2, vr_errors(qr/можно использовать только совместно с ручными стратегиями/), "автобюджетаная стратегия $strategy");
}

# и не должна ругаться на стратегии с ручным управлением ставками
for my $strategy(@Campaign::MANUAL_PRICE_STRATEGIES) {
    my $vr_3 = validate_camp_day_budget(%{hash_merge({}, \%normal_day_budget, {strategy => $strategy})});
    ok_validation_result($vr_3, "ручная стратегия $strategy");
}

# валидация не должна пропускать отрицательные суммы бюджета
my $vr_4 = validate_camp_day_budget(%{hash_merge({}, \%normal_day_budget, {new_day_budget_data => {day_budget_show_mode => 'stretched', day_budget => -234}})});
cmp_validation_result($vr_4, {day_budget => vr_errors(qr/сумма дневного бюджета/)}, 'отрицательная сумма дневного бюджета');

# валидация не должна пропускать маленьких сумм дневного бюджета
my $vr_5 = validate_camp_day_budget(%{hash_merge({}, \%normal_day_budget, {new_day_budget_data => {day_budget_show_mode => 'stretched', day_budget => 1}})});
cmp_validation_result($vr_5, {day_budget => vr_errors(qr/сумма дневного бюджета/)}, 'маленькая сумма дневного бюджета');

# валидация не должна пропускать маленьких сумм дневного бюджета даже в мультивалютных кампаниях
my $vr_6 = validate_camp_day_budget(%{hash_merge({}, \%normal_day_budget, {new_day_budget_data => {day_budget_show_mode => 'stretched', day_budget => 200}, currency => 'RUB'})});
cmp_validation_result($vr_6, {day_budget => vr_errors(qr/сумма дневного бюджета/)}, 'маленькая сумма дневного бюджета');

# валидация должна работать для новых кампаний
my $vr_7 = validate_camp_day_budget(%{hash_merge({}, \%normal_day_budget, {cid => undef, new_camp => 1, old_day_budget_data => undef})});
ok_validation_result($vr_7, 'новая кампания');

my $vr_10 = validate_camp_day_budget(%$day_budget_with_max_change_count);
cmp_validation_result($vr_10, vr_errors(qr/можно менять не более \d+ раз в день/), 'предельное количество изменений дневного бюджета по № кампании');

done_testing;
