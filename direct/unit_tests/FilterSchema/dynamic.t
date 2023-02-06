#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use utf8;

BEGIN {
    use_ok 'FilterSchema';
}

my $validator = FilterSchema->new(filter_type=>'dynamic_conditions');

my $rules = [
    {type => 'title', kind => 'exact', value => "fffff"},
    {type=> 'content', kind => 'not_exact', value => ["aa","bb","cc"]},
    {type => 'URL_prodlist', kind => 'equals', value => ['http://www.ru']},
];

ok $validator->check($rules)->is_valid, 'correct data' ;
ok $validator->compiled_schema, 'compilation';

is_deeply
    [ sort @{$validator->compiled_schema->{definitions}->{itemsOrder}} ],
    [ sort keys %{$validator->compiled_schema->{definitions}->{rule}} ],
    'itemsOrder contains all items';

my $rules_with_any = [@$rules, {type => 'any'}];
is_deeply $validator->reset->check($rules_with_any)->get_error_descriptions(),
    ["Правило 'Все страницы' в условии нацеливания не может комбинироваться с другими правилами"],
    'rules with "any"';

my $non_unique_rules = [@$rules, @$rules];
is_deeply $validator->reset->check($non_unique_rules)->get_error_descriptions(),
    ["Правила в условии нацеливания должны быть уникальны"],
    'non unique rules';

my $rules_with_duplicate_url_prodlist = [@$rules,   {type => 'URL_prodlist', kind => 'equals', value => ['http://site.sample']}];
is_deeply $validator->reset->check($rules_with_duplicate_url_prodlist)->get_error_descriptions(),
    ["Правило 'URL списка предложений' должно быть в единственном экземпляре в пределах одного отношения"],
    'duplicate URL_prodlist';

my $rules_with_different_url_prodlists = [@$rules,   {type => 'URL_prodlist', kind => 'not_equals', value => ['http://site.sample']}];

ok $validator->reset->check($rules_with_different_url_prodlists)->is_valid,  'different URL_prodlists';


my $url_with_spaces = [@$rules,   {type => 'URL', kind => 'exact', value => ['  dsd  ']}];

ok $validator->reset->check($url_with_spaces)->is_valid, 'url with spaces';

my $spaces_only = [@$rules,   {type => 'title', kind => 'exact', value => ['    ']}];
is_deeply $validator->reset->check($spaces_only)->get_error_descriptions(),
    ["Неправильный формат правила: значение №1 правила №4 должно содержать хотя бы один значащий символ"],
    'spaces only';
my $alien_rules = [
                { field => 'price', relation =>"\u003c-\u003e", value => ["222-33333"], },
                { field => 'vendor', relation => 'like', value => ['sdf', 'dfg'], },
];
#$validator->debug(1);# $validator->die_on_error(1);
is_deeply $validator->reset->check($alien_rules)->get_error_descriptions(),
    [
    'Неправильный формат правила: правило №1 имеет неверную структуру',
    'Неправильный формат правила: правило №2 имеет неверную структуру',
    ],
    'alien_rules';
    

my $wrong_url = [{type => 'URL_prodlist', kind => 'not_equals', value => ['https://ムラrld.ru']}];
is_deeply $validator->reset->check($wrong_url)->get_error_descriptions(),
    [
     "Неправильный формат ссылки №1 для правила №1",
     "В значении №1 правила №1 допускается использование букв латинского, турецкого, русского, украинского, казахского алфавитов, цифр и знаков пунктуации.",
     ],
    'wrong url';

my $too_many_arguments = [{type => 'URL', kind => 'exact', value => [('ihqHXHuYYG')x11]}];
is_deeply $validator->reset->check($too_many_arguments)->get_error_descriptions(),
    [
     "Количество аргументов в правиле №1 условия нацеливания должно быть от 1 до 10"
    ],
    'too many arguments';

my $empty_string_at_value = [@$rules,   {type => 'title', kind => 'exact', value => ['']}];
is_deeply $validator->reset->check($empty_string_at_value)->get_error_descriptions(),
    [
     "Неправильный формат правила №4: значение аргумента №1 не может быть пустой строкой"
     ],
    'empty string at value';

my $long_url = [{type => 'URL_prodlist', kind => 'equals', value => [
    'http://ApKqiaX.DXTKsW.UpMTeQ.qMmjgF.oklQxq.UtWmTP.TcqjCt.opWnYe.WzuIid.AeWIle.'.('z'x1000).'.mSweaw.UsQuJtlO'
    ]}];
is_deeply $validator->reset->check($long_url)->get_error_descriptions(),
    [
     "Неправильный формат ссылки №1 для правила №1",
     "Превышена максимальная длина значения №1 правила №1 в 1024 символа",
     ],
    'long url';



done_testing;
