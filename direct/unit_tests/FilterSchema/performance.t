#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use utf8;


BEGIN {
    use_ok 'FilterSchema';
}

sub vr2hash{
    my $vr = shift;
    my $vr_hash = $vr->convert_to_hash;

    return {
        generic_errors => [map {$_->{description}} @$vr_hash] ,
    } if ref $vr_hash eq 'ARRAY';
    foreach my $err_item (@{$vr_hash->{objects_results} // {}}){
        next unless $err_item;
        if (ref $err_item eq 'ARRAY'){
             $err_item = { GENERIC =>  [map { $_->{description} } @$err_item ] };
             next;
        }
        foreach my $field (keys %$err_item){
            $err_item->{$field} = [map { $_->{description} } @{$err_item->{$field}}]
        }
    }

    return {
        object_errors =>  $vr_hash->{objects_results},
        generic_errors => [map {$_->{description}} @{$vr_hash->{generic_errors}}],
    }
}

my $validator = FilterSchema->new(filter_type=>'performance');

ok $validator->compiled_schema, 'compilation';

my $rules = [
    {field => 'id', relation => '<', value => [3]},
    {field => 'id', relation => '<->', value => ["7-8", "2-5"]},
    {field => 'categoryId', relation => '<', value => [1]},
    {field => 'url', relation => 'ilike', value => ['dfg']},
    {field => 'url', relation => '==', value => ['http://www.ru']},
    {field => 'name', relation => 'not ilike', value => ['performance']},
    {field => 'vendor', relation => 'ilike', value => ['vendor']},
    {field => 'price', relation => '>', value => [3.5]},
    {field => 'price', relation => '<->', value => ['1-2', '3.5-4.8']},
    {field => 'model', relation => 'ilike', value => ['aaa', 'bbb']},
];
ok $validator->reset->check($rules)->is_valid, 'correct data' ;

my $rules_with_exists = [
    {field => 'pickup', relation => 'exists', value => 1},
];

ok $validator->reset->check($rules_with_exists)->is_valid, 'relation "exists"' ;

my $wrong_rules = [
    {field => 'id', relation => '>', value => ["string"]},
    {field => 'id', relation => 'not ilike', value => ["7-8", "2-5"]},
    {field => 'categoryId', relation => '<', value => [38.4]},
    {field => 'url', relation => 'ilike', value => [777]},
    {field => 'url', relation => '==', value => ['www.ru']},
    {field => 'name', relation => 'not ilike', value => ['~`~']},
    {field => 'unknown', relation => 'ilike', value => ['vendor']},
    {field => 'price', relation => '<->', value => ['-']},
    {field => 'id', relation => '<->', value => ["7.0-8", "2-5"]},
];

is_deeply vr2hash($validator->reset()->check($wrong_rules)),
{
    generic_errors => [],
    object_errors =>
        [
           { value => ['???????????????????????? ???????????? ??????????????: ???????????????? ???????????? ???????? ?????????? ?????????????????????????? ????????????']},
           {
              relation => ['???????????????????????? ???????????? ??????????????: ???????????? ???????????????????????? ??????????????????'],
              value => ['???????????????????????? ???????????? ??????????????: ?????? ?????????????????? ?????????????????? ???? ?????????????????? ?????? ????????????????'],
           },
           { value => ['???????????????????????? ???????????? ??????????????: ???????????????? ???????????? ???????? ?????????? ?????????????????????????? ????????????']},
           { value => ['???????????????????????? ???????????? ??????????????: ???????????????? ????????????']},
           { value => ['???????????????????????? ???????????? ??????????????: ???????????????? URL']},
           { value => ['???????????????????????? ???????????? ??????????????: ???????????????? ???????????????? ???????????????????????? ??????????????']},
           { GENERIC => ['???????????????? ?????????????????? ??????????????'] },
           { value => ['???????????????????????? ???????????? ??????????????: ???????????????? ?????????????? ?????????????????? ???????????? ???????? ?????????????????????????? ????????????']},
           { value => ['???????????????????????? ???????????? ??????????????: ???????????????? ?????????????? ?????????????????? ???????????? ???????? ?????????? ?????????????????????????? ????????????'],}
        ]
}, 'wrong data';

is_deeply vr2hash($validator->reset()->check([@$rules, @$rules])),
{
    generic_errors => ['???????????????????? ?????????????? ?? ?????????????? ???????????? ???????? ???? 1 ???? 10'],
}, 'too many rules';

#$validator->debug(1);
#$validator->die_on_error(1);


is_deeply vr2hash($validator->reset()->check(
    [{field => 'id', relation => '==', value => [1x22]}],
)),
{
    generic_errors => [],
    object_errors =>
        [
           { value => ['???????????????????????? ???????????? ??????????????: ???????????????????????? ?????????? ???????????????? - 20 ????????????????']},
        ]
}, 'ID too long';

is_deeply vr2hash($validator->reset()->check(
    [
        {field => 'categoryId', relation => '<->', value => [('1'x66).'-'.('2'x70)]},
        {field => 'id', relation => '<->', value => [('1'x66).'-'.('2'x70)]}
    ],
)),
{
    generic_errors => [],
    object_errors =>
        [
            { value => ['???????????????????????? ???????????? ??????????????: ???????????????? categoryId ???? ?????????? ???????? ?????????????? 18 ????????????????.']},
            { value => ['???????????????????????? ???????????? ??????????????: ???????????????? ?????????????? ?????????????????? ???? ?????????? ???????? ?????????????? 20 ????????????????.']},
        ]
}, 'boundary too long';
    
done_testing;
