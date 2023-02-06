#!/usr/bin/perl

=pod
    $Id: validate_key_phrase.t 17260 2010-09-20 15:24:50Z skaurus $
=cut

use warnings;
use strict;
use Test::More;

use PhraseText;

use utf8;
use open ':std' => ':utf8';


*vkp = \&PhraseText::validate_key_phrase;

# real failures
is_deeply(vkp("одежда оптом -опт"), ['опт'], 'bad minus word');
is_deeply(vkp("каркасные дома -дом"), ['дом'], 'bad minus word');

# cases from qa
is_deeply(vkp("кухня -кухонь"), ['кухонь'], 'bad minus word');

# Тестовые фразы, придуманные программистами
is_deeply(vkp("белый -!белые -!белый -!белая"), [], 'good minus word');
is_deeply(vkp("!белый -!белый"), ['!белый'], 'bad minus word');
is_deeply(vkp("!белый -белый"), ['белый'], 'bad minus word');
is_deeply(vkp("!белый -белые"), ['белые'], 'bad minus word');
is_deeply(vkp("белые -!белый"), [], 'good minus word');
is_deeply(vkp("белый -белые"), ['белые'], 'bad minus word');
is_deeply(vkp("белые -!белые"), [], 'good minus word');
is_deeply(vkp("!белые -!белый"), [], 'good minus word');
is_deeply(vkp("в -в"), [], 'good minus word');
is_deeply(vkp("каркасные дома -дом -домам -домов -домом -домик"), [qw/дом домам домов домом/], 'bad minus word');

# Тестовые фразы, придуманные Настей Балакиной, которой этот файл обязан жизнью
is_deeply(vkp("белая -!белый"), [], 'good minus word');
is_deeply(vkp("деревянный дом -!деревянное"), [], 'good minus word');
is_deeply(vkp("кий -!киев"), [], 'good minus word');
is_deeply(vkp("кий -киев"), ['киев'], 'bad minus word');
is_deeply(vkp("белая -!белая"), [], 'good minus word');
is_deeply(vkp("!белая -белый"), ['белый'], 'bad minus word');
is_deeply(vkp("!белая -!белый"), [], 'good minus word');
is_deeply(vkp("киев -кий"), [], 'good minus word');


is_deeply(vkp("для девочек -!длинные -+для -+своими -белье -зимнее -купить -летние -сарафан"), []);
is_deeply(vkp("+для девочек -!длинные -+для -+своими -белье -зимнее -купить -летние -сарафан"), ["+для"]);

# for stop words operators ! and + are equal
is_deeply(vkp("одежда +для девочек -!белье -!длинные -!платье -!для -+своими -белье -зимнее -купить -летние -сарафан"), ["!для"]);
is_deeply(vkp("одежда !для девочек -!белье -!длинные -!платье -+для -+своими -белье -зимнее -купить -летние -сарафан"), ["+для"]);

is_deeply(vkp("!летние платья +в пол -!длинные -+для -+своими -купить -!летние"), ["!летние"]);
is_deeply(vkp("!летние платья +в пол -!длинные -+для -+своими -купить -летний"), ["летний"]);
is_deeply(vkp("одежда +для девочек -!белье -!длинные -!платье -+для"), ["+для"]);
is_deeply(vkp("окно", ["!окна"]), []);
is_deeply(vkp("!окна", ["окно"]), ["окно"]);


done_testing();
