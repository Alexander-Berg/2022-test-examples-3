#!/usr/bin/perl

# $Id: can_day_budget_be_set.t 25613 2011-09-15 14:56:34Z pankovpv $

use strict;
use warnings;
use utf8;

use Yandex::Test::UTF8Builder;
use Test::More tests => 7;

BEGIN { use_ok('Direct::Validation::DayBudget'); }

*can_day_budget_be_set = \&Direct::Validation::DayBudget::_can_day_budget_be_set;

# при включении дневного бюджета должно проверяться число изменений за день
is(can_day_budget_be_set(before => {day_budget => 0, day_budget_daily_change_count => 1}, after => {day_budget => 456}), undef, 'включение дневного бюджета');
isnt(can_day_budget_be_set(before => {day_budget => 0, day_budget_daily_change_count => 3}, after => {day_budget => 456}), undef, 'включение дневного бюджета с превышением числа изменений');

# и при изменении также должно проверяться число изменений за день
is(can_day_budget_be_set(before => {day_budget => 123, day_budget_daily_change_count => 1}, after => {day_budget => 456}), undef, 'изменение дневного бюджета');
isnt(can_day_budget_be_set(before => {day_budget => 123, day_budget_daily_change_count => 3}, after => {day_budget => 456}), undef, 'изменение дневного бюджета с превышением числа изменений');

# а при выключении число изменений не должно проверяться
is(can_day_budget_be_set(before => {day_budget => 123, day_budget_daily_change_count => 1}, after => {day_budget => 0}), undef, 'отключение дневного бюджета');
is(can_day_budget_be_set(before => {day_budget => 123, day_budget_daily_change_count => 3}, after => {day_budget => 0}), undef, 'отключение дневного бюджета с превышением числа изменений');
