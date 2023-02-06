#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use Digest::CRC qw/crc32/;
use List::MoreUtils qw/before after/; 
use YAML;

use Test::ListFiles;
use Yandex::Shell qw/yash_quote/;

use my_inc '../..';

use Settings;

# пороги придется время от времени подкручивать; добавлять в список новые тяжелые модули
# если будет сильно шуметь -- можно отказаться от проверки времени; оно сильнее портится на загруженной машине
#
# как составлены первичные значения порогов: 
# запущен тест с закрученными дефолтными лимитами, 
# вывод отсортирован по возрастанию реально потребленных ресурсов, 
# взят хвост, для него пороги увеличены примерно в 1.5 раз по памяти
# дефолтный порог -- максимум "до хвоста" * 1.5
# время -- в то же самое с множителем 3
# grep -o 'compiling.*consumed too much resources: mem = [^ ]*' log |sort -k 9nr |head -n 115 >> unit_tests/perl/compile_all_tresholds.yaml
my $tresholds = YAML::LoadFile("$Settings::ROOT/unit_tests/perl/compile_all_tresholds.yaml");
my $default_tresholds = {
    mem  => 350_000,
    time => 9
}; 
# память -- в килобайтах, время -- в секундах
my $units = {
    mem => 'KB',
    time => 's',
};

my ($par_id, $par_level) = (0, 1);
if (@ARGV == 1 && $ARGV[0] =~ /^(\d+)[:\/](\d+)$/) {
    ($par_id, $par_level) = ($1, $2);
} elsif (@ARGV) {
    die "Usage: $0 [par_id/par_level]";
}
$par_id %= $par_level;

# ищем все перловые файлы
my @files = grep {-f && /\.p[lm]$/ && !/\/(t|unit_tests|deploy|one\-shot\/archive|data\/lego\/tools)\// && !/CompileSpeedups/ && !/api\/lib\/API\/PreforkLoad.pm/ && crc32($_) % $par_level == $par_id} Test::ListFiles->list_repository($Settings::ROOT);
# добавляем перл-миграции, нерекурсивно (т.к. deploy/archive проверять не надо)

push @files, grep {-f && /\.p[lm]$/ && crc32($_) % $par_level == $par_id} Test::ListFiles->list_repository("$Settings::ROOT/deploy", depth => 'files');
# с абсолютными путями выдаёт не все варнинги :-(
@files = map {s/^$Settings::ROOT\///; $_} sort @files;

Test::More::plan(tests => 4 * scalar(@files));

for my $file (@files) {
    my @inc;
    if ($file =~ /\.pl$/ && $file !~ m!(^|/)registered/!) {
        # pass
    } elsif ($file =~ m~api/lib/~) {
        @inc = ("$Settings::ROOT/api/services/v5", my_inc::dirs($file));
    } else {
        @inc = my_inc::dirs($file);
    }

    my $cmd = join " ",
        "cd $Settings::ROOT;",
        "time -v $^X -cw -MO=Lint,no-context",
        "-Munit_tests::perl::CompileSpeedups",
        (map {-I => yash_quote($_)} @inc),
        yash_quote($file),
        "2>&1";

    my @raw_out = grep {
        !/syntax OK$/
            && !/Deep recursion on subroutine "B::walkoptree_slow"/

            # модули, загружаемые по требованию через require (для именьшения runtime)
            && !/Nonexistent subroutine '(BerkeleyDB::|CairoGraph::|Yandex::(Svn|Metadata|Queryrec)::|Math::Int64)/

            # нормальный код, который не нравится Lint
            && !(/<>/ && $file =~ /(MRStreaming|\.pl$)/)
            && !(/Subroutine Settings::hostname redefined/)
            && !(/^\s*at \/\S+ line \d+\.$/)
            && !/Nonexistent subroutine 'Campaign::Creator::Types/ # no Mouse::Util::TypeConstraints
            && !/Nonexistent subroutine 'Yandex::ORM::Types::/ # no Mouse::Util::TypeConstraints
            && !/Nonexistent subroutine 'Yandex::ORM::Meta::Attribute::Trait::Column::/
            && !/Nonexistent subroutine 'Yandex::ORM::Meta::Class::Trait::TableSchema::/
            && !/Nonexistent subroutine 'Settings::register_db_names' .*SettingsALL.pm/
            && !/Nonexistent subroutine 'delete_(?:adgroup|campaign|banner)' .*?fix_ids_14nov2014\.pl/

            # косяки, которые стоит со временем поправить
            && !/DoCmd::cmd_retryLater/
            && !/Nonexistent subroutine 'User::get_user_options' .*Client.pm/
            && !/Nonexistent subroutine 'Direct::ReShard::din'.*ReShard.pm/
            && !/Nonexistent subroutine 'chown' called at .*sync_avatars.pl/
            && !/Nonexistent subroutine 'chmod' called at .*sync_avatars.pl/

    } `$cmd`;
    my $err = $?;
    
    my @out = before {/^\s*Command being timed:/} @raw_out;
    my @resources = after {/^\s*Command being timed:/} @raw_out;

    # ошибки и ворнинги компиляции
    ok($err == 0, "compiling $file");
    ok(@out == 0, "warnings $file:\n".join("\n", map {chomp; "    $_"} @out));

    # потребленные ресурсы
    my $consumed = {};
    $consumed->{mem} = (map { /^\s*Maximum resident set size \(kbytes\):\s*([0-9]+)/ ? $1 : () } @resources)[0];
    my $time_user    = (map { /^\s*User time \(seconds\):\s*([0-9\.]+)/              ? $1 : () } @resources)[0];
    my $time_sys     = (map { /^\s*System time \(seconds\):\s*([0-9\.]+)/            ? $1 : () } @resources)[0];
    $consumed->{time} = $time_user + $time_sys;

    # проверяем, что компиляция не заняла слишком много времени и/или памяти
    for my $r ( qw/mem time/ ){
        my $treshold = exists $tresholds->{$file} && exists $tresholds->{$file}->{$r} ? $tresholds->{$file}->{$r} : $default_tresholds->{$r};
        ok( $consumed->{$r} <= $treshold, "compiling $file consumed too much resources: $r = $consumed->{$r} $units->{$r} > treshold $treshold $units->{$r}" . ($r eq 'time' ? " ($consumed->{$r} = $time_user user + $time_sys sys)" : ''));
    }
}

