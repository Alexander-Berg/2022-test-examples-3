#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use Yandex::Test::UTF8Builder;
use ADVQ6;

my @test_data = (
    #КФ без операторов <красное платье>:
    #МФ могут содержать любые слова и операторы, исключение: нельзя использовать ключевую фразу (или слова из фразы по отдельности) без операторов в качестве минус-фразы.
    ['красное платье', ['красное'] => 'красное платье'],
    ['красное платье', ['купить платье', 'ушанка'] => 'красное платье -(купить платье) -ушанка'],
    ['красное платье', ['купить', 'платье'] => 'красное платье -купить'],

    #КФ в кавычках <"красное платье">
    #МФ запрещаются, так как показ возможен только по точному соответствию
    ['"красное платье"', ['купить платье', 'ушанка'], '"красное платье"'],
    
    #КФ содержит ! для значимого слова <!красное платье>
    #МФ может содержать любые слова и операторы, кроме ключевой фразы (или слов из фраз по отдельности) с идентичным сочетание операторов, <-!красное платье>,<-!красное>,
    #либо без операторов <-красное платье>, <-красное>
    ['!красное платье', ['!красное платье', '!красное', 'платье', 'красное', 'зеленое', 'красное яблоко'] => '!красное платье -зеленое -(красное яблоко)'],

    #КФ содержит ! или + для стоп-слова <!где купить красное платье>
    #МФ может содержать любые слова и операторы, кроме ключевой фразы (или отдельных слов из фразы) с идентичным сочетание операторов, <-!где купить красное платье>, <-!где>,
    #либо без операторов <-красное платье>
    ['!где +еще купить красное платье', ['!где', '+еще', '+еще купить красное платье', 'красное', 'зеленое', 'красное яблоко'] => '!где +еще купить красное платье -зеленое -(красное яблоко)'],

    #КФ содержит квадратные скобки <билет [из Москвы в Питер]>
    #МФ может содержать любые слова и операторы, кроме ключевой фразы с идентичным сочетание операторов, <-[из Москвы в Питер]>,
    #либо ключевой фразы (или отдельных слов из фразы) без операторов <-Москва>
    ['билет [из Москвы в Питер]', ['[из Москвы в Питер]', '+Питер', 'Москва', '[поезд в Воронеж]'] => 'билет [из Москвы в Питер] -([поезд !в Воронеж])'],
    #Если общая длинна минус-слов превышают 4096 символов - они тоже обрабатываются корректно
    ['автомойка самостоятельная', ['автомойка'] => 'автомойка самостоятельная'],
    ['автомойка самостоятельная', [ (map {"$_"x15} ('aa'..'gz')),  'автомойка' ] => 'автомойка самостоятельная '.join(' ', (map {'-'."$_"x15} ('aa'..'gz'))) ],

);

Test::More::plan(tests => scalar(@test_data));

for my $data (@test_data) {
    my ($phrase, $minus_phrases, $res) = @$data;

    is(ADVQ6::format_advq_query($phrase, minus_words => $minus_phrases, remove_intersection => 1), $res);
}
