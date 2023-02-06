#!/usr/bin/perl

=head1 

    Шарится с Модерацией.
    TODO: сделать универсальным

    Тест проверяет, что на текущей машине установлены все зависимости, которые требуются в packages/yandex-direct/debian/control для пакета yandex-direct

    К моменту коммита в trunk все зависимости должны быть уже установлены на ppcdev.

    Есть список исключений, см. %SKIP

=cut

=pod

    Тест не зависит от рабочей копии директа и может быть запущен отдельно
    Для этого ему нужно передать аргументом командной строки путь до папки с миграциями

    NB! Этот режим используется при запуске в buildbot, и если тест начнет зависеть от чего-то еще,
    то там он сломается. В таком случае стоит исключить отдельный запуск этого теста в buildbot.
    Об этом можно попросить ppalex@ или lena-san@.

=cut

use strict;
use warnings;

use Dpkg::Version;
use List::MoreUtils qw/after zip/;
use Path::Tiny;
use Test::More;

my $CHECK_PACKAGE = 'yandex-direct';

*cmp_versions = Dpkg::Version->can('compare_versions')
    || Dpkg::Version->can('version_compare_relation')
    || die "Unsupported version of Dpkg::Version";

# список исключений. Эти строчки из Depends игнорируем и не проверяем
my %SKIP = map { $_ => 1 } (
    "$CHECK_PACKAGE-zk-delivery-configs (>=\${binary:Version})",
    "$CHECK_PACKAGE-dpkg-monitor-configs (>=\${binary:Version})",
    'libpod-simple-perl',   # этот пакет предоставлен пакетом perl-modules
);

#...................................................................................

my $control_file;
if ($ARGV[0]) {
    die "control file doesn't exists" unless -f $ARGV[0];
    $control_file = path($ARGV[0]);
} else {
    $control_file = path( path($0)->dirname  )->child("../../packages/$CHECK_PACKAGE/debian/control");
}
my $control = $control_file->slurp;

# текст с зависимостями для сборки
my ($build_dep_text) = ($control =~ m/Build-Depends: (.*?)Standards-Version:/sm);
my @build_deps = grep {$_ && !$SKIP{$_}} split /,\s*/, join "\n", grep {!/^#/} split "\n", $build_dep_text;

# текст со списком зависимостей
my ($dep_text) = ($control =~ /Package: $CHECK_PACKAGE\b.*?Depends: \${perl:Depends}(.*?)Description:/sm);

# массив отельных строчек вида 'libtemplate-perl (= 2.22-1)'
my @deps = grep {$_ && !$SKIP{$_}} split /,\s*/, join "\n", grep {!/^#/} split "\n", $dep_text;

# установленные пакеты
# copy-paste из dpkg-monitor
my $dpkg_text = `dpkg -l`;
die "Can't start dpkg: $!" if $?;
my %status =
    # некоторые пакеты (libapr1, libgearman7) dpkg показывает с суффиксом :amd64, его игнорируем
    map {($_->{name} =~ s/:amd64$//r) => $_}
    # пропускаем неустановленные пакеты
    grep {$_->{status} !~ /^[pur]/}
    # парсим строчку
    map { {zip @{[qw/status name version desc/]}, @{[split /\s+/, $_, 4]}}; }
    after {/^\+\+\+/}
    split /\n/,
    $dpkg_text;

Test::More::plan(tests => 1 + @build_deps + @deps);

ok(@deps > 20, "number of dependencies");

for my $rec (@build_deps, @deps){
    my @simple_deps = split /\s*\|\s*/, $rec;
    
    my @results = map {check_one_package(\%status, $_)} @simple_deps; 
    my $name = join " OR ", map {$_->{name}} @results;
    my $ok = scalar grep {$_->{ok}} @results;
    ok($ok, $name);
}


sub check_one_package
{
    my ($status, $dep) = @_;

    my $name = '';
    my $ok = 0;

    $dep =~ /^\s*([^ \(]+)\s*(.*)/;
    my ($package, $cond) = ($1, $2);
    $cond =~ s/^\((.*)\)$/$1/;
    if (!$cond){
        $ok = exists $status{$package};
        $name = "$package should be installed";
    } else {
        $cond =~ /^(=|<=|>=|<<?|>>?)\s*(\d.*)$/;
        my ($cmp, $required_version) = ($1, $2);
        die "can't parse condition '$cond' from dependency '$dep'" if !$cmp || !$required_version;
        $ok = exists $status{$package} && cmp_versions($status{$package}->{version} || '', $cmp, $required_version);
        $name = "$package $cmp $required_version should be installed";
    } 

    return {name => $name, ok => $ok};
}
