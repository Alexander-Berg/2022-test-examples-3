#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Campaign::Types;

while(my ($kind, $types) = each %Campaign::Types::KINDS) {
    for my $type (keys %$types) {
        ok($Campaign::Types::TYPES{$type} && $types->{$type}, "correct $kind - $type");
    }
}

done_testing;
