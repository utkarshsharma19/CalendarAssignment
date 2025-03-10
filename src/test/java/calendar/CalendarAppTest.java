package calendar;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
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

  @Test
  public void testInvalidCommand() {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand("invalid command", manager);
      fail("Expected exception for invalid command");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }
  }

  @Test
  public void testMainHeadlessMode() throws Exception {
    // Create a temporary command file with a couple of commands and "exit"
    File temp = File.createTempFile("commands", ".txt");
    try (PrintWriter writer = new PrintWriter(temp)) {
      writer.println("create event Test on 2025-03-05");
      writer.println("exit");
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    // Call main with headless mode arguments.
    CalendarApp.main(new String[]{"--mode", "headless", temp.getAbsolutePath()});
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue(output.contains("All-day event created:"));
    temp.delete();
  }

  @Test
  public void testExportCalCommand() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CalendarApp.CommandParser.processCommand(createCmd, manager);
    String exportCmd = "export cal test_export.csv";
    CalendarApp.CommandParser.processCommand(exportCmd, manager);
    File f = new File("test_export.csv");
    assertTrue("Exported CSV file should exist", f.exists());
    assertTrue("Exported file should not be empty", f.length() > 0);
    f.delete(); // Cleanup
  }

  @Test
  public void testShowStatusOutput() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CalendarApp.CommandParser.processCommand(createCmd, manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CalendarApp.CommandParser.processCommand("show status on 2025-03-01T10:30", manager);
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Expected status output to contain 'Busy'", output.contains("Busy"));
  }

  @Test
  public void testEditEventsWithoutFromClause() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    CalendarApp.CommandParser.processCommand("create event Holiday on 2025-03-05", manager);
    CalendarApp.CommandParser.processCommand("create event Holiday on 2025-03-06", manager);
    CalendarApp.CommandParser.processCommand("edit events location Holiday with Beach", manager);
    for (CalendarApp.CalendarEvent event : manager.events) {
      if (event.eventName.equals("Holiday")) {
        assertEquals("Beach", event.location);
      }
    }
  }

  @Test
  public void testPrintEventsRange() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    CalendarApp.CommandParser.processCommand("create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    CalendarApp.CommandParser.processCommand("create event Workshop on 2025-03-02", manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CalendarApp.CommandParser.processCommand("print events from 2025-03-01T00:00 to 2025-03-03T00:00", manager);
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue(output.contains("Meeting"));
    assertTrue(output.contains("Workshop"));
  }

  @Test
  public void testMainInsufficientArgs() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    // Call main with no arguments.
    CalendarApp.main(new String[]{});
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Should print usage instructions",
            output.contains("Usage: --mode interactive OR --mode headless <commandFile.txt>"));
  }

  // Test that if an invalid mode is provided, the proper message is printed.
  @Test
  public void testMainInvalidMode() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CalendarApp.main(new String[]{"--mode", "foobar"});
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Should indicate invalid mode",
            output.contains("Invalid mode. Use interactive or headless."));
  }

  // Test interactive mode by simulating user input.
  @Test
  public void testMainInteractiveMode() throws Exception {
    // Simulate input "exit" so that interactive mode terminates.
    String simulatedInput = "exit\n";
    ByteArrayInputStream bais = new ByteArrayInputStream(simulatedInput.getBytes());
    InputStream oldIn = System.in;
    System.setIn(bais);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));

    // Call the interactive mode method directly.
    CalendarApp.runInteractiveMode(new CalendarApp.CalendarManager());

    System.setOut(oldOut);
    System.setIn(oldIn);
    String output = baos.toString();
    assertTrue("Interactive mode should print exiting message",
            output.contains("Exiting."));
  }

  // Test headless mode by creating a temporary command file.
  @Test
  public void testMainHeadlessModeUsingBaos() throws Exception {
    File temp = File.createTempFile("commands", ".txt");
    try (PrintWriter writer = new PrintWriter(temp)) {
      writer.println("create event Test on 2025-03-05");
      writer.println("exit");
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));

    CalendarApp.main(new String[]{"--mode", "headless", temp.getAbsolutePath()});

    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Headless mode should create an all-day event",
            output.contains("All-day event created:"));
    temp.delete();
  }

  @Test
  public void testConflictsWithOverlapping() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 10, 30);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 11, 30);

    CalendarApp.CalendarEvent event1 = new CalendarApp.CalendarEvent("Event1", start1, end1, false);
    CalendarApp.CalendarEvent event2 = new CalendarApp.CalendarEvent("Event2", start2, end2, false);

    assertTrue("Events that overlap should conflict", event1.conflictsWith(event2));
    assertTrue("Events that overlap should conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testConflictsWithNonOverlapping() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 12, 0);

    CalendarApp.CalendarEvent event1 = new CalendarApp.CalendarEvent("Event1", start1, end1, false);
    CalendarApp.CalendarEvent event2 = new CalendarApp.CalendarEvent("Event2", start2, end2, false);

    assertFalse("Events that do not overlap should not conflict", event1.conflictsWith(event2));
    assertFalse("Events that do not overlap should not conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testConflictsWithBoundaryTouching() {
    // When one event ends exactly when the other begins, they should not conflict.
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 9, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 11, 0);

    CalendarApp.CalendarEvent event1 = new CalendarApp.CalendarEvent("Event1", start1, end1, false);
    CalendarApp.CalendarEvent event2 = new CalendarApp.CalendarEvent("Event2", start2, end2, false);

    assertFalse("Events that touch boundaries should not conflict", event1.conflictsWith(event2));
    assertFalse("Events that touch boundaries should not conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testToString_AllDayWithDescriptionAndLocation() {
    LocalDateTime start = LocalDate.of(2025, 3, 1).atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    CalendarApp.CalendarEvent event = new CalendarApp.CalendarEvent("Holiday", start, end, true);
    event.description = "Vacation";
    event.location = "Beach";
    event.isPublic = false;
    String expected = "Holiday (All Day on 2025-03-01), Description: Vacation, Location: Beach, Private";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_TimedEventWithoutExtras() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 1, 11, 0);
    CalendarApp.CalendarEvent event = new CalendarApp.CalendarEvent("Meeting", start, end, false);
    String expected = "Meeting from 2025-03-01 10:00 to 2025-03-01 11:00, Public";
    assertEquals(expected, event.toString());
  }

  @Test(expected = Exception.class)
  public void testAddEventWithConflictAutoDecline() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    // Create an event.
    CalendarApp.CalendarEvent event1 = new CalendarApp.CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);
    // Create a conflicting event; should throw exception because autoDecline is true.
    CalendarApp.CalendarEvent event2 = new CalendarApp.CalendarEvent("Meeting2",
            LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(event2, true);
  }

  @Test
  public void testAddEventWithConflictWarning() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    // Create an event.
    CalendarApp.CalendarEvent event1 = new CalendarApp.CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);

    // Capture the output.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));

    // Create a conflicting event; autoDecline is false, so it should print a warning.
    CalendarApp.CalendarEvent event2 = new CalendarApp.CalendarEvent("Meeting2",
            LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(event2, false);

    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue("Should print warning about conflict", output.contains("Warning: Event conflicts with Meeting"));
  }

  @Test
  public void testEventsAreSortedAfterAdd() throws Exception {
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();

    // Create events out of order.
    CalendarApp.CalendarEvent event1 = new CalendarApp.CalendarEvent(
            "Event1",
            LocalDateTime.of(2025, 3, 1, 12, 0),
            LocalDateTime.of(2025, 3, 1, 13, 0),
            false);

    CalendarApp.CalendarEvent event2 = new CalendarApp.CalendarEvent(
            "Event2",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0),
            false);

    CalendarApp.CalendarEvent event3 = new CalendarApp.CalendarEvent(
            "Event3",
            LocalDateTime.of(2025, 3, 1, 14, 0),
            LocalDateTime.of(2025, 3, 1, 15, 0),
            false);

    // Add events in unsorted order.
    // We use addEvent so that after each insertion, the events list is re-sorted.
    manager.addEvent(event1, false);
    manager.addEvent(event2, false);
    manager.addEvent(event3, false);

    // Now the sorted order should be: event2, event1, event3.
    assertEquals("First event should be Event2", "Event2", manager.events.get(0).eventName);
    assertEquals("Second event should be Event1", "Event1", manager.events.get(1).eventName);
    assertEquals("Third event should be Event3", "Event3", manager.events.get(2).eventName);
  }
}
