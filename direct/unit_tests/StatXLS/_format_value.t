#!/usr/bin/perl

use Direct::Modern;

use Yandex::Test::UTF8Builder;

use Test::More;

use StatXLS;

*_format_value=\&StatXLS::_format_value;

is(_format_value(undef, field => 'sum'), '-', 'прочерк вместо undef');
is(_format_value(1.2399, field => 'sum'), '1.24', 'округление');
is(_format_value(666, field => 'sum'), 666, 'нормальное число');
is(_format_value(666, field => 'ctr'), '-', 'CTR за 100%');
is(_format_value(1.2399, field => 'sum', comma_sep => 1), '1,24', 'округление');

done_testing();
