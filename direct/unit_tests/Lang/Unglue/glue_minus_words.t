#!/usr/bin/perl
use warnings;
use strict;
use Yandex::Test::UTF8Builder;
use Test::More tests => 17;

BEGIN { use_ok('Lang::Unglue', 'glue_minus_words'); }

use utf8;
use open ':std' => ':utf8';

sub glue {
    my $phrases = shift;
    return join ", ", map {$_->{phrase}} glue_minus_words($phrases);
}

is (glue([{phrase =>"молоко белое -парное"}, {phrase =>"молоко белое -коровье"}]), "молоко белое -парное -коровье", 'Simple glue');
is (glue([{phrase =>"молоко белое"}, {phrase =>"молоко белое -коровье"}]), "молоко белое -коровье", 'Glue phrases WITH and WITHOUT minus words');
is (glue([{phrase =>"молоко   белое    -парное"}, {phrase =>"молоко белое -коровье"}]), "молоко белое -парное -коровье", 'Spaces between words');
is (glue([{phrase =>"молоко белое -парное -коровье"}, {phrase =>"молоко белое -пастеризованное -топленое"}]), "молоко белое -коровье -парное -пастеризованное -топленое", 'Simple glue with repeated minus words');
is (glue([{phrase =>"молоко белое -парное -коровье"}, {phrase =>"молоко белое -коровье -пастеризованное"}]), "молоко белое -коровье -парное -пастеризованное", 'Simple glue with repeated minus words');
is (glue([{phrase =>"[Москва-Рим]"}, {phrase =>"[Рим-Москва]"}]), "[Москва-Рим], [Рим-Москва]", 'склейка фраз с оператором фиксированного порядка слов; порядок слов разный');
is (glue([{phrase =>"[Москва-Рим]"}, {phrase =>"[Москва-Рим]"}]), "[Москва-Рим]", 'склейка фраз с оператором фиксированного порядка слов; порядок слов одинаковый');

# При склеивании слов по возможности минус-слова от новых фраз приклеиваются к старым фразам.
is_deeply ([glue_minus_words([{phrase =>"молоко белое -парное", phrase_old=>"text"}, {phrase =>"молоко белое -коровье", is_new=>1}])], 
                            [{phrase => "молоко белое -парное -коровье", phrase_old=>"text"}], 'Glue old and new phrases');

is_deeply ([glue_minus_words([{phrase =>"молоко белое -парное", is_new=>1}, {phrase =>"молоко белое -коровье", phrase_old=>"text"}])], 
                            [{phrase => "молоко белое -парное -коровье", phrase_old=>"text"}], 'Glue old and new phrases');

is_deeply ([glue_minus_words([{phrase =>"молоко белое -парное", is_new=>1}, {phrase =>"молоко белое -коровье", is_new=>1}])], 
                            [{phrase => "молоко белое -парное -коровье", is_new=>1}], 'Glue two new phrases');

is_deeply ([glue_minus_words([{phrase =>"молоко белое -парное", phrase_old=>"text1"}, {phrase =>"молоко белое -коровье", phrase_old=>"text2"}])], 
                            [{phrase => "молоко белое -парное -коровье", phrase_old=>"text1"}], 'Glue two old phrases');

is_deeply ([glue_minus_words([{phrase =>"молоко белое -парное", phrase_old=>"text1"}, {phrase =>"молоко белое -коровье", phrase_old=>"text2"}, {phrase =>"молоко белое -топленое", phrase_old=>"text3"}])], 
                            [{phrase => "молоко белое -парное -коровье -топленое", phrase_old=>"text1"}], 'Glue three old phrases');

is_deeply ([glue_minus_words([{phrase =>"молоко белое -парное", is_new=>1}, {phrase =>"молоко белое -коровье", phrase_old=>"text2"}, {phrase =>"молоко белое -топленое", phrase_old=>"text3"}])], 
                            [{phrase => "молоко белое -парное -коровье -топленое", phrase_old=>"text2"}], 'Glue three phrases (new and old)');

# Процесс взятие в кавычки плохих цифровых фраз нам не вредит при склейке минус-слов
is_deeply (
    [glue_minus_words([{phrase=> "185 70 -купить -шина"}])],
    [{phrase=> "185 70 -купить -шина"}]
);
is_deeply (
    [glue_minus_words([{phrase=> "185 70 -купить -шина"}, {phrase=> "185 70 -продать -диск"}])],
    [{phrase=> "185 70 -купить -шина -диск -продать"}]
);
is_deeply (
    [glue_minus_words([{phrase=> "185 70"}, {phrase=> "185 70 -купить -шина"}])],
    [{phrase=> '"185 70"'}, {phrase=> "185 70 -купить -шина"}]
);

1;
