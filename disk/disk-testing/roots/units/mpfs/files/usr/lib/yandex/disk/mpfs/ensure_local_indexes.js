
dry_run = (typeof dry_run === 'undefined') ? 'false' : dry_run;
r_code = 0 
is_changed  = 'no'
msg = ''

indexes = ['_id', 'type', 'ctime'];
['queue', 'queue_index', 'queue_photoslice', 'queue_minor'].forEach(function(coll) {
  curr_indexes = db.getCollection(coll).getIndexes();
  indexes.forEach(function(index) {
      if (curr_indexes.reduce(function(fail,ind) {
          return fail && !ind.key.hasOwnProperty(index);
      },true)) {
          i = {}; i[index] = 1;
          is_changed = 'yes'
          if (dry_run == 'false') {
              printjson(db.getCollection(coll).ensureIndex(i));
              msg += coll + "[" + index + "], "
          }   

     
      }   
  }); 
})

print ()
print ("changed=" + is_changed + " comment='" + msg + "'")

quit(r_code)

