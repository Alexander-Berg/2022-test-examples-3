#!/usr/bin/perl

=pod

    Проверка консистентности существования и использования скриптов: 
    если есть скрипт в protected (но не в one-shot/maintenance) -- он должен вызываться в каком-нибудь crontab'е.
    если скрипт вызывается в каком-нибудь crontab'е -- он должен существовать (в т.ч., возможно, и в maintenance)

    Скрипты, которые есть в protected, но которые не используются, следует добавлять в @KNOWN_UNUSED_SCRIPTS, иначе тест не будет пройдет.

    Дополнительно проверяется корректность секций METADATA в скриптах: нормально извлекается, содержит ссылки на корректные пакеты. 

    Погрешность: сравниваются только имена скриптов, без каталогов. 
    Т.е. если вызывается protected/maintenance/my_script.pl, но он не существует, а есть protected/my_script.pl -- тест ничего не отловит. 

=cut

# $Id$

use warnings;
use strict;

use File::Slurp;
use File::Basename qw/basename/;
use Test::More;

use Test::ListFiles;

use Settings;

use utf8;
use open ':std' => ':utf8';

#..........................................................................................................

# Списки исключений. Чем короче списки -- тем лучше.  

# скрипты, которые не используются, и мы про это знаем
my @KNOWN_UNUSED_SCRIPTS = (
    '__init__.py',
    'startup2.pl',
    'warmup_geotools.pl',

    'checkModerate.pl',       # можно перенести в maintenance

    'mk_regions.pl', # вызывается вручную

    'SubscribeApiDeveloper.pl', # DIRECT-54229

    # источник YaCatalog умер; модуль зафиксировали и запакетировали, см. DIRECT-68876
    'get_yacatalog.pl',

    'ppcOtherScriptsMonruns.pl', # скрипт только для метаданных, не предполагает, что будет что-то запускаться
);

# скрипты, которые не находятся в protected, и это нормально
my @KNOWN_NONEXISTENT_SCRIPTS = (
    'httplogarc-direct.pl', # лежит в maintenance, называется httplogarc.pl
    'log_arc.pl', # ставится отдельным пакетом
    'nginx_soap_update_geo_conf.pl', # копируется от nginx_update_geo_conf.pl при установке пакета
    'test_alive.pl', # ставится отдельным пакетом
    'check_db_schema.pl', # ставится отдельным пакетом 
    'refreshStopWords.pl', # ставится отдельным пакетом
    'update-translations-auto.pl', # ставится отдельным пакетом
);

# Конец исключений

#..........................................................................................................

# Какие файлы считаем скриптами, регвыр на окончание имени файла
# '$' в конце не нужен -- при поиске файлов и так добавим, а при поиске вызовов скриптов из кронтабов вреден
my @SCRIPT_FILE_REGEXP = qw/
    \.pl 
    \.py 
    \.sh 
/;

# каталоги, где искать шаблоны и perl-код
my $SCRIPTS_ROOTS = ["$Settings::ROOT/protected", "$Settings::ROOT/python/scripts"];
my $CRONTABS_ROOTS = ["$Settings::ROOT/packages/yandex-direct/debian"];

# каталоги, скрипты из которых не обязательно должны запускаться из кронтабов
my $IGNORE_DIRS = [
    "protected/maintenance", 
    "protected/one-shot", 
    "protected/prebuild", 
    "protected/API/Samples", 
];

#..........................................................................................................

# переделываем списки в более удобные форматы
my $ignore_dirs_regexp = "^(?:".join("|", map { "$Settings::ROOT/$_" } @$IGNORE_DIRS).")";
my $script_filename_regexp = "([^/]+(?:".join("|", @SCRIPT_FILE_REGEXP)."))";
my $known_unused_scripts_regexp = "^(?:".join("|", @KNOWN_UNUSED_SCRIPTS).")\$";
my $known_nonexistent_scripts_regexp = "^(?:".join("|", @KNOWN_NONEXISTENT_SCRIPTS).")\$";

#..........................................................................................................

# смотрим, какие скрипты существуют
my %FULL_PATH;
my %SCRIPT_EXISTS;
my %SCRIPT_SHOULD_BE_USED;
for my $file (grep {-f && m/$script_filename_regexp$/ } Test::ListFiles->list_repository($SCRIPTS_ROOTS)) {
    my $basename = basename($file);
    $SCRIPT_EXISTS{$basename} = 1;
    push @{$FULL_PATH{$basename}}, $file;
    if( $file !~ $ignore_dirs_regexp ){
        $SCRIPT_SHOULD_BE_USED{$basename} = 1; 
    }
}

