#!/usr/bin/perl 

=head2 DESCRIPTION

    В правилах решардинга могут существовать записи с only_delete => 1.
    Это может быть нужно, если данные уже вставлены в правиле с dont_delete, а повторять вставку с replace по каким-то причинам нельзя.
    Этот тест проверяет, что для правил с only_delete есть предшествующее правило, в котором данные копируются.

=cut

use Direct::Modern;

use Test::More;

use Yandex::Test::UTF8Builder;
use Yandex::ListUtils qw/enumerate_iter/;

use Direct::ReShard::Rules qw/@RULES/;

my $it = enumerate_iter(\@RULES);
my %seen_tables;

while (my ($idx, $rule) = $it->()) {
    for my $table_name ( @{ $rule->{tables} } ) {
        if ($rule->{only_delete}) {
            ok(exists $seen_tables{$table_name}, "Для таблицы $table_name в правиле с only_delete должно существовать предшествующее правило, копирующее данные");
        }
        $seen_tables{$table_name} = undef;
    }
}

done_testing();

