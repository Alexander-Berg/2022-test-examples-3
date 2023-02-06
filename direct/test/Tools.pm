=head1 NAME

Yandex::Test::Tools -- полезные функции для тестирования

=head1 DESCRIPTION

Полезные функции-хелперы для использования в юнит-тестах

=cut

package Yandex::Test::Tools;

use strict;
use warnings;
use utf8;

use base qw/Exporter/;

our @EXPORT_OK = qw(
    generate_string
);

use List::MoreUtils qw/any all/;

=head2 generate_string(%options)

    Посимвольная генерация строки/массива_строк по заданным параметрам.

    Параметры:
        %options:
            min_chr => минимальный код символа, с которого следует начать; по умолчанию = 0
            max_chr => максимальный код символа, на котором следует закончить; по умолчанию = 2047

            re => регулярное выражение, которому должен удовлетворять каждый символ
            re_and => массив регулярных выражений, которым должен удовлетворять каждый символ
            re_or => массив регулярных выражений, любому из которых должен удовлетворять каждый символ
            not_re => регулярное выражение, которому НЕ должен удовлетворять каждый символ
            not_re_and => массив регулярных выражений, которым НЕ должен удовлетворять каждый символ
            not_re_or => массив регулярных выражений, любому из которых НЕ должен удовлетворять каждый символ

            len => вернуть строку длиной не более len символов
            chunk_len => разбить строку на подстроки, максимальной длины в chunk_len символов

        Примечания:
            chunk_len приоритетней len

    Результат:
        Строка или массив строк в списковом контексте

=cut

sub generate_string {
    my %options = @_;

    my @str;
    my $min_chr = $options{min_chr} // 0;
    my $max_chr = $options{max_chr} // 2047;
    for my $i ($min_chr .. $max_chr) {
        my $c = chr($i);
        $c = undef if defined $c && $options{re} && $c !~ $options{re};
        $c = undef if defined $c && $options{re_and} && any { $c !~ $_ } @{$options{re_and}};
        $c = undef if defined $c && $options{re_or} && all { $c !~ $_ } @{$options{re_or}};
        $c = undef if defined $c && $options{not_re} && $c =~ $options{not_re};
        $c = undef if defined $c && $options{not_re_and} && any { $c =~ $_ } @{$options{not_re_and}};
        $c = undef if defined $c && $options{not_re_or} && all { $c =~ $_ } @{$options{not_re_or}};
        push @str, $c if defined $c;
    }
    return '' unless @str;

    if ($options{chunk_len}) {
        my @chunks;
        while (my @x = splice(@str, 0, $options{chunk_len})) {
            push @chunks, join('', @x);
        }
        return @chunks;
    }

    if ($options{len} && @str > $options{len}) {
        @str = splice @str, 0, $options{len};
    }

    return join('', @str);
}

1;
