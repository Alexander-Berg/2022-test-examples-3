#!/usr/bin/perl 

=head2 DESCRIPTION

    Юнит-тест проверяет, что правила для таблиц не дублируются, кроме случаев
    описания в соседних правилах, состоящих из одной таблицы.

    Если одна и та же таблица копируется несколькими правилами - проверяем,
    что правила совпадают, за исключением "источника данных" и ключей по которым
    производится получение данных

    Если тест начнет падать - стоит внимательно подумать, почему для одной
    и той же таблицы часть данных должна переезжать по-другому

=cut

use Direct::Modern;

use Test::More;
use Test::Deep;

use Yandex::Test::UTF8Builder;
use Yandex::ListUtils qw/enumerate_iter/;

use Direct::ReShard::Rules qw/@RULES/;

my $it = enumerate_iter(\@RULES);
my %rules_for_check_by_table_name;
my %seen_tables;
my %postponed_deletion_tables;
my $prev_table_name = '';
my $prev_table_rule_idx = -1;

while (my ($idx, $rule) = $it->()) {
    for my $table_name ( @{ $rule->{tables} } ) {

        SKIP: {
            if ($table_name eq $prev_table_name && $prev_table_rule_idx != $idx) {
                skip("Разрешено использовать несколько правил решардинга для одной таблицы ($table_name), если это соседние правила, состоящие только из одной таблицы", 1);
            }

            ok(!exists $seen_tables{$table_name} || exists $postponed_deletion_tables{$table_name}, "Таблица $table_name не используется повторно в правилах решардинга, или не удаляется первым правилом");
        }

        if ($rule->{dont_delete}) {
            $postponed_deletion_tables{$table_name} = undef;
        } else {
            delete $postponed_deletion_tables{$table_name};
        }
        $seen_tables{$table_name} = undef;
        $prev_table_rule_idx = $idx;
        $prev_table_name = $table_name;
    }



    next if @{ $rule->{tables} } > 1;
    push @{ $rules_for_check_by_table_name{ $rule->{tables}->[0] } }, $rule;
}

my @skip_keys = qw/key key_type tables from/;

for my $table_name (keys %rules_for_check_by_table_name) {
    my $rules = $rules_for_check_by_table_name{$table_name};
    my $first_rule = shift @$rules;
    delete @{$first_rule}{@skip_keys};
    for my $rule_for_compare (@$rules) {
        delete @{$rule_for_compare}{@skip_keys};
        if ($first_rule->{dont_delete} && ($rule_for_compare->{only_delete} || $rule_for_compare->{replace})) {
            delete @{$first_rule}{qw/dont_delete replace/};
            delete @{$rule_for_compare}{qw/only_delete replace/};
        }
        cmp_deeply($first_rule, $rule_for_compare, "Правила решардинга для таблицы $table_name имеют идентичные параметры");
    }
}

done_testing();

