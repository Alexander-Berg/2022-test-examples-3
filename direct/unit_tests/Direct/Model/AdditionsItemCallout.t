#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use Test::Exception;

use POSIX qw/strftime/;

BEGIN {
    use_ok('Direct::Model::AdditionsItemCallout');
}

sub mk_item_callout { Direct::Model::AdditionsItemCallout->new(@_) }

subtest "Model" => sub {
    lives_ok { mk_item_callout() };
    lives_ok { mk_item_callout(id => 1) };
    lives_ok { mk_item_callout(id => 1, callout_text => "text") };
    dies_ok { mk_item_callout(invalid_key => 1) };
    lives_ok { mk_item_callout(status_moderate => "Yes") };
    dies_ok { mk_item_callout(status_moderate => "InvalidStatus") };
};

subtest "calc_hash" => sub {
    my $item_callout = mk_item_callout(id => 1, callout_text => "text");
    $item_callout->calc_hash();

    is $item_callout->hash, 2067805253194386918;
};

subtest "set_last_change" => sub {
    my $item_callout = mk_item_callout(id => 1, callout_text => "text");
    $item_callout->set_last_change();
    my $moderate_date = $item_callout->last_change =~ s/^(\S+).+$/$1/r;

    # при переходе дат в момент выполнения теста - сломается
    is $moderate_date, strftime("%Y-%m-%d", localtime());
};

subtest "set_datetimes" => sub {
    my $item_callout = mk_item_callout(id => 1, callout_text => "text");
    $item_callout->set_datetimes();
    my $moderate_date = $item_callout->last_change =~ s/^(\S+).+$/$1/r;
    my $create_time = $item_callout->create_time =~ s/^(\S+).+$/$1/r;

    # при переходе дат в момент выполнения теста - сломается
    is $moderate_date, strftime("%Y-%m-%d", localtime());
    is $create_time, strftime("%Y-%m-%d", localtime());
};

subtest "to_template_hash" => sub {
    my $item_callout = mk_item_callout(id => 1, callout_text => "text");
    my $template_hash = $item_callout->to_template_hash;

    is ref($template_hash), "HASH";
    is join(",", sort keys %$template_hash), "additions_item_id,callout_text";
    is join(",", sort values %$template_hash), "1,text";
};

done_testing;
