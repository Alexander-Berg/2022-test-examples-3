#!/usr/bin/y-local-env perl

use strict;
use warnings;
use utf8;

use Data::Dumper;
use LWP::UserAgent;
use HTTP::Request;

use JSON qw/from_json to_json/;
use JSON::XS::Boolean;

#BETA:
#use constant BASE_TASK_ID => 16;
#use constant SANDBOX_URL => 'http://yulika01.serp.yandex.ru:18675/api/v1.0/';

#PROD:
use constant BASE_TASK_ID => '32931257';
use constant SANDBOX_URL => 'https://sandbox.yandex-team.ru/api/v1.0/';

use constant SUCCESS_STATUSES => [qw/SUCCESS/];
use constant FAILED_STATUSES => [qw/FAILURE DELETED NO_RES STOPPED TIMEOUT EXCEPTION/];

use constant SLEEP_TIME => 10;

my $true = 1;
my $false = 0;

my $ua = LWP::UserAgent->new();
$ua->default_header( 'Content-Type' => 'application/json' );
my $data;

=cut
$data = create_task_request($ua);

print Dumper $data;

update_task_info($data);
start_task($data);
=cut

$data = wait_finish({id => '33453140'});


print Dumper $data;

exit;

sub process_request {
    my %params = @_;
    
    return undef unless $params{url} && $params{method};

    my $request = HTTP::Request->new($params{method}, $params{url}, $params{header}, $params{content});
    my $data;

    my $response = $ua->request($request);
    print $response->status_line(), "\n";

    if ($response->is_success && $params{need_data}) {
        $data = $response->decoded_content;
    }

    return { code => $response->code, status_line => $response->status_line, data => $data };
}

sub create_task_request {
    print "create_task\n";
    my $url = SANDBOX_URL().'task/';
    my $content = '{"source":'.BASE_TASK_ID().'}';

    my $response = process_request(method => "POST", url => $url, content => $content, need_data => 1);

    if ($response->{code} == 201) {
        return from_json($response->{data});
    }
    else {
        die "Something wrong with creating task: $response->{status_line}";
    }
}

sub update_task_info {
    my ($data) = @_;

    print "update\n";

    my $url = SANDBOX_URL()."task/$data->{id}";
    $data->{owner} = 'yulika';
#    $data->{priority}{class} = 'USER';
    $data->{priority}{class} = 'SERVICE';
    $data->{priority}{subclass} = 'HIGH';
#    $data->{important} = \$true;

    my $content = to_json($data);
    my $response = process_request(method => "PUT", url => $url, content => $content);
    
    if ($response->{code} == 204) {
        return 1;
    }
    elsif ($response->{code} == 400) {
        die "Error while updating task: incorrect task id ($data->{id})";
    }
    elsif ($response->{code} == 404) {
        die "Error while updating task: task not found ($data->{id})";
    }
    else {
        die "Something wrong with updating task: $data->{status_line}";
    }
}

sub start_task {
    my ($data) = @_;

    my $url = SANDBOX_URL().'batch/tasks/start';
    my $content = "[$data->{id}]";

    my $response = process_request(method => "PUT", url => $url, content => $content);
    if ($response->{code} == 200) {
        return 1;
    }
    else {
        die "Something went wrong when start a task $response->{status_line}";
    }
}

sub task_info {
    my ($data) = @_;

    my $url = SANDBOX_URL()."task/$data->{id}";

    my $response = process_request(method => "GET", url => $url, need_data => 1);
    if ($response->{code} == 200) {
        return from_json($response->{data});
    }
    else {
        die "Something went wrong when getting task ($data->{id}) info: $response->{status_line}";
    }
}

sub wait_finish {
    my ($data) = @_;

    my $statuses = [ @{SUCCESS_STATUSES()}, @{FAILED_STATUSES()} ];

    my $task;
    while ($task->{status} !~~ $statuses) {
        sleep SLEEP_TIME;
        $task = task_info($data);
    }

    return $task;
}
