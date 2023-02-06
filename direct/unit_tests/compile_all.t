#!/usr/bin/perl

use strict;
use warnings;
use utf8;
use lib::abs;
use Test::More;
use Getopt::Long;

use Path::Tiny;

my @LIBS;
GetOptions(
    "lib=s@" => \@LIBS,
);

my $LIB_ROOT = path(lib::abs::path('..'));

if (!@LIBS) {
    @LIBS = ($LIB_ROOT);
}

my @files;
for my $lib (@LIBS) {
    my $iter = path($lib)->iterator({recurse => 1});
    while (my $p = $iter->()) {
        if ($p =~ /\.p[lm]$/) {
            push @files, ''.$p->absolute;
        }
    }
}

for my $f (sort @files) {
    my ($dir) = ($f =~ m!$LIB_ROOT/([^/]+)/.*!);
    my $out = `( cd $LIB_ROOT/$dir && perl -Ilib -cw $f 2>&1)`;
    my $rv = $? >> 8;
    ok($rv == 0, "$f compile error");
    if ($out !~ /syntax OK/) {
        diag($out);
    }
    ok($out =~ /syntax OK/, "syntax $f")
}

done_testing();
