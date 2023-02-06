#!/usr/bin/perl

use warnings;
use strict;
use BerkeleyDB ;
use Test::More;
# tests => 7;

use MirrorsTools;

my %mirrors;
tie (%mirrors, "BerkeleyDB::Btree", -Flags => DB_CREATE()) or die "Cannot create database: $! $BerkeleyDB::Error\n" ;

my @tests = (
    [
        # Простое транзитивное замыкание: a->b->c => a->c и b->c
        {'a' => 'b',
         'b' => 'c'},

        {'a' => 'c', 
         'b' => 'c'},
        "Transitive closure 1"
    ],
    [
        # Простое транзитивное замыкание но без сортировки по алфавиту: f->d->z->y => f->y, d->y, z->y
        {'f' => 'd',
         'd' => 'z',
         'z' => 'y'},

        {'f' => 'y', 
         'd' => 'y',
         'z' => 'y'},
        "Transitive closure 2"
    ],
    [
        # Транзитивное замыкание по несколько одинаковых значений: a->b e,c->x, x->i => a->b, e->i, x->i, c->i 
        {'a' => 'b',
         'e' => 'x',
         'x' => 'i',
         'c' => 'x'},

        {'a' => 'b',
         'e' => 'i',
         'x' => 'i',
         'c' => 'i'},
        "Transitive closure 3"
    ],
    [
        # Круговое замыкание: a->b->c->a => b,c->a 
        {'a' => 'b',
         'b' => 'c',
         'c' => 'a'},

        {'b' => 'a',
         'c' => 'a'},
         "Cycles"
    ],
    [
        # Круговое замыкание: a->d->b->a, e->a => a,e,d->b 
        {'b' => 'a',
         'd' => 'b',
         'e' => 'a',
         $MirrorsTools::HEAVY_LETTER.'a' => 'd'},

        {$MirrorsTools::HEAVY_LETTER.'a' => 'b',
         'e' => 'b',
         'd' => 'b'},
        "Cycles with Manual correction"
    ],
    [
        # Круговое замыкание: a->d->b->a, e->a => b,e,d->a 
        {'b' => 'a',
         'd' => 'b',
         'e' => 'a',
         'a' => 'd'},

        {'b' => 'a',
         'e' => 'a',
         'd' => 'a'},

        "Cycles without Manual correction"
    ]
);

Test::More::plan(tests => scalar (@tests));

foreach my $test (@tests) {
	%mirrors = %{$test->[0]};
	MirrorsTools::remove_cycles(\%mirrors);
	is_deeply(\%mirrors, $test->[1], $test->[2])
}

