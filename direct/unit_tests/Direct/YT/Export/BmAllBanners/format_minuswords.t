#!/usr/bin/perl

use Direct::Modern;

use warnings;
use strict;
use Test::More;

use Direct::YT::Export::BmAllBanners;
use MinusWordsTools;

my @tests = (
    undef,
    "",
    "[]",
    '["привет"]',
    '["hey", "you"]',
    '["hey you"]',
    '["hey you", "привет"]',
    );

# проверяем, что реализация не разошлась
for my $case (@tests) {
    my $minus_words = MinusWordsTools::minus_words_str2array($case);
    my $etalon = MinusWordsTools::minus_words_array2str_with_brackets($minus_words);

    my $yt_result = Direct::YT::Export::BmAllBanners::format_minuswords($case);

    is($yt_result, $etalon);
}

# проверяем, что на кривых данных не падаем
is(Direct::YT::Export::BmAllBanners::format_minuswords("asdfgsd "), "");

done_testing;
