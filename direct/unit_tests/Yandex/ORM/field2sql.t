#!/usr/bin/perl

use Direct::Modern;
use Test::More;

use_ok('Yandex::ORM::Model::Base');

package Direct::Model::TestModel {
    use Mouse;
    extends 'Yandex::ORM::Model::Base';
    __PACKAGE__->_setup(
        default_table => 'test',
        fields => [
            id   => { type => 'Int', primary_key => 1, column => 'test_id' },
            name => { type => 'Str', column => 'test_name' },
        ],
    );
}

is(Direct::Model::TestModel->field2sql('id'), 'test_id');
is(Direct::Model::TestModel->field2sql('name'), 'test_name');
is(Direct::Model::TestModel->field2sql('unknown'), undef);

done_testing();
