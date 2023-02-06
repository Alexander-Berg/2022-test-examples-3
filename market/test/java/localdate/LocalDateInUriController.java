package localdate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocalDateInUriController {

    @RequestMapping("/api/localDateAsPathVariable/{date}")
    public void localDateAsPathVariable(@PathVariable("date") LocalDate date) {
    }

    @RequestMapping("/api/localDateTimeAsPathVariable/{dateTime}")
    public void localDateTimeAsPathVariable(@PathVariable("dateTime") LocalDateTime dateTime) {
    }

    @RequestMapping("/api/localDateAsRequestParam")
    public void localDateAsRequestParam(@RequestParam("date") LocalDate date) {
    }

    @RequestMapping("/api/localDateTimeAsRequestParam")
    public void localDateTimeAsRequestParam(@RequestParam("dateTime") LocalDateTime dateTime) {
    }
}
