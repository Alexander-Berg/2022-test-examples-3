#!/usr/bin/perl -ln 

# скрипт превращает запрос create table в шаблон описания таблицы
# структуру таблицы читает с stdin, скелет описания пишет на stdout
# использовать можно так (из корня рабочей копии): 
# cat db_schema/ppc/my_table.schema.sql |./unit_tests/db_schema/table_doc_template.pl > db_schema/ppc/my_table.text

use strict;
use warnings;

# название таблицы
if ( /CREATE +TABLE +\`?([^\`]+)\`?/i ){
    print $1; 
    print "-" x (length ($1)); 
    print "\nTODO: описание таблицы (см. рекомендации в https://wiki.yandex-team.ru/direct/development/howto/db-documentation\n\n### Столбцы: ###\n";
} 

# определение колонки
if ( /^ *\`(.*)\`/ ){
    print "$1\n:   TODO: описание колонки\n";
}

