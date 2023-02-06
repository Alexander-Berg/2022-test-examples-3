#!/usr/bin/perl

=pod

    у всех команд должен обязательно присутствовать атрибут с описанием команды :Description

=cut

use strict;
use warnings;

use Test::More;

use DoCmd;

my @cmds = sort keys %DoCmd::Base::cmds;

Test::More::plan(tests => scalar(@cmds));

for my $cmd (@cmds) {
    ok(defined $DoCmd::Base::CmdDescriptions{$cmd}, "Descriptions attribute missing for cmd='$cmd'");
}
