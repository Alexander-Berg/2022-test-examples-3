#!/usr/bin/perl

=pod

  Постулаты:
    - все целочисленные идентификаторы должны быть беззнаковыми
    - все флаги (is_*) должны быть беззнаковыми
    - все хеши (*hash*) должны быть беззнаковыми
    - все численные колонки, которые не идентификаторы, должны быть знаковыми

    - enum должен иметь явный дефолт либо быть nullable (и, значит, неявный defautl null). Подробности: https://wiki.yandex-team.ru/Direct/CodeStyleGuide/DB/#enum

    - все внешние ключи должны иметь тип, совпадающий с исходным типом

=cut

use warnings;
use strict;

use File::Slurp;
use Test::More;
use FindBin qw/$Bin/;

use Yandex::DBSchema;

use Test::ListFiles;
use Settings;

use utf8;

my (%int_mods, %skip_fk, %skip_enum_defaults);
my %exceptions;

init();

my %tbl_info;
for my $schema_file (grep {-f && /\.schema.sql$/} Test::ListFiles->list_repository($Yandex::DBSchema::DB_SCHEMA_ROOT)) {
    my ($db, $table) = $schema_file =~ / ([^\/]+) \/+ ([^\/]+)\.schema\.sql $ /x;

    for my $line (read_file($schema_file)) {
        $line =~ s/[,\s]+$//;
        if ($line =~ /^\s*`(\w+)`\s*(\S+(?:\s+unsigned)?)(?:\s+(.*)\s*$)?/i) {
            # определение колонки
            $tbl_info{"$db.$table"}{cols}{$1}{type} = $2;
            $tbl_info{"$db.$table"}{cols}{$1}{modificators} = $3;
        } elsif ( $line =~ /^\s*(?:PRIMARY |UNIQUE )?KEY.*?\((.*)\)/i ){
            # индекс
            my $key_definition = $1;
            my @key_columns = map { s/(^`|`$)//gr } split /,\s*/, $key_definition;
            push @{$tbl_info{"$db.$table"}{keys}}, \@key_columns;
        } elsif ( $line =~ /^\s*CONSTRAINT .*FOREIGN KEY\s*\(\`(.*?)\`\) REFERENCES \`(.*?)\` \(\`(.*?)\`\)/ ) {
            # mysql-ный внешний ключ
            # CONSTRAINT `manager_hierarchy_ibfk_1` FOREIGN KEY (`manager_client_id`) REFERENCES `clients` (`ClientID`) 
            $tbl_info{"$db.$table"}{fk_mysql}{$1} = "$2.$3";
        }
    }

    my $text_file = $schema_file =~ s/\.schema\.sql$/.text/r;
    if (-f $text_file) {
        $tbl_info{"$db.$table"}{fk} = {
            scalar(read_file($text_file)) =~ /\n(\w+)\s*\n:\s+FK\(([\w\.]+)\)/g
        };
    }
}

=for none

теперь в %tbl_info лежит такая структура:

  (
    'ppc.agency_client_relations' => {
        'fk' => {
            'agency_client_id' => 'clients.ClientID'
        },
        'keys' => [
            [ 'agency_client_id', 'client_client_id' ],
            [ 'client_client_id' ] 
        ],
        'cols' => {
            'client_description' => {
                'modificators' => undef,
                'type' => 'text'
            },
            'agency_client_id' => { 
                'modificators' => "NOT NULL DEFAULT '0'",
                'type' => 'int(11)'
            },
            'client_archived' => {
                'type' => "enum('No','Yes')",
                'modificators' => "NOT NULL DEFAULT 'No'"
            },
            ...
        }
    },
    ...
  )

=cut

for my $tbl (sort keys %tbl_info) {
    my $db = $tbl =~ s/\..*//r;
    my $tinfo = $tbl_info{$tbl};
    for my $col (sort keys %{$tinfo->{cols}}) {
        my $col_type = $tinfo->{cols}->{$col}->{type};
        my $col_modificators = $tinfo->{cols}->{$col}->{modificators} // '';

        # проверки про типы колонок
        if ($col_type =~ /^\w*int/) {
            # 1. int-ы
            my $is_bigint = $col_type =~ /^bigint/i;
            # id-шники, флаги (is_..., has_) и хеши -- беззнаковые, остальное -- знаковое, за исключением исключений
            my $autodetected_int_mod =    ( $is_bigint ? 'signed' : undef )
                                       // ( $col =~ /^(has|is)_/i ? 'unsigned' : undef )
                                       // ( $col =~ /id$/i ? 'unsigned' : undef )
                                       // ( $col =~ /client_id/i ? 'unsigned' : undef )
                                       // ( $col =~ /hash/ ? 'unsigned' : 'signed' );
            my $expected_int_mod;
            if (exists $int_mods{"$tbl.$col"}) {
                $expected_int_mod = delete $int_mods{"$tbl.$col"};
                isnt($expected_int_mod, $autodetected_int_mod, "int modificator exception is needed for $tbl.$col");
            } else {
                $expected_int_mod = $autodetected_int_mod;
            }
            my $int_mod = $col_type =~ /unsigned/i ? 'unsigned' : 'signed';
            my $test_name = "int modificator for $tbl.$col";
            if ($is_bigint) {
                $test_name .= ' (unsigned int64 not supported in Java, Lua, ...)';
            }
            $test_name .= ' exceptions: unit_tests/db_schema/unsigned_ids.exceptions.unsigned.data';
            is($int_mod, $expected_int_mod, $test_name);
        } elsif ( $col_type =~ /^enum\b/ ){
            # 2. enum-ы
            my $has_proper_default = $col_modificators !~ /\bnot null\b/i || $col_modificators =~ /\bdefault\b/i;
            if (delete $skip_enum_defaults{"$tbl.$col"}) {
                ok(!$has_proper_default, "do you really need skip-enum-defaults for $tbl.$col?");
            } else {
                # У enum ожидаем явный дефолт либо nullable (и, значит, неявный defautl null)
                ok($has_proper_default, "enum expected to have default or to accept null: $tbl.$col ($col_type $col_modificators)");
            }
        }

        # FK из mysql должны быть описаны и в документации (из mysql можем удалить по соображениям производительности или др., в док-ции останутся)
        if (my $fk_mysql = $tinfo->{fk_mysql}->{$col}){
            my $fk_doc  = $tinfo->{fk}->{$col} // '';
            if ( delete $exceptions{fk_documented}{"$tbl.$col"} ){
                isnt( $fk_doc, $fk_mysql, "no useless exceptions in fk_documented ($tbl.$col)" );
            } else {
                is( $fk_doc, $fk_mysql, "foreign key from mysql should be documented ($tbl.$col)" );
            }
        }

        # проверки про FK из документации
        if (my $fk = $tinfo->{fk}->{$col}) {
            my @ref = split /\./, $fk;
            my ($ref_db, $ref_tbl, $ref_col) = @ref == 3 ? @ref : ($db, @ref);

            # 1. колонка, на которую ссылаемся, должна сущестовать
            ok( exists $tbl_info{"$ref_db.$ref_tbl"}{cols}{$ref_col}, "column $ref_db.$ref_tbl.$ref_col referenced from $tbl.$col must exist" );

            # 2. fk должны быть проиндексированы
            my ($has_index_prefix, $has_composite_index);
            for my $k ( @{$tinfo->{keys}} ){
                if ( $k->[0] eq $col ){
                    $has_index_prefix = 1; 
                    last;
                } elsif ( grep {$_ eq $col} @$k ) {
                    $has_composite_index = 1;
                    # не last, потому что может еще найдем индекс получше
                }
            }
            my $formal_fk = exists $tinfo->{fk_mysql}->{$col};
            if ( delete $exceptions{fk_indexed}->{"$tbl.$col"} ) {
                ok( !($has_index_prefix || $has_composite_index || $formal_fk), "index for fk $tbl.$col (do you really need an exception in unsigned_ids.exceptions.fk-indexed.data?)" );
            } else {
                ok( $has_index_prefix || $has_composite_index || $formal_fk, "fk $tbl.$col needs an index (exceptions: unsigned_ids.exceptions.fk-indexed.data)" );
            }

            # 3. тип ссылки и поля, на которое ссылаемся, должны совпадать 
            my $ref_type = $tbl_info{"$ref_db.$ref_tbl"}{cols}{$ref_col}->{type};
            if (delete $skip_fk{"$tbl.$col"}) {
              isnt($col_type, $ref_type, "fk type for $tbl.$col -> $fk (do you really need this exception in unsigned_ids.exceptions.fk.data?)");
            } else {
              # Игнорируем отличия в длине для целочисленных типов: int(10) == int(11)
              if (defined $col_type && defined $ref_type && $col_type ne $ref_type) {
                my $stripped_col_type = $col_type;
                my $stripped_ref_type = $ref_type;

                $stripped_col_type =~ s/^(\w*int)\(\d+\)/$1/;
                $stripped_ref_type =~ s/^(\w*int)\(\d+\)/$1/;

                if ($stripped_col_type eq $stripped_ref_type) {
                    $col_type = $stripped_col_type;
                    $ref_type = $stripped_ref_type;
                }
              }
              is($col_type, $ref_type, "fk type for $tbl.$col -> $fk");
            }
        }
    }
}

my $left_int_mods_str = join(', ', sort keys %int_mods);
is($left_int_mods_str, '', 'no int_mods for unexisting fields');

my $left_skip_fk_str = join(', ', sort keys %skip_fk);
is($left_skip_fk_str, '', 'no skip_fk for unexisting fields');

my $left_skip_enum_defaults_str = join(', ', sort keys %skip_enum_defaults);
is($left_skip_enum_defaults_str, '', 'no skip_enum_defaults for unexisting fields');

for my $t (sort keys %exceptions) {
    my $left = join(', ', sort keys %{$exceptions{$t}});
    is($left, '', "no excetions '$t' for for unexisting fields");
}

done_testing;


sub init {
$int_mods{$_} = 'signed' for @{list_from_file("$Bin/unsigned_ids.exceptions.signed.data")};

$int_mods{$_} = 'unsigned' for @{list_from_file("$Bin/unsigned_ids.exceptions.unsigned.data")};

$skip_fk{$_} = 1 for @{list_from_file("$Bin/unsigned_ids.exceptions.fk.data")};

$skip_enum_defaults{$_} = 1 for @{list_from_file("$Bin/unsigned_ids.exceptions.enum-defaults.data")};

$exceptions{fk_indexed}{$_} = 1 for @{list_from_file("$Bin/unsigned_ids.exceptions.fk-indexed.data")};

$exceptions{fk_documented}{$_} = 1 for @{list_from_file("$Bin/unsigned_ids.exceptions.fk-documented.data")};

}

=head2

    Читает файл, разбивает по пробелам, возвращает ссылку на массив получившихся строк
    От решетки # до конца строки -- комментарий, игнорируется

=cut
sub list_from_file
{
    my ($filename) = @_;
    my @res;
    open(my $fh, "<", $filename);
    while (my $l = <$fh>){
        chomp($l);
        $l =~ s/#.*$//;
        next if $l =~ /^\s*$/;
        my @values = split /\s+/, $l;
        push @res, @values;
    }
    return \@res;
}
