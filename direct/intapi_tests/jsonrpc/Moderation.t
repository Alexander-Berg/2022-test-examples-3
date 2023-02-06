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

my %input_data = (
    process_magic_queue => [
        [{ cid => 263, cmd => 'updateFlags', data => [{ bid => 350, flags => { age => "age18" }}], id => 1660 }],
    ],

    process_mod_result => [
        [
            {  bid => 350, catalogia_ids => [ 200035116 ], cid => 263, errors => [], export_version => "f0a077ca-c70c-11e3-a9f9-001851247f16", flags => { age => "age18" },
               force => undef, id => 350, object_id => 262620917, pid => 350, statusModerate => "Yes", statusPostModerate => "No", statusSync => "Sending", type => "banner" },
            {  bid => 350, catalogia_ids => [ 200035116 ], cid => 263, errors => [], export_version => "f0a4390c-c70c-11e3-a9f9-001851247f16", flags => { age => "age18" },
               force => undef, id => 350, object_id => 179085845, pid => 350, statusModerate => "Yes", statusPostModerate => "No", statusSync => "Sending", type => "phrases" },
            {  cid => 263, id => 263, statusModerate => "New", statusPostModerate => "No", statusServicing => "Yes", statusSync => "New", type => "campaign", uid => 6138950 }
        ],
    ],

);

my @tests;

foreach my $method ( sort keys %input_data ) {
    foreach my $params ( @{ $input_data{$method} } ) {
        push @tests, {
            name       => 'Moderation',
            url        => base_url() . '/jsonrpc/Moderation',
            method     => 'POST',
            read_only  => 1,
            data       => [ $params ],
            preprocess => sub {
                my ($data) = @_;
                my $params = $data;
                return encode_json({
                    method => $method,
                    params => $params,
                });
            },
            check_num => $RETVAL_TEST_COUNT,
            check     => sub {
                my ( $data, $resp, $name ) = @_;

                my $params = encode_json( $data );

                my $display  = "$method($params)";
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

                    is( ref $result,           'HASH', "$display: RPC result is a hash" );
                    is( ref $result->{result}, 'ARRAY', "$display: call result is an array" );
                }
            },
        };
    }
}

run_tests( \@tests );
