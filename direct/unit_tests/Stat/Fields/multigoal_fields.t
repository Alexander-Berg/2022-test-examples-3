#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use Stat::StreamExtended;

use_ok('Stat::Fields');

my @module_fields = Stat::Fields::get_multigoal_field_names();
my @converted_fields = sort values %Stat::StreamExtended::multi_goals_fields_bs2direct;
is_deeply([sort @module_fields], \@converted_fields, "multigoal fields names list is up to date");

done_testing;
