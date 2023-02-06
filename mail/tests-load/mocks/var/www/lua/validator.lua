local utils = require "utils"

function make_ok_response()
    return [[
<?xml version="1.0" encoding="windows-1251"?>
<page>
  <validator-rpop-added address="test-usert@mail.ru" uid="12345"/>
</page>
]]
end

function make_error_response()
    return [[
<?xml version="1.0" encoding="windows-1251"?>
<page>
  <validator-no-grants missing="email_validator.api_addrpop"/>
</page>
]]
end

utils.respond(make_ok_response, make_error_response)
