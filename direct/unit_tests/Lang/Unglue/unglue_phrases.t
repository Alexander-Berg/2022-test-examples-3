#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Yandex::Test::UTF8Builder;
use Test::More;
use Settings;

BEGIN { use_ok( 'Lang::Unglue', 'unglue_phrases' ); }

use utf8;
use open ':std' => ':utf8';

sub ug {
    my ($in, $len) = @_;
    my $i = 1;
    my $block = [
        {
            bid => 1,
            Phrases => [
                map { s/^\s+|\s+$//g; {phrase=>$_, md5 => $i++} } split(/,/, $in)
                ]
        }
        ];
    unglue_phrases($block, $len);
    return join ", ", map {$_->{phrase}.$_->{phrase_unglued_suffix}} @{ $block->[0]->{Phrases} };
}

is(ug("asd, asd sdf"), "asd -sdf, asd sdf");
is(ug("бп санкт-петербург, бп"), "бп санкт-петербург, бп");
is(ug("aaa, aaa bbb ccc"), "aaa, aaa bbb ccc");

#Проверяем, как соблюдается ограничение на длину ключевых слов. 
is(ug("aaa,aaa bbb,ccc"), "aaa -bbb, aaa bbb, ccc");
is(ug("aaa,aaa bbb,ccc", 15), "aaa, aaa bbb, ccc");

is(ug("aaa,aaa bbb,aaa bbb cccc", 125), "aaa -bbb, aaa bbb -cccc, aaa bbb cccc");
is(ug("aaa,aaa bbb,aaa bbb cccc", 29), "aaa -bbb, aaa bbb, aaa bbb cccc");
is(ug("aaa,aaa bbb,aaa bbb cccc", 28), "aaa, aaa bbb, aaa bbb cccc");

# Случай со сложной нормализацией
is(ug("поставщик керамогранит, керамогранит поставщик керамогранита"), "поставщик керамогранит, керамогранит поставщик керамогранита");

# проверяем работу при наличии во фразе дефисов
is(ug("aaa bbb, aaa -bbb-ccc"), "aaa bbb, aaa -bbb-ccc");
is(ug("aaa, aaa-ccc, aaa bbb"), "aaa -bbb -ccc, aaa-ccc, aaa bbb");


# восклицательные знаки во фразах
is(ug("aaa,aaa !bbb,aaa bbb cccc"), "aaa -!bbb, aaa !bbb -cccc, aaa bbb cccc");
is(ug("aaa -!bbb,aaa !bbb,aaa bbb cccc"), "aaa -!bbb, aaa !bbb -cccc, aaa bbb cccc");

is(ug("львы в автомобиле, львы красивом автомобиле"), "львы в автомобиле -красивый, львы красивом автомобиле");
is(ug("львы !в автомобиле, львы автомобиле"), "львы !в автомобиле, львы автомобиле -!в");
is(ug("львы в автомобиле, львы в !красивом автомобиле"), "львы в автомобиле -!красивом, львы в !красивом автомобиле");

# Здесь есть погрешность
is(ug("львы в !красивом автомобиле, львы в красивом автомобиле"), "львы в !красивом автомобиле, львы в красивом автомобиле");
# правильнее, чтобы получилось: 
#is(ug("львы в !красивом автомобиле, львы в красивом автомобиле"), "львы в !красивом автомобиле, львы в красивом автомобиле -!красивом");
# но это довольно-таки мелочь.


# кавычки во фразах
is(ug(q/"aaa",aaa bbb,aaa bbb cccc/), q/"aaa", aaa bbb -cccc, aaa bbb cccc/);

# плюсы в фразах
is(ug("aaa,aaa +bbb,aaa bbb cccc"), "aaa -bbb, aaa +bbb -cccc, aaa bbb cccc"); # 

is(ug("aaa -bbb,aaa +bbb,aaa bbb cccc"), "aaa -bbb, aaa +bbb -cccc, aaa bbb cccc"); #
is(ug("aaa,aaa +bbb,aaa bbb cccc"), "aaa -bbb, aaa +bbb -cccc, aaa bbb cccc"); #

# Здесь есть погрешность
is(ug("львы +в автомобиле, львы автомобиле"), "львы +в автомобиле, львы автомобиле -!в"); 
# Правильнее, чтобы получилось: 
#is(ug("львы +в автомобиле, львы автомобиле"), "львы +в автомобиле, львы автомобиле -в"); 
# но это довольно-таки мелочь.

is(ug("львы в автомобиле, львы в +красивом автомобиле"), "львы в автомобиле -красивый, львы в +красивом автомобиле");

is(ug("aaa 1.15, aaa"), "aaa 1.15, aaa -1.15", "unglue numbers with dots");
is(ug("aaa 1.15.22, aaa"), "aaa 1.15.22, aaa", "unglue numbers with multi dots");
is(ug("aaa b.bb, aaa"), "aaa b.bb, aaa", "dont unglue words with dots");

is(ug("aaa b.bb ccc, aaa"), "aaa b.bb ccc, aaa", "dont unglue words with dots and additional words");
is(ug("aaa 1.15 ccc, aaa"), "aaa 1.15 ccc, aaa", "dont unglue numbers with dots and additional words");

is(ug('[aaa bbb] ccc, [aaa bbb]'), '[aaa bbb] ccc, [aaa bbb] -ccc', 'расклейка фраз с оператором "фиксированный порядок слов"');
is(ug('[aaa-bbb] ccc, [aaa-bbb]'), '[aaa-bbb] ccc, [aaa-bbb] -ccc', 'расклейка фраз с оператором "фиксированный порядок слов" и дефисами');
is(ug('[aaa-bbb], [bbb-aaa]'), '[aaa-bbb], [bbb-aaa]', 'расклейка фраз с оператором "фиксированный порядок слов" и дефисами, имеющих разный порядок');

is(ug('aaa bbb, [aaa bbb]'), 'aaa bbb, [aaa bbb]',
    'расклейка одинаковых фраз, одна из которых с "фиксированным порядком слов"');
is(ug('!aaa !bbb, [!aaa !bbb]'), '!aaa !bbb, [!aaa !bbb]',
    'расклейка одинаковых фраз с фиксированной формой слов, одна из которых с "фиксированным порядком слов"');
is(ug('aaa, [aaa]'), 'aaa, [aaa]',
    'расклейка одинаковых фраз из одного слова, одна из которых с "фиксированным порядком слов"');
is(ug('!aaa, [!aaa]'), '!aaa, [!aaa]',
    'расклейка одинаковых фраз из одного слова с фиксированной формой слова, одна из которых с "фиксированным порядком слов"');

done_testing;


