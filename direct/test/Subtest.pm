package Test::Subtest;
## no critic (TestingAndDebugging::RequireUseStrict, TestingAndDebugging::RequireUseWarnings)

=head1 NAME

Test::Subtest - обёртка над Test::More::subtest()

=head1 SYNOPSIS

    $ENV{TEST_METHOD} = 'test name/nested name';
    subtest_ "test name" => sub {
        subtest_ "nested name" => sub {
            # ... do some testing
        }
    };

=head1 DESCRIPTION

Аналогично Test::More::subtest(), но позволяет выбирать подмножество тестов для запуска при помощи переменной
окружения. И не требует обязательного наличия хотя бы одного assertion'а внутри subtest.

Всё для удобства добавления и запуска тестов в процессе разработки, для отлаженных наборов тестов этот
фукнционал не нужен.

=head1 FUNCTIONS

=cut

use Direct::Modern;

use base qw/Exporter/;
our @EXPORT = qw/subtest_ run_subtests/;

use Test::More;

my $is_running;
my @subtests;
our $names = [];

=head2 subtest_

    subtest_ "test description" => sub {
        # test code
    };

=cut
sub subtest_($$) {
    my ($name, $code) = @_;
    if (!$is_running) {
        push @subtests, [$name, $code];
        return;
    }

    local $names = [@$names, $name];
    my $names_str = join("/", @$names);
    my $should_descend;
    if (!$ENV{TEST_METHOD}) {
        $should_descend = 1;
    } else {
        if (length $names_str > length $ENV{TEST_METHOD}) {
            $should_descend = $names_str =~ /^\Q$ENV{TEST_METHOD}\E/;
        } else {
            $should_descend = $ENV{TEST_METHOD} =~ /^\Q$names_str\E/;
        }
    }

    if ($should_descend) {
        subtest(
            $name => sub {
                $code->(@_);
                unless (@{Test::Builder->new->{Test_Results}}) {
                    pass('no assertions, but it\'s ok');
                }
            }
        );
    } else {
        pass('skip');
    }
}

=head2 run_subtests

Запуск ранее объявленных тестов.

Включает done_testing

=cut

sub run_subtests {
    $is_running = 1;
    for (@subtests) {
        my ($name, $sub) = @$_;
        subtest_ $name => $sub;
    }
    done_testing;
}

1;
