#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use utf8;

use Test::More;
use Yandex::Test::UTF8Builder;
use BS::ResyncQueue;

# Проверять ли что все константы должны иметь различные значения
use constant SKIP_DUPLICATE_PRIORITITES => 0;

# в таблице bs_resync_queue для приоритета используется TINYINT
use constant MIN_TABLE_VALUE => -128;
use constant MAX_TABLE_VALUE =>  127;

# Проверяем, что все указанные приоритеты имеют разное значение
my $priorities = BS::ResyncQueue::get_priorities_hash();

Test::More::plan(tests => 3 * scalar keys %$priorities);

my %priority_values;
while (my ($name, $value) = each %$priorities) {
    cmp_ok(MIN_TABLE_VALUE, '<=', $value,
       sprintf("проверка значения %s - нижняя граница допустимого диапазона: %d <= %d", $name, MIN_TABLE_VALUE, $value));
    cmp_ok($value, '<=', MAX_TABLE_VALUE,
       sprintf("проверка значения %s - верхняя граница допустимого диапазона: %d <= %d", $name, $value, MAX_TABLE_VALUE));

    SKIP: {
        skip 'Не проверяем константы приоритетов на уникальность значений', 1 if SKIP_DUPLICATE_PRIORITITES;
        if (!exists $priority_values{$value}) {
            $priority_values{$value} = $name;
            pass("константа $name имеет уникальное значение");
        } else {
            fail("константа $name совпадает по значению с $priority_values{$value}");
        }
    }
}
