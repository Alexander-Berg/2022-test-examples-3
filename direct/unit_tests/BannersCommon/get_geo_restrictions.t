#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use Test::Deep;

use Models::Banner;
use Yandex::DBUnitTest qw/:all/;
use Settings;

use utf8;

copy_table(PPCDICT, 'ppc_properties');

my @tests = (
    {
        params => ['Добро пожаловать в Википедию', '225', tree => "ru"],
        result => {}
    },
    {
        params => ['Добро пожаловать в Википедию', '977', tree => "ru"],
        result => {}
    },
    {
        params => ['Добро пожаловать в Википедию', '977', tree => "ua"],
        result => {}
    },
    {
        params => ['Ласкаво просимо до Вікіпедії', '187', tree => "ua"],
        result => {}
    },
    {
        params => ['Ласкаво просимо до Вікіпедії', '225', tree => "ua"],
        result => {error => {
            for_banner => 'Язык объявлений должен соответствовать региону их показа. Измените настройки геотаргетинга на Украину или измените текст объявления.',
            for_group => 'Язык объявлений должен соответствовать региону их показа. Измените настройки геотаргетинга на Украину, поскольку часть текстов в группе написаны на украинском языке (%s).'
            },
            lang => 'uk',
        }
    },
    {
        params => ['Ласкаво просимо до Вікіпедії', '225', tree => "ua", for_banner_only => 1],
        result => {error => 'Язык объявлений должен соответствовать региону их показа. Измените настройки геотаргетинга на Украину или измените текст объявления.',
                   lang => 'uk',
        }
    },
);

for my $test (@tests) {
    my ($text, $geo, %opt) = @{$test->{params}};
    cmp_deeply(Models::Banner::get_geo_restrictions($text, $geo, %opt), $test->{result});
}

my @tests_warnings = (
    {
        params => ['Добро пожаловать в Википедию', '977', tree => "ru"],
        result => {}
    },
    {
        params => ['Добро пожаловать в Википедию', '225', tree => "ru"],
        result => {}
    },
    {
        params => ['Wikipedia', '225', tree => "ru"],
        result => {warning => "Если для показов объявления выбран регион Россия, то текст объявления должен быть на русском языке или продублирован на русском языке."}
    },
    {
        params => ['Wikipedia', '977', tree => "ru"],
        result => {warning => "Если для показов объявления выбран регион Россия, то текст объявления должен быть на русском языке или продублирован на русском языке."}
    },
    {
        params => ['Wikipedia', '187', tree => "ru"],
        result => {}
    },
    # транслокальное украинское дерево (поведение такое же)
    {
        params => ['Добро пожаловать в Википедию', '977', tree => "ua"],
        result => {}
    },
    {
        params => ['Добро пожаловать в Википедию', '225', tree => "ua"],
        result => {}
    },
    {
        params => ['Wikipedia', '225', tree => "ua"],
        result => {warning => "Если для показов объявления выбран регион Россия, то текст объявления должен быть на русском языке или продублирован на русском языке."}
    },
    {
        params => ['Wikipedia', '977', tree => "ua"],
        result => {warning => "Если для показов объявления выбран регион Россия, то текст объявления должен быть на русском языке или продублирован на русском языке."}
    },
    {
        params => ['Wikipedia', '187', tree => "ua"],
        result => {}
    },
);

{
    # udnef is equal en language
    no warnings 'redefine';
    local *Lang::Guess::call_external_queryrec = sub { return undef; };
    for my $test (@tests_warnings) {
        my ($text, $geo, %opt) = @{$test->{params}};
        cmp_deeply(Models::Banner::get_geo_warnings($text, $geo, %opt), $test->{result});
    }
}

done_testing();
