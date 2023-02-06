#!/usr/bin/perl

=head1 DESCRIPTION

    Тест проверяет Currency::Texts на наличие констант для всех валют, поддерживаемых в Currencies.

=cut

use strict;
use warnings;

use Test::More;
use Test::Deep;

use Currencies;

use Yandex::Test::UTF8Builder;
use utf8;

use Currency::Texts;

Test::More::plan(tests => scalar(keys %Currency::Texts::_CURRENCY_TEXTS_DESCRIPTION));

my @currencies_should_be = keys %Currencies::_CURRENCY_DESCRIPTION;
while (my($text_name, $text_data) = each %Currency::Texts::_CURRENCY_TEXTS_DESCRIPTION) {
    my @currencies_for_text = keys %$text_data;
    cmp_bag(\@currencies_for_text, \@currencies_should_be, "валюты для текста $text_name");
}
