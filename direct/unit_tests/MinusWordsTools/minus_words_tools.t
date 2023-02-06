#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;

BEGIN { use_ok('MinusWordsTools'); }

use open ':std' => ':utf8';

subtest 'minus_words_array2str' => sub {
    my $f = \&MinusWordsTools::minus_words_array2str;

    is($f->([qw/минус слово !в массиве/]), '["!в","массиве","минус","слово"]', 'Simple array');
    is($f->(), undef, 'not defined array');
    is($f->([]), undef, 'empty array');
};

subtest 'minus_words_array2str' => sub {
    my $f = \&MinusWordsTools::minus_words_str2array;

    is_deeply($f->("минус слово !в массиве"), [qw/минус слово !в массиве/], 'Simple text');
    is_deeply($f->(), [], 'not defined string');
    is_deeply($f->(""), [], 'empty string');
    is_deeply($f->('["бесплатно", "скачать", "реферат"]'), ["бесплатно", "скачать", "реферат"], 'JSON text');
    is_deeply($f->('[]'), [], 'JSON empty text');
};

subtest 'are_minus_words_equal' => sub {
    my $f = \&MinusWordsTools::are_minus_words_equal;

    is_deeply($f->([qw/бесплатно скачать реферат/], [qw/бесплатно скачать реферат/]), 1, 'the same list and order');
    is_deeply($f->([qw/бесплатно скачать реферат/], [qw/скачать бесплатно реферат/]), 1, 'the same list, but diff order');
    is_deeply($f->([], []), 1, 'The same list, but empty');
    is_deeply($f->([qw/бесплатно скачать реферат/], [qw/скачать реферат/]), 0, 'Not the same list 1');
    is_deeply($f->([qw/бесплатно скачать реферат/], [qw/крокодил скачать реферат/]), 0, 'Not the same list 2');
    is_deeply($f->([qw/бесплатно скачать реферат/], []), 0, 'Not the same list 3');
};

subtest 'minus_words_add_minus' => sub {
    my $f = \&MinusWordsTools::minus_words_add_minus;

    is_deeply($f->([qw/бесплатно скачать реферат/]), [qw/-бесплатно -скачать -реферат/], 'Simple text');
    is_deeply($f->([]), [], 'The same list, but empty');
};

subtest 'minus_words_interface_show_format2array' => sub {
    my $f = \&MinusWordsTools::minus_words_interface_show_format2array;

    is_deeply($f->("-бесплатно скачать реферат"), ["бесплатно скачать реферат"], 'Simple text');
    is_deeply($f->(""), [], 'Empty string');
    is_deeply($f->("-бесплатно скачать реферат -мереорит тунгузский"), ["бесплатно скачать реферат", "мереорит тунгузский"], 'Minus with space as delimeter');
    is_deeply($f->("-бесплатно скачать реферат-мереорит тунгузский"), ["бесплатно скачать реферат-мереорит тунгузский"], 'Minus without space is not a delimeter');
    is_deeply($f->("-бесплатно скачать реферат, мереорит тунгузский"), ["бесплатно скачать реферат", "мереорит тунгузский"], 'Comma as delimeter');
};

done_testing();

1;
