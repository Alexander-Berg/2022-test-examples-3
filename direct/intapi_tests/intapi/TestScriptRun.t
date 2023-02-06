#!/usr/bin/perl

# $Id $

use strict;
use warnings;

use Readonly;
use Test::Intapi;
use Test::More;

use JSON;
use utf8;
use open ':std' => ':utf8';

Readonly my $SECRET => 'b3uPkvVcVQGdGKpu';

my @TESTS_CONCISE = (
    {
        params    => { secret => $SECRET, script => 'ppcCampAutoPrice.pl', cmdline => '--shard-id 1 --cid 5194568' },
        expect_ok => 1,
        result => { stdout => '', stderr => '',  exit_code => 0 },
    },
    {
        params    => { secret => $SECRET, script => 'bogus.pl' },
        expect_ok => 0,
        error_re  => qr/is not in the whitelist/,
    },
    {
        params    => { secret => $SECRET, script => ';' },
        expect_ok => 0,
        error_re  => qr/is not in the whitelist/,
    },
    {
        params    => { script => 'ppcCampAutoPrice.pl' },
        expect_ok => 0,
        error_re  => qr/invalid secret/i,
    },
);

my @tests;
foreach my $testinfo (@TESTS_CONCISE) {
    my $get_params = {
        method => 'run_script',
        params => encode_json( $testinfo->{'params'} ),
    };

    push @tests, {
        name       => 'TestScriptRun',
        read_only  => 0,
        url        => base_url() . '/secret-jsonrpc/TestScriptRun',
        method     => 'GET',
        data       => [ $get_params ],
        check_num => 4,
        check     => sub {
            my ( $data, $resp, $name ) = @_;

            my $script = $testinfo->{'params'}->{'script'};

            my $display  = "run_script($script)";
            my $status   = $resp->status_line;
            my $skip_msg = "$display response tests useless: "
                . "invalid status $status";

            my $result;

        SKIP: {
                if ( !$resp->is_success ) {
                    skip $skip_msg, 4;
                }

                lives_ok { $result = decode_json( $resp->content ); } "$display: decode json";

                is( ref $result, 'HASH', "$display: RPC result is a hash" );

                if ( $testinfo->{'expect_ok'} ) {
                    ok( exists $result->{result}, "$display: returned a result" );
                    cmp_deeply($result->{result}, $testinfo->{result}, "$display: result matches what is expected");
                } else {
                    ok( exists $result->{error}, "$display: returned an error" );
                    like( $result->{error}->{message}, $testinfo->{'error_re'},
                        "$display: error matches what is expected" );
                }
            }
        },
    };
}

push @tests, {
    name => 'TestScriptRun',
    read_only  => 1,
    url        => base_url() . '/secret-jsonrpc/TestScriptRun',
    method     => 'GET',
    data       => [ { method => 'get_whitelist' } ],
    check_num => 2,
    check => sub {
        my ($data, $resp, $name) = @_;
        my $json;
        lives_ok {$json = JSON->new()->utf8(1)->allow_nonref(1)->decode($resp->content)} "$name: decode JSON";
        cmp_deeply($json,
                   {
                        id => ignore(),
                        jsonrpc => ignore(),
                        result => array_each(ignore()),
                   },
                   "$name: good answer"
                   );
    },
};

run_tests( \@tests );
