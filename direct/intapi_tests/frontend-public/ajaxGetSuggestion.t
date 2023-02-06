#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;
use Encode;

use utf8;
use open ':std' => ':utf8';


my $url = base_url()."/public?cmd=ajaxGetSuggestion";


my @suggest_data = (
    {
        srcPhrases => ['кондиционер','nokia'],
    }, 
    {
        geo => "1,-213,2",
        srcPhrases => ['кондиционер', 'nokia'],
    }, 
);

my @suggest_stat_data = (
    {
        srcPhrases => ['кондиционер','nokia'],
        get_stat => 1,
    }, 
    {
        geo => "1,-213,2",
        srcPhrases => ['кондиционер', 'nokia'],
        get_stat => 1,
    }, 
);

my $pure_suggest_res = {
    is_something_after => re('.*'),
    is_something_before => re('.*'),
    phrases => array_each(re('^[\w ]+$')),
};

my $suggest_stat_res = {
    is_something_after => re('.*'),
    is_something_before => re('.*'),
    phrases => array_each(re('^[\w ]+$')),
    stat => superhashof({}),
};

my @tests = (
    {
        name => 'pure suggest',
        read_only => 1,
        url => $url, 
        method => 'GET',
        data => \@suggest_data,
        preprocess => sub { return {%{$_[0]}, srcPhrases => join ",", @{$_[0]->{srcPhrases}||[]}}; },
        check_num => 3, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            cmp_deeply($v, $pure_suggest_res, "$name: structure");
        },
    },
    {
        name => 'suggest + stat',
        read_only => 1,
        url => $url, 
        method => 'GET',
        data => \@suggest_stat_data,
        preprocess => sub { return {%{$_[0]}, srcPhrases => join ",", @{$_[0]->{srcPhrases}||[]}}; },
        check_num => 4, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            cmp_deeply($v, $suggest_stat_res, "$name: structure");
            is(scalar keys %{$v->{stat}}, scalar @{$data->{srcPhrases}}, "$name: stat for all phrases");
        },
    },
);

run_tests(\@tests);