# смотрим, какие есть кронтабы
my @crontabs;
for my $file (grep {-f && /\.cron\.d$/} Test::ListFiles->list_repository($CRONTABS_ROOTS)) {
    push @crontabs, $file;
}

# смотрим, какие скрипты вызываются в предопределенных кронтабах
# USED_FROM_CRONTAB: (скрипт => [список файлов, в которых он используется] )
my %USED_FROM_CRONTAB;
for my $file (@crontabs){
    my @crontab_lines = read_file($file, binmode => ':utf8');
    s/#.*// for @crontab_lines;

    my @used_list;
    for my $line (@crontab_lines){
        push @used_list, $line =~ m!$script_filename_regexp\b!g;
    }

    # оставляем только имя файла, без каталогов
    s!^.*/!! for @used_list;

    push @{$USED_FROM_CRONTAB{$_}}, $file for @used_list;
}

# смотрим, для каких скриптов кронтабы генерируются из METADATA
# TODO передавать /usr/local/bin в PATH для юнит-тестов
my $cmd = "/usr/local/bin/pod2crontab.pl  --project-root $Settings::ROOT --scripts-path $Settings::ROOT/protected"
    . " --scripts-path $Settings::ROOT/python/scripts"
    . " --default-user ppc"
    . " --crontab-path $Settings::ROOT/fake --crontab-name '[%package%]-auto'"
    . " --scripts-path-prefix /var/www/ppc.yandex.ru --package-prefix yandex-direct"
    . " --default-mailto 'ppc-admin\@yandex-team.ru' --mailto yandex-direct-conf-test='direct-test-cron\@yandex-team.ru'"
    . " --ps --pp -n";
my $out = qx!$cmd!;
die if $?;
push @{$USED_FROM_CRONTAB{$_}}, "METADATA" 
    for 
    grep { $_ } 
    map { s!.*/!! ; $_}
    map { s/.* ://; $_ } 
    grep { /^script/ }
    split /\n+/, $out;

my @packages_from_metadata =  
    grep { $_ } 
    map { s/.*: //; $_ } 
    grep { /^package/ }
    split /\n+/, $out;

my $control_file = read_file("$Settings::ROOT/packages/yandex-direct/debian/control");
my @correct_packages = ($control_file =~ /^Package:\s+(\S+)\s*$/gm);

# Сколько будет тестов: 
#   + для каждого существующего файла проверим, что используется 
#   + для каждого используемого -- что существует
#   + для каждого пакета, упомянутого в METADATA -- что он есть в control
Test::More::plan(tests => scalar(keys %USED_FROM_CRONTAB) + scalar(keys %SCRIPT_SHOULD_BE_USED) + scalar(@packages_from_metadata) + 2*scalar(@KNOWN_UNUSED_SCRIPTS));

#   для каждого существующего файла проверим, что используется 
for my $existent_file (keys %SCRIPT_SHOULD_BE_USED){
    my $ok = $USED_FROM_CRONTAB{$existent_file} || $existent_file =~ /$known_unused_scripts_regexp/;
    ok($ok, "file $existent_file may be unused (full path: ".join(', ', @{$FULL_PATH{$existent_file}}).")");
}

#   для каждого используемого -- что существует
for my $used_file (keys %USED_FROM_CRONTAB){
    my $ok = $SCRIPT_EXISTS{$used_file} || $used_file =~ /$known_nonexistent_scripts_regexp/;
    ok($ok, "$used_file seems to be nonexistent but used in ".join(", ", @{$USED_FROM_CRONTAB{$used_file}}));
}

#   для каждого исключения про неиспользуемый скрипт -- что существует и не используется
for my $unused_script (@KNOWN_UNUSED_SCRIPTS) {
    ok($SCRIPT_EXISTS{$unused_script}, "$unused_script seems to be nonexistent but skipped IN \@KNOWN_UNUSED_SCRIPTS");
    ok(!$USED_FROM_CRONTAB{$unused_script}, "$unused_script seems to be used but skipped as unused IN \@KNOWN_UNUSED_SCRIPTS");
}

#   для каждого пакета, упомянутого в METADATA -- что он есть в control
my $packages_regexp = join "|", map {"\Q$_\E"} @correct_packages;
for my $p (@packages_from_metadata){
    ok( $p =~ /^($packages_regexp)$/, "$p: every package from METADATA should correspond to something from control file" );
}
