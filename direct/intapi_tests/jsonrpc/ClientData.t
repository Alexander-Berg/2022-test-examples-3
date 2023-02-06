#!/usr/bin/perl

use Direct::Modern;

use Test::Intapi;

use JSON;

my @tests = (
    {
        name => 'check valid format',
        read_only => 1,
        url => '/jsonrpc/ClientData',
        method => 'POST',
        data => [
            {
                client_ids => [15155],
                mark_chief_reps => 1,
            },
        ],
        preprocess => sub {
            return to_json {
                method => 'get_by_ClientID',
                params => $_[0],
            };
        },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            my $result = $v->{result};
            is(ref $result, "HASH", "$name: result is a hash");
        },
    },
);

run_tests(\@tests);
