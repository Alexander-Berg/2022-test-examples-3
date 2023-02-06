#!/usr/bin/perl

=pod

    Проверяет функцию, которая определяет язык(регион) Гарантийных Писем

=cut

use warnings;
use strict;

use utf8;

use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use GuaranteeLetter;

my %tests = (
    'ru' => [qw/225 167 168 167 169 170 171 208 209/],
    'ua' => [qw/187/],
    'tr' => [qw/983/],
    'us' => [qw/127 179 203/],
    'by' => [qw/149/],
    'kz' => [qw/159/],
);

init_test_dataset(&get_test_dataset);

# Для "регионов" (шаблонов) задана существующая кодировка
while (my ($lang, $region_ids) = each %tests) {
    foreach (@$region_ids) {
        is(GuaranteeLetter::get_letter_region($_), $lang, "valid letter lang for region $_");
    }
}


sub get_test_dataset { +{
    country_currencies => {original_db => PPCDICT,
        rows => [{region_id => 127, firm_id => 7},
                 {region_id => 149, firm_id => 1},
                 {region_id => 149, firm_id => 7},
                 {region_id => 159, firm_id => 1},
                 {region_id => 159, firm_id => 3},
                 {region_id => 167, firm_id => 1},
                 {region_id => 167, firm_id => 7},
                 {region_id => 168, firm_id => 1},
                 {region_id => 168, firm_id => 7},
                 {region_id => 169, firm_id => 1},
                 {region_id => 169, firm_id => 7},
                 {region_id => 170, firm_id => 1},
                 {region_id => 170, firm_id => 7},
                 {region_id => 171, firm_id => 1},
                 {region_id => 171, firm_id => 7},
                 {region_id => 179, firm_id => 7},
                 {region_id => 187, firm_id => 2},
                 {region_id => 203, firm_id => 7},
                 {region_id => 208, firm_id => 1},
                 {region_id => 208, firm_id => 7},
                 {region_id => 209, firm_id => 1},
                 {region_id => 209, firm_id => 7},
                 {region_id => 225, firm_id => 1},
                 {region_id => 983, firm_id => 8},
        ]}
    }
}


done_testing;
