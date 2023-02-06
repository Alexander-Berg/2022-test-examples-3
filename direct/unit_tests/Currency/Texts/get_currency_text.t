#!/usr/bin/perl

=head1 DESCRIPTION

    Простейший тест Currency::Texts::get_currency_text
    Фактически, проверяет, что функция не всегда падает :)

=cut

use strict;
use warnings;


use Test::More tests => 2;
use Yandex::I18n;
use Settings; # init_i18n требует пути для файлов с переводами

use Yandex::Test::UTF8Builder;
use utf8;

BEGIN {use_ok('Currency::Texts', 'get_currency_text');};

Yandex::I18n::init_i18n('ru');
is(get_currency_text('RUB', 'pay_in'), 'Оплатить в российских рублях', 'значение константы pay_in в рублях на русском языке');
