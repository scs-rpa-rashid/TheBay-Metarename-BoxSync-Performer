package utility;

import com.scs.dateutils.DateUtil;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public class Test {
    public static void main(String[] args) {

        System.out.println(postponeTime());
    }
    public static String postponeTime() {
        String currentDateTimeString = DateUtil.currentDateTime("yyyy-MM-dd HH:mm:ss");
        try {
            // Parse the current date-time string to LocalDateTime
            LocalDateTime parsedDateTime = LocalDateTime.parse(currentDateTimeString, DateUtil.dateFormatter("yyyy-MM-dd HH:mm:ss"));

            // Assume the current date-time string is in the system's default time zone
            ZonedDateTime parsedDateTimeWithZone = parsedDateTime.atZone(ZoneId.systemDefault());

            // Convert the parsed date-time to IST
            ZonedDateTime parsedDateTimeIST = parsedDateTimeWithZone.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));

            // Postpone the time by the specified minutes
            ZonedDateTime postponedDateTime = parsedDateTimeIST.withNano(0).plusHours(0);

            // Format the postponed date-time to the desired format
            return postponedDateTime.format(DateUtil.dateFormatter("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            System.err.println("Parsing error: " + e.getMessage());
            return null;
        }
    }
}
