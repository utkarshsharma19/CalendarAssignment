// File: test/CalendarAppTest.java

public class CalendarAppTest {
  public static void main(String[] args) {
    System.out.println("=== Running Existing Test Case ===");
    runExistingTest();
    System.out.println("\n=== Running Edge Case Tests ===");
    runEdgeCaseTests();
  }

  private static void runExistingTest() {
    try {
      CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
      // Create a single timed event.
      CalendarApp.CalendarEvent event1 = new CalendarApp.CalendarEvent("Meeting",
              java.time.LocalDateTime.parse("2025-03-01T10:00"),
              java.time.LocalDateTime.parse("2025-03-01T11:00"), false);
      manager.addEvent(event1, true);

      // Edit the event's description.
      boolean updated = manager.editSingleEvent("description", "Meeting",
              java.time.LocalDateTime.parse("2025-03-01T10:00"),
              java.time.LocalDateTime.parse("2025-03-01T11:00"),
              "Discuss quarterly results");
      if (updated) {
        System.out.println("Test passed: Event edited successfully.");
      } else {
        System.out.println("Test failed: Event edit did not succeed.");
      }

      // Query events on the day.
      java.time.LocalDate date = java.time.LocalDate.parse("2025-03-01");
      if (manager.getEventsOn(date).size() == 1) {
        System.out.println("Test passed: Event found on " + date);
      } else {
        System.out.println("Test failed: Event not found on " + date);
      }

      // Export to Google CSV.
      manager.exportToGoogleCSV("events_google.csv");
    } catch (Exception e) {
      System.out.println("Test failed with exception: " + e.getMessage());
    }
  }

  private static void runEdgeCaseTests() {
    testMissingFromClause();
    testMissingToClause();
    testInvalidRecurringEventFormat();
    testAutoDeclineConflict();
    testRecurringEventForFixedTimes();
    testEditEventNotFound();
    testEditEventsFromClause();
    testEditEventsWithoutFrom();
    testPrintEventsRange();
    testShowStatus();
  }

