select distinct c.id, t.id from tasks c inner join companies c on t.id = c.task_id inner join (select straight_join x, y, z from coords c) crd using (company_id) where c.company_id = ? and ? and ? != ? and company_name like ? and task.id in (...) and (? = c.index and ? = c) or a = ? or v < ? limit ? offset ?