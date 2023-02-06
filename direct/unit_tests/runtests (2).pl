#!/usr/bin/perl

# $Id$

=head1 NAME
    
    unit_tests/runtests.pl

=head1 DESCRIPTION

    На практике чаще всего нужны следующие способы запуска: 

    Запустить все, что нужно, перед коммитом в транк:
    direct-mk test-full-heavy
    cd data3 && make tests.standalone

    проверить компиляцию всего:
    ./unit_tests/runtests.pl unit_tests/perl/compile_all_PARALLEL.t
    ./unit_tests/runtests.pl --only-tag perl-compile

    проверить консистентность использования модулей:
    ./unit_tests/runtests.pl unit_tests/perl/used_modules_PARALLEL.t
    ./unit_tests/runtests.pl --only-tag used-modules

    проверить миграции (все тесты из каталога unit_tests/deploy):
    ./unit_tests/runtests.pl unit_tests/deploy

    проверить отдельный тест:
    ./unit_tests/runtests.pl unit_tests/AutoBroker/calc_coverage.t

    проверить все тесты из определенного каталога: 
    ./unit_tests/runtests.pl unit_tests/js-perl
    ./unit_tests/runtests.pl unit_tests/DBStat
    ./unit_tests/runtests.pl unit_tests/db_schema

=head1 OPTIONS

    --cover - запускает тест с Devel::Cover для сбора данных по покрытию, часть
        тестов при этом может падать.

    --timer -- дополнительно выводить время работы для каждого теста

    --no-fake-settings - работать с текущей конфигурацией рабочей копии,
        а не SettingsForUnitTests

=head1 TAGS

    Теги - это способ группировать юнит-тесты произвольным образом для
    совместного запуска или исключения. Список тегов находится в файле
    с метаданными о юнит-тестах (meta.yaml), в секции tags. Тегами можно
    маркировать как директории с тестами, так и отдельные файлы.

    Теги используются для фильтрации списка запускаемых тестов. Для этого
    используются параметры --only-tag и --skip-tag. Их можно указывать
    несколько раз и использовать одновременно. Примеры:

    Запустить тесты шаблонов
        ./unit_tests/runtests.pl --only-tag templates

    Запустить perl-тесты
        ./unit_tests/runtests.pl --only-tag perl

    Запустить perl-тесты кроме тяжелых
        ./unit_tests/runtests.pl --only-tag perl --skip-tag heavy

=head1 EXAMPLES

    Экзотика, почти никогда не требуется в реальной жизни:

    ./unit_tests/runtests.pl --no-db --jobs=8
    ./unit_tests/runtests.pl --jobs=1 --verbose
    ./unit_tests/runtests.pl --cover

=cut

use warnings;
use strict;

use Digest::CRC qw/crc32/;
use File::Find;
use Getopt::Long;
use List::MoreUtils qw/uniq/;
use List::Util qw/min/;
use Path::Tiny;
use POSIX qw//;
use Sys::Hostname;
use TAP::Harness;
use YAML ();

use Yandex::Shell;

# workaround для arc-репозитория
$File::Find::dont_use_nlink = 1;

# все юнит-тесты директа следует искать в этих директориях
my @ALL_TESTS_PATH = (
    my_inc::path('.'),
    my_inc::path('../api/t/')
);

my $NIGHT_MODE = (localtime)[2] > 22 || (localtime)[2] < 10;

my $FAKE_SETTINGS = 1;
my $DB = 1;
my $VERBOSE = 0; 
my $TIMER = 0;
my $COVER = 0;
my $COVER_EXEC = '/usr/bin/cover';
my $CPUS = (`grep ^processor /proc/cpuinfo |wc -l`)*1 || 32;
my $JOBS_LIMIT = int($CPUS * ($NIGHT_MODE ? 1 : 0.5));
my $JOBS = int($ENV{JOBS}||0) || ($NIGHT_MODE && -t *STDOUT ? $JOBS_LIMIT : 8);

my (@ONLY_TAGS, @SKIP_TAGS, %TESTS_TAGS);

