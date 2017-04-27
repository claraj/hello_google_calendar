package com.company;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.services.calendar.model.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Clara on 4/27/17, with lots of help from the sample provided (URL below).
 * Does Google Calendar authentication,
 * Creates a new Calendar with a particular name for the user, if that calendar does not already exist
 * Adds new events to that named Google Calendar. (One user can have multiple calendars).
 * See sample application and docs for other Calendar operations
 * https://github.com/google/google-api-java-client-samples/blob/master/calendar-cmdline-sample/src/main/java/com/google/api/services/samples/calendar/cmdline/View.java
 * Make sure you add a file with the JSON credentials for your own application in. Replace the src/main/resources/client_secret.json with your own copy
 */
public class GoogleCalendar {

    private static final String APPLICATION_NAME = "HelloGoogleCalendar";   // TODO Replace with your app name

    private static final String CALENDAR_NAME = "Events Created By Java";  // TODO Replace with an appropriate calendar name for your app

    /**
     * Directory to store user credentials.
     */
    private static final java.io.File DATA_STORE_DIR =
            new java.io.File(System.getProperty("user.home"), ".store/calendar_sample");

    /**
     * Global instance of the {DataStoreFactory}. The best practice is to make it a single
     * globally shared instance across your application.
     */
    private static FileDataStoreFactory dataStoreFactory;

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static com.google.api.services.calendar.Calendar client;

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private static Credential authorize() throws Exception {
        // load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(GoogleCalendar.class.getResourceAsStream("/client_secrets.json")));
        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println(
                    "Enter Client ID and Secret from https://code.google.com/apis/console/?api=calendar "
                            + "into calendar-cmdline-sample/src/main/resources/client_secrets.json");
            System.exit(1);
        }
        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory)
                .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }


    public static void addEvent(String eventName) {

        try {
            // initialize the transport
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // initialize the data store factory
            dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

            // authorization
            Credential credential = authorize();

            // set up global Calendar instance
            client = new com.google.api.services.calendar.Calendar.Builder(
                    httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();

            // Get the calendar the app will use. Will create a Calendar if it does not exist
            Calendar appCalendar = getAppCalendar();

            // And add the event to this Calendar
            add(eventName, appCalendar);

        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }



    private static Calendar getAppCalendar() throws IOException {

        // Get list of Calendars...
        CalendarList cs = client.calendarList().list().execute();

        // Does a Calendar exist with the name "Events Created By Java" ?
        for (CalendarListEntry calendarListEntry : cs.getItems()) {
            // Does this calendar have the same summary name.
            if (calendarListEntry.getSummary().equals(CALENDAR_NAME)) {
                // Calendar exists. Get this Calendar's unique ID
                String calendarId = calendarListEntry.getId();
                // And fetch the Calendar object and return it. (There must be a way of getting a Calendar from the CalendarList?? I must be missing something in the docs.)
                return client.calendars().get(calendarId).execute();
            }
        }


        // A Calendar with the summary name does not exist. Create a new calendar and return it.
        Calendar newAppCalendar = addCalendar();
        return newAppCalendar;

    }


    private static Calendar addCalendar() throws IOException {
        Calendar entry = new Calendar();
        entry.setSummary(CALENDAR_NAME);
        Calendar result = client.calendars().insert(entry).execute();
        return result;
    }

    private static void add(String eventName, Calendar calendar) throws IOException {
        Event event = newEvent(eventName);
        System.out.println("Event to add: " + event);
        Event result = client.events().insert(calendar.getId(), event).execute();
        System.out.println("Result of adding event:" + result);
    }


    // TODO your code will set the start and end date and time.
    private static Event newEvent(String eventName) {
        Event event = new Event();
        event.setSummary(eventName);
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 3600000);
        DateTime start = new DateTime(startDate, TimeZone.getTimeZone("UTC"));
        event.setStart(new EventDateTime().setDateTime(start));
        DateTime end = new DateTime(endDate, TimeZone.getTimeZone("UTC"));
        event.setEnd(new EventDateTime().setDateTime(end));
        return event;
    }

}