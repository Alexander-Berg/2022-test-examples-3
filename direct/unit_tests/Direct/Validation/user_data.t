#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use Test::Exception;
use Yandex::Test::ValidationResult;

use Mouse::Util::TypeConstraints;

BEGIN {
    use_ok('Direct::Validation::UserData', qw/check_user_data/);
}

subtest 'Hash constraint' => sub {
    ok_validation_result(check_user_data({a => 1, b => 2}, {}));
    ok_validation_result(check_user_data({a => 1, b => 2}, {a => "Int"}));
    cmp_validation_result(check_user_data([1, 2], {}), vr_errors('InvalidFormat'));
    cmp_validation_result(check_user_data(undef, {}), vr_errors('InvalidFormat'));
    cmp_validation_result(check_user_data({a => 1}, {a => "Int", b => {c => "Int"}}), {a => {}, b => vr_errors('ReqField')});
};

subtest 'Simple array constraint' => sub {
    ok_validation_result(check_user_data([1, 2], []));
    cmp_validation_result(check_user_data({}, []), vr_errors('InvalidFormat'));
    cmp_validation_result(check_user_data(undef, []), vr_errors('InvalidFormat'));
    cmp_validation_result(check_user_data({}, {a => []}), {a => vr_errors('ReqField')});
};

subtest 'Chained array constraint' => sub {
    ok_validation_result(check_user_data([1, 2], [1, "ArrayRef[Int]", "Int"]));
    ok_validation_result(check_user_data(["a", "b", undef], [1, "ArrayRef[Maybe[Str]]", "Maybe[Str]"]));
    cmp_validation_result(check_user_data(["a", "b"], [1, "HashRef", {a => "Int"}]), vr_errors('InvalidFormat'));
    cmp_validation_result(check_user_data(["a", undef], [1, "ArrayRef", "Str"]), [{}, vr_errors('InvalidFormat')]);
    ok_validation_result(check_user_data(undef, [1, "Maybe[HashRef]", {}]));
    ok_validation_result(check_user_data({a => []}, [1, "Maybe[HashRef]", {a => "ArrayRef"}]));
    ok_validation_result(check_user_data({}, {a => [0]}));
    ok_validation_result(check_user_data({}, {a => [sub { 0 }]}));
    cmp_validation_result(check_user_data({}, {a => [1, "Str"]}), {a => vr_errors('ReqField')});
    cmp_validation_result(check_user_data({}, {a => [sub { 1 }, "Str"]}), {a => vr_errors('ReqField')});
    cmp_validation_result(check_user_data({a => 1}, {a => [undef]}), {a => vr_errors('InvalidField')});
};

subtest 'Code constraint' => sub {
    ok_validation_result(check_user_data("qwerty", sub { $_[0] eq 'qwerty' }));
    ok_validation_result(check_user_data([1, 2], sub { ref($_[0]) eq 'ARRAY' && $_[0]->[0] == 1 }));
    cmp_validation_result(check_user_data([1, 2], sub { ref($_[0]) eq 'ARRAY' && $_[0]->[0] == 0 }), vr_errors('InvalidFormat'));
    cmp_validation_result(check_user_data("a", sub { $_[0] eq 'b' }), vr_errors('InvalidFormat'));
    cmp_validation_result(check_user_data({}, {a => sub { 1 }}), {a => vr_errors('ReqField')});
    ok_validation_result(check_user_data({a => 5, b => -3}, {a => sub { $_[0] == 5 && $_[1]->{b} == -3 }}));
};

subtest 'Mouse constraint' => sub {
    ok_validation_result(check_user_data("a", "Str"));
    ok_validation_result(check_user_data([], "ArrayRef"));
    ok_validation_result(check_user_data("123", "Defined"));
    ok_validation_result(check_user_data("0", "Bool"));
    ok_validation_result(check_user_data(undef, "Maybe[Str]"));
    cmp_validation_result(check_user_data(undef, "Str"), vr_errors('InvalidFormat'));
    cmp_validation_result(check_user_data({}, {a => "Int"}), {a => vr_errors('ReqField')});

    subtype 'TestPositiveInt' => as 'Int' => where { $_ > 0 };
    ok_validation_result(check_user_data(1, "TestPositiveInt"));
    cmp_validation_result(check_user_data(-1, "TestPositiveInt"), vr_errors('InvalidFormat'));

    enum 'TestRGBColors' => qw(red green blue);
    ok_validation_result(check_user_data("red", "TestRGBColors"));
    ok_validation_result(check_user_data("green", "TestRGBColors"));
    ok_validation_result(check_user_data("blue", "TestRGBColors"));
    cmp_validation_result(check_user_data("white", "TestRGBColors"), vr_errors('InvalidFormat'));
};

subtest 'Deep checking' => sub {
    my $data = {
        field1 => "ok",
        field2 => [{a => 1, b => 2}],
        field3 => "Yes",
        field5 => "good",
    };
    my $user_var = {ok => 1};

    ok_validation_result(check_user_data($data, {
        field1 => "Str",
        field2 => [1, "ArrayRef", {a => "Int", b => "Int"}],
        field3 => sub { my ($val, $parent, $user_var) = @_; $val eq 'Yes' && $user_var->{ok} },
        field4 => [undef, "Maybe[Str]"],
        field5 => [sub { my ($parent, $user_var) = @_; !$user_var->{ok} }, "Str"],
    }, $user_var));

    cmp_validation_result(check_user_data($data, {
        field1 => "Str",
        field2 => [1, "ArrayRef", {a => "Int", b => "Int", c => "Int"}],
        field3 => enum([qw/Yes No/]),
        field4 => [1, "Str"],
        field5 => [undef],
    }), {
        field1 => {},
        field2 => [{a => {}, b => {}, c => vr_errors('ReqField')}],
        field3 => {},
        field4 => vr_errors('ReqField'),
        field5 => vr_errors('InvalidField'),
    });

    cmp_validation_result(check_user_data($data, {
        field1 => "Str",
        field2 => [1, "ArrayRef", {a => "Int", b => "Int"}],
        field3 => enum([qw/true false/]),
    }), {
        field1 => {},
        field2 => {},
        field3 => vr_errors('InvalidFormat'),
    });
};

subtest 'Exceptions' => sub {
    dies_ok { check_user_data([], undef); };
    dies_ok { check_user_data([], ""); };
    dies_ok { check_user_data([], "[Unknown]"); };
    dies_ok { check_user_data("ok", [1, "Str", "Str", "Str"]); };
    dies_ok { check_user_data({}, bless({}, 'Some::Thing')) };
    dies_ok { check_user_data({}, {a => [{}]}) };
    dies_ok { check_user_data({}, {a => [2]}) };
};

done_testing;
