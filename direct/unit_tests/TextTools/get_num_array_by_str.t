#!/usr/bin/env perl
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;


sub t {
    &TextTools::get_num_array_by_str;
}

sub load_modules: Tests(startup => 1) {
    use_ok 'TextTools';
}

sub only_one_element_with_trailing_space: Test {
    is_deeply t("123 "), [123]
}

sub only_one_element_with_leading_space: Test {
    is_deeply t(" 123"), [123]
}

sub two_elements_with_spaces_everywhere: Test {
    is_deeply t("   123 ,     44 "), [123, 44]
}

sub two_elements_with_trailing_whitespace: Test {
    is_deeply t("123,44 "), [123, 44]
}

sub two_elements_without_spaces: Test {
    is_deeply t("123,44"), [123, 44]
}

sub one_element_without_spaces: Test {
    is_deeply t("1313"), [1313];
}

__PACKAGE__->runtests();
