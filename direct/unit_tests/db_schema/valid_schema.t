#!/usr/bin/perl

=head1 DESCRIPTION

    Тест проверяет файлы из каталога db_schema:
      * в каждом файле .schema.sql должен быть валидный запрос для создания таблицы;
      * create table в файле должен добуквенно совпадать с тем, что покажет show create table;

    Проверяем так: для каждого файла создаем таблицу, сравниваем что получилось с файлом.

=cut

use Direct::Modern;
use Test::More;
use Test::Exception;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Test::ListFiles;
use Settings;
use Path::Tiny;
use Text::Diff;

use utf8;
use open ':std' => ':utf8';


# Исключения. Пополняться не должны. С этими -- разобраться и удалить.
my %EXCEPT = map {$_ => 1}(
    'db_schema/fakebalance/clients.schema.sql',
    'db_schema/fakebalance/payments.schema.sql',
    'db_schema/fakebalance/reps.schema.sql',
    'db_schema/fakebalance/requests.schema.sql',
    'db_schema/fakeblackbox/users.schema.sql',
);

my @files = grep { /\.schema\.sql$/ } Test::ListFiles->list_repository("$Settings::ROOT/db_schema/");

do_sql(UT, 'set foreign_key_checks = 0');
for my $file (@files) {
    my $file_rel = $file =~ s!^\Q$Settings::ROOT/\E!!r;
    next if $EXCEPT{$file_rel};
    my $schema_from_file = path($file)->slurp;
    my $sql = $schema_from_file =~ s!^CREATE\s+TABLE!CREATE TABLE IF NOT EXISTS!r;
    local $SIG{__WARN__} = sub {};
    # 1. Таблица создается
    lives_ok { do_sql(UT, $sql) } "error in $file";
    my ($table_name) = ($file =~ m!.*/([^/]+)\.schema.sql!);
    die "bad table name $table_name" unless $table_name =~ /^[\w\.]+$/; 
    my $schema_from_db = get_one_line_sql(UT, "show create table $table_name")->{'Create Table'};

    $schema_from_db =~ s/\s+$//gsm;
    $schema_from_file =~ s/\s+$//gsm;
    $schema_from_file =~ s! +\Q/* 5.5 binary format */\E +! !g;
    # 2. Файл совпадает с show create table
    is($schema_from_file, $schema_from_db, "в файле $file_rel должен храниться добуквенно точный show create table для таблицы $table_name, diff: ".diff(\$schema_from_db, \$schema_from_file));
    
    do_sql(UT, "drop table $table_name");
}

done_testing();
