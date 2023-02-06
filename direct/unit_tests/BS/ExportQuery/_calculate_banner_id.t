#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use Test::Deep;
use Test::Exception;

use BS::ExportQuery qw/_calculate_banner_id/;

use Yandex::Test::UTF8Builder;
use Yandex::Log;

use Settings;


BS::ExportQuery::init(log => Yandex::Log->new( use_syslog => 0, no_log => 1 ));

# [\@args, \@expected_result]
my $EID = 123456789;
my @tests = (
    [
        [ 1, 'min EID' ], 72057594037927937,
    ],
    [
        [ 123456789, 'some EID' ], 72057594161384725,
    ],
    [
        [ 72057594037927935, 'max EID' ], 144115188075855871,
    ],
);

dies_ok ( sub { BS::ExportQuery::_calculate_banner_id(72057594037927936) }, 'вызов _calculate_banner_id со слишком большим EID умирает');

for my $test (@tests) {
    my ($args, $expected_result, $test_name) = @$test;
    my $result = BS::ExportQuery::_calculate_banner_id(@$args);
    cmp_deeply($result, $expected_result, $test_name);
}

done_testing();

