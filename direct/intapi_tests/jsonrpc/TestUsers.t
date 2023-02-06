#!/usr/bin/perl

# $Id $

use strict;
use warnings;

our $VERSION = 1.0;

use Readonly;
use Test::Intapi;
use Test::More;

use JSON;
use utf8;
use open ':std' => ':utf8';

Readonly my $RETVAL_TEST_COUNT => 3;

my @tests = (
    {
        name       => 'TestUsers',
        read_only  => 1,
        url        => base_url() . '/jsonrpc/TestUsers',
        method     => 'GET',
        data       => ['{}'],
        preprocess => sub {
            my ($data) = @_;
            my $params = $data;
            return {
                method => 'get_all',
                params => $params,
            };
        },
        check_num => $RETVAL_TEST_COUNT,
        check     => sub {
            my ( $data, $resp, $name ) = @_;

            my $params = $data;

            my $display  = "get_all($params)";
            my $status   = $resp->status_line;
            my $skip_msg = "$display response tests useless: "
                . "invalid status $status";

            my $result;

        SKIP: {
                if ( !$resp->is_success ) {
                    skip $skip_msg, $RETVAL_TEST_COUNT;
                }

                lives_ok {
                    $result = decode_json( $resp->content );
                }
                "$display: decode json";

                is( ref $result, 'HASH', "$display: RPC result is a hash" );

                is( ref $result->{result},
                    'ARRAY', "$display: call result is an array" );
            }
        },
    },
);

run_tests( \@tests );
