#!/usr/bin/perl

=pod

    Тесты на правильность работы экранирующей функции, которая используется при построении PDF-отчетов, например.
    $Id$

=cut

use strict;
use warnings;

use Test::More;
use Test::Deep;

use TeXEscape;


my @tests = (
    [123, '123'],
    ['abcde', 'abcde'],
    ['#', '\#'],
    ['$', '\$'],
    ['&', '\&'],
    ['%', '\%'],
    ['^', '\textasciicircum{}'],
    ['~', '\textasciitilde{}'],
    ['\\', '\textbackslash{}'],
    ['{', '\{'],
    ['}', '\}'],
    ['"', '``'],
    [chr(0xFA), '\\\'u'],
    [chr(0xF7), '\textdiv{}'],
    [chr(0xD7), 'x'],
    [chr(0xAA), '\textordfeminine{}'],
    ['R BN-GC-RU^CHN-Hong+Kong-Russian', 'R BN-GC-RU\textasciicircum{}CHN-Hong+Kong-Russian'],
    ['{}\\\\R BN-GC-RU^CHN-Hong+Kong-Russian', '\{\}\textbackslash{}\textbackslash{}R BN-GC-RU\textasciicircum{}CHN-Hong+Kong-Russian'],
    ['azaza ~ 123 # $ % &', 'azaza \textasciitilde{} 123 \# \$ \% \&'],
);

Test::More::plan(tests => scalar @tests);

foreach my $test (@tests) {
    is(tex_escape($test->[0]), $test->[1]);
}
