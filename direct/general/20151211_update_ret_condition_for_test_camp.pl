#!/usr/bin/perl

=head1 DEPLOY

# approved by lena-san
# .migr
{
    type => 'script',
    when => 'after',
    time_estimate => '1 секунда devtest',
    comment => "Для тестовой РК меняем значение условия ретаргетинга, который невозможно поменять в интерфейсе",
}

=cut

use Direct::Modern;

use my_inc '..';

use Settings;
use ScriptHelper;
use JSON;

use Yandex::DBTools;

$log->out('START');

my $condition_json = get_one_field_sql(PPC(shard => 1), "SELECT condition_json FROM retargeting_conditions WHERE ret_cond_id=4");

my $conditions = from_json($condition_json);

$_->{type} = 'not' foreach (@$conditions);

do_update_table(PPC(shard=>1), "retargeting_conditions", {condition_json=>to_json($conditions)}, where => {ret_cond_id=>4});

$log->out('FINISH');
