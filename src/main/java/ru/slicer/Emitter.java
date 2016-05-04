package ru.slicer;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.websocket.Session;
import java.io.IOException;

public class Emitter {
    public void sendData(final Session session, final String sequenceId, final String messageType,
                         final JsonObject data) {
        try (final JsonWriter jsonWriter = Json.createWriter(session.getBasicRemote().getSendWriter())) {
            final JsonObject datagram = Json.createObjectBuilder()
                    .add("type", messageType)
                    .add("sequence_id", sequenceId)
                    .add("data", data)
                    .build();
            jsonWriter.writeObject(datagram);
        } catch (IOException e) {
            System.out.println(messageType);
            e.printStackTrace();
        }
    }

    public void sendErrorResponse(final Session session, final String message,
                                  final String sequenceId, final String description, String errorCode) {
        try (final JsonWriter jsonWriter = Json.createWriter(session.getBasicRemote().getSendWriter())) {
            final JsonObject response = Json.createObjectBuilder()
                    .add("type", "CUSTOMER_ERROR")
                    .add("sequence_id", sequenceId)
                    .add("data",
                            Json.createObjectBuilder()
                                    .add("error_description", description)
                                    .add("error_code", errorCode)
                                    .build())
                    .build();
            jsonWriter.writeObject(response);
        } catch (IOException e) {
            System.out.println(message);
            e.printStackTrace();
        }
    }
}
