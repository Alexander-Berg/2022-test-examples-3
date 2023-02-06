#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use Stat::StreamExtended;

use_ok('Stat::Fields');

my $deps = Stat::Fields::_get_countable_fields_dependencies();
my @countable_fields = Stat::StreamExtended::_get_all_countable_fields();
for my $field (@countable_fields) {
    ok(defined $deps->{$field}, "Field $field is known");
}

done_testing;
