function is_corp(uid)
    return uid >= 1120000000000000 and uid < 1130000000000000
end

function list_childs(family_info)
    if family_info == nil then
        return nil
    else
        admin_uid = family_info.admin_uid
        result = ""
        for i, v in ipairs(family_info.users) do
            if v.uid ~= admin_uid then
                if string.len(result) > 0 then
                    result = result .. "\n"
                end
                result = result .. v.uid
            end
        end
        return result
    end
end
