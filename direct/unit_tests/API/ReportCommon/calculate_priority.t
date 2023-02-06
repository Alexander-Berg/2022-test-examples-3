#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use API::ReportCommon;

{
    local $API::Settings::WORDSTAT_PRIORITY_STEP = 20;
    is(calculate_priority(1), 0, "1/20");
    is(calculate_priority(3), 40, "3/20");
    is(calculate_priority(6), 100, "6/20");
    is(calculate_priority(7), 0, "7/20");
}

{
    local $API::Settings::WORDSTAT_PRIORITY_STEP = 50;
    is(calculate_priority(1), 0, "1/50");
    is(calculate_priority(2), 50, "2/50");
    is(calculate_priority(3), 100, "3/50");
    is(calculate_priority(6), 100, "6/50");
    is(calculate_priority(7), 0, "7/50");
}

{
    local $API::Settings::WORDSTAT_PRIORITY_STEP = 25;
    is(calculate_priority(1), 0, "1/25");
    is(calculate_priority(2), 25, "2/25");
    is(calculate_priority(9), 75, "9/25");
    is(calculate_priority(10), 100, "10/25");
    is(calculate_priority(18), 50, "18/25");
}

done_testing;
