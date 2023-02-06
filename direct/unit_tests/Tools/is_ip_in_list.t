#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 7;
use File::Temp qw/tempfile/;
use File::Slurp;

use Settings;
use IpTools;

my (undef, $tmpfile) = tempfile(UNLINK => 1);

write_file($tmpfile, "
213.180.215.0

# comment

# 10.10.10.10

213.180.216.0/24

");


ok(is_ip_in_list("213.180.215.0", $tmpfile), "single ip");
ok(!is_ip_in_list("213.180.215.1", $tmpfile), "near single ip");

ok(is_ip_in_list("213.180.216.0", $tmpfile), "network");
ok(is_ip_in_list("213.180.216.60", $tmpfile), "network");
ok(is_ip_in_list("213.180.216.255", $tmpfile), "network");
ok(!is_ip_in_list("213.180.217.0", $tmpfile), "network");

ok(!is_ip_in_list("10.10.10.10", $tmpfile), "comment");

