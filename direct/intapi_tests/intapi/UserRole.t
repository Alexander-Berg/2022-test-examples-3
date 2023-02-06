#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;
use XML::LibXML;
use Data::Dumper;

use utf8;
use open ':std' => ':utf8';

my $base = "http://8804.beta.direct.yandex.ru/intapi";

#wget -O - 'http://beta.direct.yandex.ru:8804/UserRole?uid=11787588&format=xml'
#wget -O - 'http://beta.direct.yandex.ru:8804/UserRole?uid=11787588'           

my $uid_ag = 11787588; # icontext
my $uid_cl = 6138950; # holodilnikru
my $client_id_ag = 5131;
my $client_id_cl = 15155;

my $url = base_url()."/UserRole";

my @tests = (
    {
        name => 'empty parameters',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 500,
        data => ['dummy'],
        preprocess => sub { return {}, },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "decode json";
            is(ref $v, "HASH", 'result is a hash');
            ok(exists $v->{result}, 'message');
        },
    },
    {
        name => 'wrong parameters',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 500,
        data => [$uid_ag],
        preprocess => sub { return {uids => $_[0]} },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "decode json";
            is(ref $v, "HASH", 'result is a hash');
            ok(exists $v->{result}, 'message');
        },
    },
    {
        name => 'agency',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => [$uid_ag],
        preprocess => sub { return {uid => $_[0]} },
        check_num => 4,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "decode json";
            is(ref $v, "HASH", 'result is a hash');
            is($v->{role}, 'agency', 'role');
            is($v->{client_id}, $client_id_ag, 'client_id');
        },
    },
    {
        name => 'agency xml',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => [$uid_ag],
        preprocess => sub { return {uid => $_[0], format => 'xml'} },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $doc;
            lives_ok { $doc = XML::LibXML->new()->parse_string($resp->content) } "parse xml";
            my $role = XML::LibXML::XPathContext->new($doc)->find("/user/role")->string_value();
            is($role, 'agency', 'xml: role for agency');
        },
    },
    {
        name => 'client',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => [$uid_cl],
        preprocess => sub { return {uid => $_[0]} },
        check_num => 4,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "decode json";
            is(ref $v, "HASH", 'result is a hash');
            is($v->{role}, 'client', 'role');
            is($v->{client_id}, $client_id_cl, 'client_id');
        },
    },
);

run_tests(\@tests);
