package ru.slicer;

import javax.json.JsonObject;
import javax.websocket.Session;

public interface RequestHandler {
    void processRequest(String sequence_id, JsonObject data, Session session);
}
