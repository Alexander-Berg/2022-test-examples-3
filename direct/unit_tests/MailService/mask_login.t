#!/usr/bin/env perl

use Direct::Modern;

use Test::More;

use Yandex::Test::UTF8Builder;

use MailService ();

my @tests = (
    ['wwww', ''],
    ['qq', ''],
    ['qeeeq', 'qe***q'],
    ['lansdf-fgdgh', 'la***h'],
    ['dfhggfdh.restdty.4', 'df***4'],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    my ($login, $expected_result) = @$test;
    my $result = MailService::_mask_login($login);
    is($result, $expected_result);
}
