#pragma once

#include <yplatform/util/sstream.h>
#include <util/system/shellcommand.h>
#include <string>
#include <vector>

inline std::string make_tickets(const std::vector<std::pair<std::string, std::string>>& tickets)
{
    std::string resp;
    yplatform::sstream r(resp);
    r << "{";
    for (auto& ticket : tickets)
    {
        if (resp.size() > 1) r << ",";
        r << '"' << ticket.first << "\":{\"";
        if (ticket.second.empty())
        {
            r << "error\":\"Dst is not found\"";
        }
        else
        {
            r << "ticket\":\"" << ticket.second << '"';
        }
        r << "}";
    }
    r << "}";
    return resp;
}

inline std::string get_keys()
{
    TShellCommand cmd("tvmknife unittest public_keys");
    cmd.Run();
    return cmd.GetOutput();
}

inline std::string get_service_ticket(unsigned src, unsigned dst)
{
    TShellCommand cmd("tvmknife unittest service");
    cmd << "-s" << std::to_string(src) << "-d" << std::to_string(dst);
    cmd.Run();
    std::string ticket = cmd.GetOutput();
    ticket.erase(std::remove_if(ticket.begin(), ticket.end(), isspace), ticket.end());
    return ticket;
}
