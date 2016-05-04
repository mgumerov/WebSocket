package ru.slicer;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.websocket.Session;
import java.io.IOException;

@ApplicationScoped
public class LoginRequestHandler implements RequestHandler {
    @Override
    public void processRequest(final String sequenceId, final JsonObject data, final Session session) throws ExpectedException {
        throw new ExpectedException("Not implemented", "customer.NotImplemented");
        //session.getBasicRemote().sendText("Authenticated");
    }
}
