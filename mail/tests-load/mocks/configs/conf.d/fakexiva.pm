package fakexiva;

use nginx;

sub fakesxiva {
    my $r = shift;
    $r->sleep(0, \&next);
    return OK;
}

sub fakeuxiva {
    my $r = shift;
    $r->send_http_header("application/json");
    $r->print('OK');    
    return OK;
}

sub next {
    my $r = shift;
    $pattern = "1234567890abcdefghijklmnopqrstuvwxyz";
    for (1..40) {$pass .= substr($pattern, (int(rand(35))), 1)}
    $r->send_http_header("application/json");
    $r->print('{"subscription-id":"'.$pass .'","ttl":8}');
    $pass ='';
    return OK;
}

1;

__END__
