#!/usr/bin/perl

# $Id$

=pod

     Для всех таблиц, описанных в db_schema (.schema.sql) должен быть файл
     с описанием (.text), который успешно проходит парсинг.
     В описаниях должны быть указаны все столбцы.

=cut

use warnings;
use strict;

use File::Slurp;
use Test::More;
use Test::Deep;

use Yandex::HashUtils;
use Yandex::DBSchema;
use Test::ListFiles;
use Settings;

use utf8;
use Yandex::Test::UTF8Builder;

my @TABLES_WITHOUT_DOCS = (
    # Если хочешь добавить исключение -- не надо! Сгенерируй и закоммить шаблон для описания -- это лучше, чем отсутствие файла
    # шаблон для описания можно сгенерировать скриптом: 
    # cat db_schema/ppc/my_table.schema.sql |./unit_tests/db_schema/table_doc_template.pl > db_schema/ppc/my_table.text
);

# где лежит db_schema
my $SCHEMA_ROOT = "$Settings::ROOT/db_schema";

# список таблиц, у которых нет ПОКА описания, или оно ПОКА неверное
my %KNOWN_PROBLEMS = map {s/\s//g; $_ => 1} grep {!/^\s*$/} @TABLES_WITHOUT_DOCS;

# базы данных, для которых документации ПОКА нет
my %SKIP_DB = ();

# составляем список: все новые файлы, за исключением исключений
my @schema_files = grep {-f && /\.schema.sql$/} Test::ListFiles->list_repository($SCHEMA_ROOT);

my @wrong_exceptions;
for my $schema_file (@schema_files) {
    (my $db_name = $schema_file) =~ s/ .*? ([^\/]+) \/ ([^\/]+) $/$1/x;

    next if $SKIP_DB{$db_name};

    (my $table_name = $schema_file) =~ s/.*?([^\/\.]+)\.schema\.sql$/$1/;
    (my $text_file = $schema_file) =~ s/\.schema\.sql$/.text/;

    if (!-f $text_file) {
        if ($KNOWN_PROBLEMS{$table_name}) {
            pass("passed test for $table_name - skip listed as known problem");
            $KNOWN_PROBLEMS{$table_name} = 2; # запоминаем, что исключение срабатывало (меняем 1 -> 2)
            next;
        }
        fail("no text file for $schema_file (expecting: $text_file)");
        next;
    } elsif ($KNOWN_PROBLEMS{$table_name}) {
        # для исключения есть описание, пора удалить таблицу из исключений
        push @wrong_exceptions, $table_name;
        $KNOWN_PROBLEMS{$table_name} = 2; # но исключение при этом сработало
    }

    # если уж описание появилось - оно должно парситься и содержать все столбцы из схемы
    my $text_file_content = Yandex::DBSchema::get_table_text_desc(db => $db_name, table => $table_name);
    my $table_desc = Yandex::DBSchema::parse_table_text_desc($text_file_content);
    ok(defined $table_desc, "Parsing $text_file") or diag($@);
    my @text_columns = map { $_->{name} } @{$table_desc->{columns}};
    my @schema_columns = map {/^\s*`(\S+)`/ ? ($1) : ()} read_file($schema_file);
    #print Dumper \@text_columns, \@schema_columns;
    cmp_deeply(\@text_columns, bag(@schema_columns), "Columns of $schema_file / $text_file");
}

# проверяем актуальность списка исключений: для них должна быть схема, но не должно быть описания
cmp_bag(\@wrong_exceptions, [], 'Исключённые из проверки таблицы для которых есть описание');
my $unused_problems = hash_grep {$_ == 1} \%KNOWN_PROBLEMS;
my @wrong_excludes = keys %$unused_problems;
cmp_bag(\@wrong_excludes, [], 'Исключения для несуществующих таблиц');

done_testing;

