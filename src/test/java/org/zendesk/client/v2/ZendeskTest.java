package org.zendesk.client.v2;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import com.squareup.okhttp.mockwebserver.SocketPolicy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zendesk.client.v2.model.Ticket;
import org.zendesk.client.v2.model.Type;

import java.util.Calendar;
import java.util.Date;

import static java.util.Calendar.JANUARY;
import static java.util.Calendar.MILLISECOND;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.fail;

public class ZendeskTest {

    @Rule
    public MockWebServer server = new MockWebServer();

    private Zendesk zendesk;

    @Before
    public void setUp() {
        zendesk = new Zendesk.Builder(server.url("/").toString())
                .setUsername("user@example.com")
                .setToken("qwerty")
                .build();
    }

    @Test
    public void createTicketAsTaskWithDueAtSet() throws Exception {
        // given
        server.enqueue(new MockResponse().setBody("\"ticket\": {}"));

        final Calendar cal = Calendar.getInstance();
        cal.set(2015, JANUARY, 1, 0, 1, 2);
        cal.set(MILLISECOND, 0);
        final Date dueAt = cal.getTime();

        final Ticket ticket = new Ticket();
        ticket.setType(Type.TASK);
        ticket.setDueAt(dueAt);

        // when
        zendesk.createTicket(ticket);

        // then
        final RecordedRequest request = server.takeRequest();
        assertThat(request.getRequestLine(), startsWith("POST /api/v2/tickets.json"));

        final String body = request.getBody().readUtf8();
        assertThat(body, containsString("\"type\":\"task\""));
        assertThat(body, containsString("\"due_at\":\"2015-01-01T00:01:02.000Z\""));
    }

    @Test(timeout = 60 * 1000)
    public void timeoutOnSocketClose() throws Exception {
        // given
        server.enqueue(new MockResponse()
                .setBody("\"ticket\": {}")
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));

        Throwable th = null;

        // when
        try {
            zendesk.getTicket(1L);
            fail("Expecting ZendeskException to be thrown");
        } catch (ZendeskException e) {
            th = e;
        }

        // then
        assertThat(th, instanceOf(ZendeskException.class));
    }
}