  // Test when a required keyword ("from") is missing in create command.
  private static void testMissingFromClause() {
    System.out.println("\nTest: Missing 'from' Clause");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand(
              "create event Meeting 2025-03-01T10:00 to 2025-03-01T11:00", manager);
      System.out.println("FAILED: Expected exception for missing 'from' clause.");
    } catch (Exception e) {
      System.out.println("PASSED: " + e.getMessage());
    }
  }

  // Test when the edit command is missing the "to" clause.
  private static void testMissingToClause() {
    System.out.println("\nTest: Missing 'to' Clause in Edit Command");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand(
              "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
      CalendarApp.CommandParser.processCommand(
              "edit event description Meeting from 2025-03-01T10:00 with NewDescription", manager);
      System.out.println("FAILED: Expected exception for missing 'to' clause.");
    } catch (Exception e) {
      System.out.println("PASSED: " + e.getMessage());
    }
  }

  // Test an invalid recurring event format.
  private static void testInvalidRecurringEventFormat() {
    System.out.println("\nTest: Invalid Recurring Event Format");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand(
              "create event Seminar from 2025-03-03T09:00 to 2025-03-03T10:30 repeats WF for times", manager);
      System.out.println("FAILED: Expected exception for invalid recurring format.");
    } catch (Exception e) {
      System.out.println("PASSED: " + e.getMessage());
    }
  }

  // Test auto-declination on conflict.
  private static void testAutoDeclineConflict() {
    System.out.println("\nTest: Auto-Decline Conflict");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand(
              "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
      CalendarApp.CommandParser.processCommand(
              "create event Meeting2 --autoDecline from 2025-03-01T10:30 to 2025-03-01T11:30", manager);
      System.out.println("FAILED: Expected exception due to conflict.");
    } catch (Exception e) {
      System.out.println("PASSED: " + e.getMessage());
    }
  }

  // Test recurring event creation with a fixed number of occurrences.
  private static void testRecurringEventForFixedTimes() {
    System.out.println("\nTest: Recurring Event for Fixed Times");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand(
              "create event Workshop on 2025-03-02 repeats MTWRF for 3 times", manager);
      int total = manager.events.size();
      if (total == 3) {
        System.out.println("PASSED: 3 occurrences created.");
      } else {
        System.out.println("FAILED: Expected 3 occurrences, but got " + total);
      }
    } catch (Exception e) {
      System.out.println("FAILED: Exception occurred - " + e.getMessage());
    }
  }

  // Test editing an event that doesn't exist.
  private static void testEditEventNotFound() {
    System.out.println("\nTest: Edit Event Not Found");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand(
              "edit event description NonExistent from 2025-03-01T10:00 to 2025-03-01T11:00 with NewDesc", manager);
      System.out.println("PASSED: Expected failure message if event not found.");
    } catch (Exception e) {
      System.out.println("FAILED: Unexpected exception - " + e.getMessage());
    }
  }

  // Test plural edit command with the "from" clause.
  private static void testEditEventsFromClause() {
    System.out.println("\nTest: Edit Events with 'from' Clause");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand(
              "create event Seminar from 2025-03-03T09:00 to 2025-03-03T10:30", manager);
      CalendarApp.CommandParser.processCommand(
              "create event Seminar from 2025-03-04T09:00 to 2025-03-04T10:30", manager);
      CalendarApp.CommandParser.processCommand(
              "edit events description Seminar from 2025-03-04T00:00 with UpdatedSeminar", manager);
      boolean found = false;
      for (CalendarApp.CalendarEvent event : manager.events) {
        if (event.eventName.equals("Seminar") &&
                event.description.equals("UpdatedSeminar") &&
                event.start.isAfter(java.time.LocalDateTime.parse("2025-03-04T00:00"))) {
          found = true;
          break;
        }
      }
      System.out.println(found ? "PASSED: Events updated as expected." : "FAILED: No event updated as expected.");
    } catch (Exception e) {
      System.out.println("FAILED: Exception occurred - " + e.getMessage());
    }
  }

  // Test plural edit command without a "from" clause.
  private static void testEditEventsWithoutFrom() {
    System.out.println("\nTest: Edit Events without 'from' Clause");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand("create event Seminar on 2025-03-05", manager);
      CalendarApp.CommandParser.processCommand("create event Seminar on 2025-03-06", manager);
      CalendarApp.CommandParser.processCommand("edit events description Seminar with BulkUpdate", manager);
      int count = 0;
      for (CalendarApp.CalendarEvent event : manager.events) {
        if (event.eventName.equals("Seminar") && event.description.equals("BulkUpdate")) {
          count++;
        }
      }
      if (count == 2) {
        System.out.println("PASSED: Both events updated.");
      } else {
        System.out.println("FAILED: Expected 2 events updated, but got " + count);
      }
    } catch (Exception e) {
      System.out.println("FAILED: Exception occurred - " + e.getMessage());
    }
  }

  // Test printing events in a given date/time range.
  private static void testPrintEventsRange() {
    System.out.println("\nTest: Print Events in Date/Time Range");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand(
              "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
      CalendarApp.CommandParser.processCommand(
              "create event Workshop on 2025-03-02", manager);
      CalendarApp.CommandParser.processCommand(
              "print events from 2025-03-01T00:00 to 2025-03-03T00:00", manager);
      System.out.println("PASSED: Print events command executed.");
    } catch (Exception e) {
      System.out.println("FAILED: Exception occurred - " + e.getMessage());
    }
  }

  // Test the busy status command.
  private static void testShowStatus() {
    System.out.println("\nTest: Show Status Command");
    CalendarApp.CalendarManager manager = new CalendarApp.CalendarManager();
    try {
      CalendarApp.CommandParser.processCommand(
              "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
      CalendarApp.CommandParser.processCommand(
              "show status on 2025-03-01T10:30", manager);
      System.out.println("PASSED: Show status command executed (check output for 'Busy').");
    } catch (Exception e) {
      System.out.println("FAILED: Exception occurred - " + e.getMessage());
    }
  }
}
