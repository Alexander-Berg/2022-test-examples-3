#!/usr/bin/perl

use warnings;
use strict;
use Test::More;
use Test::Deep;

use Tools;
use Data::Dumper;
use JSON;

$Data::Dumper::Indent = 1;
$Data::Dumper::Terse = 1;
$Data::Dumper::Quotekeys = 0;
$Data::Dumper::Sortkeys = 1;


# скаляры самые разные, строковые представления которых не должны меняться после "оцифренния"
# (поэтому нет чисел с ведущими нулями или лишними нулями в дробной части)
my @scalars = (
    '',
    'aaa',
    '42aa',
    'aa43',
    '44aa45',
    '46.47',
    '48.49.50',
    '51.52aaa',
);

# строки, которые должны становиться числами (возможно, со сменой строкового представления '2.50' => 2.5)
my $numbers = [
    '1', 
    '12345', 
    '123.45',
    '123.45678',
    '123.4500',
];

# собираем разные структуры данных их скаляров
my @tests = (
    @scalars, 
    [@scalars], 
    {map {$_ => 1} @scalars }, 
    {map {$_ => $_} @scalars },
    [ {map {$_ => [$_]} @scalars } ],
);

my @dumper;
$dumper[$_]->{before} = Dumper($tests[$_]) for 0 .. scalar @tests -1;
Tools::force_number_recursive($_) for @tests;
$dumper[$_]->{after} = Dumper($tests[$_]) for 0 .. scalar @tests -1;

# проверяем: для более-менее обычных значений строковое представление не поменялось после приведения чисел к числам
for my $i (0 .. scalar @tests -1 ){
    is($dumper[$i]->{after}, $dumper[$i]->{before}, "$dumper[$i]->{after}");
}

# проверяем: строки, похожие на числа, сериализуются в json без кавычек,
# при этом числа не меняются
my $numbers_json_before = to_json($numbers);
Tools::force_number_recursive($numbers);
my $numbers_json_after = to_json($numbers);
ok($numbers_json_after !~ /['"]/, "array of looks-like-number: $numbers_json_after");
my $numbers_restored_before = from_json($numbers_json_before);
my $numbers_restored_after = from_json($numbers_json_after);
for my $i ( 0 .. scalar @$numbers_restored_before - 1 ){
    ok($numbers_restored_before->[$i] == $numbers_restored_after->[$i], "${i}th element of $numbers_json_before, $numbers_json_after" );
}

done_testing();

1;
