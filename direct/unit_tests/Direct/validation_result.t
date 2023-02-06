#!/usr/bin/perl
use my_inc "../..";
use Direct::Modern;

package Test::Errors;

use Direct::Errors;

error InvalidChars => (code => 892, text => '');
warning UnreachableHref => (code => 307, text => '');
warning BadLang => (code => 102, text => '');
error MinusWords => (code => 105, text => '');
error InvalidField => (code => 5005, text => '');
error LimitExceeded => (code => 7000, text => '');

package main;

Test::Errors->import();

use Test::More;
use Test::Exception;

BEGIN { use_ok('Direct::ValidationResult'); }

my $parent_vr = new Direct::ValidationResult;

my $third_vr = new Direct::ValidationResult;
$third_vr->next->add(some_field => $parent_vr);

my $child_vr = new Direct::ValidationResult;
$child_vr->next->add(some_field => $third_vr);

$parent_vr->next->add(some_field => $child_vr);

throws_ok {
    $parent_vr->is_valid
} qr/circular reference defined in validation result/;

my $vr_2 = new Direct::ValidationResult;
$vr_2->add_generic(error_InvalidChars());
my $obj_vr_2 = $vr_2->next;
$obj_vr_2->add_generic(error_MinusWords());
$obj_vr_2->add(field => error_MinusWords());
$obj_vr_2->add(field => warning_BadLang());
ok(!$vr_2->is_valid);

my $vr_3 = new Direct::ValidationResult();
my $obj_vr_3 = $vr_3->next;
$obj_vr_3->add(field => warning_UnreachableHref());
$obj_vr_3->add(field => warning_BadLang());

ok($vr_3->is_valid, 'warnings only');
ok(scalar @{$vr_3->get_warnings});

throws_ok {
    $obj_vr_3->add(field => $vr_2);
} qr/don't know how to merge two/;

lives_ok {
    $vr_3->next->add(field => $vr_2);
};

$vr_2->next->add(field => $vr_3);

throws_ok {
    $vr_3->is_valid;
} qr/circular reference defined in validation result/;

my $vr_99 = new Direct::ValidationResult;
$vr_99->next->add(field => $vr_99);

throws_ok {
    $vr_99->is_valid
} qr/circular reference defined in validation result/;

subtest "process_descriptions() processes field" => sub {
    my %VR_SUBST_FIELD_NAMES = (
        ret_cond_id => { ret_cond_id_name => 'RetargetingConditionId' },
    );
    my $ret_vr = Direct::ValidationResult->new;
    my $cond_vr = Direct::ValidationResult->new;
    $cond_vr->add(ret_cond_id => error_InvalidField("Значение поля #ret_cond_id_name# должно быть целым положительным числом"));
    $ret_vr->add(condition => $cond_vr);
    
    $ret_vr->process_descriptions(%VR_SUBST_FIELD_NAMES);

    is_deeply [map { $_->description} @{$ret_vr->get_errors}],
                ['Значение поля RetargetingConditionId должно быть целым положительным числом'];
};

subtest "process_descriptions() processes field with generic error" => sub {
    my $ret_vr = Direct::ValidationResult->new;
    my $cond_vr = Direct::ValidationResult->new;
    $cond_vr->add_generic(error_InvalidField("Значение поля #ret_cond_id_name# должно быть целым положительным числом"));
    $ret_vr->add(condition => $cond_vr);
    
    $ret_vr->process_descriptions(condition => { ret_cond_id_name => 'RetargetingConditionId' });
    
    is_deeply [map { $_->description} @{$ret_vr->get_errors}],
                ['Значение поля RetargetingConditionId должно быть целым положительным числом'];
};

subtest "process_descriptions() processes field when __globals given" => sub {
    my $ret_vr = Direct::ValidationResult->new;
    my $cond_vr = Direct::ValidationResult->new;
    $cond_vr->add_generic(error_InvalidField("Значение поля #ret_cond_id_name# должно быть целым положительным числом"));
    $ret_vr->add(0 => $cond_vr);

    $ret_vr->process_descriptions(__global => { ret_cond_id_name => 'RetargetingConditionId'});

    is_deeply [map { $_->description} @{$ret_vr->get_errors}], ['Значение поля RetargetingConditionId должно быть целым положительным числом'];
};

subtest "process_descriptions() processes field with lists" => sub {
    my %VR_SUBST_FIELD_NAMES = (
        condition => { ret_cond_id_name => 'RetargetingConditionId' },
    );
    my $ret_vr = Direct::ValidationResult->new;
    my $conds_vr = Direct::ValidationResult->new;
    my $cond_vr = $conds_vr->next;
    $cond_vr->add_generic(error_InvalidField("Значение поля #ret_cond_id_name# должно быть целым положительным числом"));
    $ret_vr->add(condition => $conds_vr);
    $ret_vr->process_descriptions(%VR_SUBST_FIELD_NAMES);

    is_deeply [map { $_->description} @{$ret_vr->get_errors}],
                ['Значение поля RetargetingConditionId должно быть целым положительным числом'];
};



done_testing;
