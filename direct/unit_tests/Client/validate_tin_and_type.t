#!/usr/bin/env perl

#use my_inc '../../..';

use Direct::Modern;

use Test::More;

use Yandex::Test::UTF8Builder;

use Client ();

my $bad_inn_text = "Неверный формат ИНН";

my @tests = (
    [undef, undef, undef, 'Ничего на входе — допустимо'],
    ['legal', undef, undef, 'Юрлицо без ИНН — допустимо'],
    ['physical', undef, undef, 'Физлицо без ИНН — допустимо'],
    ['psychic', undef, 'Ошибка в выборе между физическим и юридическим лицом', 'Непонятное лицо — недопустимо'],
    [undef, '1234567890', 'Не сделан выбор между физическим и юридическим лицом', 'ИНН без выбора лица — недопустимо'],
    ['physical', '8657968970', $bad_inn_text, 'ИНН неверной длины у физлица — недопустимо'],
    ['legal', '8657968970', undef, 'Верный ИНН у юрлица — допустимо'],
    ['legal', '238397150159', $bad_inn_text, 'ИНН неверной длины у юрлица — недопустимо'],
    ['physical', '238397150159', undef, 'Верный ИНН у физлица — допустимо'],
    ['physical', '238397150150', $bad_inn_text, 'Неверный ИНН у физлица — недопустимо'],
    ['legal', '8657968971', $bad_inn_text, 'Неверный ИНН у юрлица — недопустимо'],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    my ($tin_type, $tin, $expected_result, $test_name) = @$test;
    my $result = Client::validate_tin_and_type($tin, $tin_type);
    is($result, $expected_result, $test_name);
}
