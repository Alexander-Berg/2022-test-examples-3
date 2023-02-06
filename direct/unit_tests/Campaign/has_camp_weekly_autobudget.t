#!/usr/bin/perl

# $Id$

use strict;
use warnings;
use utf8;

use Test::More;

use Settings;
use Yandex::Test::UTF8Builder;

BEGIN { 
    require_ok('Campaign');
}

*has_camp_weekly_autobudget = *Campaign::has_camp_weekly_autobudget;

is has_camp_weekly_autobudget(), undef, 'without camp';
is has_camp_weekly_autobudget({ autobudget => 'No' }), 0, 'camp with autobudget eq to "no"';
is has_camp_weekly_autobudget({ autobudget => 'Yes' }), 0, 'camp without autobudget sum';

my $strategy = { name => 'autubudget' };
is has_camp_weekly_autobudget({ autobudget => 'Yes', strategy_decoded => $strategy }), 0, 'camp with autobudget sum eq to 0';

$strategy->{sum} = 1;
is has_camp_weekly_autobudget({ autobudget => 'Yes', strategy_decoded => $strategy, statusBsSynced => 'No' }), 0, 'camp with statusBsSynced eq to "no"';
is has_camp_weekly_autobudget({ autobudget => 'Yes', strategy_decoded => $strategy, statusBsSynced => 'Yes' }), 1, 'camp with weekly autobudget';

done_testing();
