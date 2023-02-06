#!/usr/bin/perl

=head1 

    Тест проверяет состав каталога packages/yandex-direct/debian. 
    Разрешаются файлы двух видов: 
      * вообще нужные файлы (список @common_files)
      * файлы, относящиеся к конкретным пакетам (имя пакета с одним из допустимых суффиксов)
        список пакетов извлекается из файла control, 
        список допустимых суффиксов -- в @package_suffixes

=cut

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;

my @common_files = qw/
    compat
    control
    rules
/;

my @package_suffixes = qw/
    .cron.d
    .dirs
    .bash_completion.d
    .environment

    .preinst
    .postinst

    .prerm
    .postrm
/;

my $PKG_ROOT = "$Settings::ROOT/packages/yandex-direct/debian";

my $control = read_file("$PKG_ROOT/control");
my @packages = ($control =~ /^Package:\s+(\S+)\s*$/gm);

# список имен файлов (относительно $PKG_ROOT)
my @files = sort grep {-f} Test::ListFiles->list_repository($PKG_ROOT);
s!^\Q$PKG_ROOT\E/!! for @files;

# фрагменты регвыр для проверки имен файлов
my $common_files_regexp = join "|", map {"\Q$_\E"} @common_files;
my $packages_regexp = join "|", map {"\Q$_\E"} @packages;
my $suffix_regexp = join "|", map {"\Q$_\E"} @package_suffixes;

Test::More::plan(tests => scalar(@files));

for my $file (@files) {
    # проверяем: файл или "вообще нужный", или имя имеет вид "существующий пакет"."допустимый суффикс"
    ok( $file =~ /^$common_files_regexp$/ || $file =~ /^($packages_regexp)($suffix_regexp)$/, "file $file doesn't belong to any package" );
}
