#!/usr/bin/perl 

=head2 DESCRIPTION

    В правилах решардинга могут существовать записи с dont_delete => 1.
    Это означает, что в этом правиле данные только копируются (а удалиться должны позже другим правилом).
    Это нужно, потому что для репликатора данных для нового интерфейса в YT нужен другой порядок вставки записей: DIRECT-124672
    Этот тест проверяет, что для правил с dont_delete существуюет соответствующее правило, по которому данные удалятся.

=cut

use Direct::Modern;

use Test::More;

use Yandex::Test::UTF8Builder;
use Yandex::ListUtils qw/enumerate_iter/;

use Direct::ReShard::Rules qw/@RULES/;

my $it = enumerate_iter(\@RULES);
my %postponed_deletion_tables;

while (my ($idx, $rule) = $it->()) {
    for my $table_name ( @{ $rule->{tables} } ) {
        if ($rule->{dont_delete}) {
            $postponed_deletion_tables{$table_name} = undef;
        } else {
            delete $postponed_deletion_tables{$table_name};
        }
    }
}

diag('Нет правила с удалением данных для таблиц: ' . join(', ', sort keys %postponed_deletion_tables)) if %postponed_deletion_tables;
ok(!%postponed_deletion_tables, 'Для таблиц в правилах с dont_delete должно существовать последующее правило без dont_delete');

done_testing();

