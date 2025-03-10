package calendar;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CalendarAppTest {

  @Test
  public void testCreateTimedEvent() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String command = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CalendarApp.CommandParser.processCommand(command, manager);
    assertEquals("One timed event should be created", 1, manager.events.size());
  }

  @Test
  public void testCreateAllDayEvent() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String command = "create event Holiday on 2025-03-05";
    CalendarApp.CommandParser.processCommand(command, manager);
    assertEquals("One all-day event should be created", 1, manager.events.size());
    assertTrue("Event should be all-day", manager.events.get(0).isAllDay);
  }

  @Test
  public void testRecurringEventFixedTimes() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String command = "create event Workshop on 2025-03-02 repeats MTWRF for 3 times";
    CalendarApp.CommandParser.processCommand(command, manager);
    assertEquals("Three occurrences should be created", 3, manager.events.size());
  }

  @Test
  public void testRecurringEventUntil() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String command = "create event Seminar from 2025-03-03T09:00 to 2025-03-03T10:30 repeats WF until 2025-03-10T00:00";
    CalendarApp.CommandParser.processCommand(command, manager);
    assertTrue("At least one occurrence should be created", manager.events.size() > 0);
  }

  @Test
  public void testEditSingleEvent() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    // Create event
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CalendarApp.CommandParser.processCommand(createCmd, manager);
    // Edit event description
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 to 2025-03-01T11:00 with Quarterly results";
    CalendarApp.CommandParser.processCommand(editCmd, manager);
    assertEquals("Quarterly results", manager.events.get(0).description);
  }

  @Test
  public void testEditEventsByStart() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    // Create two events with the same name at different times
    CalendarApp.CommandParser.processCommand(
            "create event Seminar from 2025-03-03T09:00 to 2025-03-03T10:30", manager);
    CalendarApp.CommandParser.processCommand(
            "create event Seminar from 2025-03-04T09:00 to 2025-03-04T10:30", manager);
    // Bulk edit: update events starting from 2025-03-04T00:00
    String editCmd = "edit events description Seminar from 2025-03-04T00:00 with UpdatedSeminar";
    CalendarApp.CommandParser.processCommand(editCmd, manager);
    for(CalendarApp.CalendarEvent event : manager.events) {
      if(event.start.equals(LocalDateTime.parse("2025-03-04T09:00"))){
        assertEquals("UpdatedSeminar", event.description);
      } else {
        assertEquals("", event.description);
      }
    }
  }

  @Test
  public void testEditEventsByName() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    // Create two all-day events with the same name
    CalendarApp.CommandParser.processCommand("create event Holiday on 2025-03-05", manager);
    CalendarApp.CommandParser.processCommand("create event Holiday on 2025-03-06", manager);
    // Bulk edit by event name without 'from' clause
    String editCmd = "edit events location Holiday with Beach";
    CalendarApp.CommandParser.processCommand(editCmd, manager);
    for (CalendarApp.CalendarEvent event : manager.events) {
      if (event.eventName.equals("Holiday")) {
        assertEquals("Beach", event.location);
      }
    }
  }

  @Test(expected = Exception.class)
  public void testMissingFromKeyword() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String command = "create event Meeting 2025-03-01T10:00 to 2025-03-01T11:00";
    CalendarApp.CommandParser.processCommand(command, manager);
  }

  @Test(expected = Exception.class)
  public void testMissingToKeywordInEdit() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CalendarApp.CommandParser.processCommand(createCmd, manager);
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 with NoToClause";
    CalendarApp.CommandParser.processCommand(editCmd, manager);
  }

  @Test
  public void testPrintEventsOn() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CalendarApp.CommandParser.processCommand(createCmd, manager);
    String printCmd = "print events on 2025-03-01";
    // Execute print command (output to console; here we simply ensure no exception is thrown)
    CalendarApp.CommandParser.processCommand(printCmd, manager);
    List<CalendarApp.CalendarEvent> events = manager.getEventsOn(LocalDate.parse("2025-03-01"));
    assertFalse("There should be events on 2025-03-01", events.isEmpty());
  }

  @Test
  public void testShowStatus() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CalendarApp.CommandParser.processCommand(createCmd, manager);
    String statusCmd = "show status on 2025-03-01T10:30";
    // Execute status command (should print "Busy" to console)
    CalendarApp.CommandParser.processCommand(statusCmd, manager);
    // There's no direct return value; the printed output indicates status.
  }

  @Test
  public void testExportGoogleCSV() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CalendarApp.CommandParser.processCommand(createCmd, manager);
    String exportCmd = "export googlecsv test_google.csv";
    CalendarApp.CommandParser.processCommand(exportCmd, manager);
    File file = new File("test_google.csv");
    assertTrue("The exported Google CSV file should exist", file.exists());
    assertTrue("The exported file should not be empty", file.length() > 0);
    file.delete(); // Cleanup after test
  }
}
