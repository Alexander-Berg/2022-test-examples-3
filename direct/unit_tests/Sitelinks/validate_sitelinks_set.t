#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More;
use Test::Deep;
use Yandex::I18n;
use Sitelinks qw//;
use Direct::Validation::SitelinksSets qw/$SITELINKS_MAX_LENGTH $SITELINKS_NUMBER $ONE_SITELINK_MAX_LENGTH/;

*vss = \&Sitelinks::validate_sitelinks_set;

use utf8;
use open ':std' => ':utf8';

my @tests = (
    {
        sitelinks => [
            { title => 'фва', href => 'http://ya.ru/1' },
        ],
        href => 'http://ya.ru',
        result => [
        ],
    },
    {
        sitelinks => [
            { title => 'фва', href => 'https://ya.ru/1' },
        ],
        href => 'https://ya.ru',
        result => [
        ],
    },

    {
        sitelinks => [
            { title => 'фва', href => 'http://ya.ru/1' },
            { title => 'a-sdf12', href => 'http://ya.ru/2' },
        ],
        href => 'http://ya.ru',
        result => [
        ],
    },

    {
        sitelinks => [
            { title => 'фва', href => 'http://ya.ru/1' },
            { title => 'a-sdf12', href => 'http://ya.ru/2' },
            { title => "\x{4B0}\x{492} \x{404}\x{406}", href => 'http://ya.ru/3' },
        ],
        href => 'http://ya.ru',
        result => [
        ],
    },

    {
        sitelinks => [
            { title => 'фва', href => 'http://ya.ru/1' },
            { title => 'a-sdf12', href => 'http://ya.ru/2' },
            { title => "\x{4B0}\x{492} \x{404}\x{406}", href => 'http://ya.ru/3' },
            { title => "\x{4B0}\x{492} \x{404}\x{406} four", href => 'http://ya.ru/4' },
        ],
        href => 'http://ya.ru',
        result => [
        ],
    },

    {
        sitelinks => [
            { title => '1', href => 'http://ya.ru' },
            { title => '1', href => 'http://ya.ru' },
            { title => '1', href => 'http://ya.ru' },
        ],
        href => 'http://ya.ru/main',
        result => [
            "Тексты быстрых ссылок не должны быть одинаковыми",
        ],
    },

    {
        sitelinks => [
            { title => '1', href => 'http://ya.ru/main' },
            { title => '2', href => 'http://ya.ru/2' },
            { title => '3', href => 'http://ya.ru/3' },
        ],
        href => 'http://ya.ru/main',
        result => [
        ],
    },

    {
        sitelinks => [
            { title => '1 2', href => 'http://ya.ru' },
            { title => '1  2', href => 'http://ya.ru' },
            { title => '1', href => 'http://ya.ru/sd kflj' },
        ],
        href => 'http://ya.ru/main',
        result => [
            "Тексты быстрых ссылок не должны быть одинаковыми",
            "Неправильный формат быстрой ссылки: http://ya.ru/sd kflj",
        ],
    },
    {
        sitelinks => [
            { title => 'Быстрая ссылка 1', href => 'http://ya.ru/tr' },
            { title => 'Быстрая ссылка 2', href => 'ya.ru/mn' },
            { title => 'Быстрая ссылка 3', href => 'http://ya.ru/sd' },
        ],
        href => 'http://ya.ru/main',
        result => [
            "Неправильный формат быстрой ссылки: ya.ru/mn",
        ],
    },

    {
        sitelinks => [
            { title => '1 2', href => 'http://ya.ru' },
            { title => '12', href => 'http://ya.ru' },
            { title => '1', href => '' },
        ],
        href => 'http://ya.ru',
        result => [
            "Не указан адрес быстрой ссылки",
        ],
    },

    {
        sitelinks => [
        ],
        href => 'http://ya.ru',
        result => [
            "Количество быстрых ссылок должно быть от 1 до $SITELINKS_NUMBER",
        ],
    },

    {
        sitelinks => [
            { title => '1 1', href => 'http://ya.ru/1' },
            { title => '1 2', href => 'http://ya.ru/2' },
            { title => '1 3', href => 'http://ya.ru/3' },
            { title => '1 4', href => 'http://ya.ru/4' },
            { title => '1 5', href => 'http://ya.ru/5' },
            { title => '1 6', href => 'http://ya.ru/6' },
            { title => '1 7', href => 'http://ya.ru/7' },
            { title => '1 8', href => 'http://ya.ru/8' },
            { title => '1 9', href => 'http://ya.ru/9' },
        ],
        href => 'http://ya.ru',
        result => [
            "Количество быстрых ссылок должно быть от 1 до $SITELINKS_NUMBER",
        ],
    },

    {
        sitelinks => [
            { title => '1 2aaaaaaaaaaaaaaaaaaaaaaaa', href => 'http://ya.ru/1' },
            { title => '12 zzzzzzzzzzzzzzzzzzzzzzzz', href => 'http://ya.ru/2' },
            { title => '1 ccccccccccccccccccccccccc', href => 'http://ya.ru/3' },
            { title => '1 4', href => 'http://ya.ru/4' },
            { title => '1 5', href => 'http://ya.ru/5' },
            { title => '1 6', href => 'http://ya.ru/6' },
        ],
        href => 'http://ya.ru',
        result => [
            ["Суммарная длина текстов быстрых ссылок № 1-4 превышает %s символов", $SITELINKS_MAX_LENGTH],
        ],
    },

    {
        sitelinks => [
            { title => '1 1', href => 'http://ya.ru/1' },
            { title => '1 2', href => 'http://ya.ru/2' },
            { title => '1 3', href => 'http://ya.ru/3' },
            { title => '1 4aaaaaaaaaaaaaaaaaaaaa', href => 'http://ya.ru/4' },
            { title => '1 5bbbbbbbbbbbbbbbbbbbbb', href => 'http://ya.ru/5' },
            { title => '1 6ccccccccccccccccccccc', href => 'http://ya.ru/6' },
            { title => '1 7ddddddddddddddddddddd', href => 'http://ya.ru/7' },
        ],
        href => 'http://ya.ru',
        result => [
            ["Суммарная длина текстов быстрых ссылок № 5-8 превышает %s символов", $SITELINKS_MAX_LENGTH],
        ],
    },

    {
        sitelinks => [
            { title => '1 2aaaaaaaaaaaaaaaaaaaaaaaa', href => 'http://ya.ru/1' },
            { title => '12 zzzzzzzzzzzzzzzzzzzzzzzz', href => 'http://ya.ru/2' },
            { title => '1 ccccccccccccccccccccccccc', href => 'http://ya.ru/3' },
        ],
        href => 'http://ya.ru',
        result => [
            ["Превышена суммарная длина текста быстрых ссылок — максимум %s символов", $SITELINKS_MAX_LENGTH],
        ],
    },

    {
        sitelinks => [
            { title => '1 2aaaxxxxxaaaaaaaaaaaaaaaaaaaaa', href => 'http://ya.ru/1' },
            { title => '12', href => 'http://ya.ru/2' },
            { title => '1 ', href => 'http://ya.ru/3' },
        ],
        href => 'http://ya.ru',
        result => [
            ["Превышена допустимая длина текста одной быстрой ссылки в %s символов", $ONE_SITELINK_MAX_LENGTH]
        ],
    },

    {
        sitelinks => [
            { title => '1!!!', href => 'http://ya.ru/1' },
            { title => '12', href => 'http://ya.ru/2' },
            { title => '1', href => 'http://ya.ru/3' },
        ],
        href => 'http://ya.ru',
        result => [
            "В тексте быстрых ссылок можно использовать только буквы латинского, турецкого, русского, украинского, казахского или белорусского алфавита, знаки пунктуации, за исключением !,?",
        ],
    },

    {
        sitelinks => [
            { title => "Forte Village Resort\n", href => 'http://ya.ru/1' },
            { title => 'Pitrizza', href => 'http://ya.ru/2' },
            { title => 'Romazzino', href => 'http://ya.ru/3' },
        ],
        href => 'http://ya.ru',
        result => [
            "В тексте быстрых ссылок можно использовать только буквы латинского, турецкого, русского, украинского, казахского или белорусского алфавита, знаки пунктуации, за исключением !,?",
        ],
    },

    {
        sitelinks => [
            { title => "1", href => 'http://1.ya.ru' },
            { title => '2', href => 'http://2.ya.ru' },
            { title => '3', href => 'http://3.ya.ru/3333' },
        ],
        href => 'http://ya.ru',
        result => [
            # 'Домен быстрых ссылок должен совпадать с доменом объявления, либо вести на сайты социальных сетей или Яндекс.Маркет'
        ],
    },

    # медиапланы
    {
        sitelinks => [
            { title => "1", href => 'http://1.ya.ru' },
            { title => '2', href => 'http://2.ya.ru' },
            { title => '3', href => 'http://3.ya.ru' },
        ],
        href => undef,
        OPT => {skip_main_href_for_sitelinks => 1},
        result => [
        ],
    },
);

Test::More::plan(tests => scalar(@tests));
foreach my $t (@tests) {
    my @actual_result = vss($t->{sitelinks}, $t->{href}, ClientID => 0, $t->{OPT} ? %{$t->{OPT}} : ());
    cmp_deeply(
        \@actual_result
        , [map { ref($_) ? iget(@$_) : iget($_) } @{$t->{result}}]
    );
}
