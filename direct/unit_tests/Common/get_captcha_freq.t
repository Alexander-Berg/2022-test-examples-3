#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More tests => 14;

BEGIN {use_ok('Common', 'get_captcha_freq');};

use utf8;

my @test_data = (
    # [{<аргументы get_captcha_freq>}, <ожидаемое возвращаемое значение (раз в сколько запросов показывать каптчу>, <название теста>]

    [{karma =>   0, captcha_freq =>  0, is_autobanned => 'No'},   0, 'не показываем каптчу хорошим пользователям'],
    [{karma =>   0, captcha_freq => 69, is_autobanned => 'No'},  69, 'фиксированная в настройках пользователя вероятность показа каптчи'],

    # проверяем вколотость/выколотость порогов для кармы (см. @Settings::KARMA_CONST_LIMITS)
    [{karma =>  50, captcha_freq =>  0, is_autobanned => 'No'},   0, 'карма 50 => не показываем каптчу'],
    [{karma =>  51, captcha_freq =>  0, is_autobanned => 'No'}, 100, 'карма 50 => показываем каптчу раз в 100 запросов'],
    [{karma =>  85, captcha_freq =>  0, is_autobanned => 'No'}, 100, 'карма 85 => показываем каптчу раз в 100 запросов'],
    [{karma =>  86, captcha_freq =>  0, is_autobanned => 'No'},  50, 'карма 86 => показываем каптчу раз в 50 запросов'],
    [{karma =>  99, captcha_freq =>  0, is_autobanned => 'No'},  50, 'карма 99 => показываем каптчу раз в 50 запросов'],
    [{karma => 100, captcha_freq =>  0, is_autobanned => 'No'},  10, 'карма 100 => показываем каптчу на каждый запрос'],

    # забаненым пользователям показываем каптчу не реже раза в 10 запросов
    [{karma =>   0, captcha_freq =>  0, is_autobanned => 'Yes'},  10, 'забанненый пользователь с хорошей кармой и без фиксации вероятности показа каптчи'],
    [{karma =>   0, captcha_freq => 69, is_autobanned => 'Yes'},  10, 'забанненый пользователь с хорошей кармой и фиксацией вероятности показа каптчи на уровне 1/69'],
    [{karma => 100, captcha_freq =>  0, is_autobanned => 'Yes'},  10, 'забанненый пользователь с плохой кармой'],

    # в настройках пользователя суперы могу принудительно перекрыть карму, приходящую из паспорта
    [{karma =>  -1, captcha_freq =>  0, is_autobanned => 'No'},   0, 'не показываем каптчу "принудительно хорошим" пользователям'],
    [{karma => 101, captcha_freq =>  0, is_autobanned => 'No'},   10, 'показываем каптчу на каждый 10-ый запрос "принудительно плохим" пользователям'],
);

for my $test(@test_data) {
    is(get_captcha_freq($test->[0]), $test->[1], $test->[2]);
}
