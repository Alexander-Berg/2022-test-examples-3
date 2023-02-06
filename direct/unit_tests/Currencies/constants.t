#!/usr/bin/perl

=head1 

    Проверяем консистентность описаний валют: 
    для валюты должны быть указаны все необходимые константы, и не должно быть лишних.
    
    Лишние константы можем обнаружить только явные, если что-то вычисляется в get_currency_constant -- проверке не поддается.

=cut

#    $Id$

use warnings;
use strict;
use Test::More;
use Test::Exception;

use Currencies;

use utf8;
use open ':std' => ':utf8';

my $currency_count = keys %Currencies::_CURRENCY_DESCRIPTION;
my $named_fields_count = keys %Currencies::CURRENCY_FIELDS;
my $present_fields_count = map { values %$_ } values %Currencies::_CURRENCY_DESCRIPTION;

# количество тестов: для каждой валюты все обязательные поля + каждое фактическое поле
Test::More::plan(tests => 1 + $currency_count*$named_fields_count + $present_fields_count);

ok(!$Currencies::CURRENCY_DEBUG, "\$Currencies::CURRENCY_DEBUG should be false");

$Currencies::CURRENCY_DEBUG = 0;
for my $currency (keys %Currencies::_CURRENCY_DESCRIPTION){
    for my $field (keys %{$Currencies::_CURRENCY_DESCRIPTION{$currency}}){
        ok(exists $Currencies::CURRENCY_FIELDS{$field}, "extra field '$field' for $currency");
    }

    for my $const (keys %Currencies::CURRENCY_FIELDS){
        lives_ok { get_currency_constant($currency, $const) } "missed field '$const' for $currency";
    }
}

