#!/usr/bin/perl

=head2

    Проверят, что курсы у.е. есть для всех поддерживаемых валют.

=cut

use strict;
use warnings;

use Test::More tests => 2;
use Test::Deep;

use Currencies;

use Yandex::Test::UTF8Builder;
use utf8;

BEGIN {use_ok('Currency::Rate');};

my @supported_currencies = grep {$_ ne 'YND_FIXED'} keys %Currencies::_CURRENCY_DESCRIPTION;
my @conv_unit_rate_currencies = keys %{$Currency::Rate::_CURRENCY_RATE{'YND_FIXED'}};
cmp_bag(\@conv_unit_rate_currencies, \@supported_currencies, 'Для всех валют есть курс у.е.');
