#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Path::Tiny qw/path/;
use JSON qw/decode_json/;
use Encode;

BEGIN {
    use_ok('Direct::Organizations');
}

subtest 'Parse json response' => sub {
    my $data = path(path($0)->dirname)->child('resources/organizations_1.json')->slurp;
    my $json = decode_json(Encode::encode_utf8($data));
    my $result = Direct::Organizations::to_hash($json->{result}[0]);
    is($result->{companyName}, 'Контрабас');
    is($result->{address}, 'Дегтярск');
    is($result->{phone}, '+7 (922) 225-53-67');
    is($result->{isOnline}, 'true');
};

done_testing;
