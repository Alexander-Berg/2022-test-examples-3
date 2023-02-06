#!/usr/bin/perl
use strict;
use warnings;

use Test::Intapi;
use Test::More;

use JSON;
use utf8;
use open ':std' => ':utf8';

my %test_args = (
    'get_users' => [
        {},
        { 'easy' => 1 },
        { 'stat_start' => '2013-08-01', 'stat_end' => '2013-08-02' },

        { camps_count    => 10 },
        { currency       => 'RUB' },
        { with_discount  => 1 },
        { with_overdraft => 1 },
    ],

    'get_campaigns' => [
        {},

        { 'phrases_count' => 10 },

        { 'status' => 'moderate' },
        { 'status' => 'active' },
        { 'status' => 'inactive' },

        { 'metrika_goals'   => 1 },
        { 'nonzero_balance' => 1 },
    ],

    'get_banners' => [
        {},

        { 'status' => 'active' },
        { 'status' => 'stopped' },
        { 'status' => 'moderate_rejected' },
        { 'status' => 'archived' },

        { 'with_vcard'       => 1 },
        { 'with_href'        => 1 },
        { 'with_sitelinks'   => 1 },
        { 'with_image'       => 1 },
        { 'with_retargeting' => 1 },
        { 'lowctr_disabled'  => 1 },
    ],
);

my @test_data;
foreach my $method ( sort keys %test_args ) {
    my $params_opts = $test_args{$method};
    my @params_opts_json = map { encode_json($_) } @$params_opts;

    foreach my $params ( sort @params_opts_json ) {
        push @test_data, [ $method, $params ];
    }
}

my @tests = (
    {
        name => 'TestDataGenerator',
        read_only => 1,
        url => base_url() . '/secret-jsonrpc/TestDataGenerator',
        method => 'GET',
        data => \@test_data,
        preprocess => sub {
        	my ($data) = @_;
            my ( $method, $params ) = @$data;
        	return {
                'method' => $method,
                'params' => $params,
            };
    	},
        check_num => 3,
        check => sub {
            my ( $data, $resp, $name ) = @_;

            my ( $method, $params ) = @$data;

            my $display  = "$method($params)";
            my $status   = $resp->status_line;
            my $skip_msg = "$display response tests useless: " .
                "invalid status $status";

            my $result;

            SKIP: {
                skip $skip_msg, 3 unless $resp->is_success;
                lives_ok {
                    $result = decode_json( $resp->content )
                } "$display: decode json";

                is( ref $result, 'HASH',
                    "$display: RPC result is a hash" );

                is( ref $result->{'result'}, 'ARRAY',
                    "$display: call result is an array" );
            };
        },
    },
);

$Test::Intapi::lwp_opt->{'timeout'} = 60;
run_tests(\@tests);
