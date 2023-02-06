// Used by jmeter
import ru.yandex.load.imap.replay.*;
import java.util.concurrent.ThreadLocalRandom;
def generate_seq(int size, String type, int count) {
//+ 133423 all
//+  89111 sequentially
//+  43442 unknown
//+  10651 last
//?   1919 random
	if( type.equals("all")) {
		return "1:*";
	} else if (type.equals("sequentially") || (type.equals("random") && count > 1)) {
		if(count >= size) {
			return "1:*"
		}
		int right = ThreadLocalRandom.current().nextInt(count, size + 1);
		int left = right - count + 1;
		return String.valueOf(left) + ":" + String.valueOf(right);
	} else if ( type.equals("last") ) {
		int left = size - 10;
		if(left < 1) left = 1; 
		return String.valueOf(left) + ":*"; 
	} else if (type.equals("random") && count == 1) {
		int random = ThreadLocalRandom.current().nextInt(1, size + 1);
		return String.valueOf(random);
	}
	return null;
}

def handle_request(Scenario s) {
	IMAPRequest request = s.nextCommand();
	if(request == null) {
		vars.put("run", "false");
	} else {
		String command = request.getRequest();
		vars.put("command", command);
		String[] args = request.getArgs();
		
		if(command.equals("sleep")) {
			sleep_time = Integer.parseInt(args[0]);
			if(sleep_time > 0 ) {
				vars.put("command", command);
				if (sleep_time > 15000) sleep_time =  15000;
				vars.put("sleep_time", Integer.toString(sleep_time))
			}
		}
   		if (command.equals("disconnect")) {
			vars.put("run", "false");
			s.reset();
			s.release();
            vars.remove("hibernation");
			return true;
        }
        String hibernation = vars.get("hibernation");
        if (hibernation != null) {
            if("true".equals(hibernation)) {
                log.info("session hibernated. Skiped: " + command);
                return false;
            }
        }
        if (command.equals("list") || command.equals("select") || command.equals("examine") || command.equals("lsub")) {
			vars.put("reference_mailbox", args[0]);
		} else if (command.equals("status")) {
			vars.put("folder", args[0]);
			vars.put("flags", args[1]);
		} else if (command.equals("disconnect")) {
			vars.put("run", "false");
			s.reset();
			s.release();
			return true;
		} else if (command.equals("uid search") || command.equals("uid fetch") || command.equals("fetch" ) || command.equals("uid store") || command.equals("search")) {
			String type = args[0];
			String payload;
			int count = 0;
			if(type.equals("random") || type.equals("sequentially")) count = Integer.parseInt(args[1]);
			String exists_string = vars.get("exists");
			int exists = 0;
			if(exists_string == null) return false;
			try{
				exists = Integer.parseInt(exists_string);
			} catch  (NumberFormatException nfe) {
				return false;
			}
			String flags = args[2];
			if(flags == "null")	return false;
			String seq = generate_seq(exists, type, count);
			if( seq == null ) {
				payload = flags;
			} else { 
                if( seq.equals("1:*") && (command.equals("uid fetch") || command.equals("fetch")) && flags.contains("BODY")) {
                    return false;
                }
				payload = seq + " " + flags;
			}
			vars.put("payload", payload);	
		}
	}
	return true;
}


try {
	ScenarioStorage ss = ScenarioStorage.getInstance();

	long start = System.nanoTime();
	sleep_time = 0;

	
	Scenario s = ss.getScenario("users", Integer.parseInt(vars.get("script_id")));
	
	
	while(handle_request(s) == false) {
	}
	
	long end = System.nanoTime();
} catch (InterruptedException ie) {
	vars.put("run", false);
}
	//log.info("handle time: " + String.valueOf((int)(end - start)/1000) + " mks, sleep: " + String.valueOf(sleep_time) + "ms");

