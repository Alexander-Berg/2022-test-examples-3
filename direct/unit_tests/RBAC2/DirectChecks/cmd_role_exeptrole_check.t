#!/usr/bin/perl

=pod

    Нельзя использовать ExceptRole одновременно с Role, и без указания дополнительных проверок (Code, Perm или PerlClear)
    в атрибуте :Rbac для контроллеров

    super_manager - это псевдо-роль, её в ExceptRole не учитываем

=cut

use warnings;
use strict;
use Test::More;

use DoCmd;
use RBAC2::DirectChecks;

my @cmds;

my %rcmds = %RBAC2::DirectChecks::cmds;

for my $cmd (sort keys %rcmds) {
    my $ExceptRole = $rcmds{$cmd}->{ExceptRole};
    next unless $ExceptRole;

    $ExceptRole = [$ExceptRole] if ref($ExceptRole) eq '';
    $ExceptRole = [grep {$_ ne 'super_manager'} @$ExceptRole];
    next unless @$ExceptRole;

    $rcmds{$cmd}->{ExceptRole} = $ExceptRole;
    push @cmds, $cmd;
}

Test::More::plan(tests => 2 * scalar(@cmds));

for my $cmd (@cmds) {
    ok($rcmds{$cmd}->{ExceptRole} && ! $rcmds{$cmd}->{Role}, "dont use ExceptRole and Role simultaneously in :Rbac for '$cmd'");
    ok($rcmds{$cmd}->{ExceptRole} && ($rcmds{$cmd}->{Code}
                                      || $rcmds{$cmd}->{Perm}
                                      || $rcmds{$cmd}->{PermClear}
       ), "dont use ExceptRole without Code, Perm or PermClear in :Rbac for '$cmd'");
}
