#!/usr/bin/perl

use Direct::Modern;

use Test::Intapi;

use JSON;

my @tests = (
    {
        name => 'check get_YND_FIXED_rates',
        read_only => 1,
        url => '/jsonrpc/CurrencyRates',
        method => 'POST',
        preprocess => sub {
            return to_json {
                method => 'get_YND_FIXED_rates',
            };
        },
        data => [{}],
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            my $result = $v->{result};
            is(ref $result, "HASH", "$name: result is a hash");
            is($result->{RUB}->{0}->{with_nds}, 30, "$name: correct RUB rate");
        },
    },
);

run_tests(\@tests);
