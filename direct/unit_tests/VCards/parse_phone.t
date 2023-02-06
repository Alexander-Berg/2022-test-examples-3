#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use VCards;

use utf8;
use open ':std' => ':utf8';

# Создаем псевдоним для функции, для удобства использовани.
# $_[0] вместо @_ обусловлено тем, что у функции задан прототип ($)
# При использовании @_ parse_phone трактует то, что ей передают в скалаярном контексте, то есть получает размер массива, 1.
*pph = sub { parse_phone($_[0]); };

# Частичный тест. Проверяем что функция правильно выделила номер телефона из строки
is(parse_phone('+7#812#2128506#340')->{phone}, '2128506', 'phone correct');

# Проверяем, что функция правильно разобрала строку на хеш, сравнивая два хеша: тот, что вернула функция, и эталонный
ok(eq_hash(parse_phone('+7#812#2128506#340'),
                                              { country_code => '+7',
                                                city_code    => '812',
                                                phone        => '2128506',
                                                ext          => '340'}),
                                                                           'hash correct');

# Проверяем, что функция возвращает пустой хеш при передаче ей неопределенного аргумента.
ok(eq_hash(parse_phone(undef), {}), 'empty hash correct');

# Проверяем функцию на неполных строках: 1 элемент для выделения
ok(eq_hash(pph('+1'),
                      { country_code => '+1',
                        city_code    => undef,
                        phone        => undef,
                        ext          => undef}),
                                                 'partial hash correct (country_code)');

# Проверяем функцию на неполных строках: 2 элемента для выделения
ok(eq_hash(pph('+1#614'),
                          { country_code => '+1',
                            city_code    => '614',
                            phone        => undef,
                            ext          => undef}),
                                                     'partial hash correct (country_code, city_code)');

# Проверяем функцию на неполных строках: 3 элемента для выделения
ok(eq_hash(pph('+1#614#232322232'),
                                    { country_code => '+1',
                                      city_code    => '614',
                                      phone        => '232322232',
                                      ext          => undef}),
                                                               'partial hash correct (country_code, city_code, phone)');

done_testing();
