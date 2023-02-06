use Test::More;
use Direct::Modern;

use Client;

ok(Client::show_support_chat({
    is_any_client => 1,
    client_is_serviced => 0,
    client_have_agency => 0,
}, 225, 'ru'
), 'free client from Russia'
);

ok(!Client::show_support_chat({
    is_any_client => 0,
    client_is_serviced => 0,
    client_have_agency => 0,
}, 225, 'ru'
), 'manager (probably), from Russia'
);

ok(!Client::show_support_chat({
    is_any_client => 1,
    client_is_serviced => 0,
    client_have_agency => 0,
}, 123, 'ru'
), 'free client not from Russia'
);

ok(!Client::show_support_chat({
    is_any_client => 1,
    client_is_serviced => 0,
    client_have_agency => 0,
}, 225, 'ua'
), 'free client not in Russian'
);

ok(!Client::show_support_chat({
    is_any_client => 1,
    client_is_serviced => 0,
    client_have_agency => 1,
}, 225, 'ru'
), 'agency client'
);

ok(!Client::show_support_chat({
    is_any_client => 1,
    client_is_serviced => 1,
    client_have_agency => 0,
}, 225, 'ru'
), 'manager client'
);

done_testing();
