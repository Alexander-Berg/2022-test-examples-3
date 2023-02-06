package API::Test::MockHelper;
use Direct::Modern;

=pod

    $Id$

=head1 NAME

    API::Test::MockHelper — набор функций для упрощения написания модульных тестов API

=head1 DESCRIPTION

   Функции, упрощающие написание модульных тестов для сервисов API.

=cut

use Test::Deep;
use Test::More;

use Exporter qw/import/;

our @EXPORT_OK = qw/mock_subs restore_mocked_subs/;

my (%sub_args_and_results, %mocked_subs);

=head2 mock_subs($default_package, %sub_args_and_results)

    Заменяет каждую функцию, заданную в ключах хэша %sub_args_and_results на функцию, проверяющую аргументы и возвращающую
    заданный результат (переданные в значении для ключа).
    Функции, заданные без указания модуля заменяются в $default_module.

    mock_subs($default_package,
        mocked_sub1 => [
            {
                args => [...],
                result => [...] # возвращаем одно значение (скаляр или ссылка на массив, хэш и т.д.)
            }
        ],
        mocked_sub2 => [
            {
                args => [...],
                result_set => [...] # возвращаем список значений
            }
        ],
        mocked_sub3 => [
            {
                args => [...],
                result_in_args => {
                    0 => [...], # меняем значение аргумента с указанным индексом (начиная с 0), значение может быть ссылкой на массив, хэш или функцию, которая меняет значение аргумента
                    1 => {...},
                    2 => sub { $_->{value} = 17 }
                }
            }
        ]
    )

=cut

sub mock_subs {
    my $default_package;
    $default_package = shift if @_ % 2; # если передано нечетное число аргументов, то первый аргумент - имя пакета по умолчанию
    $default_package .= '::' if $default_package;
    %sub_args_and_results = @_;

    no strict qw/refs/;
    no warnings qw/once redefine prototype/;

    foreach (keys %sub_args_and_results) {
        my $sub_name = $_;
        my $package = ($sub_name =~ /::/) ? '' : $default_package;
        my $sub_name_with_package = $package.$sub_name;
        if (defined *{$sub_name_with_package}{CODE}) {
            $mocked_subs{$sub_name_with_package} = *{$sub_name_with_package}{CODE};
            *{$sub_name_with_package} = sub { _check_sub_args_and_get_result($sub_name, @_) };
        } else {
            diag "unknown sub $sub_name_with_package";
        }
    }

    return;
}

=head2 restore_mocked_subs()

    Восстанавливаем подмененные функции

=cut

sub restore_mocked_subs {
    no strict qw/refs/;
    no warnings qw/redefine prototype/;
    %sub_args_and_results = ();
    *{$_} = $mocked_subs{$_} foreach keys %mocked_subs;
    return;
}

sub _check_sub_args_and_get_result {
    my $sub_name = shift;

    if ($sub_args_and_results{$sub_name} and @{$sub_args_and_results{$sub_name}}) {
        my $args_and_result = shift @{$sub_args_and_results{$sub_name}};
        eq_deeply(\@_, $args_and_result->{args}) or cmp_deeply(\@_, $args_and_result->{args}, "check call of $sub_name");
        if ($args_and_result->{result_in_args}) {
            while (my ($index, $value) = each %{$args_and_result->{result_in_args}}) {
                if (ref $value eq 'HASH') {
                    %{$_[$index]} = %{$value};
                } elsif (ref $value eq 'ARRAY') {
                    @{$_[$index]} = @{$value};
                } elsif (ref $value eq 'CODE') {
                    $value->($_[$index]);
                }
            }
        } else {
            return exists $args_and_result->{result_set} ? @{$args_and_result->{result_set}} : $args_and_result->{result};
        }
    } else {
        diag("no args for $sub_name");
        fail("check call of $sub_name");
    }

    return;
}

1;
