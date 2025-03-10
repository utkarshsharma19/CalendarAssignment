// File: src/CalendarApp.java

import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.io.*;

public class CalendarApp {

  public static void main(String[] args) {
    CalendarManager calendar = new CalendarManager();
    if (args.length < 2) {
      System.out.println("Usage: --mode interactive OR --mode headless <commandFile.txt>");
      return;
    }
    if (args[0].equalsIgnoreCase("--mode")) {
      if (args[1].equalsIgnoreCase("interactive")) {
        runInteractiveMode(calendar);
      } else if (args[1].equalsIgnoreCase("headless")) {
        if (args.length < 3) {
          System.out.println("Headless mode requires a command file.");
          return;
        }
        runHeadlessMode(calendar, args[2]);
      } else {
        System.out.println("Invalid mode. Use interactive or headless.");
      }
    }
  }

  private static void runInteractiveMode(CalendarManager calendar) {
    Scanner scanner = new Scanner(System.in);
    System.out.println("Calendar App Interactive Mode. Type 'exit' to quit.");
    while (true) {
      System.out.print("> ");
      String command = scanner.nextLine();
      if (command.equalsIgnoreCase("exit")) {
        System.out.println("Exiting.");
        break;
      }
      try {
        CommandParser.processCommand(command, calendar);
      } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
      }
    }
    scanner.close();
  }

  private static void runHeadlessMode(CalendarManager calendar, String fileName) {
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String command;
      while ((command = br.readLine()) != null) {
        System.out.println("> " + command);
        if (command.equalsIgnoreCase("exit")) {
          System.out.println("Exiting.");
          break;
        }
        CommandParser.processCommand(command, calendar);
      }
    } catch (IOException e) {
      System.out.println("Error reading file: " + e.getMessage());
    } catch (Exception e) {
      System.out.println("Command error: " + e.getMessage());
    }
  }

  // Represents a calendar event.
  public static class CalendarEvent {
    String eventName;
    LocalDateTime start;
    LocalDateTime end;
    boolean isAllDay;
    String description;
    String location;
    boolean isPublic;

    public CalendarEvent(String eventName, LocalDateTime start, LocalDateTime end, boolean isAllDay) {
      this.eventName = eventName;
      this.start = start;
      this.end = end;
      this.isAllDay = isAllDay;
      this.description = "";
      this.location = "";
      this.isPublic = true;
    }

    // Two events conflict if their time intervals overlap.
    public boolean conflictsWith(CalendarEvent other) {
      return this.start.isBefore(other.end) && this.end.isAfter(other.start);
    }

    @Override
    public String toString() {
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      String base = "";
      if (isAllDay) {
        base = String.format("%s (All Day on %s)", eventName, start.toLocalDate());
      } else {
        base = String.format("%s from %s to %s", eventName, start.format(dtf), end.format(dtf));
      }
      if (!description.isEmpty()) {
        base += ", Description: " + description;
      }
      if (!location.isEmpty()) {
        base += ", Location: " + location;
      }
      base += ", " + (isPublic ? "Public" : "Private");
      return base;
    }
  }

  // Manages the list of calendar events and provides operations on them.
  public static class CalendarManager {
    List<CalendarEvent> events;

    public CalendarManager() {
      events = new ArrayList<>();
    }

    // Adds an event; if autoDecline is true, a conflict will cancel creation.
    public boolean addEvent(CalendarEvent newEvent, boolean autoDecline) throws Exception {
      for (CalendarEvent event : events) {
        if (newEvent.conflictsWith(event)) {
          if (autoDecline) {
            throw new Exception("Conflict detected with event: " + event.eventName);
          } else {
            System.out.println("Warning: Event conflicts with " + event.eventName);
          }
        }
      }
      events.add(newEvent);
      // Sort events based on start date/time.
      Collections.sort(events, Comparator.comparing(e -> e.start));
      return true;
    }

    // Returns events that occur on the given date.
    public List<CalendarEvent> getEventsOn(LocalDate date) {
      List<CalendarEvent> result = new ArrayList<>();
      for (CalendarEvent event : events) {
        if (event.isAllDay) {
          if (event.start.toLocalDate().equals(date)) {
            result.add(event);
          }
        } else {
          // For timed events, check if the event spans the queried date.
          if (!event.start.toLocalDate().isAfter(date) && !event.end.toLocalDate().isBefore(date)) {
            result.add(event);
          }
        }
      }
      return result;
    }

    // Returns events within the given time range.
    public List<CalendarEvent> getEventsInRange(LocalDateTime startRange, LocalDateTime endRange) {
      List<CalendarEvent> result = new ArrayList<>();
      for (CalendarEvent event : events) {
        if (event.start.isBefore(endRange) && event.end.isAfter(startRange)) {
          result.add(event);
        }
      }
      return result;
    }

    // Exports the current calendar events to a CSV file in our custom format.
    public void exportToCSV(String fileName) {
      try (PrintWriter writer = new PrintWriter(new File(fileName))) {
        StringBuilder sb = new StringBuilder();
        sb.append("EventName,Start,End,AllDay,Description,Location,Public\n");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (CalendarEvent event : events) {
          sb.append("\"" + event.eventName + "\",");
          sb.append(event.start.format(dtf) + ",");
          sb.append(event.end.format(dtf) + ",");
          sb.append(event.isAllDay + ",");
          sb.append("\"" + event.description + "\",");
          sb.append("\"" + event.location + "\",");
          sb.append(event.isPublic + "\n");
        }
        writer.write(sb.toString());
        File file = new File(fileName);
        System.out.println("Exported to CSV: " + file.getAbsolutePath());
      } catch (Exception e) {
        System.out.println("Error exporting CSV: " + e.getMessage());
      }
    }

    // Exports the calendar events to a CSV file that follows the Google Calendar import format.
    public void exportToGoogleCSV(String fileName) {
      try (PrintWriter writer = new PrintWriter(new File(fileName))) {
        // Google Calendar CSV headers:
        // Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private
        StringBuilder sb = new StringBuilder();
        sb.append("Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private\n");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (CalendarEvent event : events) {
          sb.append("\"" + event.eventName + "\",");
          if (event.isAllDay) {
            // For all-day events, only the date is needed. Leave time blank.
            sb.append(event.start.format(dateFormatter) + ",,");
            sb.append(event.start.format(dateFormatter) + ",,");
            sb.append("True,");
          } else {
            sb.append(event.start.format(dateFormatter) + ",");
            sb.append(event.start.format(timeFormatter) + ",");
            sb.append(event.end.format(dateFormatter) + ",");
            sb.append(event.end.format(timeFormatter) + ",");
            sb.append("False,");
          }
          sb.append("\"" + event.description + "\",");
          sb.append("\"" + event.location + "\",");
          // Google CSV uses 'Private' where True means the event is private.
          // Here, if event.isPublic is true, we output False.
          sb.append(event.isPublic ? "False" : "True");
          sb.append("\n");
        }
        writer.write(sb.toString());
        File file = new File(fileName);
        System.out.println("Exported to Google CSV: " + file.getAbsolutePath());
      } catch (Exception e) {
        System.out.println("Error exporting Google CSV: " + e.getMessage());
      }
    }

    // Checks if the calendar is busy at the given date/time.
    public boolean isBusyAt(LocalDateTime dateTime) {
      for (CalendarEvent event : events) {
        if (!event.start.isAfter(dateTime) && event.end.isAfter(dateTime)) {
          return true;
        }
      }
      return false;
    }

    // Edit a single event identified by eventName, start, and end.
    public boolean editSingleEvent(String property, String eventName, LocalDateTime start, LocalDateTime end, String newValue) {
      for (CalendarEvent event : events) {
        if (event.eventName.equals(eventName) && event.start.equals(start) && event.end.equals(end)) {
          if (updateProperty(event, property, newValue)) {
            return true;
          }
        }
      }
      return false;
    }

    // Edit all events in the series starting at or after a given start date/time.
    // Returns the number of events modified.
    public int editEventsByStart(String property, String eventName, LocalDateTime start, String newValue) {
      int count = 0;
      for (CalendarEvent event : events) {
        if (event.eventName.equals(eventName) && (event.start.equals(start) || event.start.isAfter(start))) {
          if (updateProperty(event, property, newValue)) {
            count++;
          }
        }
      }
      return count;
    }

    // Edit all events with the given event name.
    // Returns the number of events modified.
    public int editEventsByName(String property, String eventName, String newValue) {
      int count = 0;
      for (CalendarEvent event : events) {
        if (event.eventName.equals(eventName)) {
          if (updateProperty(event, property, newValue)) {
            count++;
          }
        }
      }
      return count;
    }

    // Helper method to update event properties.
    // Allowed properties: name, description, location, public.
    private boolean updateProperty(CalendarEvent event, String property, String newValue) {
      switch (property.toLowerCase()) {
        case "name":
          event.eventName = newValue;
          break;
        case "description":
          event.description = newValue;
          break;
        case "location":
          event.location = newValue;
          break;
        case "public":
          event.isPublic = Boolean.parseBoolean(newValue);
          break;
        default:
          return false;
      }
      return true;
    }
  }

  // Parses and processes the commands given by the user.
  public static class CommandParser {

    // Expected date format: "yyyy-MM-dd" and dateTime format: "yyyy-MM-dd'T'HH:mm"
    static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void processCommand(String command, CalendarManager calendar) throws Exception {
      String lowerCmd = command.toLowerCase();
      if (lowerCmd.startsWith("create event")) {
        processCreateEvent(command, calendar);
      } else if (lowerCmd.startsWith("edit events")) {  // Check plural first
        processEditCommand(command, calendar, true);
      } else if (lowerCmd.startsWith("edit event")) {
        processEditCommand(command, calendar, false);
      } else if (lowerCmd.startsWith("print events on")) {
        processPrintEventsOn(command, calendar);
      } else if (lowerCmd.startsWith("print events from")) {
        processPrintEventsRange(command, calendar);
      } else if (lowerCmd.startsWith("export cal")) {
        processExportCal(command, calendar);
      } else if (lowerCmd.startsWith("show status on")) {
        processShowStatus(command, calendar);
      } else if (lowerCmd.startsWith("export googlecsv")) {
        processExportGoogleCSV(command, calendar);
      } else {
        throw new Exception("Invalid command: " + command);
      }
    }


    // Processes commands that create events.
    private static void processCreateEvent(String command, CalendarManager calendar) throws Exception {
      boolean autoDecline = false;
      if (command.toLowerCase().contains("--autodecline")) {
        autoDecline = true;
        command = command.replace("--autoDecline", "").trim();
      }
      // Determine if the command is for a timed event (using "from") or an all-day event (using "on").
      if (command.contains(" from ")) {
        // Pattern: create event <eventName> from <startDateTime> to <endDateTime> [repeats ...]
        String[] parts = command.split(" from ", 2);
        String eventName = parts[0].replace("create event", "").trim();
        String remainder = parts[1];
        if (!remainder.contains(" to ")) {
          throw new Exception("Invalid format: missing 'to' keyword.");
        }
        String[] timeParts = remainder.split(" to ", 2);
        String startStr = timeParts[0].trim();
        String afterTo = timeParts[1].trim();

        // Check if this is a recurring event.
        if (afterTo.toLowerCase().contains(" repeats ")) {
          String[] toParts = afterTo.split(" repeats ", 2);
          String endStr = toParts[0].trim();
          String repeatPart = toParts[1].trim();
          LocalDateTime startDateTime = LocalDateTime.parse(startStr, dateTimeFormatter);
          LocalDateTime endDateTime = LocalDateTime.parse(endStr, dateTimeFormatter);
          List<CalendarEvent> occurrences = generateRecurringEvents(eventName, startDateTime, endDateTime, repeatPart, false);
          for (CalendarEvent occurrence : occurrences) {
            calendar.addEvent(occurrence, autoDecline);
          }
          System.out.println("Recurring event created with " + occurrences.size() + " occurrences.");
        } else {
          // Single timed event.
          String endStr = afterTo.trim();
          LocalDateTime startDateTime = LocalDateTime.parse(startStr, dateTimeFormatter);
          LocalDateTime endDateTime = LocalDateTime.parse(endStr, dateTimeFormatter);
          CalendarEvent event = new CalendarEvent(eventName, startDateTime, endDateTime, false);
          calendar.addEvent(event, autoDecline);
          System.out.println("Event created: " + event);
        }
      } else if (command.contains(" on ")) {
        // Pattern: create event <eventName> on <date> [repeats ...]
        String[] parts = command.split(" on ", 2);
        String eventName = parts[0].replace("create event", "").trim();
        String remainder = parts[1].trim();
        if (remainder.toLowerCase().contains(" repeats ")) {
          String[] dateParts = remainder.split(" repeats ", 2);
          String dateStr = dateParts[0].trim();
          String repeatPart = dateParts[1].trim();
          LocalDate date = LocalDate.parse(dateStr, dateFormatter);
          // For an all-day event, the start is the beginning of the day and the end is the beginning of the next day.
          LocalDateTime startDateTime = date.atStartOfDay();
          LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
          List<CalendarEvent> occurrences = generateRecurringEvents(eventName, startDateTime, endDateTime, repeatPart, true);
          for (CalendarEvent occurrence : occurrences) {
            calendar.addEvent(occurrence, autoDecline);
          }
          System.out.println("Recurring all-day event created with " + occurrences.size() + " occurrences.");
        } else {
          // Single all-day event.
          String dateStr = remainder.trim();
          LocalDate date = LocalDate.parse(dateStr, dateFormatter);
          LocalDateTime startDateTime = date.atStartOfDay();
          LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
          CalendarEvent event = new CalendarEvent(eventName, startDateTime, endDateTime, true);
          calendar.addEvent(event, autoDecline);
          System.out.println("All-day event created: " + event);
        }
      } else {
        throw new Exception("Invalid create event command format.");
      }
    }

    // Generates recurring event occurrences.
    // repeatPart examples: "MTWRF for 5 times" or "MRU until 2025-03-10T00:00"
    private static List<CalendarEvent> generateRecurringEvents(String eventName, LocalDateTime startDateTime,
                                                               LocalDateTime endDateTime, String repeatPart, boolean isAllDay) throws Exception {
      List<CalendarEvent> occurrences = new ArrayList<>();
      // The weekdays string is assumed to be the first token.
      String[] tokens = repeatPart.split(" ");
      String weekdaysStr = tokens[0].trim().toUpperCase();
      Set<DayOfWeek> weekdays = new HashSet<>();
      for (char c : weekdaysStr.toCharArray()) {
        switch (c) {
          case 'M': weekdays.add(DayOfWeek.MONDAY); break;
          case 'T': weekdays.add(DayOfWeek.TUESDAY); break;
          case 'W': weekdays.add(DayOfWeek.WEDNESDAY); break;
          case 'R': weekdays.add(DayOfWeek.THURSDAY); break;
          case 'F': weekdays.add(DayOfWeek.FRIDAY); break;
          case 'S': weekdays.add(DayOfWeek.SATURDAY); break;
          case 'U': weekdays.add(DayOfWeek.SUNDAY); break;
          default: throw new Exception("Invalid weekday character: " + c);
        }
      }
      // Determine recurrence based on a fixed number of occurrences or an end date.
      if (repeatPart.toLowerCase().contains(" for ")) {
        // Expected format: <weekdays> for <N> times
        if (tokens.length < 4 || !tokens[1].equalsIgnoreCase("for") || !tokens[3].equalsIgnoreCase("times")) {
          throw new Exception("Invalid recurring event format (for N times).");
        }
        int occurrencesCount = Integer.parseInt(tokens[2]);
        LocalDateTime current = startDateTime;
        while (occurrences.size() < occurrencesCount) {
          if (weekdays.contains(current.getDayOfWeek())) {
            LocalDate currentDate = current.toLocalDate();
            LocalDateTime occStart = LocalDateTime.of(currentDate, startDateTime.toLocalTime());
            LocalDateTime occEnd = LocalDateTime.of(currentDate, endDateTime.toLocalTime());
            occurrences.add(new CalendarEvent(eventName, occStart, occEnd, isAllDay));
          }
          current = current.plusDays(1);
        }
      } else if (repeatPart.toLowerCase().contains(" until ")) {
        // Expected format: <weekdays> until <dateTime> (or date for all-day events)
        int index = repeatPart.toLowerCase().indexOf("until");
        String untilPart = repeatPart.substring(index + "until".length()).trim();
        LocalDateTime untilDateTime;
        if (isAllDay) {
          LocalDate untilDate = LocalDate.parse(untilPart, dateFormatter);
          untilDateTime = untilDate.plusDays(1).atStartOfDay();
        } else {
          untilDateTime = LocalDateTime.parse(untilPart, dateTimeFormatter);
        }
        LocalDateTime current = startDateTime;
        while (!current.isAfter(untilDateTime.minusSeconds(1))) {
          if (weekdays.contains(current.getDayOfWeek())) {
            LocalDate currentDate = current.toLocalDate();
            LocalDateTime occStart = LocalDateTime.of(currentDate, startDateTime.toLocalTime());
            LocalDateTime occEnd = LocalDateTime.of(currentDate, endDateTime.toLocalTime());
            occurrences.add(new CalendarEvent(eventName, occStart, occEnd, isAllDay));
          }
          current = current.plusDays(1);
        }
      } else {
        throw new Exception("Invalid recurring event format.");
      }
      return occurrences;
    }

    // Processes commands for editing events.
    // If plural is true, handles "edit events" commands; otherwise, "edit event" commands.
    private static void processEditCommand(String command, CalendarManager calendar, boolean plural) throws Exception {
      // Remove the command prefix ("edit event" or "edit events")
      String prefix = plural ? "edit events" : "edit event";
      String remainder = command.substring(prefix.length()).trim();

      // For singular, expect:
      // <property> <eventName> from <dateTime> to <dateTime> with <NewPropertyValue>
      // For plural, there are two cases:
      // 1. <property> <eventName> from <dateTime> with <NewPropertyValue>
      // 2. <property> <eventName> <NewPropertyValue>
      if (remainder.contains(" with ")) {
        String[] parts = remainder.split(" with ", 2);
        String beforeWith = parts[0].trim();
        String newValue = parts[1].trim();
        if (beforeWith.contains(" from ")) {
          // Either singular or plural with "from" clause.
          String[] splitFrom = beforeWith.split(" from ", 2);
          String firstPart = splitFrom[0].trim();
          String afterFrom = splitFrom[1].trim();
          if (!plural && !afterFrom.contains(" to ")) {
            throw new Exception("Missing 'to' clause for singular edit command.");
          }
          String property;
          String eventName;
          // Split the firstPart into property and eventName (assume single token for event name)
          String[] tokens = firstPart.split(" ", 2);
          if (tokens.length < 2) {
            throw new Exception("Invalid edit command format.");
          }
          property = tokens[0].trim();
          eventName = tokens[1].trim();

          if (!plural) {
            // Singular edit: expect "to" clause.
            String[] splitTo = afterFrom.split(" to ", 2);
            if (splitTo.length < 2) {
              throw new Exception("Missing 'to' clause for singular edit command.");
            }
            String startStr = splitTo[0].trim();
            String endStr = splitTo[1].trim();
            LocalDateTime startDateTime = LocalDateTime.parse(startStr, dateTimeFormatter);
            LocalDateTime endDateTime = LocalDateTime.parse(endStr, dateTimeFormatter);
            boolean updated = calendar.editSingleEvent(property, eventName, startDateTime, endDateTime, newValue);
            if (updated) {
              System.out.println("Event updated successfully.");
            } else {
              System.out.println("Event not found or update failed.");
            }
          } else {
            // Plural edit with "from": update all events with eventName starting at or after given date/time.
            LocalDateTime startDateTime = LocalDateTime.parse(afterFrom, dateTimeFormatter);
            int count = calendar.editEventsByStart(property, eventName, startDateTime, newValue);
            System.out.println(count + " event(s) updated starting from " + startDateTime);
          }
        } else {
          // Plural edit without "from": update all events with the given eventName.
          String[] tokens = beforeWith.split(" ", 2);
          if (tokens.length < 2) {
            throw new Exception("Invalid edit command format.");
          }
          String property = tokens[0].trim();
          String eventName = tokens[1].trim();
          int count = calendar.editEventsByName(property, eventName, newValue);
          System.out.println(count + " event(s) updated with new " + property);
        }
      } else {
        throw new Exception("Edit command must contain 'with' clause.");
      }
    }

    // Processes the command to print events on a specific date.
    // Command pattern: print events on <date>
    private static void processPrintEventsOn(String command, CalendarManager calendar) throws Exception {
      String[] parts = command.split(" on ", 2);
      if (parts.length < 2) {
        throw new Exception("Invalid command format for printing events.");
      }
      String dateStr = parts[1].trim();
      LocalDate date = LocalDate.parse(dateStr, dateFormatter);
      List<CalendarEvent> events = calendar.getEventsOn(date);
      if (events.isEmpty()) {
        System.out.println("No events found on " + date);
      } else {
        System.out.println("Events on " + date + ":");
        for (CalendarEvent event : events) {
          System.out.println(" - " + event);
        }
      }
    }

    // Processes the command to print events in a date/time range.
    // Command pattern: print events from <dateTime> to <dateTime>
    private static void processPrintEventsRange(String command, CalendarManager calendar) throws Exception {
      String[] parts = command.split(" from ", 2);
      if (parts.length < 2) {
        throw new Exception("Invalid command format for printing events in range.");
      }
      String remainder = parts[1].trim();
      if (!remainder.contains(" to ")) {
        throw new Exception("Missing 'to' clause in range query.");
      }
      String[] timeParts = remainder.split(" to ", 2);
      String startStr = timeParts[0].trim();
      String endStr = timeParts[1].trim();
      LocalDateTime startDateTime = LocalDateTime.parse(startStr, dateTimeFormatter);
      LocalDateTime endDateTime = LocalDateTime.parse(endStr, dateTimeFormatter);
      List<CalendarEvent> events = calendar.getEventsInRange(startDateTime, endDateTime);
      if (events.isEmpty()) {
        System.out.println("No events found between " + startDateTime + " and " + endDateTime);
      } else {
        System.out.println("Events between " + startDateTime + " and " + endDateTime + ":");
        for (CalendarEvent event : events) {
          System.out.println(" - " + event);
        }
      }
    }

    // Processes the command to export the calendar to a CSV file (custom format).
    // Command pattern: export cal <fileName.csv>
    private static void processExportCal(String command, CalendarManager calendar) throws Exception {
      String[] tokens = command.split(" ");
      if (tokens.length < 3) {
        throw new Exception("Invalid export command format.");
      }
      String fileName = tokens[2].trim();
      calendar.exportToCSV(fileName);
    }

    // Processes the command to export the calendar to a Google CSV file.
    // Command pattern: export googlecsv <fileName.csv>
    private static void processExportGoogleCSV(String command, CalendarManager calendar) throws Exception {
      String[] tokens = command.split(" ");
      if (tokens.length < 3) {
        throw new Exception("Invalid export googlecsv command format.");
      }
      String fileName = tokens[2].trim();
      calendar.exportToGoogleCSV(fileName);
    }

    // Processes the command to show status at a specific date/time.
    // Command pattern: show status on <dateTime>
    private static void processShowStatus(String command, CalendarManager calendar) throws Exception {
      String[] parts = command.split(" on ", 2);
      if (parts.length < 2) {
        throw new Exception("Invalid command format for show status.");
      }
      String dateTimeStr = parts[1].trim();
      LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, dateTimeFormatter);
      boolean busy = calendar.isBusyAt(dateTime);
      System.out.println("Status at " + dateTime + ": " + (busy ? "Busy" : "Available"));
    }
  }
}
