package com.mycorp;

import com.mycorp.support.Ticket;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

public class ZendeskTest {

    private static final String URL ="http://localhost:8082/env" ;

    public Builder builder;

    @InjectMocks
    private ZendeskService zendeskService;


    PodamFactory factory = new PodamFactoryImpl();

    Ticket ticket = factory.manufacturePojoWithFullData(Ticket.class);

    @Before public void setUp() throws Exception {
    }

    @After public void tearDown() throws Exception {
        builder = new Builder(URL);
    }

    @Test public void createTicket() {

        Assert.assertNotNull(ticket);
    }

    @Test public void handle() {
    }

    @Test public void isClosed() {
    }

    @Test public void close() {
    }
}
