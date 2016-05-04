package ru.slicer;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.websocket.Session;
import java.io.IOException;

@ApplicationScoped
public class LoginRequestHandler implements RequestHandler {
    @Override
    public void processRequest(final String sequenceId, final JsonObject data, final Session session) {
        try {
            session.getBasicRemote().sendText("Authenticated");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
