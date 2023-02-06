#!/usr/bin/env perl
use 5.14.0;
use strict;
use warnings;
use utf8;
use Carp;

use base qw/Test::Class/;
use Test::More;
use Test::Warn;


local $SIG{__WARN__} = undef;

sub merge {
    shift;
    BS::History::merge_mediaplan_history(@_);
}

sub load_modules: Tests(startup => 1) {
    use_ok 'BS::History';
}

my $some_empty_history = '';

my $some_other_OrderID = '111';
my $some_other_GroupID = '333';
my $some_other_PhraseID = '555';
my $some_other_history = "O$some_other_OrderID;G$some_other_GroupID;P$some_other_PhraseID;777:888";

my $some_our_OrderID = '222';
my $some_our_GroupID = '444';
my $some_our_PhraseID = '666';
my $some_our_history_common = "O$some_our_OrderID;G$some_our_GroupID;P$some_our_PhraseID";
my $some_our_history = "$some_our_history_common;999:1000";

my $some_bid_with_banner_id = 778;
my $some_banner_id_for_bid_with_banner_id = 889;
my $some_bid_without_banner_id = 779;
my $some_our_group_banners = {
    $some_bid_with_banner_id => $some_banner_id_for_bid_with_banner_id,
    $some_bid_without_banner_id => 0,
};

sub empty_history__has_order_id__has_phrase_id__expect_new_history: Test {
    my ($self) = @_;
    is(
        $self->merge(
            $some_empty_history, $some_our_OrderID, $some_our_PhraseID, $some_our_GroupID, $some_our_group_banners,
        ),
        "$some_our_history_common;$some_bid_with_banner_id:$some_banner_id_for_bid_with_banner_id",
    );
}

sub empty_history__has_order_id__no_phrase_id__expect_empty_history: Test {
    my ($self) = @_;
    is(
        $self->merge(
            $some_empty_history, $some_our_OrderID, undef, $some_our_GroupID, $some_our_group_banners,
        ),
        "",
    );
}

sub empty_history__no_order_id__has_phrase_id__expect_empty_history_and_warning: Tests(2) {
    my ($self) = @_;
    warning_like {
        is(
            $self->merge(
                $some_empty_history, undef, $some_our_PhraseID, $some_our_GroupID, $some_our_group_banners,
            ),
            "",
        );
    } qr/merge_mediaplan_history called for bid with PhraseID but without OrderID/;
}

sub empty_history__no_order_id__no_phrase_id__expect_empty_history: Test {
    my ($self) = @_;
    is(
        $self->merge(
            $some_empty_history, undef, undef, $some_our_GroupID, $some_our_group_banners,
        ),
        "",
    );
}

sub other_history__has_order_id__has_phrase_id__expect_other_history_discarded: Test {
    my ($self) = @_;
    is(
        $self->merge(
            $some_other_history, $some_our_OrderID, $some_our_PhraseID, $some_our_GroupID, $some_our_group_banners,
        ),
        "O222;G444;P666;778:889",
    );
}

sub other_history__has_order_id__no_phrase_id__expect_other_history_used_as_is: Test {
    my ($self) = @_;
    is(
        $self->merge(
            $some_other_history, $some_our_OrderID, undef, $some_our_GroupID, $some_our_group_banners,
        ),
        $some_other_history,
    );
}

sub other_history__no_order_id__has_phrase_id__expect_empty_history_and_warning: Tests(2) {
    my ($self) = @_;
    warning_like {
        is(
            $self->merge(
                $some_other_history, undef, $some_our_PhraseID, $some_our_GroupID, $some_our_group_banners,
            ),
            "",
        );
    } qr/merge_mediaplan_history called for bid with PhraseID but without OrderID/;
}

sub other_history__no_order_id__no_phrase_id__expect_other_history_used_as_is: Test {
    my ($self) = @_;
    is(
        $self->merge(
            $some_other_history, undef, undef, $some_our_GroupID, $some_our_group_banners,
        ),
        $some_other_history,
    );
}

sub our_history__has_order_id__has_phrase_id__expect_merged_history: Test {
    my ($self) = @_;
    is(
        $self->merge(
            $some_our_history, $some_our_OrderID, $some_our_PhraseID, $some_our_GroupID, $some_our_group_banners,
        ),
        "O222;G444;P666;778:889;999:1000",
    );
}
sub our_history__has_order_id__no_phrase_id__expect_merged_history: Test {
    my ($self) = @_;
    is(
        $self->merge(
            $some_our_history, $some_our_OrderID, undef, $some_our_GroupID, $some_our_group_banners,
        ),
        "O222;G444;P666;778:889;999:1000",
    );
}

__PACKAGE__->runtests();
