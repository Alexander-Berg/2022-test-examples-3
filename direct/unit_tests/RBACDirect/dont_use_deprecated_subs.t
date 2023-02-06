#!/usr/bin/perl

=pod

    проверка на использование устаревших ф-ций проверки из RBACDirect.

    $Id$

=cut

use strict;
use warnings;

use Test::More;

use DoCmd;
use RBAC2::DirectChecks;

my %deprecated_subs = map {$_ . "" => 1} (
    \&RBAC2::DirectChecks::rbac_cmd_showCamp     # нужно использовать rbac_cmd_by_owners/rbac_cmd_user_allow_edit_camps
);

my %skip_docmd_subs = map {$_ => 1} qw/
    showCamp
/;

my @cmds = sort keys %DoCmd::Base::cmds;

Test::More::plan(tests => scalar(@cmds));

for my $cmd (@cmds) {
    ok(
        $RBAC2::DirectChecks::cmds{$cmd}
        &&
        (! (grep {
                    ref $_ ne 'CODE'
                    ||
                    ! $skip_docmd_subs{$cmd} && $deprecated_subs{ $_ . "" }
                 } @{$RBAC2::DirectChecks::cmds{$cmd}->{Code}}
           )
        ), "Dont use deprecated rbac-check sub for '$cmd'"
    );
}
