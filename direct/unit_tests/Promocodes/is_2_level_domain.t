#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Promocodes;
use Settings;
use Encode;


my @tests = (
    ["www.yandex.ru", 1],
    ["direct.yandex.ru", 1],
    ["adv.spb.ru", 1],
    ["www.adv.spb.ru", 1],
    ["lenta.yandex", 1],
    ["сайт.рф", 1],
    ["server.example.com", 0],
    ["1234.server.example.com", 0],
    ["www.1234.server.example.com", 0],
    ["subsite.сайт.рф", 0],
    ["подсайт.сайт.рф", 0],
);


Test::More::plan(tests => scalar (@tests));


# Проверка, что метод возвращает ожидаемое значение
foreach my $test (@tests) {
	my $res = Promocodes::_is_2_level_domain($test->[0]);
    my $dom_to_print = Encode::encode_utf8($test->[0]);
    ok($res == $test->[1], "_is_2_level_domain('$dom_to_print') should return $test->[1]");
}

1;
