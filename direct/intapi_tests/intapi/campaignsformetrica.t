#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use Yandex::HTTP;
use YAML;

use utf8;
use open ':std' => ':utf8';

my $url = "/campaignsformetrica";

my @clients_with_campaigns = (
    28037182, # lena-san-std
         613, 
      925525, 
     1485158, 
     5151531, 
);

my @fatal_wrong_uids = qw/aaa 13bb ccc45/;
my @wrong_uids = qw/00/;

my @tests = (
    {
        name => 'no uid',
        read_only => 1,
        url => $url,
        method => 'GET',
        code => 404,
        data => [{}, {login => "holodilnikru"}, {iud => 13},],
        check_num => 1,
        check => sub {
            my ($data, $resp, $name) = @_;
            is($resp->content, "Invalid uid", "$name:error message");
        }
    },
    {
        name => 'fatal wrong uid',
        read_only => 1,
        url => $url,
        method => 'GET',
        code => 404,
        lwp_opt => {timeout => 15},
        data => \@fatal_wrong_uids,
        preprocess => sub { return {uid => $_[0]} },
        check_num => 1,
        check => sub {
            my ($data, $resp, $name) = @_;
            is($resp->content, 'Invalid uid', "$name:error message");
        }
    },
    {
        name => 'wrong uid',
        read_only => 1,
        url => $url,
        method => 'GET',
        code => 404,
        data => \@wrong_uids,
        preprocess => sub { return {uid => $_[0]} },
        check_num => 1,
        check => sub {
            my ($data, $resp, $name) = @_;
            is($resp->content, 'Invalid uid', "$name:error message");
        }
    },
    {
        name => 'simple query',
        read_only => 1,
        url => $url,
        method => 'GET',
        data => \@clients_with_campaigns,
        preprocess => sub { return {uid => $_[0]} },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok {$v = YAML::Load($resp->content)} "$name: decode YAML";
            is(ref $v, "ARRAY", "$name: result is an array");
            ok(@$v > 0, "$name: campaigns count > 0");
        }
    },
    {
        name => 'back to back test for metrica campaigns',
        read_only => 1,
        is_backtoback => 1,
        url => $url,
        method => 'GET',
        data => \@clients_with_campaigns,
        preprocess => sub { return {uid => $_[0]} },
        check_num => 3,
        check => sub {
            my ($data, $responses, $name) = @_;
            my @campaigns;
            foreach my $r (@$responses) {
                my $c;
                lives_ok {$c = YAML::Load($r->content)} "$name: decode YAML";
                push @campaigns, $c;
            }
            cmp_deeply(@campaigns, 'compare data format');
        }
    },
);

run_tests(\@tests);

