#include "mocks.h"

const boost::asio::io_service::id mock_timers_storage::id;

#include <yplatform/module_registration.h>
DEFINE_SERVICE_OBJECT(mock_processor)
