#!/usr/bin/perl

=pod
    Наличие проверок для всех контроллеров
=cut

use warnings;
use strict;
use Test::More;

use DoCmd;
use RBAC2::DirectChecks;
use Campaign::Types;

my @cmds = sort keys %DoCmd::Base::cmds;

my %rcmds = %RBAC2::DirectChecks::cmds;

for my $cmd (@cmds) {
    my $r = $rcmds{$cmd};
    ok(
        $r
        && ( ( ref $r->{Code} eq 'ARRAY' && !(grep { ref $_ ne 'CODE'} @{$r->{Code}}) )
            || $r->{Perm}
            || $r->{PermClear}
            || $r->{Role}
        ), 
        "Access checker missed for '$cmd'");
    if ($r->{CampKind}) {
        my $ok = 1;
        while(my ($kind, $rule) = each %{$r->{CampKind}}) {
            $ok &&= exists $Campaign::Types::KINDS{$kind} 
                && (!ref $rule || ref $rule eq 'ARRAY');
        }
        ok($ok, "correct :CampKind for $cmd");
    }
}

done_testing;
