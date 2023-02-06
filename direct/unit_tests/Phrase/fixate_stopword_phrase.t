#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Exception;
use Settings;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

BEGIN { use_ok( 'Phrase' ); }

# фиксация стоп-слов - это замечательный пример преимущества test-driven development:
# можно добавлять новые фичи (или чинить баги), написав сначала тест, затем код, и даже не проверяя в браузере :)
# главное, не забывать рестартить апач

use utf8;
use open ':std' => ':utf8';

*fs = \&Phrase::fixate_stopword_phrase;

copy_table(PPCDICT, 'stopword_fixation');
do_insert_into_table(PPCDICT, 'stopword_fixation', { phrase => 'love is' });
do_insert_into_table(PPCDICT, 'stopword_fixation', { phrase => 'на все' });
do_insert_into_table(PPCDICT, 'stopword_fixation', { phrase => 'то' });

do_insert_into_table(PPCDICT, 'stopword_fixation', { phrase => '(?{ die "no quotemeta" })' });
do_insert_into_table(PPCDICT, 'stopword_fixation', { phrase => '(invalid regex' });
lives_ok { fs("whatever") } 'lives ok';

is(fs('love is -minus -words'), 'love +is -minus -words','фиксация по списку с минус-словами');

is(fs('all you need is love is'), 'all you need is love +is', 'stop word at end');
is(fs('all you need is glove is'), 'all you need is glove is', 'part of stop word');
is(fs('buy love isnt'), 'buy love isnt', 'part of stop word at end');
is(fs('на все'), '+на +все', 'two stop words as one phrase');
is(fs('на все деньги'), '+на +все деньги', 'two stop words as one phrase 2');
is(fs('+love is'), '+love +is', 'keeping existing +');
is(fs('love !is'), 'love !is', 'not fixating stopword with !');
is(fs('"love is"'), '"love is"', 'not fixating in quotes');
is(fs('[love !is]'), '[love !is]', 'not fixating in []');
is(fs('[купить love is жвачка]'), '[купить love is жвачка]', 'not fixating in [] 2');
is(fs('[!на все деньги]'), '[!на все деньги]', 'not fixating [] 3');

is(fs('buy love +is'), 'buy love +is', 'keeping existing +');
is(fs('buy glove +is'), 'buy glove +is', 'keeping existing + 2');
is(fs('buy love +isnt'), 'buy love +isnt', 'keeping existing + 3');
is(fs('+на все деньги'), '+на +все деньги', 'keeping existing + 4');

is(fs('на все деньги love is'), '+на +все деньги love +is', 'two fixating rules in one phrase');
is(fs('"покупай love is"'), '"покупай love is"', 'quotes 2');
is(fs('"love is жвачка"'), '"love is жвачка"', 'quotes 3');
is(fs('"покупай love is жвачка"'), '"покупай love is жвачка"', 'quotes 4');
is(fs('[love is]'), '[love is]', 'skip []');
is(fs('[love is] love is'), '[love is] love +is', 'skip [] and fixate');
is(fs('love is [love is]'), 'love +is [love is]', 'fixate and skip []');
is(fs('Love IS'), 'Love +IS', 'case-insensitive');

is(fs('А 100'),'+А 100','стоп-слово с числом, полное совпадение');
is(fs('слово а 100'),'слово а 100','стоп-слово с числом, неполное совпадение');
is(fs('слово аа 100'),'слово аа 100','не стоп-слово с числом');
is(fs('аа 100'),'аа 100','не стоп-слово с числом');

is(fs('100 А'),'100 +А','стоп-слово после числа, полное совпадение');
is(fs('слово 100 а'),'слово 100 а','стоп-слово с числом, неполное совпадение');
is(fs('слово 100 аа'),'слово 100 аа','не стоп-слово с числом');
is(fs('100 аа'),'100 аа','не стоп-слово с числом');

is(fs('a 100 -one -two'),'+a 100 -one -two','стоп-слово с числом, с минус-словами');
is(fs('a 100 test -one -two'),'a 100 test -one -two','стоп-слово с числом');

is(fs('100 a -one -two'),'100 +a -one -two','стоп-слово с числом, с минус-словами');
is(fs('100 a test -one -two'),'100 a test -one -two','стоп-слово с числом');

is(fs('а 100 но'),'+а 100 +но','несколько стоп-слов и числа');
is(fs('100 а а'),'100 +а +а','несколько стоп-слов и числа');

is(fs('а 50 но -минус'),'+а 50 +но -минус','несколько стоп-слов и числа, с минус словами');
is(fs('50 а но -минус -слова'),'50 +а +но -минус -слова','несколько стоп-слов и числа, с минус словами');

is(fs('а 50 но слово'),'а 50 но слово','несколько стоп-слов и числа, с не-стоп словами');

is(fs('а 50 +но'),'+а 50 +но','числа и стоп-слова с плюсом и без');
is(fs('а 50 "но"'),'+а 50 "но"','числа и стоп-слова в кавычках и без');
is(fs('а 50 [но у]'),'+а 50 [но у]','числа и стоп-слова в квадратных скобках и без');
is(fs('а 50 !но'),'+а 50 !но','числа и стоп-слова с восклицанием и без');

is(fs('a 0.5'),'+a 0.5','стоп-слова и вещественные числа');
is(fs('а-1500'), '+а-1500', 'слово и число, разделенные дефисом');
is(fs('1500-а'), '1500-+а', 'слово и число, разделенные дефисом');
is(fs('а-1500-34-18'), '+а-1500-34-18', 'слово и числа, разделенные дефисом');
is(fs('18-34-1500-а'), '18-34-1500-+а', 'слово и числа, разделенные дефисом');

is(fs('а-1500 -минус'), '+а-1500 -минус', 'слово и число, разделенные дефисом');
is(fs('а-1500 слово'), 'а-1500 слово', 'слово и число, разделенные дефисом');
is(fs('1500-а -минус'), '1500-+а -минус', 'слово и число, разделенные дефисом');
is(fs('слово а-100'), 'слово а-100','слово перед сочетанием стоп-число');
is(fs('love is-900'), 'love +is-900', 'фиксация по списку и число');

is(fs('а ты уже не'),'а ты уже не', 'фраза из только стоп-слов');

is(fs(''), '', 'пустая фраза');
is(fs('-сбербанк -альфабанк -авто -в -залог -на -под -дом -автомобиль -машину -помощь -100000'), ' -сбербанк -альфабанк -авто -в -залог -на -под -дом -автомобиль -машину -помощь -100000', 'фраза только из минус-слов');

is(fs('ts-800t'), 'ts-800t', "слово+дефис+цифра+слово");

is(fs('то авто'), "+то авто");
is(fs('!то авто'), "!то авто");
is(fs('!то авто 812'), "!то авто 812");

is(fs('авто то'), "авто +то");
is(fs('авто !то'), "авто !то");
is(fs('авто 812 !то'), "авто 812 !то");


done_testing();
