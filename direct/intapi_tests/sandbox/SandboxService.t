#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;
use XML::LibXML;
use Data::Dumper;

use utf8;
use open ':std' => ':utf8';

#curl 'http://8805.beta.direct.yandex.ru/SandboxService/?method=GetCurrentState&uid=28037182'

my @ag_uids = (
    28073828, 
    18166733, 
    10764791, 
);

my @cl_uids = (
    48736123, 
    56079534, 
    56335739, 
    63434190, 
    64302442, 
);

my @wrong_uids = (
    'aaa',
    '28073828aaa',
);

my $url = base_url()."/SandboxService";

my @tests = (
    {
        name => 'wrong uids',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => \@wrong_uids,
        preprocess => sub { return {uid => $_[0], method => 'GetCurrentState'} },
        check_num => 1,
        check => sub {
            my ($data, $resp, $name) = @_;
            ok($resp->content eq "null", 'result is "null"');
        },
    },
    {
        name => 'agency',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => \@ag_uids,
        preprocess => sub { return {uid => $_[0], method => 'GetCurrentState'} },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "decode json";
            is(ref $v, "HASH", 'result is a hash');
            is($v->{role}, 'agency', "$name: role for $data");
        },
    },
    {
        name => 'client',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => \@cl_uids,
        preprocess => sub { return {uid => $_[0], method => 'GetCurrentState'} },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "decode json";
            is(ref $v, "HASH", 'result is a hash');
            is($v->{role}, 'client', "$name: role for $data");
        },
    },
);

run_tests(\@tests);
