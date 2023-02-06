#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Exception;

use lib::abs '.';

{
    no warnings 'once';
    $Direct::Errors::NO_HASH_TAG = 1;
}

throws_ok {
    require TestErrors::Module1;
} qr/error ReqField already defined in module TestErrors::Module1/;

throws_ok {
    require TestErrors::Module2;
} qr/error MaxLength has the same code that error ReqField in module TestErrors::Module2/;

throws_ok {
    require TestErrors::Module3;
} qr/function error_MaxLength already defined in module TestErrors::Module3/;

lives_ok {
    package pkg1;
    require TestErrors::Module4;
    TestErrors::Module4->import();
};

lives_ok {
    package pkg2;
    require TestErrors::Module5;
    TestErrors::Module5->import();
};

throws_ok {
    package pkg3;
    require TestErrors::Module4;
    TestErrors::Module4->import();
    require TestErrors::Module5;
    TestErrors::Module5->import();
} qr/package pkg3: error MaxLength from module TestErrors::Module5 already defined in module TestErrors::Module4/;

throws_ok {
    package pkg4;
    require TestErrors::Module5;
    TestErrors::Module5->import();
    require TestErrors::Module6;
    TestErrors::Module6->import();
} qr/package pkg4: error BadRequest from module TestErrors::Module6 has the same code \(55\) as that error NoPhraseId in module TestErrors::Module5/;

throws_ok {
    package pkg5;
    require TestErrors::GoodModule;
    TestErrors::GoodModule->import();
    sub error_BadRequest {}
} qr/function error_BadRequest already defined in package pkg5/;

throws_ok {
    require TestErrors::Module7;
} qr/text_code R_Field has underscore/;


BEGIN {use_ok('TestErrors::GoodModule')};


my $error_req_field = error_ReqField();
is($error_req_field->code, 10);
is($error_req_field->is_error, 1);
is($error_req_field->is_warning, '');
is($error_req_field->name, 'ReqField');
is($error_req_field->text, 'The field is required');

$error_req_field = error_ReqField('Group name is required');
is($error_req_field->code, 10);
is($error_req_field->is_error, 1);
is($error_req_field->is_warning, '');
is($error_req_field->name, 'ReqField');
is($error_req_field->description, 'Group name is required');

$error_req_field = error_ReqField("It's description");
is($error_req_field->code, 10);
is($error_req_field->is_error, 1);
is($error_req_field->is_warning, '');
is($error_req_field->name, 'ReqField');
is($error_req_field->description, "It's description");

my $warnign_req_field = warning_ReqField();
is($warnign_req_field->code, 10);
is($warnign_req_field->is_error, '');
is($warnign_req_field->is_warning, 1);
is($warnign_req_field->name, 'ReqField');
is($warnign_req_field->text, '');

$warnign_req_field = warning_ReqField('Another description');
is($warnign_req_field->code, 10);
is($warnign_req_field->is_error, '');
is($warnign_req_field->is_warning, 1);
is($warnign_req_field->name, 'ReqField');
is($warnign_req_field->description, "Another description");

my $error_no_request = error_NoRequest();
is($error_no_request->code, 200_204);
is($error_no_request->text, 'ZZ');
my $error_no_request_ab = error_NoRequest_AB();
is($error_no_request_ab->code, 200_204);
is($error_no_request_ab->text, 'ZZ');
is($error_no_request_ab->description, 'EE');
my $error_no_request_abc = error_NoRequest_ABC('DD');
is($error_no_request_abc->code, 200_204);
is($error_no_request_abc->description, 'DD');

use Direct::Errors;
ok(Direct::Errors::_similar_name('error_asdf', 'error_asdf_foo'), 'Suffixes: main name and one with a suffix, one error');
ok(Direct::Errors::_similar_name('warning_asdf_bar', 'warning_asdf_foo'), 'Suffixes: two names with suffixes, one error');
ok(!Direct::Errors::_similar_name('warning_qwer_bar', 'warning_asdf_foo'), 'Suffixes: two names with suffixes, different errors');
ok(!Direct::Errors::_similar_name('warning_asdf_bar', 'warning_qwer'), 'Suffixes: main name and one with a suffix, different errors');

done_testing;