my %DEF = (
    'verbose' => \$VERBOSE,
    'fake-settings!' => \$FAKE_SETTINGS,
    'db!' => \$DB,
    'timer' => \$TIMER,
    'jobs=i' => \$JOBS,
    'cover' => \$COVER,
    'only-tag=s@' => \@ONLY_TAGS,
    'skip-tag=s@' => \@SKIP_TAGS,
    'help' => sub {
        system("podselect -section NAME -section DESCRIPTION -section SYNOPSIS -section OPTIONS --section TAGS -section EXAMPLES $0");
        exit 0;
    },
);
GetOptions(%DEF) || die;
$JOBS = 1 if $VERBOSE || $COVER;
$JOBS = $JOBS_LIMIT if $JOBS > $JOBS_LIMIT;
my $DB_CNT = POSIX::ceil($JOBS/2) || 1;
my $canonical_options = ($DB && !@ARGV && !@SKIP_TAGS && !@ONLY_TAGS) ? 1 : 0;

use my_inc "..";

use Settings;

# подгружаем метаданные
my $tests_meta = YAML::LoadFile(my_inc::path('meta.yaml'));

for my $tag (keys(%{ $tests_meta->{tags} // {} })) {
    my %paths;
    for my $test_path (@{ $tests_meta->{tags}->{$tag} }) {
        my $abs_test_path = my_inc::path("../$test_path");
        if ($abs_test_path && (-d $abs_test_path || -f $abs_test_path)) {
            # директории запоминаем всегда с хвостовым слешом
            $test_path =~ s|/*$|/| if -d $abs_test_path;
            $paths{$test_path} = undef;
        } else {
            die "Invalid path '$test_path' for tag <$tag>: no such file or directory";
        }
    }
    my $all_paths = sprintf('^%s/(?:%s)', my_inc::path('..'), join('|', sort keys %paths));
    $TESTS_TAGS{$tag} = qr/$all_paths/;
}

# проверяем указанные теги на существование
for my $tag (uniq(@ONLY_TAGS, @SKIP_TAGS)) {
    die "No metadata for tag: <$tag>" unless exists $TESTS_TAGS{$tag};
}

# порядок сортировки тестов
my @order = (
    qr/all_subs_used/,
    qr/PARALLEL\d+/,
    qr/PARALLEL/,
    qr/\/(repos|compile|perl)/,
    qr/template/,
    );

# имена файлов с тестами
my @test_files;
find( {
        wanted => sub {push @test_files, path($_)->absolute if -f $_},
        preprocess => sub {
            return grep { !/^\./ && wanted_path($_) } @_;},
        }
        , ( @ARGV
            ? map { path($_)->absolute } @ARGV
            : @ALL_TESTS_PATH
        )
    );

if ($FAKE_SETTINGS) {
    $ENV{SETTINGS_LOCAL_SUFFIX} = 'ForUnitTests';
}

# чтобы при прерывании lock-файлы не висели вечно
$ENV{UNITTESTS_TMP_DIRECTORY} = '/tmp/temp-ttl/ttl_2d' if (-d '/tmp/temp-ttl/ttl_2d');

# создаём несколько баз, из которых будем в тестах выбирать случайную
# (для ускорения за счёт меньшего количества блокировок)
$Yandex::DBTools::QUERIES_LOG = undef;
my @unit_tests_dbs;
if ($DB) {
    require Yandex::DBUnitTest;
    for (1 .. $DB_CNT) {
        Yandex::DBUnitTest->import(qw/:create_db/);
        push @unit_tests_dbs, $ENV{$Yandex::DBUnitTest::ENV_DB_NAME};
    }
}

my $cwd = POSIX::getcwd();
# сортируем файлы так, чтобы тяжёлые скрипты оказались по-возможности раньше
@test_files = sort {
    for my $or (@order) {
        my $res = ($a !~ $or) <=> ($b !~ $or);
        return $res if $res;
    }
    $a cmp $b;
}
map {s!^$cwd/!!; $_} @test_files;

# размножаем файлы с PARALLEL для параллельного запуска
@test_files = map {
    my $file = $_;
    if (/PARALLEL([0-9]*)/) {
        my $jobs = $1 ? min($JOBS, $1) : $JOBS;
        (map {my $name = "$file ".($_)."/$jobs"; [$name, $name]} 1..$jobs);
    } else {
        $_;
    }
} @test_files;


my $cover_arg;
my $cover_db_dir = "$Settings::ROOT/cover_db";
if($COVER) {
    system($COVER_EXEC, '-delete', $cover_db_dir) if -d $cover_db_dir;

    $cover_arg = "-MDevel::Cover=-db,$cover_db_dir,-silent,1";
}

my $harness = TAP::Harness->new( {
    verbosity => $VERBOSE,
    timer => $TIMER,
    jobs => $JOBS,
    exec => sub {
        my ( $harness, $test_file ) = @_;
        my $db_idx;
        if (my ($file, $job_idx) = $test_file =~ /^(.*) \s (\d+) \/ \d+$/x) {
            $db_idx = (crc32($file) + $job_idx) % $DB_CNT;
        } else {
            $db_idx = crc32($test_file) % $DB_CNT;
        }
        if ($DB) {
            $ENV{$Yandex::DBUnitTest::ENV_DB_NAME} = $unit_tests_dbs[$db_idx];
        } else {
            $ENV{UNIT_OPT_DB} = 0;
        }
        if ($test_file =~ /PARALLEL/) {
            return ['/usr/bin/perl', '-MTest::FailWarnings', '-MCarp::AlwaysChain', "-Mmy_inc=$Settings::ROOT", split /\s+/, $test_file ];
        } elsif ($test_file =~ /\.py$/) {
            return ["PYTHONPATH=$Settings::ROOT/python python $test_file"];
        } else {
            return ['/usr/bin/perl', '-MTest::FailWarnings', '-MCarp::AlwaysChain', "-Mmy_inc=$Settings::ROOT", ($cover_arg||()), $test_file ];
        }
    },
    } );


# подозрительного вида хак для совместимости TAP::Harness и utf-ного вывода тестов
# binmode(STDOUT, ':raw');
# binmode(STDERR, ':raw');

my $agg = $harness->runtests(@test_files);

my $status = $agg->has_errors ? 1 : 0;

if ( $canonical_options ){
    my $status_text = $status == 0 ? "OK" : "FAIL";
    # не yash_system, потому что падения не критичны
    system("/usr/local/bin/test-status --set $status_text --type perl-unit-tests $Settings::ROOT");
}


if( # генерируем отчет по покрытию и выкладываем на веб только для беты
    $COVER
    && $Settings::BETA_HOST
    && hostname() =~ /ppcdev(\d+)/
) {
    my $beta_host_number = $1;
    my $out = "$Settings::ROOT/data/coverage_report";
    unlink $out;

    my $url = $Settings::BETA_HOST;
    $url =~ s/beta\d\./beta$beta_host_number\./;
    $url = "https://$url/coverage_report/coverage.html";

    yash_system($COVER_EXEC, '-outputdir', $out, '-silent', $cover_db_dir); # сформировать отчет
    print "You may have your report at $url\n";
}

exit $status;

=head2 wanted_path($path)

    Использовать ли относительный путь $path при отборе тестов?
    Путь может быть как именем файла, так и именем поддиректории.
    Является относительным к $File::Find::dir

    Если были указаны теги для выбора (--only-tag) или фильтрации
    (--skip-tag), то проверка делается на их основе,
    иначе берем все тесты

    Кроме этого файлы отбираются только с расширениями .t или .py

=cut

sub wanted_path {
    my $path = shift;

    if (-d $path) {
        # директории берем все, детально проверим на файлах
        return 1;
    } elsif (-f $path) {
        return 0 unless $path =~ m/\.(?:t|py)$/;
        if (@ONLY_TAGS || @SKIP_TAGS) {
            return check_tags_on_path($path);
        } else {
            return 1;
        }
    };
}

=head2 check_tags_on_path($path)

    Проверить ли относительный путь $path при отборе тестов?
    Путь является относительным к $File::Find::dir

    Как проверяется:
    Если полный путь до файла совпадает (полностью или
    начальной частью) с путями, перечисленными в указанных
    --skip-tags - то файл не проходит
    Если полный путь совпадает (аналогично) с путями,
    перечисленными в --only-tag - то файл подходит
    Если предыдущие две проверки не дали ответа, и --only-tag
    указаны не были - то файл подходит, иначе - нет.

=cut

sub check_tags_on_path {
    my $path = shift;
    my $check_path = path($File::Find::dir, $path);

    # проверяем по черному списку
    for my $skip_tag (@SKIP_TAGS) {
        return 0 if $check_path =~ $TESTS_TAGS{$skip_tag};
    }

    # проверяем по белому списку
    for my $only_tag (@ONLY_TAGS) {
        return 1 if $check_path =~ $TESTS_TAGS{$only_tag};
    }

    # если белый список был и мы дошли до этого места - значит не прошли фильтрацию
    return @ONLY_TAGS ? 0 : 1;
}
