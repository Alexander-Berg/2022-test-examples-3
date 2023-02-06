#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More tests => 6;

BEGIN { use_ok( 'TTTools' ); }

use utf8;

is(TTTools::format_int(0), '0', 'форматирование ноля');
is(TTTools::format_int(undef), undef, 'форматирование undef');
is(TTTools::format_int(0.9999999), '0', 'отбрасывание дробной части');
# \x{00A0} == неразрывный пробел
is( TTTools::format_int(1234123456789.123456), "1\x{00A0}234\x{00A0}123\x{00A0}456\x{00A0}789", 'разделение разрядов на группы с разделителем по умолчанию');
is( TTTools::format_int(1234123456789.123456, {separator => '_X_'}), '1_X_234_X_123_X_456_X_789', 'разделение разрядов на группы с произвольным разделителем');
