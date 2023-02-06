#!/usr/bin/perl

use warnings;
use strict;
use File::Temp qw/tempfile/;

use Test::More tests => 14;

$Settings::MIRRORS_FILE_BDB = '';

use MirrorsTools;
$MirrorsTools::SKIP_PREPROCESSOR_RUN = 1;

my (undef, $mirror_file) = tempfile(UNLINK => 1, SUFFIX => ".gz");
open(my $fh, "|-", "gzip >$mirror_file") || die $!;
print $fh
qq{
0\t0\t5ballov.ru
0\t0\t5balov.ru

0\t0\twww.rbc.ru
0\t0\twww1.rbc.ru
0\t0\trbk.ru

0\t0\trbc-forum.ru
0\t0\tforum.rbk.ru

0\t0\tsuper-roof.ru
0\t0\tmproof.ru
0\t0\twww.mproof.ru
0\t0\troofdiscount.ru
0\t0\twww.roofdiscount.ru

0\t0\tsuper-roof.spb.ru
0\t0\tsuper-roof.ru

0\t0\tqqq.ru
0\t0\twww.segway.ru

0\t0\tqqq2.ru
0\t0\twww.segway2.ru

0\t0\twww2.ru
0\t0\tsegway2.ru

};
close $fh or die $!;

open(my $fh2, "|-", "gzip >$mirror_file.preprocessed.gz") || die $!;
print $fh2
qq{
5ballov.ru 5balov.ru
www.rbc.ru www1.rbc.ru rbk.ru
rbc-forum.ru forum.rbk.ru
super-roof.ru mproof.ru www.mproof.ru roofdiscount.ru www.roofdiscount.ru
super-roof.spb.ru super-roof.ru
qqq.ru www.segway.ru
qqq2.ru www.segway2.ru
www2.ru segway2.ru
};
close $fh2 or die $!;

my $mt = new MirrorsTools(mirror_file => $mirror_file);

is($mt->domain_filter("www.5balov.ru"), "5ballov.ru");
is($mt->domain_filter("www.5ballov.ru"), "5ballov.ru");
is($mt->domain_filter("www.hmn.ru"), "hmn.ru");
is($mt->domain_filter("www.hmn.narod.ru"), "hmn.narod.ru");
is($mt->domain_filter("hmn.narod.ru"), "hmn.narod.ru");
is($mt->domain_filter("forum.rbk.ru"), "rbc-forum.ru");
is($mt->domain_filter("asdf.rbk.ru"), "rbc.ru");

is($mt->domain_filter("roofdiscount.ru"), "super-roof.spb.ru");

is($mt->domain_filter("www.segway.ru"), "qqq.ru");
is($mt->domain_filter("segway.ru"), "qqq.ru");

is($mt->domain_filter("www.segway2.ru"), "qqq2.ru");
is($mt->domain_filter("segway2.ru"), "www2.ru");

my $ret = eval {
    alarm 1;
    $mt->domain_filter(".");
};
alarm 0;
ok(!$@, "no inifite loop");
ok(!defined $ret, "undefined filter for bad domain");
