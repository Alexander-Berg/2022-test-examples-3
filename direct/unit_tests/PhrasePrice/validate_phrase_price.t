#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 15;
use Test::Exception;

use Currencies;

BEGIN { use_ok( 'PhrasePrice', 'validate_phrase_price' ); }

use Yandex::Test::UTF8Builder;
use utf8;

is( validate_phrase_price('1.23', 'YND_FIXED'), undef, 'нормальное значение в у.е.' );
is( validate_phrase_price('1.23', 'RUB'), undef, 'нормальное значение в валюте' );
is( validate_phrase_price('1,23', 'RUB'), undef, 'нормальное значение с запятой в качестве десятичного разделителя' );
isnt( validate_phrase_price('1,23', 'RUB', dont_support_comma => 1), undef, 'нормальное значение с запятой в качестве десятичного разделителя + dont_replace_comma => 1' );
is( validate_phrase_price('123', 'RUB'), undef, 'нормальное целое значение' );

# граничные значения допустимы
is( validate_phrase_price(get_currency_constant('RUB', 'MIN_PRICE'), 'RUB'), undef, 'минимальная ставка в валюте' );
is( validate_phrase_price(get_currency_constant('RUB', 'MAX_PRICE'), 'RUB'), undef, 'максимальная ставка в валюте' );

isnt( validate_phrase_price('0.2', 'RUB'), undef, 'значение меньше минимального для валюты' );
isnt( validate_phrase_price('50000', 'RUB'), undef, 'значение больше максимального для валюты' );

isnt( validate_phrase_price(undef, 'RUB'), undef, 'цена не определена' );
isnt( validate_phrase_price('', 'RUB'), undef, 'пустая цена' );
isnt( validate_phrase_price('12xxx56yyy34', 'RUB'), undef, 'цена -- не число' );

dies_ok( sub { validate_phrase_price('1.23') }, 'умирает без указания валюты' );
dies_ok( sub { validate_phrase_price('1.23', 'XYZ') }, 'умирает при указания неверной валюты' );
