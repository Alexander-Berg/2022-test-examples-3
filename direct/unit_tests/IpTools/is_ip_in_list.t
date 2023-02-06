#!/usr/bin/perl

# $Id: is_ip_in_networks.t 88947 2015-07-19 20:11:37Z ppalex $

use warnings;
use strict;
use Test::More;
use Path::Tiny;

BEGIN { use_ok( 'IpTools' ); }

my $tmp_file = Path::Tiny->tempfile();
$tmp_file->spew('
192.168.50.254/30
192.168.0.1/24
127.0.0.1
5.17.140.1/32
5.17.140.2/32
192.168.42.42/24
0000:0000:0000:0000:0000:0000:c0a8:2a2a/120

3a02:6b8:c00::/40

697@2a02:6b8:c00::/40

');

*in = sub {IpTools::is_ip_in_list(shift, "$tmp_file")};

ok(in("192.168.0.254"));
ok(!in("192.168.1.254"));
ok(in("192.168.50.252"));
ok(!in("192.168.50.249"));
ok(in("192.168.42.100"));
ok(in("192.168.42.43"));
ok(!in("5.17.140.0"));
ok(in("5.17.140.1"));
ok(in("5.17.140.2"));
ok(!in("5.17.140.3"));
ok(in("0000:0000:0000:0000:0000:0000:c0a8:2a64"));
ok(in("0000:0000:0000:0000:0000:0000:c0a8:2a2a"));
ok(!in("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));

ok(in("3a02:6b8:c00:2588::697:3e4c:ec25"));
ok(in("3a02:6b8:c00:2588::698:3e4c:ec25"));
ok(!in("3a02:6b8:d0:2588::697:3e4c:ec25"));
ok(in("3a02:6b8:c00:2588:1:697:3e4c:ec25"));

ok(in("2a02:6b8:c00:2588::697:3e4c:ec25"));
ok(!in("2a02:6b8:c00:2588::698:3e4c:ec25"));
ok(!in("2a02:6b8:d0:2588::697:3e4c:ec25"));
ok(!in("2a02:6b8:c00:2588:1:697:3e4c:ec25"));

done_testing;
