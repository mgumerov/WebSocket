package ru.slicer;

import java.io.IOException;
import java.io.StringReader;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@javax.enterprise.context.ApplicationScoped
@ServerEndpoint("/echo")
public class EndPoint {

    //Now here is another odd problem, endpoint instances get their dependency-injection
    //wired, but resource-injection not wired. So we will have another middleman bean
    //be responsible for mapping.
    //@Resource
    //private String requestMapping;
    @Inject
    private RequestMapping requestMapping;

    @OnMessage
    public void onMessage(final String message, final Session session) {
        final JsonObject jsonObject = Json.createReader(new StringReader(message)).readObject();
        System.out.println(jsonObject);

        final RequestHandler requestHandler = requestMapping.findHandler(jsonObject.getString("type"));
        if (requestHandler == null) {
            //TODO return ERROR
            try (final JsonWriter jsonWriter = Json.createWriter(session.getBasicRemote().getSendWriter())) {
                jsonWriter.writeObject(jsonObject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println(requestHandler);
            requestHandler.processRequest(jsonObject.getString("sequence_id"),
                    jsonObject.getJsonObject("data"), session);
        }
    }
}
