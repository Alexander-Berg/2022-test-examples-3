#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;

use utf8;
use open ':std' => ':utf8';


my $url = base_url("https")."/DirectConfiguration";

my @tests = (
    {
        name => 'simple query',
        read_only => 1,
        url => $url, 
        method => 'GET', 
        data => [{}],
        check_num => 8, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");

            ok(exists $v->{db_info}, "$name db_info");
            ok(exists $v->{db_info}->{'ppc:1'}, "$name: info for ppc:1 db");
            ok(keys %{$v->{db_info}} > 10, "$name: there are quite a few of databases");
            ok(length $v->{db_info}->{'ppc:1'}->{host} > 5, "$name: host for ppc:1 db");

            ok(exists $v->{direct_version}, "direct_version exists");
            ok($v->{direct_version} =~ /^\d+\.\d+\S+$/, "direct_version exists");
        },
    },
);

run_tests(\@tests);

