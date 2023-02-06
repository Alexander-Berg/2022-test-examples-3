#!/usr/bin/perl

=head1 NAME
    
    unit_tests/runtests.pl

=head1 USAGE

Запустить все тесты в yandex-lib

    --changed -- запускать тесты только в тех директориях, где есть изменения.

    --timer -- дополнительно выводить время работы для каждого теста

    --verbose -- выводить результаты каждого теста

    --jobs N -- работать в N потоков (4 по-умолчанию)

    --lib lib1 --lib lib2 -- запускать тесты только для этих библиотек
        Например, --lib dbtools --lib log

    --help -- показать справку и выйти

=cut

use warnings;
use strict;
use utf8;
use lib::abs;

use TAP::Harness;
use Sys::Hostname;
use Getopt::Long;
use Pod::Usage qw/pod2usage/;
use Path::Tiny;
use List::MoreUtils qw/uniq/;

my $VERBOSE = 0; 
my $TIMER = 0;
my $JOBS = 4;
my $CHANGED = 0;
my @LIBS;

my %DEF = (
    'verbose' => \$VERBOSE,
    'timer' => \$TIMER,
    'jobs=i' => \$JOBS,
    changed => \$CHANGED,
    'lib=s@' => \@LIBS,
    help => \&pod2usage
);
GetOptions(%DEF) || die;

my $LIB_ROOT = path(lib::abs::path('..'));

if (@LIBS && $CHANGED) {
    die "--lib and --changed do not make sense together";
}

if (@LIBS) {
    @LIBS = map { path($LIB_ROOT, path($_)->basename) } @LIBS;
}

if ($CHANGED) {
    @LIBS = get_changed_libs();
}

if (!@LIBS) {
    @LIBS = ($LIB_ROOT);
}

$ENV{UNIT_SILENT} = !$VERBOSE;
$ENV{UNIT_HOST} = hostname();

my @test_files = get_test_files();
unshift @test_files, "$LIB_ROOT/unit_tests/compile_all.t";


my $harness = TAP::Harness->new({
    verbosity => $VERBOSE,
    timer => $TIMER,
    jobs => $JOBS,
    exec => sub {
        my ($harness, $test_file) = @_;

        my (@perl_param, @test_param);
        my $lib = $test_file =~ s!/t/.*!/lib/!r;
        if ($lib) {
            push @perl_param, "-I$lib"
        }
        if (path($test_file)->absolute =~ m!unit_tests/compile_all\.t$!) {
            push @test_param, map { ('--lib' => $_) } @LIBS;
        }
        return [ '/usr/bin/perl', @perl_param, $test_file, @test_param ];
    },
});

my $agg = $harness->runtests(@test_files);

my $status = $agg->has_errors ? 1 : 0;

exit $status;

##############

sub get_changed_libs
{
    my @changed =
        uniq
        map { "$LIB_ROOT/$_" }
        map { m!^([^/]+)/!; }
        map { (split /\s+/, $_, 2)[1] }
        split /\n/,
        `cd $LIB_ROOT && svn st -q`;

    return @changed;
}

sub get_test_files
{
    return map { test_files_in_dir($_) } @LIBS;
}

sub test_files_in_dir
{
    my ($dir) = @_;
    my @files;
    my $iter = path($dir)->iterator({ recurse => 1 });
    while (my $p = $iter->()) {
        next if $p->absolute =~ m!unit_tests/compile_all\.t$!;
        if ($p =~ /\.t$/) {
            push @files, ''.$p->absolute;
        }
    }
    return @files;
}

