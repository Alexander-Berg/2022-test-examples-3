#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use Yandex::Test::UTF8Builder;
use ADVQ6;

my @test_data = (
    ["слово -минус", "слово"],
    ["[слово еще] -минус -два", "слово еще"],
    ["слово !для   яда -минус", "слово для яда"],
    ["слово !для яда -(минус фраза)", "слово для яда"],
    ["[слово еще] -минус -(минус фраза)", "слово еще"],
    [" [слово еще] -минус -([минус фраза])", "слово еще"],
    ["+вода [слово еще] -(!минус +пять) -([минус фраза])", "вода слово еще"],
    ['"phrase +phrase2 !phrase3"', 'phrase phrase2 phrase3'],
    ['phrase phrase2 -minus_phrase', 'phrase phrase2'],
    ['[ticket Moscow Samara]', 'ticket Moscow Samara'],
    ['ноутбук -!тошиба', 'ноутбук']
);

Test::More::plan(tests => scalar(@test_data));

for my $data (@test_data) {
    my ($src, $res) = @$data;

    is(ADVQ6::_cut_opers($src), $res);
}
